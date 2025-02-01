package org.example.data_classes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String chatId;
    private String message;
    private ZonedDateTime scheduledTime;
}