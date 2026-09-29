package com.example.library.common.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 페이지 응답.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	/** 한 페이지 최대 크기 */
	public static final int MAX_SIZE = 100;

	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	/** 요청 페이지 크기를 1~MAX_SIZE 로 제한한다 */
	public static int clampSize(int size) {
		return Math.clamp(size, 1, MAX_SIZE);
	}
}
