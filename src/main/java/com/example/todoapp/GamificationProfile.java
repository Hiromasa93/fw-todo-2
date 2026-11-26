package com.example.todoapp;

public record GamificationProfile(int totalPoints, int level, int pointsInLevel,
        int pointsToNextLevel, int progressPercent) {}
