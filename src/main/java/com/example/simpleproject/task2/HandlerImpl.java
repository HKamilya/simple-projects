package com.example.simpleproject.task2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HandlerImpl implements Handler {
    private final Client client;

    private final Logger logger = LoggerFactory.getLogger(com.example.simpleproject.task1.HandlerImpl.class.getName());


    public HandlerImpl(Client client) {
        this.client = client;
    }

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(1);
    }

    @Override
    public void performOperation() {
        while (true) {
            Event event = client.readData();
            if (event == null) {
                continue;
            }

            event.recipients().forEach(recipient ->
                    CompletableFuture.supplyAsync(() -> client.sendData(recipient, event.payload()))
                            .thenAccept(result -> {
                                        if (result == Result.REJECTED) {
                                            logger.info("Data rejected by recipient: {}", recipient);
                                            try {
                                                Thread.sleep(timeout().toMillis());
                                            } catch (InterruptedException e) {
                                                logger.error("Interrupted exception was caught");
                                                Thread.currentThread().interrupt();
                                            }
                                            client.sendData(recipient, event.payload());
                                        }
                                        if (result == Result.ACCEPTED) {
                                            logger.info("Data successfully accepted by recipient: {}", recipient);
                                        }
                                    }
                            ));
        }
    }
}
