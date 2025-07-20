package com.example.SpringSecurity_Example.model.knox;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnoxResponse {
    private String ressult;
    private Integer currentPage;
    private Integer totalPage;
    private Integer totalCount;
}
