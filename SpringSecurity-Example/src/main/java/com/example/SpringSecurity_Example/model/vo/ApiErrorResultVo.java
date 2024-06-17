package com.example.SpringSecurity_Example.model.vo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiErrorResultVo {
	private List<String> msgs = new ArrayList<String>();
	private int status;
	private HttpStatusCode httpStatus;
}
