package com.shop.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * 设置所有 RESTful API 的统一前缀为 /api
 */
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {
    public RestApplication() {
        // 告诉 Jersey 去哪个包下扫描我们的 Controller
        packages("com.shop.controller");

        // 注册跨域过滤器
        register(CorsFilter.class);
    }
}