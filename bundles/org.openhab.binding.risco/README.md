# Risco Binding


The openHAB Risco Binding allows openHAB to communicate with Risco security panels.
The connectivity to the panel is via TCP/IP on the local network.

## Supported Things

Currently the following things are supported:

- `bridge`: The bridge is the communication point with the panel. Agility LightSys, LightSysPlus, ProSys panels should be working. Tested only on LightSysPlus. 
- `bus-expander`: Represents a bus expander within a Risco Alarm System
- `keyfob`: Represents a keyfob within a Risco Alarm System
- `keypad`: Represents a keypad within a Risco Alarm System
- `output`: Represents an output within a Risco Alarm System
- `output-expander`: Represents an output expander within a Risco Alarm System
- `partition`: Represents a partition within a Risco Alarm System
- `power-supply`: Represents a power supply within a Risco Alarm System
- `proximity-reader`: Represents a proximity reader within a Risco Alarm System
- `siren`: Represents a siren within a Risco Alarm System
- `panel`: Represents the panel within a Risco Alarm System
- `voice-module`: Represents a voice module within a Risco Alarm System
- `wireless-module`: Represents a eireless module within a Risco Alarm System
- `zone`: Represents a zone within a Risco Alarm System
- `zone-expander`: Represents a zone expander within a Risco Alarm System

## Discovery

First the bridge must be manually defined. The hostname or IP address, port, panel id, panel password, encoding and the connection delay have to be specified. Except for the ip, all the other parameters have their default values predefined. 

After the bridge is manually added and available to openHAB, the binding will automatically discover various things and add them to the discovery inbox, according to the received messages from the panel.

A full discovery cycle can be started by pressing the Scan button.

## Thing Configuration

The things can be configured either through the discovery process, or manually via the UI.
The following tables show the available configuration parameters for each thing.

### `bridge` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| hostname        | text    | Hostname or IP address                | N/A     | yes      | no       |
| port            | integer | IP port to access the device          | 1000    | yes      | no       |
| id              | integer | Risco Panel ID                        | 1       | yes      | no       |
| password        | text    | Risco Panel communication password    | 5678    | yes      | no       |
| encoding        | text    | Communication encoding                | UTF-8   | yes      | no       |
| connectionDelay | integer | Connection delay                      | 0       | yes      | no       |

### `bus-expander` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| busExpanderNumber | integer | Bus Expander Number                   | N/A     | yes      | no       |

### `keyfob` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| keyfobNumber      | integer | Keyfob Number                         | N/A     | yes      | no       |

### `keypad` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| keypadNumber      | integer | Keypad Number                         | N/A     | yes      | no       |

### `output` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| outputNumber      | integer | Output Number                         | N/A     | yes      | no       |

### `output-expander` Thing Configuration

| Name                 | Type    | Description                           | Default | Required | Advanced |
|----------------------|---------|---------------------------------------|---------|----------|----------|
| outputExpanderNumber | integer | Keyfob Number                         | N/A     | yes      | no       |

### `partition` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| partitionNumber   | integer | Partition Number                      | N/A     | yes      | no       |

### `power-supply` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| powerSupplyNumber | integer | Power Supply Number                   | N/A     | yes      | no       |

### `proximity-reader` Thing Configuration

| Name                  | Type    | Description                           | Default | Required | Advanced |
|-----------------------|---------|---------------------------------------|---------|----------|----------|
| proximityReaderNumber | integer | Proximity Reader Number               | N/A     | yes      | no       |

### `siren` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| sirenNumber       | integer | Siren Number                          | N/A     | yes      | no       |

### `wireless-module` Thing Configuration

| Name                  | Type    | Description                           | Default | Required | Advanced |
|-----------------------|---------|---------------------------------------|---------|----------|----------|
| wirelesssModuleNumber | integer | Wireless Module Number                | N/A     | yes      | no       |

### `zone` Thing Configuration

| Name              | Type    | Description                           | Default | Required | Advanced |
|-------------------|---------|---------------------------------------|---------|----------|----------|
| zoneNumber        | integer | Zone Number                           | N/A     | yes      | no       |

### `zone-expander` Thing Configuration

| Name               | Type    | Description                           | Default | Required | Advanced |
|--------------------|---------|---------------------------------------|---------|----------|----------|
| zoneExpanderNumber | integer | Zone Expander Number                  | N/A     | yes      | no       |


## Channels

The Risco binding things expose the following channels:

### `bridge` Channels

| Channel      | Type   | Read/Write | Description                                    |
|--------------|--------|------------|------------------------------------------------|
| send-command | String | W          | This is the command channel  (Not yet working) |

### `bus-expander` Channels

| Channel               | Type   | Read/Write | Description                                    |
|-----------------------|--------|------------|------------------------------------------------|
| tamper                | Switch | R          | Tamper                                         |
| communication-trouble | Switch | R          | Communication Trouble                          |
| exists                | Switch | R          | Exists                                         |

### `keyfob` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| low-battery-trouble   | Switch | R          | Low Battery Trouble                     |
| exists                | Switch | R          | Exists                                  |

### `keypad` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| low-battery-trouble   | Switch | R          | Low Battery Trouble                     |
| box-tamper            | Switch | R          | Box Tamper                              |
| communication-trouble | Switch | R          | Communication Trouble                   |
| lost                  | Switch | R          | Lost                                    |
| exists                | Switch | R          | Exists                                  |

### `output` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| output-is-active      | Switch | R          | Output is active                        |
| exists                | Switch | R          | Exists                                  |

### `output-expander` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| tamper                | Switch | R          | Tamper                                  |
| communication-trouble | Switch | R          | Communication Trouble                   |
| phone-line-trouble    | Switch | R          | Phone Line Trouble                      |
| dual-path-trouble     | Switch | R          | Dual Path Trouble                       |
| exists                | Switch | R          | Exists                                  |

### `partition` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| alarm                 | Switch | R          | Alarm or Stand By                       |
| duress                | Switch | R          | Duress or Free                          |
| false-code            | Switch | R          | False Code or Code Ok                   |
| fire                  | Switch | R          | Fire or No Fire                         |
| panic                 | Switch | R          | Panic                                   |
| medic                 | Switch | R          | Medic                                   |
| arm                   | Switch | RW         | Armed or Disarmed                       |
| home-stay             | Switch | RW         | Home Stay or Home Disarmed              |
| ready-to-arm          | Switch | R          | Ready or Not Ready                      |
| open                  | Switch | R          | At least one zone is tripped            |
| exists                | Switch | R          | Exists                                  |
| reset-required        | Switch | R          | Memory Event or Memory Ack              |
| no-activity-alert     | Switch | R          | No Activity Alert                       |
| group-a-arm           | Switch | R          | Group A Armed or Group A Disarmed       |
| group-b-arm           | Switch | R          | Group B Armed or Group B Disarmed       |
| group-c-arm           | Switch | R          | Group C Armed or Group C Disarmed       |
| group-d-arm           | Switch | R          | Group D Armed or Group D Disarmed       |
| trouble               | Switch | R          | Trouble or Ok                           |

### `power-supply` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| tamper                | Switch | R          | Tamper                                  |
| communication-trouble | Switch | R          | Communication Trouble                   |
| ac-trouble            | Switch | R          | A/C Trouble                             |
| low-battery           | Switch | R          | Low Battery                             |
| aux-trouble           | Switch | R          | AUX Trouble                             |
| bell-trouble          | Switch | R          | Bell Trouble                            |
| exists                | Switch | R          | Exists                                  |

### `proximity-reader` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| tamper                | Switch | R          | Tamper                                  |
| communication-trouble | Switch | R          | Communication Trouble                   |
| exists                | Switch | R          | Exists                                  |


### `siren` Channels

| Channel                     | Type   | Read/Write | Description                             |
|-----------------------------|--------|------------|-----------------------------------------|
| radio-low-battery-trouble   | Switch | R          | Radio Low Battery Trouble               |
| speaker-low-battery-trouble | Switch | R          | Radio Low Battery Trouble               |
| battery-load                | Switch | R          | Battery Load                            |
| communication-trouble       | Switch | R          | Communication Trouble                   |
| exists                      | Switch | R          | Speaker Low Battery Trouble             |
| proximity-tamper            | Switch | R          | Proximity Tamper                        |
| aux-trouble                 | Switch | R          | Aux Trouble                             |
| speaker-flt                 | Switch | R          | Speaker Flt                             |
| charge-trouble              | Switch | R          | Charge Trouble                          |
| invalid                     | Switch | R          | Invalid                                 |
| box-tamper                  | Switch | R          | Box Tamper                              |
| lost                        | Switch | R          | Lost                                    |
| low-battery                 | Switch | R          | Low Battery                             |

### `panel` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| name                  | String | R          | Name                                    |
| low-battery-trouble   | Switch | R          | Low Battery Trouble                     |
| ac-trouble            | Switch | R          | A/C Trouble                             |
| phone-line-trouble    | Switch | R          | Phone Line Trouble                      |
| clock-trouble         | Switch | R          | Clock Trouble                           |
| default-switch        | Switch | R          | Default Switch On                       |
| ms-1-report-trouble   | Switch | R          | MS 1 Report Trouble                     |
| ms-2-report-trouble   | Switch | R          | MS 2 Report Trouble                     |
| ms-3-report-trouble   | Switch | R          | MS 3 Report Trouble                     |
| box-tamper            | Switch | R          | Box Tamper                              |
| jamming-trouble       | Switch | R          | Jamming Trouble                         |
| prog-mode             | Switch | R          | Prog Mode On                            |
| learn-mode            | Switch | R          | Learn Mode On                           |
| three-min-bypass      | Switch | R          | Three Min Bypass On                     |
| walk-test             | Switch | R          | Walk Test On                            |
| aux-trouble           | Switch | R          | Aux Trouble                             |
| rs485-bus-trouble     | Switch | R          | RS485 Bus Trouble                       |
| ls-switch             | Switch | R          | Ls Switch On                            |
| bell-switch           | Switch | R          | Bell Switch On                          |
| bell-trouble          | Switch | R          | Bell Trouble                            |
| bell-tamper           | Switch | R          | Bell Tamper                             |
| service-expired       | Switch | R          | Service Expired                         |
| payment-expired       | Switch | R          | Payment Expired                         |
| service-mode          | Switch | R          | Service Mode                            |
| dual-path             | Switch | R          | Dual Path                               |

### `voice-module` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| tamper                | Switch | R          | Tamper                                  |
| communication-trouble | Switch | R          | Communication Trouble                   |
| exists                | Switch | R          | Exists                                  |

### `wireless-module` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| tamper                | Switch | R          | Tamper                                  |
| communication-trouble | Switch | R          | Communication Trouble                   |
| jamming               | Switch | R          | Jamming                                 |
| lost-signal           | Switch | R          | Lost Signal                             |
| exists                | Switch | R          | Exists                                  |

### `zone` Channels

| Channel               | Type    | Read/Write | Description                             |
|-----------------------|---------|------------|-----------------------------------------|
| open                  | Contact | R          | Open                                    |
| arm                   | Switch  | RW         | Armed or Disarmed                       |
| alarm                 | Switch  | R          | Alarm or Stand By                       |
| tamper                | Switch  | R          | Tamper                                  |
| trouble               | Switch  | R          | Trouble or Ok                           |
| lost                  | Switch  | R          | Lost                                    |
| low-battery           | Switch  | R          | Low Battery                             |
| bypass                | Switch  | RW         | Bypassed or Unbypassed                  |
| communication-trouble | Switch  | R          | Communication Trouble                   |
| soak-test             | Switch  | R          | Soak Test                               |
| hours24               | Switch  | R          | 24 Hours zone                           |
| not-used              | Switch  | R          | Not used                                |
| exists                | Switch  | R          | Exists                                  |
| name                  | String  | R          | Name                                    |

### `zone-expander` Channels

| Channel               | Type   | Read/Write | Description                             |
|-----------------------|--------|------------|-----------------------------------------|
| tamper                | Switch | R          | Tamper                                  |
| communication-trouble | Switch | R          | Communication Trouble                   |
| aux-trouble           | Switch | R          | AUX Trouble                             |
| exists                | Switch | R          | Exists                                  |

## Full Example

_Provide a full usage example based on textual configuration files._
_*.things, *.items examples are mandatory as textual configuration is well used by many users._
_*.sitemap examples are optional._
