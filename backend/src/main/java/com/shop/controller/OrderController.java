package com.shop.controller;

import com.shop.dto.ApiResponse;
import com.shop.entity.Order;
import com.shop.entity.OrderItem;
import com.shop.service.OrderService;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderController {

    // ==========================================
    // 内部 DTO 类定义区（安全脱敏、简化层级）
    // ==========================================

    /** 接收前端发来的结算表单 */
    public static class CheckoutRequest {
        public String receiverName;
        public String receiverPhone;
        public String receiverAddress;
    }

    /** 响应给前端的订单明细快照 DTO */
    public static class OrderItemResponseDTO {
        public Long productId;
        public String productName;
        public BigDecimal buyPrice;
        public int quantity;

        public OrderItemResponseDTO(OrderItem item) {
            this.productId = item.getProduct().getId();
            this.productName = item.getProductName();
            this.buyPrice = item.getBuyPrice();
            this.quantity = item.getQuantity();
        }
    }

    /** 响应给前端的订单主表 DTO */
    public static class OrderResponseDTO {
        public String orderNo;
        public BigDecimal totalAmount;
        public String receiverName;
        public String receiverPhone;
        public String receiverAddress;
        public int status;
        public LocalDateTime createdAt;
        public List<OrderItemResponseDTO> items;
        public Long id;

        public OrderResponseDTO(Order order) {
            this.orderNo = order.getOrderNo();
            this.totalAmount = order.getTotalAmount();
            this.receiverName = order.getReceiverName();
            this.receiverPhone = order.getReceiverPhone();
            this.receiverAddress = order.getReceiverAddress();
            this.status = order.getStatus();
            this.createdAt = order.getCreatedAt();
            this.id = order.getId();
            // 组装并映射挂载的明细列表
            this.items = order.getItems().stream()
                    .map(OrderItemResponseDTO::new)
                    .collect(Collectors.toList());
        }
    }

    // ==========================================
    // API 接口定义区
    // ==========================================

    /**
     * 提交订单（购物车结算）
     * POST /api/orders/checkout
     */
    @POST
    @Path("/checkout")
    public Response checkout(@HeaderParam("user-id") Long userId, CheckoutRequest request) {
        if (userId == null) return unauthorized();

        EntityManager em = JpaUtil.getEntityManager();
        try {
            OrderService orderService = new OrderService(em);
            Order order = orderService.checkout(
                    userId,
                    request.receiverName,
                    request.receiverPhone,
                    request.receiverAddress
            );

            return Response.ok(new ApiResponse<>(200, "订单提交成功", new OrderResponseDTO(order))).build();

        } catch (IllegalArgumentException e) {
            // 捕获普通的业务异常（如：库存不足、购物车为空）
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiResponse<>(400, e.getMessage(), null)).build();

        } catch (Exception e) {
            // 捕获 JPA 的正则拦截异常。事务回滚时，真实异常经常被外层的 RollbackException 包裹，需要剥洋葱
            Throwable cause = e.getCause();
            if (cause instanceof ConstraintViolationException) {
                // 提取我们在实体类里写的 @Pattern 等校验 message
                String errorMsg = ((ConstraintViolationException) cause)
                        .getConstraintViolations().iterator().next().getMessage();
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ApiResponse<>(400, errorMsg, null)).build();
            } else if (e instanceof ConstraintViolationException) {
                String errorMsg = ((ConstraintViolationException) e)
                        .getConstraintViolations().iterator().next().getMessage();
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ApiResponse<>(400, errorMsg, null)).build();
            }

            // 打印未知异常日志防丢失
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ApiResponse<>(500, "服务器内部错误，订单生成失败", null)).build();
        } finally {
            em.close();
        }
    }

    /**
     * 获取当前用户的历史订单列表（包含明细）
     * GET /api/orders
     */
    @GET
    public Response getUserOrders(@HeaderParam("user-id") Long userId) {
        if (userId == null) return unauthorized();

        EntityManager em = JpaUtil.getEntityManager();
        try {
            OrderService orderService = new OrderService(em);
            List<Order> orders = orderService.getUserOrders(userId);

            List<OrderResponseDTO> dtoList = orders.stream()
                    .map(OrderResponseDTO::new)
                    .collect(Collectors.toList());

            return Response.ok(new ApiResponse<>(200, "获取订单列表成功", dtoList)).build();
        } finally {
            em.close();
        }
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ApiResponse<>(401, "请先登录", null)).build();
    }
}