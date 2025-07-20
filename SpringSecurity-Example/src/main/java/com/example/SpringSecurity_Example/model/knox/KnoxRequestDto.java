package com.example.SpringSecurity_Example.model.knox;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KnoxRequestDto {
    private String resultType;
    private List<String> attributes;
}
