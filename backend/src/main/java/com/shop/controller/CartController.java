package com.shop.controller;

import com.shop.dao.CartDao;
import com.shop.dao.ProductDao;
import com.shop.dto.ApiResponse;
import com.shop.entity.CartItem;
import com.shop.service.CartService;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Path("/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartController {

    // ==========================================
    // 内部 DTO 类定义区（用于防敏脱敏和接收参数）
    // ==========================================

    /**
     * 响应给前端的购物车 DTO（绝对不包含 User 信息和敏感数据）
     */
    public static class CartResponseDTO {
        public Long id;           // 购物车记录ID
        public Long productId;    // 商品ID
        public String productName;// 商品名称
        public BigDecimal price;  // 单价
        public int quantity;      // 购买数量
        public BigDecimal subtotal; // 小计金额（后端计算好，防止前端算错造成显示不一致）

        public CartResponseDTO(CartItem item) {
            this.id = item.getId();
            this.productId = item.getProduct().getId();
            this.productName = item.getProduct().getName();
            this.price = item.getProduct().getPrice();
            this.quantity = item.getQuantity();
            // 小计 = 单价 * 数量
            this.subtotal = this.price.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    /** 接收“加入购物车”参数的请求体 */
    public static class AddCartRequest {
        public Long productId;
        public int quantity;
    }

    /** 接收“修改数量”参数的请求体 */
    public static class UpdateCartRequest {
        public int quantity;
    }


    // ==========================================
    // API 接口定义区
    // ==========================================

    /**
     * 获取当前用户的购物车列表
     * GET /api/cart
     */
    @GET
    public Response getCartList(@HeaderParam("user-id") Long userId) {
        if (userId == null) return unauthorized();

        EntityManager em = JpaUtil.getEntityManager();
        try {
            CartService cartService = new CartService(new CartDao(em), new ProductDao(em));
            List<CartItem> cartItems = cartService.getCartList(userId);

            // 将数据库实体转换为安全的 DTO 列表
            List<CartResponseDTO> dtoList = cartItems.stream()
                    .map(CartResponseDTO::new)
                    .collect(Collectors.toList());

            return Response.ok(new ApiResponse<>(200, "获取购物车成功", dtoList)).build();
        } finally {
            em.close();
        }
    }

    /**
     * 加入购物车
     * POST /api/cart
     */
    @POST
    public Response addToCart(@HeaderParam("user-id") Long userId, AddCartRequest request) {
        if (userId == null) return unauthorized();
        if (request.productId == null || request.quantity <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiResponse<>(400, "参数错误", null)).build();
        }

        EntityManager em = JpaUtil.getEntityManager();
        try {
            CartService cartService = new CartService(new CartDao(em), new ProductDao(em));
            cartService.addToCart(userId, request.productId, request.quantity);
            return Response.ok(new ApiResponse<>(200, "已成功加入购物车", null)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiResponse<>(400, e.getMessage(), null)).build();
        } finally {
            em.close();
        }
    }

    /**
     * 修改购物车内商品数量
     * PUT /api/cart/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateQuantity(
            @HeaderParam("user-id") Long userId,
            @PathParam("id") Long cartItemId,
            UpdateCartRequest request) {

        if (userId == null) return unauthorized();

        EntityManager em = JpaUtil.getEntityManager();
        try {
            CartService cartService = new CartService(new CartDao(em), new ProductDao(em));
            cartService.updateCartItemQuantity(cartItemId, userId, request.quantity);
            return Response.ok(new ApiResponse<>(200, "数量修改成功", null)).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiResponse<>(400, e.getMessage(), null)).build();
        } finally {
            em.close();
        }
    }

    /**
     * 移除购物车记录
     * DELETE /api/cart/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response removeCartItem(@HeaderParam("user-id") Long userId, @PathParam("id") Long cartItemId) {
        if (userId == null) return unauthorized();

        EntityManager em = JpaUtil.getEntityManager();
        try {
            CartService cartService = new CartService(new CartDao(em), new ProductDao(em));
            cartService.removeCartItem(cartItemId, userId);
            return Response.ok(new ApiResponse<>(200, "商品已从购物车移除", null)).build();
        } finally {
            em.close();
        }
    }

    // 统一的无权限拦截响应
    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ApiResponse<>(401, "请先登录", null)).build();
    }
}