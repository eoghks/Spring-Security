package com.example.SpringSecurity_Example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class apiController {
	@GetMapping("/api1")
	public ResponseEntity api1() {
		return new ResponseEntity<>("api1", HttpStatus.OK);
	}
	
	@GetMapping("/api2")
	public ResponseEntity api2() {
		return new ResponseEntity<>("api2", HttpStatus.OK);
	}
	
	@GetMapping("/api3")
	public ResponseEntity api3() {
		return new ResponseEntity<>("api3", HttpStatus.OK);
	}
}
