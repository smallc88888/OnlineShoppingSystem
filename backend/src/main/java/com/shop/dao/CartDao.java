package com.shop.dao;

import com.shop.entity.CartItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class CartDao {

    private final EntityManager em;

    public CartDao(EntityManager em) {
        this.em = em;
    }

    /**
     * 获取指定用户的所有购物车记录
     */
    public List<CartItem> findByUserId(Long userId) {
        String jpql = "SELECT c FROM CartItem c JOIN FETCH c.product WHERE c.user.id = :userId ORDER BY c.createdAt DESC";
        TypedQuery<CartItem> query = em.createQuery(jpql, CartItem.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    /**
     * 根据用户ID和商品ID查找指定的购物车记录（用于判断是新增还是累加）
     */
    public CartItem findByUserAndProduct(Long userId, Long productId) {
        String jpql = "SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.product.id = :productId";
        TypedQuery<CartItem> query = em.createQuery(jpql, CartItem.class);
        query.setParameter("userId", userId);
        query.setParameter("productId", productId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null; // 没查到说明该商品还没被加入过购物车
        }
    }

    /**
     * 根据主键查找记录
     */
    public CartItem findById(Long cartItemId) {
        return em.find(CartItem.class, cartItemId);
    }

    // --- 增、删、改 的事务性操作 ---

    public void save(CartItem cartItem) {
        em.getTransaction().begin();
        em.persist(cartItem);
        em.getTransaction().commit();
    }

    public void update(CartItem cartItem) {
        em.getTransaction().begin();
        em.merge(cartItem);
        em.getTransaction().commit();
    }

    public void delete(CartItem cartItem) {
        em.getTransaction().begin();
        em.remove(em.contains(cartItem) ? cartItem : em.merge(cartItem));
        em.getTransaction().commit();
    }
}