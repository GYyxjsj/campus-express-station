package com.campus.express;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园快递驿站取件提醒与代取授权管理系统
 * 支持两种运行形态：
 * 1) 开发态: mvn spring-boot:run（内嵌 Tomcat）
 * 2) 生产态: WAR 部署外置 Tomcat（见 ServletInitializer）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.campus.express.mapper")
public class CampusExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusExpressApplication.class, args);
    }
}