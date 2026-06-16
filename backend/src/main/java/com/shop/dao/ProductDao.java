package com.shop.dao;

import com.shop.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class ProductDao {

    private final EntityManager em;

    public ProductDao(EntityManager em) {
        this.em = em;
    }

    /**
     * 分页查询与模糊搜索商品
     * * @param keyword  搜索关键字（可为空）
     * @param page     当前页码（从 1 开始）
     * @param pageSize 每页显示的条数
     * @return 当前页的商品列表
     */
    public List<Product> searchProducts(String keyword, int page, int pageSize) {
        // 1. 构建基础 JPQL，强制过滤掉已下架的商品 (isActive = true)
        StringBuilder jpql = new StringBuilder("SELECT p FROM Product p WHERE p.isActive = true");

        // 2. 如果存在关键字，动态追加模糊匹配条件
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword) {
            jpql.append(" AND (p.name LIKE :keyword OR p.description LIKE :keyword)");
        }

        // 3. 默认按上架时间倒序排列（新上架的在前面）
        jpql.append(" ORDER BY p.createdAt DESC");

        TypedQuery<Product> query = em.createQuery(jpql.toString(), Product.class);

        // 4. 绑定参数
        if (hasKeyword) {
            // 使用 % 符号包裹关键字实现模糊搜索
            query.setParameter("keyword", "%" + keyword.trim() + "%");
        }

        // 5. 分页计算与设置
        int offset = (page - 1) * pageSize;
        query.setFirstResult(offset); // 相当于 SQL 中的 OFFSET
        query.setMaxResults(pageSize); // 相当于 SQL 中的 LIMIT

        return query.getResultList();
    }

    /**
     * 统计满足当前搜索条件的商品总数（用于前端计算总页数）
     * * @param keyword 搜索关键字（可为空）
     * @return 满足条件的记录总数
     */
    public long countProducts(String keyword) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(p) FROM Product p WHERE p.isActive = true");

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword) {
            jpql.append(" AND (p.name LIKE :keyword OR p.description LIKE :keyword)");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);

        if (hasKeyword) {
            query.setParameter("keyword", "%" + keyword.trim() + "%");
        }

        return query.getSingleResult();
    }

    /**
     * 根据商品 ID 精准查找
     * @param id 商品主键
     * @return Product 实体，如果不存在则返回 null
     */
    public Product findById(Long id) {
        // 使用 JPA 原生的 find 方法，通过主键直接去缓存或数据库抓取，性能极高
        return em.find(Product.class, id);
    }
}