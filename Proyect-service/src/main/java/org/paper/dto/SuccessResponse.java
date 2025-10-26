package org.paper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> {
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String message;
    private T data;
    private String correlationId;

    public static <T> SuccessResponse<T> of(String message, T data) {
        return SuccessResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }

    public static <T> SuccessResponse<T> of(T data) {
        return SuccessResponse.<T>builder()
                .data(data)
                .build();
    }

    public static SuccessResponse<Void> of(String message) {
        return SuccessResponse.<Void>builder()
                .message(message)
                .build();
    }
}