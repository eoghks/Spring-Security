package com.example.SpringSecurity_Example.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiError {
	private List<String> msg = new ArrayList<String>();
	private int status;
	private HttpStatusCode httpStatus;
}
