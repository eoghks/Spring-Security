package com.example.SpringSecurity_Example.controller;

import com.example.SpringSecurity_Example.model.knox.KnoxEmployeeResponse;
import com.example.SpringSecurity_Example.model.knox.KnoxOrganization;
import com.example.SpringSecurity_Example.model.knox.KnoxOrganizationResponse;
import com.example.SpringSecurity_Example.model.knox.KnoxRequestDto;
import com.example.SpringSecurity_Example.service.KnoxTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final KnoxTestService knoxTestSerivce;

    @PostMapping("/employee/api/v2.0/employees")
    public ResponseEntity<KnoxEmployeeResponse> selectEmployees(@RequestParam String companyCode,
                                                                @RequestParam String departmentCode,
                                                                @RequestParam String page,
                                                                @RequestBody KnoxRequestDto requestDto){
        KnoxEmployeeResponse res = new KnoxEmployeeResponse();
        res.setRessult("success");
        res.setCurrentPage(Integer.parseInt(page));
        res.setTotalPage(1);
        res.setTotalCount(1);
        res.setEmployees(knoxTestSerivce.getEmployees(departmentCode, page));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/employee/api/v2.0/organizations")
    public ResponseEntity<KnoxOrganizationResponse> selectOrganization(@RequestParam String companyCode, @RequestParam String page){
        KnoxOrganizationResponse res = new KnoxOrganizationResponse();
        res.setRessult("success");
        res.setCurrentPage(Integer.parseInt(page));
        res.setTotalPage(3);
        res.setTotalCount(3);
        res.setOrganizations(knoxTestSerivce.getOrganizations(page));
        return ResponseEntity.ok(res);
    }
}
