package com.shop.service;

import com.shop.dao.CartDao;
import com.shop.dao.ProductDao;
import com.shop.entity.CartItem;
import com.shop.entity.Product;
import com.shop.entity.User;

import java.util.List;

public class CartService {

    private final CartDao cartDao;
    private final ProductDao productDao;

    public CartService(CartDao cartDao, ProductDao productDao) {
        this.cartDao = cartDao;
        this.productDao = productDao;
    }

    /**
     * 核心流程：加入购物车
     * @param userId 当前操作用户
     * @param productId 被添加的商品
     * @param quantity 添加数量
     */
    public void addToCart(Long userId, Long productId, int quantity) {
        // 1. 基础边界防线：单次传入的数据合法性
        if (quantity < 1 || quantity > 99) {
            throw new IllegalArgumentException("购买数量必须在 1 到 99 之间");
        }

        // 2. 验证商品状态（复用 ProductDao）
        Product product = productDao.findById(productId);
        if (product == null || !product.isActive()) {
            throw new IllegalArgumentException("该商品不存在或已下架，无法加入购物车");
        }

        // 3. 检查是否已经存在该记录
        CartItem existingItem = cartDao.findByUserAndProduct(userId, productId);

        if (existingItem != null) {
            // 情况 A：商品已在购物车，走“累加”逻辑
            int newQuantity = existingItem.getQuantity() + quantity;

            // 累加后的双重边界拦截
            if (newQuantity > 99) {
                throw new IllegalArgumentException("购物车内该商品总量不能超过 99 件");
            }
            if (newQuantity > product.getStock()) {
                throw new IllegalArgumentException("库存不足，当前库存仅剩 " + product.getStock() + " 件");
            }

            existingItem.setQuantity(newQuantity);
            cartDao.update(existingItem);

        } else {
            // 情况 B：首次加入购物车，走“新增”逻辑
            if (quantity > product.getStock()) {
                throw new IllegalArgumentException("库存不足，当前库存仅剩 " + product.getStock() + " 件");
            }

            CartItem newItem = new CartItem();

            // 构建关联实体对象
            User userReference = new User();
            userReference.setId(userId);
            newItem.setUser(userReference);

            newItem.setProduct(product);
            newItem.setQuantity(quantity);

            cartDao.save(newItem);
        }
    }

    /**
     * 核心流程：直接修改购物车内某项商品的数量
     */
    public void updateCartItemQuantity(Long cartItemId, Long userId, int newQuantity) {
        CartItem cartItem = cartDao.findById(cartItemId);

        if (cartItem == null || !cartItem.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("购物车记录不存在或无权操作");
        }

        Product product = cartItem.getProduct();
        if (!product.isActive()) {
            throw new IllegalArgumentException("该商品已下架，无法修改数量");
        }

        // 依据需求说明书：修改数量为 0 时触发移除逻辑通常在前端拦截弹窗，
        // 但如果后端真收到 0，我们需要抛错或者直接将其删除。
        // 这里采用严格模式，要求传入合法数量。前端点确认删除后，应调用单独的 DELETE 接口。
        if (newQuantity < 1 || newQuantity > 99) {
            throw new IllegalArgumentException("修改后的数量必须在 1 到 99 之间");
        }
        if (newQuantity > product.getStock()) {
            throw new IllegalArgumentException("库存不足，当前库存仅剩 " + product.getStock() + " 件");
        }

        cartItem.setQuantity(newQuantity);
        cartDao.update(cartItem);
    }

    /**
     * 删除购物车记录
     */
    public void removeCartItem(Long cartItemId, Long userId) {
        CartItem cartItem = cartDao.findById(cartItemId);
        if (cartItem != null && cartItem.getUser().getId().equals(userId)) {
            cartDao.delete(cartItem);
        }
    }

    /**
     * 获取购物车列表
     */
    public List<CartItem> getCartList(Long userId) {
        return cartDao.findByUserId(userId);
    }
}