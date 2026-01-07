package com.decoaromas.decoaromaspos.dto.other;

import java.util.List;
import java.util.stream.Collectors;

import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import org.springframework.data.domain.Page;

public class PaginacionMapper {

    private PaginacionMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> PaginacionResponse<T> mapToResponse(Page<T> page) {
        List<PaginacionResponse.SortOrder> sortInfo = page.getSort().stream()
                .map(order -> PaginacionResponse.SortOrder.builder()
                        .property(order.getProperty())
                        .direction(order.getDirection().name())
                        .ignoreCase(order.isIgnoreCase())
                        .build()
                )
                .collect(Collectors.toList());

        return PaginacionResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .sort(sortInfo)
                .build();
    }
}
