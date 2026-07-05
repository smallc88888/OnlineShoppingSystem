package com.shop.unit;

import com.shop.entity.CartItem;
import com.shop.entity.Order;
import com.shop.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("实体字段校验单元测试")
class EntityValidationUnitTest extends UnitTestValidationSupport {

    @Test
    @DisplayName("UT-ADMIN-PROD-001 商品名称长度1应校验失败")
    void productNameWithOneCharacterFailsValidation() {
        Product product = validProduct();
        product.setName("A");

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-002 商品名称长度2应校验通过")
    void productNameWithTwoCharactersPassesValidation() {
        Product product = validProduct();
        product.setName("手机");

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-003 商品名称长度50应校验通过")
    void productNameWithFiftyCharactersPassesValidation() {
        Product product = validProduct();
        product.setName("A".repeat(50));

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-004 商品名称长度51应校验失败")
    void productNameWithFiftyOneCharactersFailsValidation() {
        Product product = validProduct();
        product.setName("A".repeat(51));

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-005 商品名称全空格应校验失败")
    void blankProductNameFailsValidation() {
        Product product = validProduct();
        product.setName("   ");

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-006 商品描述长度9应校验失败")
    void productDescriptionWithNineCharactersFailsValidation() {
        Product product = validProduct();
        product.setDescription("a".repeat(9));

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-007 商品描述长度10应校验通过")
    void productDescriptionWithTenCharactersPassesValidation() {
        Product product = validProduct();
        product.setDescription("a".repeat(10));

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-008 商品价格为0应校验失败")
    void productPriceZeroFailsValidation() {
        Product product = validProduct();
        product.setPrice(BigDecimal.ZERO);

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-009 商品价格为0.01应校验通过")
    void productPriceMinimumPassesValidation() {
        Product product = validProduct();
        product.setPrice(new BigDecimal("0.01"));

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-010 商品价格为999999.99应校验通过")
    void productPriceMaximumPassesValidation() {
        Product product = validProduct();
        product.setPrice(new BigDecimal("999999.99"));

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-011 商品价格为1000000.00应校验失败")
    void productPriceAboveMaximumFailsValidation() {
        Product product = validProduct();
        product.setPrice(new BigDecimal("1000000.00"));

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-012 商品价格小数超过2位应校验失败")
    void productPriceWithMoreThanTwoFractionDigitsFailsValidation() {
        Product product = validProduct();
        product.setPrice(new BigDecimal("99.999"));

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-013 库存为-1应校验失败")
    void productStockNegativeFailsValidation() {
        Product product = validProduct();
        product.setStock(-1);

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-014 库存为0应校验通过")
    void productStockZeroPassesValidation() {
        Product product = validProduct();
        product.setStock(0);

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-015 库存为99999应校验通过")
    void productStockMaximumPassesValidation() {
        Product product = validProduct();
        product.setStock(99999);

        assertTrue(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ADMIN-PROD-016 库存为100000应校验失败")
    void productStockAboveMaximumFailsValidation() {
        Product product = validProduct();
        product.setStock(100000);

        assertFalse(validate(product).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-001 中文收货人姓名应校验通过")
    void chineseReceiverNamePassesValidation() {
        Order order = validOrder();
        order.setReceiverName("张三");

        assertTrue(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-002 英文收货人姓名应校验通过")
    void englishReceiverNamePassesValidation() {
        Order order = validOrder();
        order.setReceiverName("Tom");

        assertTrue(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-003 收货人姓名长度1应校验失败")
    void receiverNameWithOneCharacterFailsValidation() {
        Order order = validOrder();
        order.setReceiverName("张");

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-004 收货人姓名长度2应校验通过")
    void receiverNameWithTwoCharactersPassesValidation() {
        Order order = validOrder();
        order.setReceiverName("张三");

        assertTrue(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-005 收货人姓名包含数字应校验失败")
    void receiverNameWithDigitsFailsValidation() {
        Order order = validOrder();
        order.setReceiverName("张三123");

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-006 合法手机号应校验通过")
    void validPhonePassesValidation() {
        Order order = validOrder();
        order.setReceiverPhone("13812345678");

        assertTrue(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-007 手机号10位应校验失败")
    void phoneWithTenDigitsFailsValidation() {
        Order order = validOrder();
        order.setReceiverPhone("1381234567");

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-008 手机号12位应校验失败")
    void phoneWithTwelveDigitsFailsValidation() {
        Order order = validOrder();
        order.setReceiverPhone("138123456789");

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-009 非13-19号段手机号应校验失败")
    void phoneWithInvalidPrefixFailsValidation() {
        Order order = validOrder();
        order.setReceiverPhone("12812345678");

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-010 地址长度9应校验失败")
    void addressWithNineCharactersFailsValidation() {
        Order order = validOrder();
        order.setReceiverAddress("a".repeat(9));

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-011 地址长度10应校验通过")
    void addressWithTenCharactersPassesValidation() {
        Order order = validOrder();
        order.setReceiverAddress("a".repeat(10));

        assertTrue(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-012 地址长度100应校验通过")
    void addressWithOneHundredCharactersPassesValidation() {
        Order order = validOrder();
        order.setReceiverAddress("a".repeat(100));

        assertTrue(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-ORDER-013 地址长度101应校验失败")
    void addressWithOneHundredOneCharactersFailsValidation() {
        Order order = validOrder();
        order.setReceiverAddress("a".repeat(101));

        assertFalse(validate(order).isEmpty());
    }

    @Test
    @DisplayName("UT-CART-018 购物车实体数量为0应校验失败")
    void cartItemQuantityZeroFailsValidation() {
        CartItem item = validCartItem();
        item.setQuantity(0);

        assertFalse(validate(item).isEmpty());
    }

    @Test
    @DisplayName("UT-CART-019 购物车实体数量为1应校验通过")
    void cartItemQuantityOnePassesValidation() {
        CartItem item = validCartItem();
        item.setQuantity(1);

        assertTrue(validate(item).isEmpty());
    }

    @Test
    @DisplayName("UT-CART-020 购物车实体数量为99应校验通过")
    void cartItemQuantityNinetyNinePassesValidation() {
        CartItem item = validCartItem();
        item.setQuantity(99);

        assertTrue(validate(item).isEmpty());
    }

    @Test
    @DisplayName("UT-CART-021 购物车实体数量为100应校验失败")
    void cartItemQuantityOneHundredFailsValidation() {
        CartItem item = validCartItem();
        item.setQuantity(100);

        assertFalse(validate(item).isEmpty());
    }

    private static Product validProduct() {
        Product product = new Product();
        product.setName("测试商品");
        product.setDescription("这是一条合法商品描述");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(10);
        product.setActive(true);
        return product;
    }

    private static Order validOrder() {
        Order order = new Order();
        order.setOrderNo("ORD202606180001");
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setReceiverName("张三");
        order.setReceiverPhone("13812345678");
        order.setReceiverAddress("北京市海淀区测试地址100号");
        return order;
    }

    private static CartItem validCartItem() {
        CartItem item = new CartItem();
        item.setQuantity(1);
        item.setProduct(validProduct());
        return item;
    }
}
