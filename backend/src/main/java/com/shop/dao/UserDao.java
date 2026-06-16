package com.shop.dao;

import com.shop.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UserDao {

    private final EntityManager em;

    // 💡 课程重点：通过构造函数传入 EntityManager，这被称为“依赖注入 (DI)”。
    // 以后写单元测试时，我们可以直接塞一个连着 H2 内存数据库的 EntityManager 进去，
    // 或者塞一个 Mockito 伪造的对象，完全不需要启动 MySQL！
    public UserDao(EntityManager em) {
        this.em = em;
    }

    /**
     * 保存新用户 (注册)
     * @param user 用户实体
     */
    public void save(User user) {
        try {
            em.getTransaction().begin();
            em.persist(user); // 将对象变为持久态，触发 INSERT 语句
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback(); // 发生异常时回滚事务
            }
            throw new RuntimeException("保存用户失败", e);
        }
    }

    /**
     * 更新用户信息 (例如：更新登录失败次数、锁定时间)
     * @param user 用户实体
     */
    public void update(User user) {
        try {
            em.getTransaction().begin();
            em.merge(user); // 合并对象状态，触发 UPDATE 语句
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("更新用户失败", e);
        }
    }

    /**
     * 根据用户名精准查找用户 (用于登录校验和重名检测)
     * @param username 用户名
     * @return User对象，如果不存在则返回 null
     */
    public User findByUsername(String username) {
        // 使用 JPQL (Java Persistence Query Language)，它是面向对象的 SQL
        String jpql = "SELECT u FROM User u WHERE u.username = :username";
        TypedQuery<User> query = em.createQuery(jpql, User.class);
        query.setParameter("username", username);

        try {
            // getSingleResult() 查不到数据时会抛出异常，这是 JPA 的规范标准
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null; // 查不到说明用户名未被注册，返回 null 给业务层判断
        }
    }
}