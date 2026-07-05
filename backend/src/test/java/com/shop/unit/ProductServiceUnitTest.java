package com.shop.unit;

import com.shop.dao.ProductDao;
import com.shop.dto.PageResultDTO;
import com.shop.entity.Product;
import com.shop.service.ProductService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品服务单元测试")
class ProductServiceUnitTest {

    @Mock
    private ProductDao productDao;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("UT-PROD-001 合法关键字应返回分页商品结果")
    void searchProductsReturnsPagedResultWithValidKeyword() {
        Product phone = product(1L, "手机", true);
        when(productDao.countProducts("手机")).thenReturn(1L);
        when(productDao.searchProducts("手机", 1, 10)).thenReturn(List.of(phone));

        PageResultDTO<Product> result = productService.searchProducts("手机", 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(List.of(phone), result.getItems());
    }

    @Test
    @DisplayName("UT-PROD-002 空字符串关键字应提示请输入搜索内容")
    void emptyKeywordFailsSearch() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.searchProducts("", 1, 10)
        );

        assertEquals("请输入搜索内容", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-003 全空格关键字应提示请输入搜索内容")
    void blankKeywordFailsSearch() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.searchProducts("   ", 1, 10)
        );

        assertEquals("请输入搜索内容", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-004 关键字前后空格应自动裁剪后查询")
    void keywordIsTrimmedBeforeSearch() {
        when(productDao.countProducts("手机")).thenReturn(0L);
        when(productDao.searchProducts("手机", 1, 10)).thenReturn(List.of());

        productService.searchProducts(" 手机 ", 1, 10);

        verify(productDao).countProducts("手机");
        verify(productDao).searchProducts("手机", 1, 10);
    }

    @Test
    @DisplayName("UT-PROD-005 关键字长度50应允许查询")
    void keywordWithFiftyCharactersPassesSearch() {
        String keyword = "a".repeat(50);
        when(productDao.countProducts(keyword)).thenReturn(0L);
        when(productDao.searchProducts(keyword, 1, 10)).thenReturn(List.of());

        PageResultDTO<Product> result = productService.searchProducts(keyword, 1, 10);

        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("UT-PROD-006 关键字长度51应拒绝查询")
    void keywordWithFiftyOneCharactersFailsSearch() {
        String keyword = "a".repeat(51);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.searchProducts(keyword, 1, 10)
        );

        assertEquals("搜索关键字不能超过50个字符", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-007 page小于1时应自动修正为1")
    void pageLowerThanOneIsNormalized() {
        when(productDao.countProducts(null)).thenReturn(0L);
        when(productDao.searchProducts(null, 1, 10)).thenReturn(List.of());

        PageResultDTO<Product> result = productService.searchProducts(null, 0, 10);

        assertEquals(1, result.getPage());
        verify(productDao).searchProducts(null, 1, 10);
    }

    @Test
    @DisplayName("UT-PROD-008 pageSize超过100时应自动修正为10")
    void pageSizeGreaterThanOneHundredIsNormalized() {
        when(productDao.countProducts(null)).thenReturn(0L);
        when(productDao.searchProducts(null, 1, 10)).thenReturn(List.of());

        PageResultDTO<Product> result = productService.searchProducts(null, 1, 101);

        assertEquals(10, result.getPageSize());
        verify(productDao).searchProducts(null, 1, 10);
    }

    @Test
    @DisplayName("UT-PROD-009 商品ID为null时查询详情失败")
    void nullProductIdFailsDetailQuery() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProductById(null)
        );

        assertEquals("无效的商品 ID", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-010 商品ID小于等于0时查询详情失败")
    void nonPositiveProductIdFailsDetailQuery() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProductById(0L)
        );

        assertEquals("无效的商品 ID", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-011 商品不存在时查询详情失败")
    void missingProductFailsDetailQuery() {
        when(productDao.findById(1L)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProductById(1L)
        );

        assertEquals("商品不存在或已下架", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-012 商品已下架时查询详情失败")
    void inactiveProductFailsDetailQuery() {
        when(productDao.findById(1L)).thenReturn(product(1L, "手机", false));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProductById(1L)
        );

        assertEquals("商品不存在或已下架", ex.getMessage());
    }

    @Test
    @DisplayName("UT-PROD-013 商品存在且上架时返回商品对象")
    void activeProductCanBeQueriedById() {
        Product product = product(1L, "手机", true);
        when(productDao.findById(1L)).thenReturn(product);

        Product result = productService.getProductById(1L);

        assertSame(product, result);
    }

    private static Product product(Long id, String name, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("这是一条合法的商品描述");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(10);
        product.setActive(active);
        return product;
    }
}
