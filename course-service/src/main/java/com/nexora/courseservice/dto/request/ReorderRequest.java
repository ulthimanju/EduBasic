package com.nexora.courseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderRequest {
    @NotNull(message = "Reorder items list is required")
    private List<ReorderItem> items;

    public record ReorderItem(
        @NotNull(message = "ID is required") UUID id,
        @NotNull(message = "Order index is required") Integer orderIndex
    ) {}
}
