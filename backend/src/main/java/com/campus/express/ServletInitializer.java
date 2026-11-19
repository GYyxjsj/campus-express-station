package com.campus.express;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 外置 Tomcat WAR 部署入口。
 * spring-boot-starter-tomcat 已置为 provided，Tomcat 通过 SPI 拉起本类。
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CampusExpressApplication.class);
    }
}