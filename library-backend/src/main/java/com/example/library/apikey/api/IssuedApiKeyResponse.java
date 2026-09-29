package com.example.library.apikey.api;

/**
 * API Key 발급 응답. apiKey 원문은 이 응답에서 한 번만 전달되며 다시 조회할 수 없다.
 */
public record IssuedApiKeyResponse(String apiKey, ApiKeyResponse detail) {

	@Override
	public String toString() {
		return "IssuedApiKeyResponse[apiKey=****, detail=" + detail + "]";
	}
}
