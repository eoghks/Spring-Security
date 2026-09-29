package com.example.library.common.net;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * IP·CIDR 패턴 검증과 매칭.
 * IpAddressMatcher 는 IP 가 아닌 문자열을 호스트 이름으로 보고 DNS 조회를 시도하므로,
 * 저장 전에 이 클래스로 형식을 엄격히 검증한 값만 매칭에 쓴다.
 */
public final class IpPatterns {

	private static final Pattern IPV4 = Pattern.compile(
			"^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
	private static final Pattern HEX_GROUP = Pattern.compile("^[0-9a-fA-F]{1,4}$");
	private static final int IPV6_GROUPS = 8;

	private IpPatterns() {
	}

	/** 단일 IP 또는 CIDR(IPv4/IPv6) 형식이면 true */
	public static boolean isValid(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		String[] parts = value.trim().split("/", -1);
		if (parts.length > 2) {
			return false;
		}
		boolean ipv4 = IPV4.matcher(parts[0]).matches();
		boolean ipv6 = !ipv4 && isIpv6(parts[0]);
		if (!ipv4 && !ipv6) {
			return false;
		}
		return parts.length == 1 || isPrefix(parts[1], ipv4 ? 32 : 128);
	}

	/** 클라이언트 IP 가 목록 중 하나와 일치하면 true. 목록이 비면 전체 허용. 패턴의 앞뒤 공백은 무시한다 */
	public static boolean matchesAny(List<String> patterns, String clientIp) {
		if (patterns.isEmpty()) {
			return true;
		}
		if (!isValid(clientIp) || clientIp.contains("/")) {
			return false;
		}
		return patterns.stream()
				.map(String::trim)
				.filter(IpPatterns::isValid)
				.anyMatch(pattern -> new IpAddressMatcher(pattern).matches(clientIp));
	}

	/** 로그용 마스킹: IPv4 는 마지막 옥텟, IPv6 는 앞 4그룹 이후를 가린다 */
	public static String mask(String ip) {
		if (IPV4.matcher(ip).matches()) {
			return ip.substring(0, ip.lastIndexOf('.')) + ".*";
		}
		String[] groups = ip.split(":");
		return groups.length > 4 ? String.join(":", List.of(groups).subList(0, 4)) + ":*" : "*";
	}

	private static boolean isIpv6(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		int doubleColon = lower.indexOf("::");
		if (doubleColon != lower.lastIndexOf("::") || !lower.contains(":")) {
			return false;
		}
		if (doubleColon < 0) {
			return groupsValid(lower.split(":", -1)) && lower.split(":", -1).length == IPV6_GROUPS;
		}
		String head = lower.substring(0, doubleColon);
		String tail = lower.substring(doubleColon + 2);
		String[] headGroups = head.isEmpty() ? new String[0] : head.split(":", -1);
		String[] tailGroups = tail.isEmpty() ? new String[0] : tail.split(":", -1);
		return groupsValid(headGroups) && groupsValid(tailGroups)
				&& headGroups.length + tailGroups.length < IPV6_GROUPS;
	}

	private static boolean groupsValid(String[] groups) {
		for (String group : groups) {
			if (!HEX_GROUP.matcher(group).matches()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isPrefix(String value, int max) {
		if (value.isEmpty() || value.length() > 3 || !value.chars().allMatch(Character::isDigit)) {
			return false;
		}
		return Integer.parseInt(value) <= max;
	}
}
