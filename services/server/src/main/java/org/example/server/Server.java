package org.example.server;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Server {

    private final int NUM_WORKERS = 5;
    private final ExecutorService executor;
    private final BlockingQueue<Object> messageQueue;

    public Server() {
        IO.println("Building Server...");
        executor = Executors.newFixedThreadPool(NUM_WORKERS);
        messageQueue = new LinkedBlockingQueue<>();

        registerShutdownHook();
    }

    protected void run() {

        for (int i = 0; i < NUM_WORKERS; i++) {
            executor.submit(() -> {
                try {
                    // Loop continuously until an interrupt signal is received
                    while (!Thread.currentThread().isInterrupted()) {
                        Object message = messageQueue.take();
                        IO.println("Processing: " + message.toString());
                    }
                } catch (InterruptedException outer) {
                    // If interrupted while waiting on .take(), the thread will catch it here
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    IO.println("Worker thread shutting down cleanly.");
                }
            });

        }

    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            IO.println("Shutting Down...");

            executor.shutdownNow();

            try {
                // Give running tasks a few seconds to finish processing
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Some workers didn't finish in time.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            IO.println("Server stopped.");
        }));
    }

    public static void main(String[] args) {
        IO.println("Running Server with: " + Arrays.toString(args));

        Server server = new Server();
        server.run();
    }

}
