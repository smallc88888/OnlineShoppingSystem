package com.shop.unit;

import com.shop.dao.CartDao;
import com.shop.dao.ProductDao;
import com.shop.entity.CartItem;
import com.shop.entity.Product;
import com.shop.entity.User;
import com.shop.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("购物车服务单元测试")
class CartServiceUnitTest {

    @Mock
    private CartDao cartDao;

    @Mock
    private ProductDao productDao;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("UT-CART-001 加购数量为0应失败")
    void addToCartFailsWhenQuantityIsZero() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 0)
        );

        assertEquals("购买数量必须在 1 到 99 之间", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-002 加购数量为1且库存充足应成功")
    void addToCartSucceedsWithQuantityOne() {
        Product product = product(1L, 10, true);
        when(productDao.findById(1L)).thenReturn(product);
        when(cartDao.findByUserAndProduct(1L, 1L)).thenReturn(null);

        cartService.addToCart(1L, 1L, 1);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartDao).save(captor.capture());
        assertEquals(1, captor.getValue().getQuantity());
        assertEquals(1L, captor.getValue().getUser().getId());
        assertSame(product, captor.getValue().getProduct());
    }

    @Test
    @DisplayName("UT-CART-003 加购数量为99且库存充足应成功")
    void addToCartSucceedsWithQuantityNinetyNine() {
        Product product = product(1L, 99, true);
        when(productDao.findById(1L)).thenReturn(product);
        when(cartDao.findByUserAndProduct(1L, 1L)).thenReturn(null);

        cartService.addToCart(1L, 1L, 99);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartDao).save(captor.capture());
        assertEquals(99, captor.getValue().getQuantity());
    }

    @Test
    @DisplayName("UT-CART-004 加购数量为100应失败")
    void addToCartFailsWhenQuantityIsOneHundred() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 100)
        );

        assertEquals("购买数量必须在 1 到 99 之间", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-005 商品不存在时不能加入购物车")
    void addToCartFailsWhenProductDoesNotExist() {
        when(productDao.findById(1L)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 1)
        );

        assertEquals("该商品不存在或已下架，无法加入购物车", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-006 商品已下架时不能加入购物车")
    void addToCartFailsWhenProductIsInactive() {
        when(productDao.findById(1L)).thenReturn(product(1L, 10, false));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 1)
        );

        assertEquals("该商品不存在或已下架，无法加入购物车", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-007 首次加购数量超过库存应失败")
    void addToCartFailsWhenNewQuantityExceedsStock() {
        when(productDao.findById(1L)).thenReturn(product(1L, 2, true));
        when(cartDao.findByUserAndProduct(1L, 1L)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 3)
        );

        assertEquals("库存不足，当前库存仅剩 2 件", ex.getMessage());
        verify(cartDao, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("UT-CART-009 已有商品再次加入时应累加数量")
    void addToCartUpdatesExistingItemQuantity() {
        Product product = product(1L, 10, true);
        CartItem existing = cartItem(10L, 1L, product, 3);
        when(productDao.findById(1L)).thenReturn(product);
        when(cartDao.findByUserAndProduct(1L, 1L)).thenReturn(existing);

        cartService.addToCart(1L, 1L, 2);

        assertEquals(5, existing.getQuantity());
        verify(cartDao).update(existing);
        verify(cartDao, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("UT-CART-010 累加后总量超过99应失败")
    void addToCartFailsWhenAccumulatedQuantityExceedsNinetyNine() {
        Product product = product(1L, 200, true);
        CartItem existing = cartItem(10L, 1L, product, 98);
        when(productDao.findById(1L)).thenReturn(product);
        when(cartDao.findByUserAndProduct(1L, 1L)).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 2)
        );

        assertEquals("购物车内该商品总量不能超过 99 件", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-011 累加后总量超过库存应失败")
    void addToCartFailsWhenAccumulatedQuantityExceedsStock() {
        Product product = product(1L, 4, true);
        CartItem existing = cartItem(10L, 1L, product, 3);
        when(productDao.findById(1L)).thenReturn(product);
        when(cartDao.findByUserAndProduct(1L, 1L)).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addToCart(1L, 1L, 2)
        );

        assertEquals("库存不足，当前库存仅剩 4 件", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-012 修改不属于当前用户的购物车项应失败")
    void updateQuantityFailsWhenCartItemBelongsToAnotherUser() {
        Product product = product(1L, 10, true);
        when(cartDao.findById(10L)).thenReturn(cartItem(10L, 2L, product, 1));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateCartItemQuantity(10L, 1L, 2)
        );

        assertEquals("购物车记录不存在或无权操作", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-013 修改数量为0应失败")
    void updateQuantityFailsWhenNewQuantityIsZero() {
        Product product = product(1L, 10, true);
        when(cartDao.findById(10L)).thenReturn(cartItem(10L, 1L, product, 1));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateCartItemQuantity(10L, 1L, 0)
        );

        assertEquals("修改后的数量必须在 1 到 99 之间", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-014 修改数量为99且库存充足应成功")
    void updateQuantitySucceedsWithNinetyNineWhenStockIsEnough() {
        Product product = product(1L, 99, true);
        CartItem item = cartItem(10L, 1L, product, 1);
        when(cartDao.findById(10L)).thenReturn(item);

        cartService.updateCartItemQuantity(10L, 1L, 99);

        assertEquals(99, item.getQuantity());
        verify(cartDao).update(item);
    }

    @Test
    @DisplayName("UT-CART-015 修改数量超过库存应失败")
    void updateQuantityFailsWhenNewQuantityExceedsStock() {
        Product product = product(1L, 5, true);
        CartItem item = cartItem(10L, 1L, product, 1);
        when(cartDao.findById(10L)).thenReturn(item);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateCartItemQuantity(10L, 1L, 6)
        );

        assertEquals("库存不足，当前库存仅剩 5 件", ex.getMessage());
    }

    @Test
    @DisplayName("UT-CART-016 当前用户拥有购物车项时删除成功")
    void removeCartItemDeletesWhenOwnedByCurrentUser() {
        Product product = product(1L, 5, true);
        CartItem item = cartItem(10L, 1L, product, 1);
        when(cartDao.findById(10L)).thenReturn(item);

        cartService.removeCartItem(10L, 1L);

        verify(cartDao).delete(item);
    }

    @Test
    @DisplayName("UT-CART-017 当前用户不拥有购物车项时不删除")
    void removeCartItemDoesNothingWhenNotOwnedByCurrentUser() {
        Product product = product(1L, 5, true);
        CartItem item = cartItem(10L, 2L, product, 1);
        when(cartDao.findById(10L)).thenReturn(item);

        cartService.removeCartItem(10L, 1L);

        verify(cartDao, never()).delete(any(CartItem.class));
    }

    @Test
    @DisplayName("获取购物车列表应调用指定用户ID查询")
    void getCartListDelegatesToDao() {
        List<CartItem> items = List.of(cartItem(10L, 1L, product(1L, 5, true), 1));
        when(cartDao.findByUserId(1L)).thenReturn(items);

        assertSame(items, cartService.getCartList(1L));
    }

    private static Product product(Long id, int stock, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName("测试商品");
        product.setDescription("这是一条合法商品描述");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(stock);
        product.setActive(active);
        return product;
    }

    private static CartItem cartItem(Long id, Long userId, Product product, int quantity) {
        User user = new User();
        user.setId(userId);

        CartItem item = new CartItem();
        item.setId(id);
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}
