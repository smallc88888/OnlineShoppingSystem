package com.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", catalog = "shopping_system")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 规格：2-50字符长度，支持中英文、数字，不允许纯空格
    @NotBlank(message = "商品名称不能为空或纯空格")
    @Size(min = 2, max = 50, message = "商品名称长度必须在2到50个字符之间")
    @Column(nullable = false, length = 50)
    private String name;

    // 规格：10-500字符长度，支持中英文以及常见标点
    @NotBlank(message = "商品描述不能为空")
    @Size(min = 10, max = 500, message = "商品描述长度必须在10到500个字符之间")
    @Column(nullable = false, length = 500)
    private String description;

    // 规格：数值范围0.01-999,999.99，最多保留两位小数
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格不能低于0.01")
    @DecimalMax(value = "999999.99", message = "商品价格不能高于999,999.99")
    @Digits(integer = 6, fraction = 2, message = "价格格式错误，最多允许6位整数和2位小数")
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    // 规格：整数，0-99,999，要求非负
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 99999, message = "库存数量不能超过99,999")
    @Column(nullable = false)
    private Integer stock; // 使用包装类 Integer，配合 @NotNull 进行严谨的空指针校验

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}