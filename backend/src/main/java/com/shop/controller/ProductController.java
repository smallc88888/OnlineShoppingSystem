package com.shop.controller;

import com.shop.dao.ProductDao;
import com.shop.dto.ApiResponse;
import com.shop.dto.PageResultDTO;
import com.shop.entity.Product;
import com.shop.service.ProductService;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/products") // 路由前缀: /api/products
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {

    /**
     * 商品分页列表与搜索接口
     * GET /api/products?keyword=xxx&page=1&pageSize=10
     */
    @GET
    public Response getProducts(
            @QueryParam("keyword") String keyword,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize) {

        // 1. 获取持久化管理器
        EntityManager em = JpaUtil.getEntityManager();
        try {
            // 2. 依赖注入组合对象
            ProductDao productDao = new ProductDao(em);
            ProductService productService = new ProductService(productDao);

            // 3. 执行带有规格校验的业务逻辑
            PageResultDTO<Product> pageResult = productService.searchProducts(keyword, page, pageSize);

            // 4. 返回标准响应体
            ApiResponse<PageResultDTO<Product>> response = new ApiResponse<>(200, "获取商品列表成功", pageResult);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            // 拦截业务异常：如用户输入全空格或关键字超长（1-50字符边界），返回 400 状态码
            ApiResponse<Void> response = new ApiResponse<>(400, e.getMessage(), null);
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        } finally {
            // 5. 严格释放连接，防止压测或多用户并发时连接池耗尽
            em.close();
        }
    }

    /**
     * 商品详情接口
     * GET /api/products/{id}
     */
    @GET
    @Path("/{id}")
    public Response getProductDetail(@PathParam("id") Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            ProductDao productDao = new ProductDao(em);
            ProductService productService = new ProductService(productDao);

            Product product = productService.getProductById(id);

            ApiResponse<Product> response = new ApiResponse<>(200, "获取商品详情成功", product);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            // 捕获“商品不存在或已下架”异常 (404 Not Found 更符合语义)
            ApiResponse<Void> response = new ApiResponse<>(404, e.getMessage(), null);
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        } finally {
            em.close();
        }
    }
}