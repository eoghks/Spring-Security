package com.example.SpringSecurity_Example.model.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageEnum {
	SignUpSuccess("회원가입 성공!!", "Singup Success!!"),
	LoginSucess("로그인 성공!!", "Login Success!!"),
	ApiSucess("api 호출 성공!!", "Api Success!!"),
	ExistLoginId("이미 사용중인 로그인 ID 입니다.", "Exist login id"),
	CheckLoginIdOrPassword("로그인 ID 또는 비밀번호를 확인하세요.", "check loign id or password"),
	NotFoundUser("사용자 정보를 찾을 수 없습니다.", "User info not found"),
	InvalidJwtSignature("잘못된 JWT 시그니처", "Invalid JWT Token"),
	InvalidJwtToke("유효하지 않은 JWT 토큰", "Invalid JWT token"),
	ExpiredToken("토큰 기한 만료", "Expired Toekn"),
	UnSupportedJwtToken("지원하지 않는 JWT 토큰", "Unsupported Jwt token"),
	EmptyJwtClaim("JWT 클레임이 존재하지않습니다.", "JWT claims string is empty"),
	JwtErrorOccurred("JWT 토큰 오류 발생", "Jwt Error Occurred"),
	NeedLoginId("로그인이 필요합니다.", "Need Login"),
	NotExistAccessPriv("접근 권한이 없습니다.", "Not Exist Aceess Privailige");

	private final String msg;
	private final String logMsg;
}
