package com.shop.selenium;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class AdminOrderTest extends EdgeBaseTest {

    /** 进入后台订单管理页，等待必要元素加载 */
    private void goToAdminOrders() {
        driver.get("http://localhost:5173/admin/orders");
        // 等待页面主体出现（.admin-container）或者 loading 文字消失
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".admin-container")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".loading"))
        ));
        // 额外等待数据加载完成（loading 文字消失）
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".loading")));
    }

    /** 关闭可能弹出的 alert */
    private void acceptAlertIfPresent() {
        try {
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            alert.accept();
        } catch (TimeoutException ignored) {}
    }

    // ======================= OM-009 =======================
    @Test
    @DisplayName("OM-009: 订单详情商品清单正确显示")
    public void testOM009_OrderItemListDisplay() {
        injectAdmin();
        goToAdminOrders();

        List<WebElement> cards = driver.findElements(By.cssSelector(".order-card"));
        assertFalse(cards.isEmpty(), "至少需要一个订单");
        WebElement firstCard = cards.get(0);

        // 展开详情
        firstCard.findElement(By.cssSelector(".toggle-btn")).click();
        wait.until(ExpectedConditions.visibilityOf(firstCard.findElement(By.cssSelector(".order-detail"))));

        // 验证商品清单表格存在且至少一行数据
        WebElement itemsTable = firstCard.findElement(By.cssSelector(".item-table"));
        assertNotNull(itemsTable, "应存在商品清单表格");
        List<WebElement> itemRows = itemsTable.findElements(By.cssSelector("tbody tr"));
        assertFalse(itemRows.isEmpty(), "商品清单至少应有一条记录");
    }

    // ======================= OM-010 =======================
    @Test
    @DisplayName("OM-010: 详情反复展开/收起无异常")
    public void testOM010_ToggleDetailMultipleTimes() {
        injectAdmin();
        goToAdminOrders();

        WebElement firstCard = driver.findElement(By.cssSelector(".order-card"));
        WebElement toggleBtn = firstCard.findElement(By.cssSelector(".toggle-btn"));

        for (int i = 0; i < 3; i++) {
            toggleBtn.click();
            // 等待动画完成（简单延时或等待样式变化）
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            // 检查按钮文字是否符合当前状态
            String btnText = toggleBtn.getText();
            if (btnText.contains("收起详情")) {
                assertTrue(firstCard.findElement(By.cssSelector(".order-detail")).isDisplayed(),
                        "按钮显示“收起详情”时详情应可见");
            } else {
                // 按钮文字是“展开详情”
                assertTrue(firstCard.findElements(By.cssSelector(".order-detail")).isEmpty() ||
                                !firstCard.findElement(By.cssSelector(".order-detail")).isDisplayed(),
                        "按钮显示“展开详情”时详情应隐藏");
            }
        }

        // 最后确保详情内容完整
        toggleBtn.click();
        wait.until(ExpectedConditions.visibilityOf(firstCard.findElement(By.cssSelector(".order-detail"))));
        WebElement detail = firstCard.findElement(By.cssSelector(".order-detail"));
        assertTrue(detail.getText().contains("收货详细"), "多次切换后详情内容应完整");
    }
    // ======================= OM-011 =======================
    @Test
    @DisplayName("OM-011: 订单金额格式验证（¥ + 两位小数）")
    public void testOM011_OrderAmountFormat() {
        injectAdmin();
        goToAdminOrders();

        List<WebElement> cards = driver.findElements(By.cssSelector(".order-card"));
        assertFalse(cards.isEmpty(), "至少需要一个订单");
        WebElement firstCard = cards.get(0);
        WebElement amountElement = firstCard.findElement(By.cssSelector(".price"));
        String amountText = amountElement.getText().trim();

        assertTrue(amountText.startsWith("¥"), "金额应以 ¥ 开头");
        String numericPart = amountText.substring(1);
        assertTrue(numericPart.matches("\\d+\\.\\d{2}"), "金额应为两位小数，实际: " + numericPart);
    }

    // ======================= OM-012 =======================
    @Test
    @DisplayName("OM-012: 下单时间格式验证（YYYY-MM-DD HH:mm:ss）")
    public void testOM012_OrderTimeFormat() {
        injectAdmin();
        goToAdminOrders();

        List<WebElement> cards = driver.findElements(By.cssSelector(".order-card"));
        assertFalse(cards.isEmpty(), "至少需要一个订单");
        WebElement firstCard = cards.get(0);

        // 展开详情
        firstCard.findElement(By.cssSelector(".toggle-btn")).click();
        wait.until(ExpectedConditions.visibilityOf(firstCard.findElement(By.cssSelector(".order-detail"))));

        // 查找下单时间字段（在收货详细信息区域）
        WebElement receiverInfo = firstCard.findElement(By.cssSelector(".receiver-info"));
        String infoText = receiverInfo.getText();

        // 从文本中提取包含“下单时间”的行
        String timeLine = infoText.lines()
                .filter(l -> l.contains("下单时间"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("应包含下单时间字段，实际文本: " + infoText));

        // 提取时间值（格式: 2026-07-01 10:00:00）
        String timeValue = timeLine.substring(timeLine.indexOf("：") + 1).trim();
        assertTrue(timeValue.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "时间格式应为 YYYY-MM-DD HH:mm:ss，实际: " + timeValue);
    }

    // ======================= OM-013 =======================
    @Test
    @DisplayName("OM-013: 不同状态下操作按钮显隐")
    public void testOM013_ButtonVisibilityByStatus() {
        injectAdmin();
        goToAdminOrders();

        List<WebElement> cards = driver.findElements(By.cssSelector(".order-card"));
        assertFalse(cards.isEmpty(), "至少需要一个订单");

        for (WebElement card : cards) {
            WebElement statusTag = card.findElement(By.cssSelector(".status-tag"));
            String statusText = statusTag.getText().trim();

            boolean expectConfirm = "待确认".equals(statusText);
            boolean expectShip = "已付款待发货".equals(statusText);

            boolean hasConfirm = !card.findElements(By.cssSelector(".confirm-btn")).isEmpty();
            boolean hasShip = !card.findElements(By.cssSelector(".ship-btn")).isEmpty();

            assertEquals(expectConfirm, hasConfirm, statusText + " 确认按钮显隐不正确");
            assertEquals(expectShip, hasShip, statusText + " 发货按钮显隐不正确");
        }
    }

}