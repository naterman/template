package org.example.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import io.github.amadeusitgroup.testcontainers.nats.NatsContainer;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;
import org.junit.jupiter.api.Test;

public class ContainerTest {

    @Test
    public void testContainer() {
        NatsContainer nats = new NatsContainer("nats:2");
        nats.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (Connection connection = Nats.connect(nats.getConnectionUrl())) {
            Subscription subscription = connection.subscribe("TESTING");
            BlockingQueue<String> queue = new LinkedBlockingDeque<>();

            Service service = new Service(nats.getConnectionUrl(), queue);

            executor.submit(service);
            queue.add("OUTPUT");
            Message message = subscription.nextMessage(1000);
            String data = new String(message.getData(), StandardCharsets.UTF_8);

            assertThat(data).isEqualTo("OUTPUT");
            IO.println("Data Received: " + data);

        } catch (Exception exception) {

        }

        nats.close();
    }

}
