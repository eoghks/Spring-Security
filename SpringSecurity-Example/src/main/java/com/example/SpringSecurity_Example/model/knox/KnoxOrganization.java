package com.example.SpringSecurity_Example.model.knox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KnoxOrganization {
    private String companyCode;
    private String companyName;
    private String departmentCode;
    private String departmentLevel;
    private String departmentName;
    private String departmentOrder;
    private String enCompanyName;
    private String enDepartmentName;
    private String enManagerTitle;
    private String enSubOrgCode;
    private String inDepartmentCode;
    private String lowDepartmentYn;
    private String managerId;
    private String managerName;
    private String managerTitle;
    private String preferredLanguage;
    private String subOrgCode;
    private String subOrgName;
    private String uprDepartmentCode;
    private String enUprDepartmentName;
    private String uprDepartmentName;
    private String hiddenDepartmentYn;
    private String corpCode;
    private String corpName;
    private String enCorpName;
}
