package com.taskmaster.taskmaster.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable JSON shape for paginated results.
 * (Returning Spring's Page object directly gives an unstable, implementation-specific JSON format.)
 *
 * Example:
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 45,
 *   "totalPages": 3,
 *   "first": true,
 *   "last": false
 * }
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}