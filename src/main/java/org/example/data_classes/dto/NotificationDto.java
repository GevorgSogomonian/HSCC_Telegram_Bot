package org.example.data_classes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationDto {
    private Long notificationId;
    private LocalDateTime notificationTime;
}
