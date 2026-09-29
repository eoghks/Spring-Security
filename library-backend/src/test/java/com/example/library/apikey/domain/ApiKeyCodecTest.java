package com.example.library.apikey.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.apikey.domain.ApiKeyCodec.IssuedKey;
import com.example.library.common.crypto.SecureTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyCodecTest {

	private final ApiKeyCodec codec = new ApiKeyCodec(new SecureTokenGenerator());

	@Test
	@DisplayName("발급 키는 lib_ 접두 47자이고, prefix 는 앞 8자, 해시는 SHA-256 hex 64자다")
	void generate() {
		IssuedKey key = codec.generate();

		assertThat(key.raw()).startsWith("lib_").hasSize(47);
		assertThat(key.prefix()).isEqualTo(key.raw().substring(0, 8));
		assertThat(key.hash()).hasSize(64).matches("[0-9a-f]+").isEqualTo(codec.hash(key.raw()));
		assertThat(codec.hasValidFormat(key.raw())).isTrue();
		assertThat(key.toString()).doesNotContain(key.raw());
	}

	@Test
	@DisplayName("매번 다른 키가 생성되고, 해시는 결정적이다")
	void uniqueAndDeterministic() {
		IssuedKey first = codec.generate();
		IssuedKey second = codec.generate();

		assertThat(first.raw()).isNotEqualTo(second.raw());
		assertThat(codec.hash(first.raw())).isEqualTo(codec.hash(first.raw()));
		assertThat(codec.hash(first.raw())).isNotEqualTo(codec.hash(second.raw()));
	}

	@Test
	@DisplayName("시드 샘플 키의 해시가 data.sql 값과 일치한다")
	void seedSampleKey() {
		assertThat(codec.hash("lib_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY"))
				.isEqualTo("565cdfe9b369ac3546605edfaeeca71b4f125b53ddf2d1c6dd488aad7116fde9");
	}

	@Test
	@DisplayName("형식이 틀린 값은 거절한다")
	void invalidFormat() {
		assertThat(codec.hasValidFormat("lib_short")).isFalse();
		assertThat(codec.hasValidFormat("key_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY")).isFalse();
		assertThat(codec.hasValidFormat(null)).isFalse();
	}
}
