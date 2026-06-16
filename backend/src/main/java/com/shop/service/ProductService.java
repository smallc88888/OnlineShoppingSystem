package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dto.PageResultDTO;
import com.shop.entity.Product;

import java.util.List;

public class ProductService {

    private final ProductDao productDao;

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    /**
     * 获取商品分页列表与搜索
     */
    public PageResultDTO<Product> searchProducts(String keyword, int page, int pageSize) {
        // 1. 分页参数的安全兜底（防止前端传乱码或负数）
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10; // 限制单页最大100条，防恶意拉爆内存

        // 2. 搜索关键字的规格校验与处理
        if (keyword != null) {
            // 需求落实：前后空格自动截断
            keyword = keyword.trim();

            // 需求落实：输入为空或全空格时提示
            if (keyword.isEmpty()) {
                throw new IllegalArgumentException("请输入搜索内容");
            }

            // 需求落实：字符串，长度1-50字符
            if (keyword.length() > 50) {
                throw new IllegalArgumentException("搜索关键字不能超过50个字符");
            }
        }

        // 3. 调用 Dao 获取数据
        long total = productDao.countProducts(keyword);
        List<Product> products = productDao.searchProducts(keyword, page, pageSize);

        // 4. 计算总页数 (向上取整：例如 11条数据，每页10条，总页数应为 2)
        int totalPages = (int) Math.ceil((double) total / pageSize);

        // 5. 组装结果
        return new PageResultDTO<>(total, totalPages, page, pageSize, products);
    }
}