package com.example.SpringSecurity_Example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.SpringSecurity_Example.model.UsersDto;

import jakarta.validation.Valid;

@RestController
public class apiController {
	@PostMapping("/signup")
	public ResponseEntity<String> signup(@Valid @RequestBody UsersDto userDto) throws Exception{
		String message = "userId: "+ userDto.getId().toString() + "의 회원가입 완료";
		return new ResponseEntity<>( message, HttpStatus.OK);
	}
	
	@GetMapping("/api1")
	public ResponseEntity<String> api1() {
		return new ResponseEntity<>("api1", HttpStatus.OK);
	}
	
	@GetMapping("/api2")
	public ResponseEntity<String> api2() {
		return new ResponseEntity<>("api2", HttpStatus.OK);
	}
	
	@GetMapping("/api3")
	public ResponseEntity<String> api3() {
		return new ResponseEntity<>("api3", HttpStatus.OK);
	}
}
