package com.shop.selenium;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class AdminProductTest extends EdgeBaseTest {

    // ========== 辅助方法：打开新增弹窗并填写 ==========
    private void openAddModalAndFill(String name, String desc, String price, String stock) {
        WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".add-btn")));
        addBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-content")));

        WebElement nameInput = driver.findElement(By.cssSelector("input[type='text']"));
        nameInput.clear();
        if (name != null) nameInput.sendKeys(name);

        WebElement descArea = driver.findElement(By.tagName("textarea"));
        descArea.clear();
        if (desc != null) descArea.sendKeys(desc);

        List<WebElement> numberInputs = driver.findElements(By.cssSelector("input[type='number']"));
        numberInputs.get(0).clear(); numberInputs.get(0).sendKeys(price);
        numberInputs.get(1).clear(); numberInputs.get(1).sendKeys(stock);
    }

    /**
     * 等待 alert 出现并获取文本，然后关闭 alert。
     * 如果超时没有 alert，返回 null。
     */
    private String getAlertTextAndAccept() {
        try {
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            String text = alert.getText();
            alert.accept();
            return text;
        } catch (TimeoutException e) {
            return null;
        }
    }

    /**
     * 忽略 alert，不管是否出现都直接关闭（用于成功保存后清理）
     */
    private void acceptAlertIfPresent() {
        try {
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            alert.accept();
        } catch (TimeoutException ignored) {
            // 没有 alert 也不影响
        }
    }

    /** 进入后台商品管理页 */
    private void goToAdminProducts() {
        driver.get("http://localhost:5173/admin/products");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".add-btn")));
    }

    // ================================================================
    // 以下每个 @Test 对应一个边界测试用例
    // ================================================================

    // ------------------- 名称边界 -------------------
    @Test
    @DisplayName("PM-007: 名称2字符 - 新增成功")
    public void testName_Min_2Chars() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("AB", "这是一个合法的描述十个字以上", "10", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();  // 处理可能弹出的成功提示

        // 弹窗应关闭，商品应出现在列表中
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'AB')]")));
    }

    @Test
    @DisplayName("PM-009: 名称1字符 - 被拒绝")
    public void testName_TooShort_1Char() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("A", "这是一个合法的描述十个字以上", "10", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        String alertText = getAlertTextAndAccept();
        assertEquals("名称必须在2-50字符之间", alertText, "应弹出名称长度提示");
        // 弹窗应仍存在
        assertTrue(driver.findElement(By.cssSelector(".modal-overlay")).isDisplayed());
    }

    @Test
    @DisplayName("PM-008:名称50字符 - 新增成功")
    public void testName_Max_50Chars() {
        injectAdmin();
        goToAdminProducts();
        String name50 = "这是一个刚好五十个字符的商品名称用于边界测试一二三四五六七八九十一二三四五六七八九十一二三四五六七八";
        assertEquals(50, name50.length(), "测试数据本身应为50字符");

        openAddModalAndFill(name50, "这是一个合法的描述十个字以上", "10", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();  // 处理成功 alert

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'" + name50.substring(0, 10) + "')]")));
    }

    // ------------------- 价格边界 -------------------
    @Test
    @DisplayName("PM-010: 价格0.01 - 新增成功")
    public void testPrice_Min_001() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("价格测试商品", "这是一个合法的描述十个字以上", "0.01", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'价格测试商品')]")));
    }

    @Test
    @DisplayName("PM-012: 价格0 - 被拒绝")
    public void testPrice_Zero() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("价格测试商品", "这是一个合法的描述十个字以上", "0", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        String alertText = getAlertTextAndAccept();
        assertEquals("价格超出允许范围", alertText);
        assertTrue(driver.findElement(By.cssSelector(".modal-overlay")).isDisplayed());
    }

    @Test
    @DisplayName("PM-011:价格999999.99 - 新增成功")
    public void testPrice_Max_99999999() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("高价商品", "这是一个合法的描述十个字以上", "999999.99", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'高价商品')]")));
    }

    // ------------------- 库存边界 -------------------
    @Test
    @DisplayName("PM-013: 库存0 - 新增成功")
    public void testStock_Min_0() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("零库存商品", "这是一个合法的描述十个字以上", "10", "0");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'零库存商品')]")));
    }

    @Test
    @DisplayName("PM-015: 库存负数 - 被拒绝")
    public void testStock_Negative() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("负数库存商品", "这是一个合法的描述十个字以上", "10", "-1");
        driver.findElement(By.cssSelector(".save-btn")).click();

        String alertText = getAlertTextAndAccept();
        assertEquals("库存必须为非负整数", alertText);
        assertTrue(driver.findElement(By.cssSelector(".modal-overlay")).isDisplayed());
    }

    @Test
    @DisplayName("PM-016: 库存小数 - 被拒绝")
    public void testStock_Decimal() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("小数库存商品", "这是一个合法的描述十个字以上", "10", "1.5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        String alertText = getAlertTextAndAccept();
        assertEquals("库存必须为非负整数", alertText);
        assertTrue(driver.findElement(By.cssSelector(".modal-overlay")).isDisplayed());
    }

    @Test
    @DisplayName("PM-014:库存99999 - 新增成功")
    public void testStock_Max_99999() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("满库存商品", "这是一个合法的描述十个字以上", "10", "99999");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'满库存商品')]")));
    }

    // ------------------- 描述边界 -------------------
    @Test
    @DisplayName("PM-019: 描述9字符 - 被拒绝")
    public void testDesc_TooShort_9Chars() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("描述测试商品", "123456789", "10", "5");  // 恰好9字符
        driver.findElement(By.cssSelector(".save-btn")).click();

        String alertText = getAlertTextAndAccept();
        assertEquals("描述必须在10-500字符之间", alertText);
        assertTrue(driver.findElement(By.cssSelector(".modal-overlay")).isDisplayed());
    }

    @Test
    @DisplayName("PM-017:描述10字符 - 新增成功")
    public void testDesc_Min_10Chars() {
        injectAdmin();
        goToAdminProducts();
        openAddModalAndFill("描述测试商品", "1234567890", "10", "5");  // 恰好10字符
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'描述测试商品')]")));
    }

    @Test
    @DisplayName("PM-018:描述500字符 - 新增成功")
    public void testDesc_Max_500Chars() {
        injectAdmin();
        goToAdminProducts();
        // 生成500字符的描述
        String desc500 = "A".repeat(500);
        assertEquals(500, desc500.length());

        openAddModalAndFill("长描述商品", desc500, "10", "5");
        driver.findElement(By.cssSelector(".save-btn")).click();

        acceptAlertIfPresent();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal-overlay")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(text(),'长描述商品')]")));
    }
}