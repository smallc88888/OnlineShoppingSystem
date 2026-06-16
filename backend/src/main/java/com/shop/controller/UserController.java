package com.shop.controller;

import com.shop.dao.UserDao;
import com.shop.dto.ApiResponse;
import com.shop.dto.UserLoginDTO;
import com.shop.dto.UserRegisterDTO;
import com.shop.entity.User;
import com.shop.service.UserService;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users") // 该控制器的路由前缀: /api/users
@Produces(MediaType.APPLICATION_JSON) // 声明返回的数据全是 JSON
@Consumes(MediaType.APPLICATION_JSON) // 声明接收的前端数据也是 JSON
public class UserController {

    /**
     * 注册接口: POST /api/users/register
     */
    @POST
    @Path("/register")
    public Response register(UserRegisterDTO dto) {
        // 1. 获取数据库连接
        EntityManager em = JpaUtil.getEntityManager();
        try {
            // 2. 组装 Dao 和 Service
            UserDao userDao = new UserDao(em);
            UserService userService = new UserService(userDao);

            // 3. 执行注册逻辑
            userService.register(dto);

            // 4. 返回标准成功格式
            ApiResponse<Void> result = new ApiResponse<>(200, "注册成功，请登录", null);
            return Response.ok(result).build();

        } catch (IllegalArgumentException e) {
            // 捕获业务异常（如：用户名已存在），返回 400 Bad Request
            ApiResponse<Void> result = new ApiResponse<>(400, e.getMessage(), null);
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        } finally {
            // 5. 在 finally 中关闭连接，防止连接池泄漏
            em.close();
        }
    }

    /**
     * 登录接口: POST /api/users/login
     */
    @POST
    @Path("/login")
    public Response login(UserLoginDTO loginDto) { // 使用 DTO 接收数据
        EntityManager em = JpaUtil.getEntityManager();
        try {
            UserDao userDao = new UserDao(em);
            UserService userService = new UserService(userDao);

            // 从 DTO 中提取真正的明文 password 传给 Service
            User user = userService.login(loginDto.getUsername(), loginDto.getPassword());

            // 登录成功，不返回密码哈希给前端
            user.setPasswordHash(null);

            ApiResponse<User> result = new ApiResponse<>(200, "登录成功", user);
            return Response.ok(result).build();

        } catch (IllegalArgumentException e) {
            ApiResponse<Void> result = new ApiResponse<>(400, e.getMessage(), null);
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        } catch (IllegalStateException e) {
            ApiResponse<Void> result = new ApiResponse<>(403, e.getMessage(), null);
            return Response.status(Response.Status.FORBIDDEN).entity(result).build();
        } finally {
            em.close();
        }
    }
}