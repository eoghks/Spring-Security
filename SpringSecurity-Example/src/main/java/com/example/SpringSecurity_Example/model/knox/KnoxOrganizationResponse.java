package com.example.SpringSecurity_Example.model.knox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class KnoxOrganizationResponse extends KnoxResponse{
    List<KnoxOrganization> organizations = new ArrayList<>();
}
