For this version:

- **MQTT 5** everywhere.
- **Mosquitto edge brokers** require username/password.
- **HiveMQ CE** requires username/password through the open-source **File RBAC Extension**.
- **TLS** is used for Edge → HiveMQ.
- **Topic ACLs** prevent Edge 1 from accessing Edge 2's topics.
- Each edge has its own HiveMQ credentials.
- The HiveMQ allow-all extension is disabled.
- HiveMQ listens on `8883` for the authenticated TLS connection.
- Local edge clients use `1883` for the test network.
- The setup is deliberately self-contained and suitable for local Docker testing.

HiveMQ CE itself does not provide username/password authentication directly in `config.xml`; authentication is provided through a security extension. HiveMQ's File RBAC extension provides username/password authentication plus topic-level authorization. ([HiveMQ Documentation][1])

## 1. Directory structure

I'd use:

```text
mqtt-test/
├── docker-compose.yml
│
├── hivemq/
│   ├── Dockerfile
│   ├── config/
│   │   └── config.xml
│   ├── extensions/
│   │   └── hivemq-file-rbac-extension/
│   │       ├── extension-config.xml
│   │       └── credentials.xml
│   └── certs/
│       └── hivemq.jks
│
├── mosquitto-edge-1/
│   ├── config/
│   │   ├── mosquitto.conf
│   │   ├── passwd
│   │   └── acl
│   ├── data/
│   └── log/
│
├── mosquitto-edge-2/
│   ├── config/
│   │   ├── mosquitto.conf
│   │   ├── passwd
│   │   └── acl
│   ├── data/
│   └── log/
│
└── certs/
    └── ca/
        ├── ca.crt
        └── ca.key
```

There is one wrinkle: the HiveMQ File RBAC extension is distributed separately from HiveMQ CE. HiveMQ documents the extension as the open-source way to add username/password authentication and topic authorization to CE. ([GitHub][2])

---

# 2. `docker-compose.yml`

```yaml
services:
  # ============================================================
  # Central MQTT broker
  # ============================================================

  hivemq:
    build:
      context: ./hivemq
      dockerfile: Dockerfile

    container_name: hivemq
    hostname: hivemq

    ports:
      # MQTT over TLS
      - "8883:8883"

      # HiveMQ Control Center
      - "8080:8080"

    volumes:
      - hivemq-data:/opt/hivemq/data
      - hivemq-log:/opt/hivemq/log

    networks:
      - mqtt-network

    restart: unless-stopped

  # ============================================================
  # Edge broker 1
  # ============================================================

  mosquitto-edge-1:
    image: eclipse-mosquitto:2

    container_name: mosquitto-edge-1
    hostname: mosquitto-edge-1

    ports:
      - "1884:1883"

    volumes:
      - ./mosquitto-edge-1/config:/mosquitto/config
      - ./mosquitto-edge-1/data:/mosquitto/data
      - ./mosquitto-edge-1/log:/mosquitto/log

    depends_on:
      - hivemq

    networks:
      - mqtt-network

    restart: unless-stopped

  # ============================================================
  # Edge broker 2
  # ============================================================

  mosquitto-edge-2:
    image: eclipse-mosquitto:2

    container_name: mosquitto-edge-2
    hostname: mosquitto-edge-2

    ports:
      - "1885:1883"

    volumes:
      - ./mosquitto-edge-2/config:/mosquitto/config
      - ./mosquitto-edge-2/data:/mosquitto/data
      - ./mosquitto-edge-2/log:/mosquitto/log

    depends_on:
      - hivemq

    networks:
      - mqtt-network

    restart: unless-stopped

networks:
  mqtt-network:
    driver: bridge

volumes:
  hivemq-data:
  hivemq-log:
```

HiveMQ's official CE image is `hivemq/hivemq-ce`, and the official image supports MQTT 3.x and MQTT 5. ([Docker Hub][3])

---

# 3. HiveMQ Dockerfile

`hivemq/Dockerfile`

```dockerfile
FROM hivemq/hivemq-ce:latest

ARG RBAC_VERSION=4.6.3

USER root

# ------------------------------------------------------------
# Install HiveMQ File RBAC Extension
# ------------------------------------------------------------

RUN mkdir -p /opt/hivemq/extensions \
    && cd /tmp \
    && curl -fsSL \
       "https://github.com/hivemq/hivemq-file-rbac-extension/releases/download/${RBAC_VERSION}/hivemq-file-rbac-extension-${RBAC_VERSION}.zip" \
       -o hivemq-file-rbac-extension.zip \
    && unzip hivemq-file-rbac-extension.zip -d /opt/hivemq/extensions \
    && rm hivemq-file-rbac-extension.zip

# ------------------------------------------------------------
# Disable HiveMQ's allow-all extension
# ------------------------------------------------------------

RUN touch /opt/hivemq/extensions/hivemq-allow-all-extension/DISABLED

# ------------------------------------------------------------
# Copy configuration
# ------------------------------------------------------------

COPY config/config.xml \
     /opt/hivemq/conf/config.xml

COPY extensions/hivemq-file-rbac-extension/extension-config.xml \
     /opt/hivemq/extensions/hivemq-file-rbac-extension/extension-config.xml

COPY extensions/hivemq-file-rbac-extension/credentials.xml \
     /opt/hivemq/extensions/hivemq-file-rbac-extension/credentials.xml

COPY certs/hivemq.jks \
     /opt/hivemq/conf/hivemq.jks

USER hivemq
```

The `4.6.3` RBAC release is documented by HiveMQ's support material, and the extension repository documents the installation procedure and configuration files. ([HiveMQ Support Forum][4])

If you want to avoid relying on `latest`, I'd eventually pin **both** the HiveMQ CE version and RBAC extension version.

---

# 4. HiveMQ `config.xml`

`hivemq/config/config.xml`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<hivemq>

    <listeners>

        <!--
            MQTT over TLS.

            This is the only MQTT listener exposed by HiveMQ.
        -->
        <tls-tcp-listener>

            <name>mqtt-tls</name>

            <port>8883</port>

            <bind-address>0.0.0.0</bind-address>

            <tls>

                <keystore>

                    <path>/opt/hivemq/conf/hivemq.jks</path>

                    <password>changeit</password>

                    <private-key-password>changeit</private-key-password>

                </keystore>

                <!--
                    Username/password authentication is performed
                    by the File RBAC extension.

                    TLS client certificates are NOT required here.
                -->
                <client-authentication-mode>NONE</client-authentication-mode>

            </tls>

        </tls-tcp-listener>

    </listeners>

</hivemq>
```

The HiveMQ TLS listener is a `tls-tcp-listener`, and the broker expects a Java keystore containing the server certificate/private key. HiveMQ documents `8883` as the standard MQTT-over-TLS port. ([GitHub][5])

Notice that I'm **not** using mutual TLS yet.

The authentication chain is:

```text
TLS
  ↓
MQTT 5 CONNECT
  ↓
username/password
  ↓
HiveMQ File RBAC
  ↓
topic authorization
```

That's a good starting point because it lets you test the MQTT authentication mechanism independently from certificate-based client identity.

---

# 5. HiveMQ RBAC configuration

`hivemq/extensions/hivemq-file-rbac-extension/extension-config.xml`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<extension-configuration>

    <!--
        Reload credentials every 10 seconds.

        Useful during development when modifying
        credentials.xml.
    -->
    <credentials-reload-interval>10</credentials-reload-interval>

    <!--
        Passwords in credentials.xml are stored as plaintext
        for this local test environment.

        Change to HASHED for anything beyond testing.
    -->
    <password-type>PLAIN</password-type>

</extension-configuration>
```

The extension supports both plaintext and hashed passwords, as well as runtime credential reloads. ([GitHub][2])

---

# 6. HiveMQ users and ACLs

`hivemq/extensions/hivemq-file-rbac-extension/credentials.xml`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<file-rbac>

    <users>

        <!-- ==================================================
             Edge 1
             ================================================== -->

        <user>

            <name>edge-1</name>

            <password>edge1-secret</password>

            <roles>
                <id>edge-1</id>
            </roles>

        </user>


        <!-- ==================================================
             Edge 2
             ================================================== -->

        <user>

            <name>edge-2</name>

            <password>edge2-secret</password>

            <roles>
                <id>edge-2</id>
            </roles>

        </user>


        <!-- ==================================================
             Central application
             ================================================== -->

        <user>

            <name>central-service</name>

            <password>central-secret</password>

            <roles>
                <id>central-service</id>
            </roles>

        </user>

    </users>


    <roles>

        <!-- ==================================================
             Edge 1
             ================================================== -->

        <role>

            <id>edge-1</id>

            <permissions>

                <!-- Edge 1 telemetry -->
                <permission>
                    <topic>edge/edge-1/telemetry/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

                <!-- Edge 1 state -->
                <permission>
                    <topic>edge/edge-1/state/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

                <!-- Commands sent TO edge 1 -->
                <permission>
                    <topic>edge/edge-1/commands/#</topic>
                    <activity>SUBSCRIBE</activity>
                    <qos>ALL</qos>
                </permission>

                <!-- Edge 1 events -->
                <permission>
                    <topic>edge/edge-1/events/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

            </permissions>

        </role>


        <!-- ==================================================
             Edge 2
             ================================================== -->

        <role>

            <id>edge-2</id>

            <permissions>

                <permission>
                    <topic>edge/edge-2/telemetry/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

                <permission>
                    <topic>edge/edge-2/state/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

                <permission>
                    <topic>edge/edge-2/commands/#</topic>
                    <activity>SUBSCRIBE</activity>
                    <qos>ALL</qos>
                </permission>

                <permission>
                    <topic>edge/edge-2/events/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

            </permissions>

        </role>


        <!-- ==================================================
             Central service
             ================================================== -->

        <role>

            <id>central-service</id>

            <permissions>

                <!-- Read telemetry -->
                <permission>
                    <topic>edge/+/telemetry/#</topic>
                    <activity>SUBSCRIBE</activity>
                    <qos>ALL</qos>
                </permission>

                <!-- Read state -->
                <permission>
                    <topic>edge/+/state/#</topic>
                    <activity>SUBSCRIBE</activity>
                    <qos>ALL</qos>
                </permission>

                <!-- Send commands -->
                <permission>
                    <topic>edge/+/commands/#</topic>
                    <activity>PUBLISH</activity>
                    <qos>ALL</qos>
                </permission>

                <!-- Read events -->
                <permission>
                    <topic>edge/+/events/#</topic>
                    <activity>SUBSCRIBE</activity>
                    <qos>ALL</qos>
                </permission>

            </permissions>

        </role>

    </roles>

</file-rbac>
```

This is one of the useful parts of the File RBAC extension: it can apply authorization at the topic level, separately controlling `PUBLISH` and `SUBSCRIBE`. ([GitHub][2])

So:

```text
edge-1
   │
   ├── publish edge/edge-1/telemetry/...
   ├── publish edge/edge-1/state/...
   ├── publish edge/edge-1/events/...
   │
   └── subscribe edge/edge-1/commands/...
```

but **not**:

```text
edge-1 → edge/edge-2/telemetry/...
```

---

# 7. Mosquitto Edge 1

`mosquitto-edge-1/config/mosquitto.conf`

```conf
# ============================================================
# MQTT listener
# ============================================================

listener 1883

protocol mqtt

# MQTT authentication
allow_anonymous false

password_file /mosquitto/config/passwd

# Topic authorization
acl_file /mosquitto/config/acl


# ============================================================
# Persistence
# ============================================================

persistence true

persistence_location /mosquitto/data/


# ============================================================
# Logging
# ============================================================

log_dest stdout
log_dest file /mosquitto/log/mosquitto.log


# ============================================================
# MQTT protocol defaults
# ============================================================

# MQTT 5 clients are used by our test clients.
# Mosquitto itself supports MQTT 5.

max_inflight_messages 100


# ============================================================
# Bridge to central HiveMQ
# ============================================================

connection central-hivemq

address hivemq:8883


# ------------------------------------------------------------
# TLS
# ------------------------------------------------------------

bridge_tls_version tlsv1.3

bridge_cafile /mosquitto/config/ca.crt


# ------------------------------------------------------------
# HiveMQ authentication
# ------------------------------------------------------------

remote_username edge-1
remote_password edge1-secret


# ------------------------------------------------------------
# MQTT 5 bridge
# ------------------------------------------------------------

bridge_protocol_version mqttv5


# ------------------------------------------------------------
# Connection behavior
# ------------------------------------------------------------

keepalive_interval 30

restart_timeout 5 30

start_type automatic

try_private false


# ============================================================
# Telemetry: Edge → HiveMQ
# ============================================================

topic edge/edge-1/telemetry/# out 1


# ============================================================
# State: Edge → HiveMQ
# ============================================================

topic edge/edge-1/state/# out 1


# ============================================================
# Events: Edge → HiveMQ
# ============================================================

topic edge/edge-1/events/# out 1


# ============================================================
# Commands: HiveMQ → Edge
# ============================================================

topic edge/edge-1/commands/# in 1
```

Mosquitto supports username/password authentication with `password_file`, and its bridge configuration supports remote credentials and TLS. ([Eclipse Mosquitto][6])

---

# 8. Edge 1 password file

`mosquitto-edge-1/config/passwd`

For this first test:

```text
edge-device:edge-device-secret
```

However, **don't actually leave plaintext passwords here**.

Mosquitto provides `mosquitto_passwd` specifically for generating the password file. ([Eclipse Mosquitto][7])

We'll generate this in the setup script below.

---

# 9. Edge 1 ACL

`mosquitto-edge-1/config/acl`

```conf
# ============================================================
# Edge 1 local devices
# ============================================================

user edge-device

topic write edge/edge-1/telemetry/#
topic write edge/edge-1/state/#
topic write edge/edge-1/events/#

topic read edge/edge-1/commands/#


# ============================================================
# Bridge account
# ============================================================

user edge-1

topic write edge/edge-1/telemetry/#
topic write edge/edge-1/state/#
topic write edge/edge-1/events/#

topic read edge/edge-1/commands/#
```

This means a local device connected to Edge 1 can't simply publish arbitrary topics.

---

# 10. Edge 2

`mosquitto-edge-2/config/mosquitto.conf`

```conf
# ============================================================
# MQTT listener
# ============================================================

listener 1883

protocol mqtt

allow_anonymous false

password_file /mosquitto/config/passwd

acl_file /mosquitto/config/acl


# ============================================================
# Persistence
# ============================================================

persistence true

persistence_location /mosquitto/data/


# ============================================================
# Logging
# ============================================================

log_dest stdout
log_dest file /mosquitto/log/mosquitto.log


# ============================================================
# MQTT
# ============================================================

max_inflight_messages 100


# ============================================================
# Bridge to HiveMQ
# ============================================================

connection central-hivemq

address hivemq:8883

bridge_tls_version tlsv1.3

bridge_cafile /mosquitto/config/ca.crt

remote_username edge-2
remote_password edge2-secret

bridge_protocol_version mqttv5

keepalive_interval 30

restart_timeout 5 30

start_type automatic

try_private false


# ============================================================
# Edge 2 → HiveMQ
# ============================================================

topic edge/edge-2/telemetry/# out 1

topic edge/edge-2/state/# out 1

topic edge/edge-2/events/# out 1


# ============================================================
# HiveMQ → Edge 2
# ============================================================

topic edge/edge-2/commands/# in 1
```

---

# 11. Edge 2 ACL

`mosquitto-edge-2/config/acl`

```conf
# ============================================================
# Edge 2 local devices
# ============================================================

user edge-device

topic write edge/edge-2/telemetry/#
topic write edge/edge-2/state/#
topic write edge/edge-2/events/#

topic read edge/edge-2/commands/#


# ============================================================
# Bridge account
# ============================================================

user edge-2

topic write edge/edge-2/telemetry/#
topic write edge/edge-2/state/#
topic write edge/edge-2/events/#

topic read edge/edge-2/commands/#
```

---

# 12. The TLS certificate

For local development, I'd use a local CA.

Create:

```text
certs/
└── ca/
    ├── ca.key
    └── ca.crt
```

Then generate the HiveMQ certificate.

For example:

```bash
mkdir -p certs/ca
mkdir -p hivemq/certs

openssl genrsa \
    -out certs/ca/ca.key \
    4096

openssl req \
    -x509 \
    -new \
    -nodes \
    -key certs/ca/ca.key \
    -sha256 \
    -days 3650 \
    -out certs/ca/ca.crt \
    -subj "/CN=MQTT Test CA"
```

Then create the HiveMQ key:

```bash
openssl genrsa \
    -out /tmp/hivemq.key \
    2048
```

Create the CSR:

```bash
openssl req \
    -new \
    -key /tmp/hivemq.key \
    -out /tmp/hivemq.csr \
    -subj "/CN=hivemq"
```

Create:

```text
/tmp/hivemq.ext
```

with:

```text
subjectAltName=DNS:hivemq,DNS:localhost
extendedKeyUsage=serverAuth
```

Sign it:

```bash
openssl x509 \
    -req \
    -in /tmp/hivemq.csr \
    -CA certs/ca/ca.crt \
    -CAkey certs/ca/ca.key \
    -CAcreateserial \
    -out /tmp/hivemq.crt \
    -days 825 \
    -sha256 \
    -extfile /tmp/hivemq.ext
```

Now create the Java keystore:

```bash
openssl pkcs12 \
    -export \
    -in /tmp/hivemq.crt \
    -inkey /tmp/hivemq.key \
    -certfile certs/ca/ca.crt \
    -name hivemq \
    -out /tmp/hivemq.p12 \
    -passout pass:changeit
```

Then:

```bash
keytool -importkeystore \
    -srckeystore /tmp/hivemq.p12 \
    -srcstoretype PKCS12 \
    -srcstorepass changeit \
    -destkeystore hivemq/certs/hivemq.jks \
    -deststoretype JKS \
    -deststorepass changeit \
    -noprompt
```

HiveMQ documents JKS-based TLS configuration for its MQTT TLS listeners. ([HiveMQ Documentation][8])

---

# 13. Copy the CA to the Mosquitto brokers

```bash
cp certs/ca/ca.crt \
   mosquitto-edge-1/config/ca.crt

cp certs/ca/ca.crt \
   mosquitto-edge-2/config/ca.crt
```

The bridge will now validate:

```text
Mosquitto
    │
    │ TLS
    ▼
HiveMQ
```

using your local CA.

---

# 14. Generate the Mosquitto password files

Rather than manually putting passwords into the files, use the Mosquitto utility.

For Edge 1:

```bash
docker run --rm \
    -v "$PWD/mosquitto-edge-1/config:/mosquitto/config" \
    eclipse-mosquitto:2 \
    mosquitto_passwd \
    -c \
    -b \
    /mosquitto/config/passwd \
    edge-device \
    edge-device-secret
```

Then add the bridge user:

```bash
docker run --rm \
    -v "$PWD/mosquitto-edge-1/config:/mosquitto/config" \
    eclipse-mosquitto:2 \
    mosquitto_passwd \
    -b \
    /mosquitto/config/passwd \
    edge-1 \
    edge1-secret
```

For Edge 2:

```bash
docker run --rm \
    -v "$PWD/mosquitto-edge-2/config:/mosquitto/config" \
    eclipse-mosquitto:2 \
    mosquitto_passwd \
    -c \
    -b \
    /mosquitto/config/passwd \
    edge-device \
    edge-device-secret
```

Then:

```bash
docker run --rm \
    -v "$PWD/mosquitto-edge-2/config:/mosquitto/config" \
    eclipse-mosquitto:2 \
    mosquitto_passwd \
    -b \
    /mosquitto/config/passwd \
    edge-2 \
    edge2-secret
```

This produces hashed password entries rather than leaving the Mosquitto credentials in plaintext. Mosquitto's documentation recommends `mosquitto_passwd` for managing these files. ([Eclipse Mosquitto][7])

---

# 15. Important: MQTT 5

The interesting part of this architecture is that MQTT 5 is now being used at both levels:

```text
                  MQTT 5
                    │
                    ▼
        ┌─────────────────────┐
        │  Mosquitto Edge 1   │
        └──────────┬──────────┘
                   │
                   │ MQTT 5
                   │ TLS
                   │ username/password
                   ▼
        ┌─────────────────────┐
        │      HiveMQ CE      │
        └─────────────────────┘
```

The HiveMQ File RBAC extension authenticates the MQTT connection based on the CONNECT credentials. HiveMQ also supports MQTT 5's enhanced authentication mechanism, but you **do not need enhanced `AUTH` packets simply because you're using MQTT 5**. ([HiveMQ Documentation][1])

---

# 16. Start everything

```bash
docker compose build
docker compose up -d
```

Check:

```bash
docker compose ps
```

Then:

```bash
docker compose logs -f hivemq
```

You want to see the File RBAC extension start and the allow-all extension disabled.

---

# 17. Test Edge 1 authentication

First, this should **fail**:

```bash
mosquitto_pub \
    -h localhost \
    -p 1884 \
    -V 5 \
    -t edge/edge-1/telemetry/test \
    -m "hello"
```

Because authentication is required.

This should succeed:

```bash
mosquitto_pub \
    -h localhost \
    -p 1884 \
    -V 5 \
    -u edge-device \
    -P edge-device-secret \
    -t edge/edge-1/telemetry/test \
    -m "hello"
```

---

# 18. Test Edge 1 → HiveMQ

Subscribe to HiveMQ:

```bash
mosquitto_sub \
    -h localhost \
    -p 8883 \
    -V 5 \
    --cafile certs/ca/ca.crt \
    -u central-service \
    -P central-secret \
    -t 'edge/+/telemetry/#' \
    -v
```

Then publish through Edge 1:

```bash
mosquitto_pub \
    -h localhost \
    -p 1884 \
    -V 5 \
    -u edge-device \
    -P edge-device-secret \
    -t edge/edge-1/telemetry/temperature \
    -m "72.5"
```

You should see:

```text
edge/edge-1/telemetry/temperature 72.5
```

---

# 19. Test commands in the opposite direction

Subscribe to Edge 1:

```bash
mosquitto_sub \
    -h localhost \
    -p 1884 \
    -V 5 \
    -u edge-device \
    -P edge-device-secret \
    -t 'edge/edge-1/commands/#' \
    -v
```

Then publish from the central side:

```bash
mosquitto_pub \
    -h localhost \
    -p 8883 \
    -V 5 \
    --cafile certs/ca/ca.crt \
    -u central-service \
    -P central-secret \
    -t edge/edge-1/commands/reboot \
    -m '{"reason":"test"}'
```

You should get:

```text
edge/edge-1/commands/reboot {"reason":"test"}
```

---

# 20. Test the ACL isolation

This is an especially important test.

Try:

```bash
mosquitto_pub \
    -h localhost \
    -p 1884 \
    -V 5 \
    -u edge-device \
    -P edge-device-secret \
    -t edge/edge-2/telemetry/test \
    -m "I should not be allowed"
```

That should be **rejected**.

Likewise, Edge 1 should not be able to subscribe to:

```text
edge/edge-2/commands/#
```

That's the behavior we ultimately want for an edge architecture with potentially thousands of devices.

---

## One architectural change I'd make before going much further

For your actual system, I would **not stop at username/password authentication**.

I'd eventually move toward:

```text
                    ┌─────────────────────┐
                    │      HiveMQ CE      │
                    │                     │
                    │  Authentication     │
                    │        +            │
                    │  Authorization      │
                    └──────────▲──────────┘
                               │
                         TLS / MQTT 5
                               │
             ┌─────────────────┴─────────────────┐
             │                                   │
       ┌─────┴─────┐                       ┌─────┴─────┐
       │ Mosquitto │                       │ Mosquitto │
       │  Edge 1   │                       │  Edge 2   │
       └─────▲─────┘                       └─────▲─────┘
             │                                   │
       Sensors/devices                     Sensors/devices
```

Then, once the basic system works, I'd add **mutual TLS (mTLS)** for the Edge → HiveMQ connection:

```text
              Edge
               │
        client certificate
               │
               ▼
          ┌─────────┐
          │  TLS    │
          │         │
          │  mTLS   │
          └────┬────┘
               │
               ▼
             HiveMQ
```

HiveMQ supports requiring client certificates with `client-authentication-mode=REQUIRED` and a truststore containing trusted client certificates. ([HiveMQ Documentation][8])

That gives you two independent security layers:

**Transport identity:**

```text
TLS certificate → "This is Edge 17"
```

**Application identity:**

```text
MQTT username/password → "This edge/device is authorized as X"
```

For a large-scale custom IoT network, that's considerably more attractive than treating usernames/passwords as the sole identity mechanism.

Also, I would keep **TLS on the Edge → HiveMQ link even if your local edge devices use a constrained network**. MQTT username/password without encryption exposes the credentials on the wire; Mosquitto's documentation explicitly recommends network encryption when using username/password authentication. ([Eclipse Mosquitto][6])

[HiveMQ File RBAC Extension](https://github.com/hivemq/hivemq-file-rbac-extension?utm_source=chatgpt.com)
[Mosquitto authentication documentation](https://www.mosquitto.org/documentation/authentication-methods/?utm_source=chatgpt.com)

[1]: https://docs.hivemq.com/hivemq/latest/extensions/authentication.html?utm_source=chatgpt.com "MQTT Client Authentication Options for Your HiveMQ Extension :: HiveMQ Documentation"
[2]: https://github.com/hivemq/hivemq-file-rbac-extension?utm_source=chatgpt.com "GitHub - hivemq/hivemq-file-rbac-extension: HiveMQ extension for managing role-based authorization · GitHub"
[3]: https://hub.docker.com/r/hivemq/hivemq-ce?utm_source=chatgpt.com "hivemq/hivemq-ce - Docker Image"
[4]: https://community.hivemq.com/t/use-jwt-token-based-authentication-with-hivemq-edge/3584?utm_source=chatgpt.com "Use JWT token based authentication with HiveMQ Edge - HiveMQ Support Forum"
[5]: https://github.com/hivemq/hivemq-community-edition/wiki/Listener-configuration?utm_source=chatgpt.com "Listener configuration · hivemq/hivemq-community-edition Wiki · GitHub"
[6]: https://www.mosquitto.org/man/mosquitto-conf-5.html?utm_source=chatgpt.com "mosquitto.conf man page | Eclipse Mosquitto"
[7]: https://www.mosquitto.org/documentation/authentication-methods/?utm_source=chatgpt.com "Authentication methods | Eclipse Mosquitto"
[8]: https://docs.hivemq.com/hivemq/latest/user-guide/howtos.html?utm_source=chatgpt.com "HiveMQ Broker How-Tos :: HiveMQ Documentation"
