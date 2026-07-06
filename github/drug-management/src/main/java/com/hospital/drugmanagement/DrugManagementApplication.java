package com.hospital.drugmanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 医院药品管理系统启动类。
 * <p>
 * 扫描 mapper 与业务组件，默认端口 8081，接口前缀 /api。
 */
@SpringBootApplication
@MapperScan("com.hospital.drugmanagement.mapper")
@ComponentScan(basePackages = "com.hospital.drugmanagement")
public class DrugManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(DrugManagementApplication.class, args);
        System.out.println("=====================================");
        System.out.println("药品管理系统后端启动成功！");
        System.out.println("接口地址:http://localhost:8081/api/drug/list");
        System.out.println("=====================================");
    }
}
