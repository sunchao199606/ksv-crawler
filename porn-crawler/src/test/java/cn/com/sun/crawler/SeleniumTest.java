package cn.com.sun.crawler;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

/**
 * @Description : Selenium测试
 * @Author : mockingbird
 * @Date : 2021/3/9 10:49
 */
public class SeleniumTest {

    @Test
    public void test() {

        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--disable-blink-features=AutomationControlled");
//        options.addArguments("--headless");
        options.setBinary(new File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"));
        WebDriver driver = new ChromeDriver(options);
        System.out.println(driver.getWindowHandle());
        //driver.manage().window().setSize(new Dimension(1,1));
        driver.manage().window().setPosition(new Point(-1000, -1000));
        //driver.manage().window().setSize();
        WebDriverWait wait = new WebDriverWait(driver, 20);
        try {
//            driver.get("http://91porn.com");
//            String html1 = driver.getPageSource();
//            System.out.println(html1);
            driver.get("http://91porn.com/view_video.php?viewkey=9bc56b7a77cc3e409163&page=1&viewtype=basic&category=hot");
            wait.until(presenceOfElementLocated(By.ByTagName.tagName("source")));
            String html = driver.getPageSource();
            System.out.println(html);
        } catch (Exception e) {
            String html = driver.getPageSource();
            System.out.println(html);
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }


    @Test
    public void testLocalLoop() throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> {
                System.out.println("new socket : " + socket.getLocalSocketAddress());
            }).start();
        }
    }
}
