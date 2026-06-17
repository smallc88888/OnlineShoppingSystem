package com.shop.config;

import com.shop.dao.UserDao;
import com.shop.entity.User;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

@Provider
public class AdminAuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // 获取请求的相对路径（例如：orders, admin/products）
        String path = requestContext.getUriInfo().getPath();

        // 只有当请求路径以 "admin/" 开头时，才触发严格的管理员校验
        if (path.startsWith("admin/")) {
            // 放行跨域的预检请求 (OPTIONS)
            if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
                return;
            }

            String userIdStr = requestContext.getHeaderString("user-id");

            // 拦截 1：未携带身份凭证
            if (userIdStr == null || userIdStr.trim().isEmpty()) {
                abortWithUnauthorized(requestContext, "访问被拒绝：请先登录");
                return;
            }

            EntityManager em = JpaUtil.getEntityManager();
            try {
                Long userId = Long.parseLong(userIdStr);
                UserDao userDao = new UserDao(em);
                User user = userDao.findById(userId);

                // 拦截 2：用户不存在或不是管理员 (role != 1)
                if (user == null || user.getRole() != 1) {
                    abortWithForbidden(requestContext, "越权访问：您没有管理员权限");
                }
            } catch (NumberFormatException e) {
                abortWithUnauthorized(requestContext, "非法的身份凭证");
            } finally {
                em.close();
            }
        }
    }

    // 终止请求并返回 401 (未认证)
    private void abortWithUnauthorized(ContainerRequestContext context, String message) {
        context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"code\": 401, \"message\": \"" + message + "\"}")
                .type("application/json;charset=UTF-8")
                .build());
    }

    // 终止请求并返回 403 (无权限/被拒绝)
    private void abortWithForbidden(ContainerRequestContext context, String message) {
        context.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity("{\"code\": 403, \"message\": \"" + message + "\"}")
                .type("application/json;charset=UTF-8")
                .build());
    }
}