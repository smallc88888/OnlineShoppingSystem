package com.shop.controller;

import com.shop.dao.ProductDao;
import com.shop.dto.ApiResponse;
import com.shop.entity.Product;
import com.shop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;

@Path("/admin/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminProductController {

    /**
     * 接收新增/修改商品参数的 DTO
     */
    public static class AdminProductRequest {
        public String name;
        public String description;
        public BigDecimal price;
        public Integer stock;
        public String imageUrl;
    }

    /**
     * 新增商品
     * POST /api/admin/products
     */
    @POST
    public Response addProduct(AdminProductRequest request) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Product product = new Product();
            product.setName(request.name);
            product.setDescription(request.description);
            product.setPrice(request.price);
            product.setStock(request.stock);
            // 这里为了防空指针，如果没有传图片，给个默认占位图或留空
            /*
            product.setImageUrl(request.imageUrl != null ? request.imageUrl : "");
            */
            ProductDao productDao = new ProductDao(em);
            productDao.save(product);

            return Response.ok(new ApiResponse<>(200, "商品上架成功", product)).build();

        } catch (Exception e) {
            return handleValidationException(e);
        } finally {
            em.close();
        }
    }

    /**
     * 编辑商品
     * PUT /api/admin/products/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateProduct(@PathParam("id") Long id, AdminProductRequest request) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            ProductDao productDao = new ProductDao(em);
            Product product = productDao.findById(id);

            if (product == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiResponse<>(404, "该商品不存在", null)).build();
            }

            product.setName(request.name);
            product.setDescription(request.description);
            product.setPrice(request.price);
            product.setStock(request.stock);
            /*
            if (request.imageUrl != null) {
                product.setImageUrl(request.imageUrl);
            }
            */
            productDao.update(product);

            return Response.ok(new ApiResponse<>(200, "商品修改成功", product)).build();

        } catch (Exception e) {
            return handleValidationException(e);
        } finally {
            em.close();
        }
    }

    /**
     * 下架商品 (软删除)
     * DELETE /api/admin/products/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deactivateProduct(@PathParam("id") Long id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            ProductDao productDao = new ProductDao(em);
            Product product = productDao.findById(id);

            if (product == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiResponse<>(404, "该商品不存在", null)).build();
            }

            // 执行软删除，阻断前端查询
            product.setActive(false);
            productDao.update(product);

            return Response.ok(new ApiResponse<>(200, "商品已下架", null)).build();

        } finally {
            em.close();
        }
    }

    /**
     * 统一处理 JPA 的正则与边界异常
     */
    private Response handleValidationException(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException) {
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

        e.printStackTrace();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiResponse<>(500, "服务器内部错误，操作失败", null)).build();
    }

    /**
     * 获取全量商品列表（后台视图）
     * GET /api/admin/products
     */
    @GET
    public Response getAllProducts() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            ProductDao productDao = new ProductDao(em);
            List<Product> products = productDao.findAllForAdmin();
            return Response.ok(new ApiResponse<>(200, "获取商品列表成功", products)).build();
        } finally {
            em.close();
        }
    }
}