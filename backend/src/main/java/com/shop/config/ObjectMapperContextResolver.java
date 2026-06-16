package com.shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * 这个 @Provider 注解会让 Jersey 在启动时自动发现它。
 * 它的作用是全面接管并自定义 JSON 的序列化规则。
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    public ObjectMapperContextResolver() {
        mapper = new ObjectMapper();

        // 1. 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());

        // 2. 关闭将时间转为时间戳数组的默认行为，强制输出 ISO-8601 标准字符串
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}