package com.shop.controller;

import com.shop.dao.OrderDao;
import com.shop.dto.ApiResponse;
import com.shop.entity.Order;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@Path("/admin/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminOrderController {

    /**
     * 获取全站所有订单
     * GET /api/admin/orders
     */
    @GET
    public Response getAllOrders() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            OrderDao orderDao = new OrderDao(em);
            List<Order> orders = orderDao.findAllOrders();

            // 复用之前写好的前台 OrderResponseDTO 进行数据脱敏（防止暴露 User 的密码等敏感字段）
            List<OrderController.OrderResponseDTO> dtoList = orders.stream()
                    .map(OrderController.OrderResponseDTO::new)
                    .collect(Collectors.toList());

            return Response.ok(new ApiResponse<>(200, "获取全站订单成功", dtoList)).build();
        } finally {
            em.close();
        }
    }

    /**
     * 确认订单（核对库存、物流后推进）
     * PUT /api/admin/orders/{id}/confirm
     */
    @PUT
    @Path("/{id}/confirm")
    public Response confirmOrder(@PathParam("id") Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            OrderDao orderDao = new OrderDao(em);
            Order order = orderDao.findById(id);

            if (order == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiResponse<>(404, "订单不存在", null)).build();
            }

            // 状态机防线：只有"待确认(0)"状态才能执行确认操作
            if (order.getStatus() != 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ApiResponse<>(400, "当前订单状态无法执行确认操作", null)).build();
            }

            order.setStatus(1); // 变更为：待付款
            orderDao.update(order);

            return Response.ok(new ApiResponse<>(200, "订单已确认，等待用户付款", null)).build();
        } finally {
            em.close();
        }
    }

    /**
     * 订单发货
     * PUT /api/admin/orders/{id}/ship
     */
    @PUT
    @Path("/{id}/ship")
    public Response shipOrder(@PathParam("id") Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            OrderDao orderDao = new OrderDao(em);
            Order order = orderDao.findById(id);

            if (order == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiResponse<>(404, "订单不存在", null)).build();
            }

            // 状态机防线：只有"已付款(2)"状态才能执行发货操作
            if (order.getStatus() != 2) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ApiResponse<>(400, "订单尚未付款或已处理，无法发货", null)).build();
            }

            order.setStatus(3); // 变更为：已发货待收货
            orderDao.update(order);

            return Response.ok(new ApiResponse<>(200, "订单已发货", null)).build();
        } finally {
            em.close();
        }
    }
}