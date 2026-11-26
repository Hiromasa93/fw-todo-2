package com.example.todoapp;

import java.time.LocalDateTime;

public record PointHistory(LocalDateTime createdAt, String action, int points) {}
