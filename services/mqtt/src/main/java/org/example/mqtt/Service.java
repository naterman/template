package org.example.mqtt;

import java.util.UUID;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
//https://www.hivemq.com/blog/mqtt-client-library-enyclopedia-hivemq-mqtt-client/
public class Service implements Runnable {

    public Service() {
        IO.println("Building Service...");

    }

    @Override
    public void run() {
        IO.println("Running Service...");

        Mqtt5AsyncClient client = MqttClient.builder().useMqttVersion5().identifier(UUID.randomUUID().toString())
                .serverHost("broker.hivemq.com").serverPort(1883).buildAsync();

        client.connect().whenComplete((connAck, throwable) -> {
            if (throwable != null) {
                // Handle connection failure
            } else {
                // Setup subscribes or start publishing
            }
        });

        client.connectWith().simpleAuth().username("my-user").password("my-password".getBytes()).applySimpleAuth()
                .send().whenComplete((connAck, throwable) -> {
                    // Handle connection complete
                });

        client.connectWith().willPublish().topic("my/will").payload("payload".getBytes()).qos(MqttQos.AT_MOST_ONCE)
                .retain(true).applyWillPublish().send().whenComplete((connAck, throwable) -> {
                    // Handle connection complete
                });

        client.publishWith().topic("my/topic").payload("".getBytes()).qos(MqttQos.EXACTLY_ONCE).send()
                .whenComplete((mqtt3Publish, throwable) -> {
                    if (throwable != null) {
                        // Handle failure to publish
                    } else {
                        // Handle successful publish, e.g. logging or incrementing a metric
                    }
                });

        client.subscribeWith().topicFilter("my/topic").callback(publish -> {
            // Process the received message
        }).send().whenComplete((subAck, throwable) -> {
            if (throwable != null) {
                // Handle failure to subscribe
            } else {
                // Handle successful subscription, e.g. logging or incrementing a metric
            }
        });

        client.unsubscribeWith().topicFilter("my/topic").send();

        client.disconnect();

        // Mqtt5AsyncClient client2 = MqttClient.builder()
        // .useMqttVersion5()
        // .serverHost("localhost")
        // .serverPort(8883);
        // .sslWithDefaultConfig()
        // .buildAsync();

        // Mqtt5AsyncClient client3 = MqttClient.builder()
        // .useMqttVersion5()
        // .serverHost("localhost")
        // .serverPort(8883);
        // .sslConfig()
        // .keyManagerFactory(myKeyManagerFactory)
        // .trustManagerFactory(myTrustManagerFactory)
        // .applySslConfig()
        // .buildAsync();
    }

}
