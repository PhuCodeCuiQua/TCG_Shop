package com.tcgshop.dto.response;

import java.util.List;

public record ReorderResponse(int addedItemCount, List<String> skippedItems) {
    public ReorderResponse {
        skippedItems = List.copyOf(skippedItems);
    }
}
