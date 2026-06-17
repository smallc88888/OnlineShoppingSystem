package com.shop.dao;

import com.shop.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class OrderDao {

    private final EntityManager em;

    public OrderDao(EntityManager em) {
        this.em = em;
    }

    /**
     * 获取指定用户的历史订单（按时间倒序，附带抓取订单明细以防 N+1 性能问题）
     */
    public List<Order> findByUserId(Long userId) {
        String jpql = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.user.id = :userId ORDER BY o.createdAt DESC";
        TypedQuery<Order> query = em.createQuery(jpql, Order.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    /**
     * 根据 ID 查找单笔订单详情
     */
    public Order findById(Long orderId) {
        return em.find(Order.class, orderId);
    }

    /**
     * 获取全站所有历史订单（后台管理员使用）
     */
    public List<Order> findAllOrders() {
        // 同样使用 FETCH 抓取明细，防止 N+1 性能雪崩
        String jpql = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items ORDER BY o.createdAt DESC";
        return em.createQuery(jpql, Order.class).getResultList();
    }

    /**
     * 更新订单状态
     */
    public void update(Order order) {
        em.getTransaction().begin();
        em.merge(order);
        em.getTransaction().commit();
    }
}