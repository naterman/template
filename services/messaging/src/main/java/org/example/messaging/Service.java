package org.example.messaging;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class Service implements Runnable {

    private final String connectionUrl;

    private final BlockingQueue<String> queue;

    public Service(String connectionUrl, BlockingQueue<String> queue) {
        IO.println("Building Service...");
        this.connectionUrl = connectionUrl;
        this.queue = queue;
    }

    @Override
    public void run() {
        IO.println("Running Service...");

        try (Connection nc = Nats.connect(connectionUrl)) {
            IO.println("Connected to NATS: " + nc.getConnectedUrl());

            String message = queue.take();
            nc.publish("TESTING", message.getBytes());

        } catch (InterruptedException e) {
            System.err.println("Interuption Exception: " + e);
        } catch (IOException e) {
            System.err.println("IO Exception: " + e);
        }
    }

}
