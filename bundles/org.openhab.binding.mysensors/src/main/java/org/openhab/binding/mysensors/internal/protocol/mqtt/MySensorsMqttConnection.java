/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mysensors.internal.protocol.mqtt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.ParseException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.internal.event.MySensorsEventRegister;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGatewayConfig;
import org.openhab.binding.mysensors.internal.protocol.MySensorsAbstractConnection;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessage;
import org.openhab.core.io.transport.mqtt.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Implements the MQTT connection to a gateway of the MySensors network.
 *
 * @author Tim Oberföll - Initial contribution
 * @author Sean McGuire - Redesign
 *
 */
@NonNullByDefault
public class MySensorsMqttConnection extends MySensorsAbstractConnection implements MqttConnectionObserver {

    private final MySensorsMqttSubscriber myMqttSub;

    @Nullable
    private MqttBrokerConnection connection;

    private PipedOutputStream out = new PipedOutputStream();

    private PipedInputStream in = new PipedInputStream();

    private final MySensorsMqttPublishCallback myMqttPublishCallback = new MySensorsMqttPublishCallback();

    public MySensorsMqttConnection(MySensorsGatewayConfig myGatewayConfig, MySensorsEventRegister myEventRegister) {
        super(myGatewayConfig, myEventRegister);
        @Nullable
        String topic = myGatewayConfig.getTopicSubscribe();
        if (topic == null) {
            throw new IllegalArgumentException("Tried to create MQTT Connection with null subscript topic");
        }
        myMqttSub = new MySensorsMqttSubscriber(topic);
        try {
            in.connect(out);
        } catch (IOException e1) {
            logger.error("Exception thrown while trying to connect input stream for MQTT messages! {}", e1.toString());
        }
    }

    /**
     * Establishes a link to the broker connection
     */
    @Override
    protected boolean establishConnection() {
        if (MySensorsMqttService.getMqttService() == null) {
            BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
            final ServiceReference sr = bc.getServiceReference(MqttService.class.getName());
            if (sr != null) {
                final MqttService mqtt = (MqttService) bc.getService(sr);
                if (mqtt != null) {
                    new MySensorsMqttService().setMqttService(mqtt);
                }
            }
        }
        // ## End workaround

        if (MySensorsMqttService.getMqttService() == null) {
            logger.error("MqttService is null!");
            return false;
        }

        @Nullable
        String brokerName = myGatewayConfig.getBrokerName();

        if (brokerName != null)
            connection = MySensorsMqttService.getMqttService().getBrokerConnection(brokerName);

        if (connection == null) {
            logger.error("No connection to broker: {}", brokerName);
            return false;
        }

        mysConReader = new MySensorsReader(in);
        mysConWriter = new MySensorsMqttWriter(new OutputStream() {

            @Override
            public void write(int b) {
            }
        });

        connection.removeConnectionObserver(this);
        connection.addConnectionObserver(this);

        connectionStateChanged(connection.connectionState(), null);

        connection.subscribe(myMqttSub.getTopic(), myMqttSub);
        logger.debug("Adding consumer for topic: {}", myMqttSub.getTopic());

        return startReaderWriterThread(mysConReader, mysConWriter);
    }

    /**
     * Removes the consumer from the broker connection
     */
    @Override
    protected void stopConnection() {
        if (connection != null) {
            connection.unsubscribe(myMqttSub.getTopic(), myMqttSub);
            connection.removeConnectionObserver(this);

            if (mysConWriter != null) {
                mysConWriter.stopWriting();
                mysConWriter = null;
            }

            if (mysConReader != null) {
                mysConReader.stopReader();
                mysConReader = null;
            }
        } else {
            logger.warn("Tried to stop null MQTT connection");
        }
    }

    /**
     * Receives messages from MQTT transport, translates them and passes them on to
     * the MySensors abstract connection
     *
     * @author Sean McGuire
     * @author Tim Oberföll
     */
    @NonNullByDefault
    public class MySensorsMqttSubscriber implements MqttMessageSubscriber {

        private String topicSubscribe;

        public MySensorsMqttSubscriber(String topicSubscribe) {
            this.topicSubscribe = topicSubscribe;
            setTopic(topicSubscribe);
        }

        @Override
        public void processMessage(String topic, byte[] payload) {
            @Nullable
            String subscribeTopic = myGatewayConfig.getTopicSubscribe();

            String payloadString = new String(payload);
            logger.debug("MQTT message received. Topic: {}, Message: {}", topic, payloadString);
            if (subscribeTopic != null && topic.indexOf(subscribeTopic) == 0) {
                String messageTopicPart = topic.replace(myGatewayConfig.getTopicSubscribe() + "/", "");
                logger.debug("Message topic part: {}", messageTopicPart);
                MySensorsMessage incomingMessage = new MySensorsMessage();
                try {
                    incomingMessage = MySensorsMessage.parseMQTT(messageTopicPart, payloadString);
                    logger.debug("Converted MQTT message to MySensors Serial format. Sending on to bridge: {}",
                            MySensorsMessage.generateAPIString(incomingMessage).trim());
                    try {
                        out.write(MySensorsMessage.generateAPIString(incomingMessage).getBytes());
                    } catch (IOException ioe) {
                        logger.error("IO Exception trying to write incoming message", ioe);
                    }
                } catch (ParseException pe) {
                    logger.debug("Unable to send message to bridge: {}", pe.toString());
                }
            }
        }

        /**
         * Get the topic that should be listened to
         */
        public String getTopic() {
            return topicSubscribe;
        }

        /**
         * Set the topic that should be listen to
         *
         * @param topicSubscribe topic that should be listened to
         */
        public void setTopic(String topicSubscribe) {
            if (!topicSubscribe.endsWith("/")) {
                topicSubscribe += "/";
            }
            this.topicSubscribe = topicSubscribe + "+/+/+/+/+";
        }
    }

    /**
     *
     * @author Sean McGuire
     * @author Tim Oberföll
     *
     */
    @NonNullByDefault
    protected class MySensorsMqttWriter extends MySensorsWriter {
        @Nullable
        private MqttBrokerConnection conn;

        public MySensorsMqttWriter(OutputStream outStream) {
            super(outStream);
            @Nullable
            String brokerName = myGatewayConfig.getBrokerName();
            assert MySensorsMqttService.getMqttService() != null;
            if (brokerName != null)
                conn = MySensorsMqttService.getMqttService().getBrokerConnection(brokerName);
        }

        @Override
        protected void sendMessage(@Nullable String msg) {
            if (msg == null) {
                logger.warn("Tried to send null msg");
                return;
            }
            logger.debug("Sending MQTT Message: Topic: {}, Message: {}", myGatewayConfig.getTopicPublish(), msg.trim());

            try {
                MySensorsMessage msgOut = MySensorsMessage.parse(msg);
                String newTopic = myGatewayConfig.getTopicPublish() + "/" + MySensorsMessage.generateMQTTString(msgOut);
                assert conn != null;
                conn.publish(newTopic, msgOut.getMsg().getBytes(), 0, false).whenComplete((m, t) -> {
                    if (t == null) {
                        myMqttPublishCallback.onSuccess(newTopic);
                    } else {
                        myMqttPublishCallback.onFailure(newTopic, t);
                    }
                });
            } catch (ParseException e) {
                logger.error("Unable to convert String to MySensorsMessage!", e);
            }
        }
    }

    @Override
    public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error) {
        if (state == MqttConnectionState.CONNECTED) {
            logger.debug("Connected to MQTT broker!");
        } else {
            if (error == null) {
                logger.error("MQTT connection offline - Reason unknown");
            } else {
                logger.error("MQTT connection offline - ", error);
            }
        }
    }

    /**
     *
     * Callback for published MQTT messages
     * We're not using the callbacks yet.
     *
     * @author Tim Oberföll
     *
     */
    public class MySensorsMqttPublishCallback implements MqttActionCallback {

        @Override
        public void onSuccess(String topic) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onFailure(String topic, Throwable error) {
            logger.error("Error sending MQTT message to broker: {}.", myGatewayConfig.getBrokerName(), error);
        }
    }
}
