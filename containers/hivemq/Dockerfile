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