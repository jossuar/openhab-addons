/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.bdrthermea.internal.handler;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.bdrthermea.internal.config.BdrThermeaBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link BdrThermeaBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class BdrThermeaBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(BdrThermeaBridgeHandler.class);

    private SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    private HttpClient httpClient = new HttpClient(sslContextFactory);

    @Nullable
    private String access_token;
    @Nullable
    private Integer access_token_expires_in;
    @Nullable
    private Integer access_token_expires_on;
    @Nullable
    private String id_token;
    @Nullable
    private Integer id_token_expires_in;
    @Nullable
    private Integer id_token_expires_not_before;
    @Nullable
    private String profile_info;
    @Nullable
    private String refresh_token;
    @Nullable
    private Integer refresh_token_expires_in;
    @Nullable
    private String resource;
    @Nullable
    private String scope;
    @Nullable
    private String token_type;

    public BdrThermeaBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        // Check configuration
        BdrThermeaBridgeConfiguration configuration = getConfigAs(BdrThermeaBridgeConfiguration.class);
        String brand = configuration.getBrand();
        String email = configuration.getEmail();
        if (email == null || email.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Set the email of your cloud account.");
            return;
        }
        String password = configuration.getPassword();
        if (password == null || password.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Set the password of your cloud account.");
            return;
        }

        // Example for background initialization:
        scheduler.execute(() -> {
            // Authenticate on the cloud
            try {
                httpClient.setRequestBufferSize(16 * 1024);

                String token = getToken(brand, email, password);
                logger.debug("token: {}", token);

                updateStatus(ThingStatus.ONLINE);
            } catch (IllegalArgumentException ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
            }
        });
    }

    private String requestNewToken(String brand, HashMap<String, String> params) {
        Request request = httpClient.newRequest("https://" + brand
                + "login.bdrthermea.net/bdrb2cprod.onmicrosoft.com/oauth2/v2.0/token?p=B2C_1A_RPSignUpSignInNewRoomV3.1");
        request.method(HttpMethod.POST);

        Fields fields = new Fields();
        for (String key : params.keySet()) {
            fields.put(key, params.get(key));
        }
        request.content(new FormContentProvider(fields));

        ContentResponse response;
        try {
            response = request.send();
        } catch (Exception e) {
            logger.debug("Error occured while authenticating", e);
            throw new IllegalArgumentException("Error occured while authenticating");
        }

        String responseJson = response.getContentAsString();

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(responseJson, JsonObject.class);
        if (jsonObject != null) {
            access_token = jsonObject.get("access_token").getAsString();
            access_token_expires_in = jsonObject.get("expires_in").getAsInt();
            access_token_expires_on = jsonObject.get("expires_on").getAsInt();
            id_token = jsonObject.get("id_token").getAsString();
            id_token_expires_in = jsonObject.get("id_token_expires_in").getAsInt();
            id_token_expires_not_before = jsonObject.get("not_before").getAsInt();
            profile_info = jsonObject.get("profile_info").getAsString();
            refresh_token = jsonObject.get("refresh_token").getAsString();
            refresh_token_expires_in = jsonObject.get("refresh_token_expires_in").getAsInt();
            resource = jsonObject.get("resource").getAsString();
            scope = jsonObject.get("scope").getAsString();
            token_type = jsonObject.get("token_type").getAsString();
        }

        return response.toString();
    }

    private String getToken(String brand, String email, String password) throws IllegalArgumentException {
        // Prepare data
        Random random = new Random();
        byte[] state = new byte[16];
        random.nextBytes(state);

        byte[] codeChallengeArray = new byte[64];
        random.nextBytes(codeChallengeArray);
        String codeChallenge = Base64.getEncoder().encodeToString(codeChallengeArray);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("SHA 256 is not supported on the current platform");
        }
        byte[] codeChallengeSha256 = digest.digest(codeChallenge.getBytes());

        // Building a request with a timeout
        Request request = httpClient.newRequest(
                "https://" + brand + "login.bdrthermea.net/bdrb2cprod.onmicrosoft.com/oauth2/v2.0/authorize");
        request.param("response_type", "code");
        request.param("client_id", "6ce007c6-0628-419e-88f4-bee2e6418eec");
        request.param("redirect_uri", "com.b2c." + brand + "app://login-callback");
        request.param("scope", "openid https://bdrb2cprod.onmicrosoft.com/iotdevice/user_impersonation offline_access");
        request.param("state", Base64.getEncoder().encodeToString(state));
        request.param("code_challenge", Base64.getEncoder().encodeToString(codeChallengeSha256));
        request.param("code_challenge_method", "S256");
        request.param("p", "B2C_1A_RPSignUpSignInNewRoomV3.1");
        request.param("brand", brand);
        request.param("lang", "en");
        request.param("nonce", "defaultNonce");
        request.param("prompt", "login");
        request.param("signUp", "False");

        ContentResponse response;
        try {
            httpClient.start();
            response = request.send();
        } catch (Exception e) {
            logger.debug("Error occured while authenticating", e);
            throw new IllegalArgumentException("Error occured while authenticating");
        }
        int status = response.getStatus();
        if (status != 200) {
            logger.debug("OAuth 1st request status: {}", status);
            throw new IllegalArgumentException("Error occured while authenticating");
        }

        HttpFields responseHeaders = response.getHeaders();
        String requestId = responseHeaders.get("x-request-id");
        String statePropertiesJson = "{\"TID\":\"" + requestId + "\"}";

        CookieStore cookieStore = httpClient.getCookieStore();
        // URI a = URI.create("http://" + brand + "login.bdrthermea.net");
        List<HttpCookie> cookies = cookieStore.getCookies();

        String csrfToken = null;
        for (HttpCookie c : cookies) {
            if ("x-ms-cpim-csrf".equals(c.getName())) {
                csrfToken = c.getValue();
            }
        }

        request = httpClient.newRequest("https://" + brand
                + "login.bdrthermea.net/bdrb2cprod.onmicrosoft.com/B2C_1A_RPSignUpSignInNewRoomv3.1/SelfAsserted");
        request.method(HttpMethod.POST);

        request.header("x-csrf-token", csrfToken);

        request.param("tx", "StateProperties=" + Base64.getEncoder().encodeToString(statePropertiesJson.getBytes()));
        request.param("p", "B2C_1A_RPSignUpSignInNewRoomv3.1");

        MultiPartContentProvider multiPart = new MultiPartContentProvider();
        multiPart.addFieldPart("request_type", new StringContentProvider("RESPONSE"), null);
        multiPart.addFieldPart("signInName", new StringContentProvider(email), null);
        multiPart.addFieldPart("password", new StringContentProvider(password), null);
        multiPart.close();
        request.content(multiPart);

        try {
            response = request.send();
        } catch (Exception e) {
            logger.debug("Error occured while authenticating", e);
            throw new IllegalArgumentException("Error occured while authenticating");
        }

        String responseJson = response.getContentAsString();

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(responseJson, JsonObject.class);
        if (jsonObject != null && !"200".equals(jsonObject.get("status").getAsString())) {
            logger.debug("Error occured while authenticating");
            throw new IllegalArgumentException("Error occured while authenticating");
        }

        request = httpClient.newRequest("https://" + brand
                + "login.bdrthermea.net/bdrb2cprod.onmicrosoft.com/B2C_1A_RPSignUpSignInNewRoomv3.1/api/CombinedSigninAndSignup/confirmed");
        request.method(HttpMethod.GET);

        request.followRedirects(false);

        request.param("rememberMe", "false");
        request.param("csrf_token", csrfToken);
        request.param("tx", "StateProperties=" + Base64.getEncoder().encodeToString(statePropertiesJson.getBytes()));
        request.param("p", "B2C_1A_RPSignUpSignInNewRoomv3.1");

        try {
            response = request.send();
        } catch (Exception e) {
            logger.debug("Error occured while authenticating", e);
            throw new IllegalArgumentException("Error occured while authenticating");
        }

        String callback_url = response.getHeaders().get("location");
        HttpURI uri = new HttpURI(callback_url);

        MultiMap<String> parameters = new MultiMap<String>();
        uri.decodeQueryTo(parameters);
        String authorizationCode = parameters.getString("code");

        HashMap<String, String> params = new HashMap<String, String>();

        /*
         * grant_params = {
         * "grant_type": "refresh_token",
         * "refresh_token": token["refresh_token"],
         * "client_id": "6ce007c6-0628-419e-88f4-bee2e6418eec",
         * }
         */
        params.put("grant_type", "authorization_code");
        params.put("code", authorizationCode);
        params.put("redirect_uri", "com.b2c.remehaapp://login-callback");
        params.put("code_verifier", codeChallenge);
        params.put("client_id", "6ce007c6-0628-419e-88f4-bee2e6418eec");
        String token = requestNewToken(brand, params);

        return token;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand(), channelUID: {}, command: {}", channelUID, command);

        switch (channelUID.getId()) {
            default:
                logger.debug("Unknown command {}", command);
                break;
        }

        if (command instanceof RefreshType) {
            // TODO: handle data refresh
        }

        // TODO: handle command

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information:
        // updateStatus(ThingStatus.OFF;LINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
    }
}
