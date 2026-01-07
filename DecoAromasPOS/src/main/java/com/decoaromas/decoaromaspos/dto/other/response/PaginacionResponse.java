package com.decoaromas.decoaromaspos.dto.other.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginacionResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    private List<SortOrder> sort;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SortOrder {
        private String property;
        private String direction;
        private boolean ignoreCase;
    }
}
