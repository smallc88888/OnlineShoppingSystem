package com.shop.unit;

import com.shop.entity.CartItem;
import com.shop.entity.Order;
import com.shop.entity.Product;
import com.shop.entity.User;
import com.shop.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务单元测试")
class OrderServiceUnitTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityTransaction transaction;

    @Mock
    private TypedQuery<CartItem> cartItemQuery;

    @Test
    @DisplayName("UT-ORDER-014 购物车为空时结算应失败并回滚事务")
    void checkoutFailsAndRollsBackWhenCartIsEmpty() {
        stubTransaction();
        stubCartItems(List.of());
        when(transaction.isActive()).thenReturn(true);

        OrderService orderService = new OrderService(entityManager);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(1L, "张三", "13812345678", "北京市海淀区测试地址100号")
        );

        assertEquals("购物车中没有商品，无法结算", ex.getMessage());
        verify(transaction).begin();
        verify(transaction).rollback();
        verify(transaction, never()).commit();
    }

    @Test
    @DisplayName("UT-ORDER-015 购物车商品已下架时结算应失败并回滚事务")
    void checkoutFailsAndRollsBackWhenProductIsInactive() {
        Product product = product("测试商品", new BigDecimal("99.99"), 10, false);
        CartItem item = cartItem(1L, product, 1);
        stubTransaction();
        stubCartItems(List.of(item));
        when(transaction.isActive()).thenReturn(true);

        OrderService orderService = new OrderService(entityManager);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(1L, "张三", "13812345678", "北京市海淀区测试地址100号")
        );

        assertEquals("商品 [测试商品] 已下架，请将其移出购物车后重试", ex.getMessage());
        verify(transaction).rollback();
        verify(entityManager, never()).persist(any(Order.class));
    }

    @Test
    @DisplayName("UT-ORDER-016 商品库存不足时结算应失败并回滚事务")
    void checkoutFailsAndRollsBackWhenStockIsInsufficient() {
        Product product = product("测试商品", new BigDecimal("99.99"), 1, true);
        CartItem item = cartItem(1L, product, 2);
        stubTransaction();
        stubCartItems(List.of(item));
        when(transaction.isActive()).thenReturn(true);

        OrderService orderService = new OrderService(entityManager);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(1L, "张三", "13812345678", "北京市海淀区测试地址100号")
        );

        assertEquals("商品 [测试商品] 库存不足，当前仅剩 1 件", ex.getMessage());
        verify(transaction).rollback();
        verify(entityManager, never()).persist(any(Order.class));
    }

    @Test
    @DisplayName("UT-ORDER-017 库存充足时结算应生成订单、扣减库存并清空购物车")
    void checkoutCreatesOrderDeductsStockAndClearsCartWhenValid() {
        Product product = product("测试商品", new BigDecimal("99.99"), 10, true);
        CartItem item = cartItem(1L, product, 2);
        stubTransaction();
        stubCartItems(List.of(item));

        OrderService orderService = new OrderService(entityManager);

        Order order = orderService.checkout(1L, "张三", "13812345678", "北京市海淀区测试地址100号");

        assertNotNull(order.getOrderNo());
        assertEquals(0, order.getStatus());
        assertEquals(new BigDecimal("199.98"), order.getTotalAmount());
        assertEquals(1, order.getItems().size());
        assertEquals("测试商品", order.getItems().get(0).getProductName());
        assertEquals(2, order.getItems().get(0).getQuantity());
        assertEquals(8, product.getStock());
        verify(entityManager).merge(product);
        verify(entityManager).remove(item);
        verify(entityManager).persist(order);
        verify(transaction).commit();
    }

    @Test
    @DisplayName("UT-ORDER-019 持久化订单异常时应回滚事务")
    void checkoutRollsBackWhenPersistThrowsException() {
        Product product = product("测试商品", new BigDecimal("99.99"), 10, true);
        CartItem item = cartItem(1L, product, 1);
        stubTransaction();
        stubCartItems(List.of(item));
        when(transaction.isActive()).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("persist failed"))
                .when(entityManager).persist(any(Order.class));

        OrderService orderService = new OrderService(entityManager);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> orderService.checkout(1L, "张三", "13812345678", "北京市海淀区测试地址100号")
        );

        assertEquals("persist failed", ex.getMessage());
        verify(transaction).rollback();
    }

    private void stubTransaction() {
        when(entityManager.getTransaction()).thenReturn(transaction);
    }

    private void stubCartItems(List<CartItem> items) {
        when(entityManager.createQuery(anyString(), eq(CartItem.class))).thenReturn(cartItemQuery);
        when(cartItemQuery.setParameter("userId", 1L)).thenReturn(cartItemQuery);
        when(cartItemQuery.getResultList()).thenReturn(items);
    }

    private static Product product(String name, BigDecimal price, int stock, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("这是一条合法商品描述");
        product.setPrice(price);
        product.setStock(stock);
        product.setActive(active);
        return product;
    }

    private static CartItem cartItem(Long userId, Product product, int quantity) {
        User user = new User();
        user.setId(userId);

        CartItem item = new CartItem();
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}
