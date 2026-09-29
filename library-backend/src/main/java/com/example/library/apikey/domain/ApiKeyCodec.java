package com.example.library.apikey.domain;

import com.example.library.common.crypto.Hashing;
import com.example.library.common.crypto.SecureTokenGenerator;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * API Key 원문 생성·해시·형식 검사.
 * 원문 = "lib_" + SecureRandom 32바이트의 base64url(패딩 없음, 43자) → 총 47자.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyCodec {

	public static final String PREFIX = "lib_";
	public static final int DISPLAY_PREFIX_LENGTH = 8;
	private static final Pattern FORMAT = Pattern.compile("^lib_[A-Za-z0-9_-]{43}$");

	private final SecureTokenGenerator tokenGenerator;

	public IssuedKey generate() {
		String raw = PREFIX + tokenGenerator.generate();
		return new IssuedKey(raw, raw.substring(0, DISPLAY_PREFIX_LENGTH), hash(raw));
	}

	public String hash(String raw) {
		return Hashing.sha256Hex(raw);
	}

	/** 형식이 틀린 값은 DB·캐시를 조회하지 않고 바로 거절한다 */
	public boolean hasValidFormat(String raw) {
		return raw != null && FORMAT.matcher(raw).matches();
	}

	/**
	 * 발급된 키.
	 *
	 * @param raw    원문(발급 응답에서 1회만 노출)
	 * @param prefix 표시용 앞 8자
	 * @param hash   SHA-256 hex(DB 저장값)
	 */
	public record IssuedKey(String raw, String prefix, String hash) {

		@Override
		public String toString() {
			return "IssuedKey[prefix=" + prefix + "]";
		}
	}
}
