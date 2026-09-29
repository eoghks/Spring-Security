package com.example.library.common.net;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.config.AppSecurityProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

	private final ClientIpResolver resolver =
			new ClientIpResolver(new AppSecurityProperties(5, List.of("10.0.0.0/8")));

	@Test
	@DisplayName("신뢰 프록시가 아닌 곳에서 온 X-Forwarded-For 는 무시한다")
	void ignoreUntrustedForwardedFor() {
		MockHttpServletRequest request = request("203.0.113.9", "1.1.1.1");

		assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
	}

	@Test
	@DisplayName("신뢰 프록시를 거치면 오른쪽부터 첫 비신뢰 주소를 클라이언트로 본다")
	void useRightmostUntrustedHop() {
		MockHttpServletRequest request = request("10.0.0.5", "6.6.6.6, 198.51.100.7, 10.0.0.9");

		assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
	}

	@Test
	@DisplayName("XFF 에 형식이 틀린 값이 섞이면 remoteAddr 를 쓴다")
	void malformedHop() {
		MockHttpServletRequest request = request("10.0.0.5", "not-an-ip");

		assertThat(resolver.resolve(request)).isEqualTo("10.0.0.5");
	}

	@Test
	@DisplayName("신뢰 프록시 설정이 없으면 항상 remoteAddr")
	void noTrustedProxies() {
		ClientIpResolver strict = new ClientIpResolver(new AppSecurityProperties(5, List.of()));

		assertThat(strict.resolve(request("10.0.0.5", "198.51.100.7"))).isEqualTo("10.0.0.5");
	}

	private MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddr);
		request.addHeader("X-Forwarded-For", forwardedFor);
		return request;
	}
}
