package com.shop.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class EdgeBaseTest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;

    @BeforeEach
    public void setUp() {
        System.setProperty("webdriver.edge.driver", "D:/ProgrammingWork/msedgedriver/msedgedriver.exe");
        EdgeOptions options = new EdgeOptions();
        // options.addArguments("--headless"); // 先不加，方便观察
        driver = new EdgeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        js = (JavascriptExecutor) driver;
    }

    /** 注入管理员身份（userId=1, role=1） */
    protected void injectAdmin() {
        driver.get("http://localhost:5173");
        clearAndSetAuth("1", "1");
        driver.navigate().refresh();
        // 等待一个肯定存在的已登录元素，比如“购物车”链接
        wait.until(ExpectedConditions.presenceOfElementLocated(By.linkText("购物车 (0)")));
    }

    /** 注入普通用户身份（userId=2, role=0） */
    protected void injectNormalUser() {
        driver.get("http://localhost:5173");
        clearAndSetAuth("2", "0");
        driver.navigate().refresh();
        // 同样等待已登录元素（因为只要 userId 存在就是登录状态）
        wait.until(ExpectedConditions.presenceOfElementLocated(By.linkText("购物车 (0)")));
    }

    private void clearAndSetAuth(String userId, String role) {
        js.executeScript("localStorage.clear();");
        js.executeScript("localStorage.setItem('userId', arguments[0]);", userId);
        js.executeScript("localStorage.setItem('userRole', arguments[0]);", role);
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}