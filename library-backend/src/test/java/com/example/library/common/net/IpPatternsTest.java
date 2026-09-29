package com.example.library.common.net;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IpPatternsTest {

	@ParameterizedTest
	@ValueSource(strings = {"10.0.0.1", "192.168.0.0/16", "0.0.0.0/0", "::1", "2001:db8::/32", "fe80::1:2:3:4",
			"1:2:3:4:5:6:7:8"})
	@DisplayName("단일 IP·CIDR(IPv4/IPv6)은 유효하다")
	void valid(String value) {
		assertThat(IpPatterns.isValid(value)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "256.1.1.1", "10.0.0", "10.0.0.1/33", "example.com", "::::::", "1:2:3:4:5:6:7:8:9",
			"10.0.0.1/", "10.0.0.1/8/1", "2001:db8::/129", "1::2::3"})
	@DisplayName("형식이 틀리거나 호스트 이름이면 무효다(DNS 조회 방지)")
	void invalid(String value) {
		assertThat(IpPatterns.isValid(value)).isFalse();
	}

	@Test
	@DisplayName("CIDR 범위와 단일 IP 로 매칭하고, 목록이 비면 전체 허용한다")
	void matching() {
		List<String> patterns = List.of("192.168.10.0/24", "10.1.2.3");

		assertThat(IpPatterns.matchesAny(patterns, "192.168.10.77")).isTrue();
		assertThat(IpPatterns.matchesAny(patterns, "10.1.2.3")).isTrue();
		assertThat(IpPatterns.matchesAny(patterns, "192.168.11.1")).isFalse();
		assertThat(IpPatterns.matchesAny(List.of(), "8.8.8.8")).isTrue();
		assertThat(IpPatterns.matchesAny(List.of("::1"), "::1")).isTrue();
	}

	@Test
	@DisplayName("패턴 앞뒤 공백은 무시하고 매칭한다(예외 없이)")
	void matchingIgnoresSurroundingSpaces() {
		assertThat(IpPatterns.matchesAny(List.of(" 10.0.0.0/8 "), "10.9.8.7")).isTrue();
		assertThat(IpPatterns.matchesAny(List.of(" 10.0.0.1"), "10.0.0.1")).isTrue();
		assertThat(IpPatterns.matchesAny(List.of(" 10.0.0.1"), "10.0.0.2")).isFalse();
	}

	@Test
	@DisplayName("로그용 마스킹")
	void mask() {
		assertThat(IpPatterns.mask("192.168.10.77")).isEqualTo("192.168.10.*");
		assertThat(IpPatterns.mask("2001:db8:1:2:3:4:5:6")).isEqualTo("2001:db8:1:2:*");
	}
}
