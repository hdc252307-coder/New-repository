package com.example.demo;

public record AppNotification(
        String level,
        String message,
        String link
) {
}
