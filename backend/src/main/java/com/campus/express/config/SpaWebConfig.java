package com.campus.express.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 前端静态资源托管：frontend/dist 构建产物打入 WAR 的 static/ 目录，
 * 由 Tomcat 同域托管前后端（http://host:8080/campus-express/）。
 * - /assets/**（带内容 hash）长缓存；index.html 等入口不缓存，发版即生效；
 * - SPA history 路由回退：非 /api 且未命中静态文件的路径一律返回 index.html。
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(31536000)
                .resourceChain(true);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API 与 actuator 等动态路径不做 SPA 回退，交给控制器/404
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}