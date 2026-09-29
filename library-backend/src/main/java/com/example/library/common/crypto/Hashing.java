package com.example.library.common.crypto;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * 토큰·API Key 저장용 해시 유틸.
 * 원문은 저장하지 않고 SHA-256 hex 만 저장해 DB 가 유출돼도 원문을 복원할 수 없게 한다.
 * (충분히 긴 무작위 값이므로 BCrypt 같은 느린 해시 없이 SHA-256 으로 충분하고, 조회 키로 쓸 수 있다)
 */
public final class Hashing {

	private Hashing() {
	}

	public static String sha256Hex(String value) {
		return DigestUtils.sha256Hex(value);
	}
}
