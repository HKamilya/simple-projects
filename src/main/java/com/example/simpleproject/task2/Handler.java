package com.example.simpleproject.task2;

import java.time.Duration;

public interface Handler {
  Duration timeout();

  void performOperation();
}