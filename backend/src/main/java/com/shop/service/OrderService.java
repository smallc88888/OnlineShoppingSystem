package com.shop.service;

import com.shop.dao.CartDao;
import com.shop.dao.OrderDao;
import com.shop.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final EntityManager em;

    public OrderService(EntityManager em) {
        this.em = em;
    }

    /**
     * 核心流程：执行订单结算的强一致性原子操作
     */
    public Order checkout(Long userId, String receiverName, String receiverPhone, String receiverAddress) {
        // 开启数据库事务控制器
        EntityTransaction tx = em.getTransaction();

        try {
            // 事务开始：以下所有数据库操作，要么全部成功，要么全部撤销
            tx.begin();

            CartDao cartDao = new CartDao(em);
            List<CartItem> cartItems = cartDao.findByUserId(userId);

            // 1. 拦截空购物车结算
            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("购物车中没有商品，无法结算");
            }

            // 2. 初始化订单基础信息
            Order order = new Order();
            // 简单生成唯一订单号：ORD + 时间戳 + 随机数 (或者使用 UUID 的前8位)
            order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());

            User userRef = new User();
            userRef.setId(userId);
            order.setUser(userRef);

            // 注入收货信息（这里的格式如果不符合你的正则，会在 persist 时抛出异常被 catch 捕获并回滚）
            order.setReceiverName(receiverName);
            order.setReceiverPhone(receiverPhone);
            order.setReceiverAddress(receiverAddress);

            BigDecimal totalAmount = BigDecimal.ZERO;

            // 3. 循环处理商品：防超卖库存拦截 + 生成快照明细
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                int buyQuantity = cartItem.getQuantity();

                // 核心防线：查验真实库存与下架状态
                if (!product.isActive()) {
                    throw new IllegalArgumentException("商品 [" + product.getName() + "] 已下架，请将其移出购物车后重试");
                }
                if (product.getStock() < buyQuantity) {
                    throw new IllegalArgumentException("商品 [" + product.getName() + "] 库存不足，当前仅剩 " + product.getStock() + " 件");
                }

                // 物理扣减库存并更新到实体
                product.setStock(product.getStock() - buyQuantity);
                em.merge(product);

                // 生成不可变的明细快照
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setProductName(product.getName());
                orderItem.setBuyPrice(product.getPrice());
                orderItem.setQuantity(buyQuantity);

                // 累加总金额
                BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(buyQuantity));
                totalAmount = totalAmount.add(subTotal);

                // 将明细挂载到主订单上
                order.getItems().add(orderItem);

                // 4. 清除该条购物车记录
                em.remove(cartItem);
            }

            // 设置总金额
            order.setTotalAmount(totalAmount);

            // 5. 将订单主表（连同挂载的明细）一次性持久化到数据库
            em.persist(order);

            // 事务提交：所有操作确认无误，正式写入物理磁盘
            tx.commit();

            return order;

        } catch (Exception e) {
            // 灾难恢复：只要中间发生任何报错（库存不足、姓名正则不通过），立即回滚所有操作
            if (tx.isActive()) {
                tx.rollback();
            }
            // 将异常继续向上抛，交给 Controller 转化为 HTTP 400 返回给前端
            throw e;
        }
    }

    /**
     * 获取历史订单列表
     */
    public List<Order> getUserOrders(Long userId) {
        return new OrderDao(em).findByUserId(userId);
    }
}