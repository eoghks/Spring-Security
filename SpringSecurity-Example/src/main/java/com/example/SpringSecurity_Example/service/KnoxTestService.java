package com.example.SpringSecurity_Example.service;

import com.example.SpringSecurity_Example.model.knox.KnoxEmployee;
import com.example.SpringSecurity_Example.model.knox.KnoxOrganization;

import java.util.List;

public interface KnoxTestService {
    List<KnoxEmployee> getEmployees(String departmentCode, String page);
    List<KnoxOrganization> getOrganizations(String page);
}
