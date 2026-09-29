package com.example.library.support;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 동시성 테스트 도우미. 모든 작업을 별도 스레드에서 준비시킨 뒤 같은 신호에 맞춰 한꺼번에 실행한다.
 */
public final class Concurrently {

	private Concurrently() {
	}

	/** 작업들을 동시에 실행하고 결과를 작업 순서대로 돌려준다 */
	public static <T> List<T> run(List<Callable<T>> tasks) {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
		List<CompletableFuture<T>> futures = tasks.stream()
				.map(task -> CompletableFuture.supplyAsync(() -> awaitAndCall(start, task), executor))
				.toList();
		start.countDown();
		List<T> results = futures.stream().map(CompletableFuture::join).toList();
		executor.shutdown();
		return results;
	}

	private static <T> T awaitAndCall(CountDownLatch start, Callable<T> task) {
		try {
			start.await();
			return task.call();
		} catch (Exception e) {
			// 작업 스레드의 검사 예외를 호출 스레드로 넘긴다(흐름 제어 목적 아님)
			throw new IllegalStateException(e);
		}
	}
}
