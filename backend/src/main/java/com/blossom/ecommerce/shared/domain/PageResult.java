package com.blossom.ecommerce.shared.domain;

import java.util.List;
import java.util.function.Function;

/**
 * Framework-agnostic page of results returned by output ports.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements, totalPages);
    }
}
