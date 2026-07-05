package com.shop.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
/*import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;*/
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Locale;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShoppingSystemSeleniumIT {

    /*private static final String BASE_URL = System.getProperty("selenium.baseUrl", "http://127.0.0.1:5173");*/
    private static final String BASE_URL = System.getProperty("selenium.baseUrl", "http://localhost:5173");
    private static final String DB_URL = System.getProperty(
            "selenium.dbUrl",
            "jdbc:mysql://127.0.0.1:3306/shopping_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
    );
    private static final Path EVIDENCE_DIR = Path.of("..", "..", "测试证据", "Selenium自动化测试")
            .toAbsolutePath().normalize();

    private static String dbUser;
    private static String dbPassword;

    private WebDriver driver;
    private WebDriverWait wait;

    @RegisterExtension
    final SeleniumEvidenceExtension evidenceExtension = new SeleniumEvidenceExtension();

    @BeforeAll
    static void prepareSuite() throws IOException {
        dbUser = environmentOrDefault("DB_USER", "root");
        dbPassword = System.getenv("DB_PWD");
        if (dbPassword == null || dbPassword.isBlank()) {
            dbPassword = System.getenv("MYSQL_PWD");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException("请通过 DB_PWD 或 MYSQL_PWD 环境变量提供本机 MySQL 密码");
        }
        Files.createDirectories(EVIDENCE_DIR);
    }

    /*@BeforeEach
    void setUp() throws SQLException {
        resetDatabase();

        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--window-size=1440,1000",
                "--disable-search-engine-choice-screen",
                "--no-default-browser-check"
        );

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }*/

    @BeforeEach
    void setUp() throws SQLException {
        resetDatabase();

        System.setProperty("webdriver.edge.driver", "D:/ProgrammingWork/msedgedriver/msedgedriver.exe");

        EdgeOptions options = new EdgeOptions();
        if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new"); // 依然支持无头模式
        }
        options.addArguments(
                "--window-size=1440,1000",
                "--disable-search-engine-choice-screen",
                "--no-default-browser-check"
        );

        driver = new EdgeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void closeDriverIfNeeded() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("CART-03-正常加入购物车")
    void normalAddToCart() {
        login("user", "Password123!");
        addProduct(1, 2);
        openCartWithItems();

        Assertions.assertTrue(driver.getPageSource().contains("GAN 356 Air魔方"));
        Assertions.assertEquals("2", driver.findElement(By.cssSelector("input.qty-input")).getAttribute("value"));
    }

    @Test
    @Order(2)
    @DisplayName("CART-04-重复加购数量累加")
    void duplicateAddAccumulates() {
        login("user", "Password123!");
        addProduct(1, 1);
        addProduct(1, 2);
        openCartWithItems();

        Assertions.assertEquals("3", driver.findElement(By.cssSelector("input.qty-input")).getAttribute("value"));
        Assertions.assertEquals("1", queryString(
                "SELECT COUNT(*) FROM cart_items WHERE user_id=11 AND product_id=1"
        ));
    }

    @Test
    @Order(3)
    @DisplayName("CART-05-加购下边界1")
    void addLowerBoundary() {
        login("user", "Password123!");
        addProduct(1, 1);
        openCartWithItems();

        Assertions.assertEquals("1", driver.findElement(By.cssSelector("input.qty-input")).getAttribute("value"));
    }

    @Test
    @Order(4)
    @DisplayName("CART-06-加购上边界99")
    void addUpperBoundary() {
        login("user", "Password123!");
        addProduct(1, 99);
        openCartWithItems();

        Assertions.assertEquals("99", driver.findElement(By.cssSelector("input.qty-input")).getAttribute("value"));
    }

    @Test
    @Order(5)
    @DisplayName("CART-10-购物车总金额计算")
    void cartTotalAmount() {
        seedCart(1, 2, 11);
        seedCart(2, 1, 11);
        login("user", "Password123!");
        openCartWithItems();

        Assertions.assertTrue(driver.getPageSource().contains("¥288.50"));
    }

    @Test
    @Order(6)
    @DisplayName("CART-11-正常修改数量")
    void updateCartQuantity() {
        seedCart(1, 2, 11);
        login("user", "Password123!");
        openCartWithItems();

        WebElement quantity = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.qty-input")));
        replaceInputValue(quantity, "3");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".subtotal"), "135.00"));

        Assertions.assertEquals("3", queryString(
                "SELECT quantity FROM cart_items WHERE user_id=11 AND product_id=1"
        ));
    }

    @Test
    @Order(7)
    @DisplayName("CART-12-修改数量下边界1")
    void updateCartLowerBoundary() {
        seedCart(1, 2, 11);
        login("user", "Password123!");
        openCartWithItems();

        WebElement quantity = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.qty-input")));
        replaceInputValue(quantity, "1");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".subtotal"), "45.00"));

        Assertions.assertEquals("1", queryString(
                "SELECT quantity FROM cart_items WHERE user_id=11 AND product_id=1"
        ));
    }

    @Test
    @Order(8)
    @DisplayName("ORDER-03-正常提交订单")
    void normalCheckout() {
        seedCart(1, 2, 11);
        login("user", "Password123!");
        submitCheckout("张三", "13812345678", "浙江省杭州市西湖区文一路100号");

        Assertions.assertTrue(driver.getPageSource().contains("订单提交成功"));
        Assertions.assertEquals("0", queryString(
                "SELECT status FROM orders WHERE user_id=11 ORDER BY id DESC LIMIT 1"
        ));
        Assertions.assertEquals("0", queryString("SELECT COUNT(*) FROM cart_items WHERE user_id=11"));
        Assertions.assertEquals("98", queryString("SELECT stock FROM products WHERE id=1"));
    }

    @Test
    @Order(9)
    @DisplayName("ORDER-05-姓名长度2和20边界")
    void receiverNameBoundaries() {
        login("user", "Password123!");
        seedCart(1, 1, 11);
        submitCheckout("张三", "13812345678", "浙江省杭州市西湖区文一路100号");

        seedCart(1, 1, 11);
        submitCheckout("ABCDEFGHIJKLMNOPQRST", "13812345678", "浙江省杭州市西湖区文一路100号");

        Assertions.assertEquals("2", queryString("SELECT COUNT(*) FROM orders WHERE user_id=11"));
    }

    @Test
    @Order(10)
    @DisplayName("ORDER-08-地址长度10和100边界")
    void receiverAddressBoundaries() {
        login("user", "Password123!");
        seedCart(1, 1, 11);
        submitCheckout("张三", "13812345678", "浙江省杭州市西湖区A");

        seedCart(1, 1, 11);
        submitCheckout("张三", "13812345678", "A".repeat(100));

        Assertions.assertEquals("2", queryString("SELECT COUNT(*) FROM orders WHERE user_id=11"));
    }

    @Test
    @Order(11)
    @DisplayName("ORDER-10-防止重复提交")
    void preventDuplicateCheckout() {
        seedCart(1, 1, 11);
        login("user", "Password123!");
        driver.get(BASE_URL + "/checkout");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[placeholder='2-20个中英文字符']")
        )).sendKeys("张三");
        driver.findElement(By.cssSelector("input[placeholder='11位手机号']")).sendKeys("13812345678");
        driver.findElement(By.cssSelector("textarea")).sendKeys("浙江省杭州市西湖区文一路100号");

        WebElement submit = driver.findElement(By.cssSelector("button.submit-btn"));
        new Actions(driver).doubleClick(submit).perform();
        wait.until(ExpectedConditions.urlContains("/order-success"));

        Assertions.assertEquals("1", queryString("SELECT COUNT(*) FROM orders WHERE user_id=11"));
    }

    @Test
    @Order(12)
    @DisplayName("ORDER-12-用户订单列表显示")
    void orderListDisplay() {
        seedOrder("ORDSELENIUM012", 0, 11, new BigDecimal("45.00"));
        login("user", "Password123!");
        driver.get(BASE_URL + "/orders");

        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "ORDSELENIUM012"));
        String source = driver.getPageSource();
        Assertions.assertTrue(source.contains("¥45.00"));
        Assertions.assertTrue(source.contains("待确认"));
        Assertions.assertFalse(source.contains("时间格式解析异常"));
    }

    @Test
    @Order(13)
    @DisplayName("ORDER-14-待付款订单付款")
    void payOrder() {
        seedOrder("ORDSELENIUM014", 1, 11, new BigDecimal("45.00"));
        login("user", "Password123!");
        driver.get(BASE_URL + "/orders");

        WebElement pay = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(.,'付款')]")
        ));
        pay.click();
        wait.until(ExpectedConditions.alertIsPresent()).accept();
        wait.until(ExpectedConditions.alertIsPresent()).accept();
        wait.until(ignored -> "2".equals(queryString(
                "SELECT status FROM orders WHERE order_no='ORDSELENIUM014'"
        )));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "已付款待发货"));
    }

    @Test
    @Order(14)
    @DisplayName("ORDER-16-已发货订单确认收货")
    void receiveOrder() {
        seedOrder("ORDSELENIUM016", 3, 11, new BigDecimal("45.00"));
        login("user", "Password123!");
        driver.get(BASE_URL + "/orders");

        WebElement receive = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(.,'确认收货')]")
        ));
        receive.click();
        wait.until(ExpectedConditions.alertIsPresent()).accept();
        wait.until(ExpectedConditions.alertIsPresent()).accept();
        wait.until(ignored -> "4".equals(queryString(
                "SELECT status FROM orders WHERE order_no='ORDSELENIUM016'"
        )));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "已完成"));
    }

    @Test
    @Order(15)
    @DisplayName("ORDER-18-用户订单数据隔离")
    void userOrderIsolation() {
        seedOrder("ORD-USER-ONLY", 0, 11, new BigDecimal("45.00"));
        seedOrder("ORD-ADMIN-ONLY", 0, 1, new BigDecimal("45.00"));
        login("user", "Password123!");
        driver.get(BASE_URL + "/orders");

        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "ORD-USER-ONLY"));
        String source = driver.getPageSource();
        Assertions.assertTrue(source.contains("ORD-USER-ONLY"));
        Assertions.assertFalse(source.contains("ORD-ADMIN-ONLY"));
    }

    private void login(String username, String password) {
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='text']")))
                .sendKeys(username);
        driver.findElement(By.cssSelector("input[type='password']")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.alertIsPresent()).accept();
        wait.until(ExpectedConditions.urlContains("/products"));
    }

    private void addProduct(long productId, int quantity) {
        driver.get(BASE_URL + "/product/" + productId);
        WebElement quantityInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='number']")
        ));
        quantityInput.clear();
        quantityInput.sendKeys(String.valueOf(quantity));
        driver.findElement(By.cssSelector("button.add-to-cart")).click();
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    private void openCartWithItems() {
        driver.get(BASE_URL + "/cart");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.qty-input")));
    }

    private void submitCheckout(String name, String phone, String address) {
        driver.get(BASE_URL + "/checkout");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[placeholder='2-20个中英文字符']")
        )).sendKeys(name);
        driver.findElement(By.cssSelector("input[placeholder='11位手机号']")).sendKeys(phone);
        driver.findElement(By.cssSelector("textarea")).sendKeys(address);
        driver.findElement(By.cssSelector("button.submit-btn")).click();
        wait.until(ExpectedConditions.urlContains("/order-success"));
    }

    private void replaceInputValue(WebElement input, String value) {
        Keys selectAll = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
                ? Keys.COMMAND : Keys.CONTROL;
        input.sendKeys(selectAll, "a");
        input.sendKeys(value, Keys.TAB);
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(DB_URL, dbUser, dbPassword);
    }

    private void resetDatabase() throws SQLException {
        executeStatements(
                "SET FOREIGN_KEY_CHECKS=0",
                "DELETE FROM order_items",
                "DELETE FROM orders",
                "DELETE FROM cart_items",
                "ALTER TABLE order_items AUTO_INCREMENT=1",
                "ALTER TABLE orders AUTO_INCREMENT=1",
                "ALTER TABLE cart_items AUTO_INCREMENT=1",
                "UPDATE products SET stock=CASE id WHEN 1 THEN 100 WHEN 2 THEN 50 " +
                        "WHEN 3 THEN 50 WHEN 4 THEN 79 ELSE stock END, " +
                        "is_active=CASE WHEN id=5 THEN 0 ELSE 1 END",
                "UPDATE users SET failed_attempts=0, locked_until=NULL",
                "SET FOREIGN_KEY_CHECKS=1"
        );
    }

    private void executeStatements(String... sqlStatements) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String sql : sqlStatements) {
                statement.executeUpdate(sql);
            }
        }
    }

    private String queryString(String sql) {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                return null;
            }
            return resultSet.getString(1);
        } catch (SQLException e) {
            throw new IllegalStateException("数据库查询失败: " + sql, e);
        }
    }

    private void seedCart(long productId, int quantity, long userId) {
        String sql = "INSERT INTO cart_items(user_id,product_id,quantity,created_at,updated_at) " +
                "VALUES(?,?,?,?,NOW())";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.setObject(4, java.time.LocalDateTime.now());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("准备购物车数据失败", e);
        }
    }

    private void seedOrder(String orderNo, int status, long userId, BigDecimal amount) {
        String orderSql = "INSERT INTO orders(order_no,user_id,total_amount,receiver_name,receiver_phone," +
                "receiver_address,status,created_at,expire_at) VALUES(?,?,?,?,?,?,?,NOW(),DATE_ADD(NOW(),INTERVAL 3 DAY))";

        try (Connection connection = connection();
             PreparedStatement orderStatement = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            orderStatement.setString(1, orderNo);
            orderStatement.setLong(2, userId);
            orderStatement.setBigDecimal(3, amount);
            orderStatement.setString(4, "张三");
            orderStatement.setString(5, "13812345678");
            orderStatement.setString(6, "浙江省杭州市西湖区文一路100号");
            orderStatement.setInt(7, status);
            orderStatement.executeUpdate();

            long orderId;
            try (ResultSet keys = orderStatement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("未获取到订单主键");
                }
                orderId = keys.getLong(1);
            }

            String itemSql = "INSERT INTO order_items(order_id,product_id,product_name,buy_price,quantity) " +
                    "VALUES(?,1,'GAN 356 Air魔方',45.00,1)";
            try (PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
                itemStatement.setLong(1, orderId);
                itemStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("准备订单数据失败", e);
        }
    }

    private final class SeleniumEvidenceExtension implements AfterTestExecutionCallback {
        @Override
        public void afterTestExecution(ExtensionContext context) throws Exception {
            if (driver == null) {
                return;
            }

            boolean failed = context.getExecutionException().isPresent();
            String result = failed ? "失败" : "通过";
            String opposite = failed ? "通过" : "失败";
            String caseName = sanitizeFileName(context.getDisplayName());

            dismissOpenAlert();
            Path screenshot = EVIDENCE_DIR.resolve(caseName + "-" + result + ".png");
            Path staleScreenshot = EVIDENCE_DIR.resolve(caseName + "-" + opposite + ".png");
            Files.deleteIfExists(staleScreenshot);

            Path temporaryScreenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(temporaryScreenshot, screenshot, StandardCopyOption.REPLACE_EXISTING);

            driver.quit();
            driver = null;
        }

        private void dismissOpenAlert() {
            try {
                Alert alert = driver.switchTo().alert();
                alert.dismiss();
            } catch (NoAlertPresentException ignored) {
                // 没有未处理的对话框时无需操作。
            }
        }

        private String sanitizeFileName(String value) {
            return value.replaceAll("[\\\\/:*?\"<>|]", "_");
        }
    }
}
