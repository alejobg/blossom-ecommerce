package com.blossom.ecommerce.shared.web;

import com.blossom.ecommerce.shared.domain.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<? super D, ? extends R> mapper) {
        PageResult<R> mapped = result.map(mapper);
        return new PageResponse<>(mapped.content(), mapped.page(), mapped.size(),
                mapped.totalElements(), mapped.totalPages());
    }
}
