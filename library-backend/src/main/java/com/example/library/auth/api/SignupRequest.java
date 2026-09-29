package com.example.library.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청.
 */
public record SignupRequest(
		@NotBlank(message = "아이디를 입력하세요.")
		@Pattern(regexp = "^[a-z0-9_]{4,20}$", message = "아이디는 영문 소문자·숫자·밑줄 4~20자입니다.")
		String username,

		@NotBlank(message = "비밀번호를 입력하세요.")
		@Size(min = 8, max = 64, message = "비밀번호는 8~64자입니다.")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
				message = "비밀번호는 영문·숫자·특수문자를 모두 포함해야 합니다.")
		String password,

		@NotBlank(message = "이름을 입력하세요.")
		@Size(max = 50, message = "이름은 50자 이하입니다.")
		String name,

		@NotBlank(message = "이메일을 입력하세요.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 200, message = "이메일은 200자 이하입니다.")
		String email) {
}
