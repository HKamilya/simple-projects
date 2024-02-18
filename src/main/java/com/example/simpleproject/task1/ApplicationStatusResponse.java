package com.example.simpleproject.task1;

import org.springframework.lang.Nullable;

import java.time.Duration;

public sealed interface ApplicationStatusResponse {
  record Failure(@Nullable Duration lastRequestTime, int retriesCount) implements ApplicationStatusResponse {}
  record Success(String id, String status) implements ApplicationStatusResponse {}
}