package com.shop.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JpaUtil {
    // 对应 persistence.xml 中的 <persistence-unit name="ShoppingPU">
    private static final String PERSISTENCE_UNIT_NAME = "ShoppingPU";
    private static EntityManagerFactory factory;

    static {
        try {
            // 1. 创建一个配置 Map，用于覆盖 XML 里的设置
            java.util.Map<String, String> properties = new java.util.HashMap<>();

            // 2. 从操作系统的环境变量（或 Tomcat 运行配置）中读取账号密码
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PWD");

            // 3. 将读取到的变量动态注入到 JPA 配置中
            if (dbUser != null && dbPassword != null) {
                properties.put("jakarta.persistence.jdbc.user", dbUser);
                properties.put("jakarta.persistence.jdbc.password", dbPassword);
            } else {
                // 防呆设计：如果 Tomcat 环境变量没配好，这里提供一个硬编码兜底，确保程序绝不崩溃
                // 注意：请确认下方填写的依然是你本地 MySQL 真实的账号和密码
                System.err.println("⚠️ 警告：未读取到环境变量，正在使用兜底数据库配置！");
                properties.put("jakarta.persistence.jdbc.user", "root");
                properties.put("jakarta.persistence.jdbc.password", "123456");
            }

            // 4. 带着这些动态属性去初始化连接池
            factory = jakarta.persistence.Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);

        } catch (Exception e) {
            System.err.println("JPA EntityManagerFactory 初始化失败: " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 获取数据库操作实体 (每次请求/每个线程应获取一个新的 EntityManager)
     */
    public static EntityManager getEntityManager() {
        return factory.createEntityManager();
    }

    /**
     * 关闭连接池 (通常在应用关闭时调用)
     */
    public static void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }
}