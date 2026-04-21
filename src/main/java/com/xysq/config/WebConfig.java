package com.xysq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把前端对 /uploads/** 的请求，映射到项目根目录下的 uploads 文件夹
        String path = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + path);
    }
}