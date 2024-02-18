package com.example.simpleproject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

public class HandlerImpl implements Handler {
    private final Client client;

    private final Logger logger = LoggerFactory.getLogger(HandlerImpl.class.getName());

    private final ExecutorService executorService;

    public HandlerImpl(Client client) {
        this.client = client;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        Instant now = Instant.now(Clock.systemUTC());
        CompletableFuture<Response> future1 = CompletableFuture.supplyAsync(
                () -> client.getApplicationStatus1(id), executorService);
        CompletableFuture<Response> future2 = CompletableFuture.supplyAsync(
                () -> client.getApplicationStatus2(id), executorService);
        CompletableFuture<ApplicationStatusResponse> result =
                future1.applyToEither(future2, response -> handleResponse(response, id, now, 0));

        try {
            return result.get(15, TimeUnit.SECONDS);
        } catch (CompletionException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ApplicationStatusResponse performOperationAfterFailure(String id, int retriesCount) {
        Instant now = Instant.now(Clock.systemUTC());
        CompletableFuture<Response> future1 = CompletableFuture.supplyAsync(
                () -> client.getApplicationStatus1(id), executorService);
        CompletableFuture<Response> future2 = CompletableFuture.supplyAsync(
                () -> client.getApplicationStatus2(id), executorService);
        CompletableFuture<ApplicationStatusResponse> result =
                future1.applyToEither(future2, response -> handleResponse(response, id, now, retriesCount));

        try {
            return result.get(15, TimeUnit.SECONDS);
        } catch (CompletionException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ApplicationStatusResponse handleResponse(
            Response response,
            String id,
            Instant lastRequestTime,
            int retriesCount) {
        if (response instanceof Response.Success successResponse) {
            return new ApplicationStatusResponse.Success(
                    successResponse.applicationId(),
                    successResponse.applicationStatus());
        } else if (response instanceof Response.RetryAfter retryAfterResponse) {
            return handleRetryAfter(retryAfterResponse, id, retriesCount);
        } else if (response instanceof Response.Failure failureResponse) {
            logger.error(
                    "Failure response with exception: {} and message: {}",
                    failureResponse.ex(),
                    failureResponse.ex().getMessage());
            Instant completeTime = Instant.now(Clock.systemUTC());
            Duration duration = Duration.between(lastRequestTime, completeTime);
            return handleFailure(duration, retriesCount);
        } else {
            throw new RuntimeException("Unexpected response type: " + response.getClass().getName());
        }
    }

    private ApplicationStatusResponse handleRetryAfter(
            Response.RetryAfter retryAfterResponse, String id, int retriesCount) {
        try {
            Thread.sleep(retryAfterResponse.delay().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return performOperationAfterFailure(id, ++retriesCount);
    }

    private ApplicationStatusResponse handleFailure(Duration lastRequestTime, int retriesCount) {
        return new ApplicationStatusResponse.Failure(lastRequestTime, retriesCount);
    }
}
