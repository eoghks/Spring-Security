package com.example.SpringSecurity_Example.model.knox;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class KnoxEmployeeResponse extends KnoxResponse{
    private List<KnoxEmployee> employees = new ArrayList<>();
}
