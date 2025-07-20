package com.example.SpringSecurity_Example.service;

import com.example.SpringSecurity_Example.model.knox.KnoxEmployee;
import com.example.SpringSecurity_Example.model.knox.KnoxOrganization;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnoxTestServiceImpl implements KnoxTestService{

    @Override
    public List<KnoxEmployee> getEmployees(String departmentCode, String page) {
        List<KnoxEmployee> res = new ArrayList<>();
        if(departmentCode.equals("D001")) {
            res.add(firstEmployee());
        } else if(departmentCode.equals("D002")) {
            res.add(secondEmployee());
        } else if(departmentCode.equals("D003")) {
            res.add(thirdEmployee());
        }
        return res;
    }

    private KnoxEmployee firstEmployee() {
        return new KnoxEmployee(
                "Active", "BUSI001", "사업부서명", "LG", "LG전자", "KR", "LG",
                "D001", "연구소", "직원 설명", "employee@lg.com", "EMP0001", "정규직",
                "Full-Time", "Business Department", "LG Electronics", "R&D Center", "Employee description",
                "Hong Gil Dong", "Gil Dong", "Senior Manager", "Seoul Office", "Seoul Region",
                "Business Dept", "LG Electronics", "R&D Center", "Senior Manager", "Seoul",
                "총괄부서", "Manager", "Hong", "연구부", "Manager", "EP12345", "N",
                "external@other.com", "홍길동", "길동", "부장", "GTIC01", "IN001", "연구부",
                "개발자", "JOB001", "Y", "010-1234-5678", "길동이", "서울시 강남구", "역삼동 123-45",
                "02-123-4567", "06134", "02-987-6543", "ko", "Y", "RG001", "서울", "High",
                "BUSI001", "LG", "LG전자", "UPR001", "연구부", "부장", "SGT01", "RG001",
                "High", "SUB001", "총괄부서", "부장", "STC01", "01", "Seoul Data Center", "홍",
                "SUB001", "연구부", "부장", "T001", "01", "OP001", "hong123", "1001", null,
                "2030-12-31", "Asia/Seoul"
        );
    }

    private KnoxEmployee secondEmployee() {
        return new KnoxEmployee(
                "Active", "BUSI002", "마케팅사업부", "LG", "LG전자", "KR", "LG",
                "D002", "마케팅부", "마케팅부서 직원", "lee.soonshin@lg.com", "EMP0002", "정규직",
                "Full-Time", "Marketing Department", "LG Electronics", "Marketing Dept", "Marketing staff",
                "Lee Soon Shin", "Soon Shin", "Manager", "Seoul Office", "Seoul Region",
                "Marketing Dept", "LG Electronics", "Advertising Team", "Manager", "Seoul",
                "Advertising HQ", "Manager", "Lee", "Advertising Team", "Manager", "EP56789", "Y",
                "external.marketing@other.com", "이순신", "순신", "매니저", "GTIC02", "IN002", "광고팀",
                "마케팅 매니저", "JOB002", "Y", "010-2345-6789", "순신이", "서울시 강남구", "역삼동 234-56",
                "02-234-5678", "06135", "02-876-5432", "en", "Y", "RG002", "서울", "Medium",
                "BUSI002", "LG", "LG전자", "UPR002", "광고팀", "매니저", "SGT02", "RG002",
                "Medium", "SUB002", "광고본부", "매니저", "STC02", "02", "Seoul Marketing Center", "이",
                "SUB002", "광고팀", "매니저", "T002", "02", "OP002", "lee123", "1002", null,
                "2031-12-31", "Asia/Seoul"
        );
    }

    private KnoxEmployee thirdEmployee() {
        return new KnoxEmployee(
                "Active", "BUSI003", "개발사업부", "LG", "LG전자", "KR", "LG",
                "D003", "개발부", "개발부서 직원", "jang.bogo@lg.com", "EMP0003", "정규직",
                "Full-Time", "Development Department", "LG Electronics", "Development Dept", "Development staff",
                "Jang Bogo", "Bogo", "Director", "Seoul Office", "Seoul Region",
                "Development Dept", "LG Electronics", "Tech Team", "Director", "Seoul",
                "Tech Division", "Director", "Jang", "Tech Team", "Director", "EP90123", "N",
                "external.development@other.com", "장보고", "보고", "이사", "GTIC03", "IN003", "기술팀",
                "개발자", "JOB003", "Y", "010-3456-7890", "보고씨", "서울시 강남구", "삼성동 345-67",
                "02-345-6789", "06136", "02-765-4321", "ko", "Y", "RG003", "서울", "High",
                "BUSI003", "LG", "LG전자", "UPR003", "기술팀", "이사", "SGT03", "RG003",
                "High", "SUB003", "기술본부", "이사", "STC03", "03", "Seoul Development Center", "장",
                "SUB003", "기술팀", "이사", "T003", "03", "OP003", "jang123", "1003", null,
                "2032-12-31", "Asia/Seoul"
        );
    }

    @Override
    public List<KnoxOrganization> getOrganizations(String page) {
        List<KnoxOrganization> res = new ArrayList<>();
        if(page.equals("1")) {
            res.add(firstOrg());
        }else if(page.equals("2")) {
            res.add(secondOrg());
        }else if(page.equals("3")) {
            res.add(thirdOrg());
        }

        return res;
    }

    private KnoxOrganization firstOrg(){
        return new KnoxOrganization(
                "LG", "LG전자", "D001", "1", "연구소", "1", "LG Electronics", "R&D Center",
                "Senior Manager", "SUB001", "IN001", "N", "M1234", "홍길동", "팀장",
                "ko", "SUB001", "연구부", "UPR001", "Head Dept", "총괄부서", "N", "C001", "LG그룹", "LG Group"
        );
    }

    private KnoxOrganization secondOrg(){
        return new KnoxOrganization(
                "LG", "LG전자", "D002", "2", "마케팅부", "2", "LG Electronics", "Marketing Dept",
                "Manager", "SUB002", "IN002", "Y", "M5678", "이순신", "매니저",
                "en", "SUB002", "광고팀", "UPR002", "Marketing HQ", "광고본부", "N", "C001", "LG그룹", "LG Group"
        );
    }

    private KnoxOrganization thirdOrg(){
        return new KnoxOrganization(
                "LG", "LG전자", "D003", "3", "개발부", "3", "LG Electronics", "Development Dept",
                "Director", "SUB003", "IN003", "N", "M9012", "장보고", "이사",
                "ko", "SUB003", "기술팀", "UPR003", "Tech Division", "기술본부", "Y", "C001", "LG그룹", "LG Group"
        );
    }
}
