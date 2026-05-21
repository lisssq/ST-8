package com.mycompany.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class App {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");

        Path downloadDir = Paths.get("result").toAbsolutePath();
        try {
            Files.createDirectories(downloadDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("http://www.papercdcase.com/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            String dataFilePath = "data/data.txt";
            List<String> lines = Files.readAllLines(Paths.get(dataFilePath));
            String artist = "", title = "";
            List<String> tracks = new ArrayList<>();
            boolean tracksSection = false;
            for (String line : lines) {
                if (line.startsWith("Artist:")) {
                    artist = line.substring("Artist:".length()).trim();
                } else if (line.startsWith("Title:")) {
                    title = line.substring("Title:".length()).trim();
                } else if (line.startsWith("Tracks:")) {
                    tracksSection = true;
                } else if (tracksSection && !line.isBlank()) {
                    tracks.add(line.trim());
                }
            }
            if (artist.isEmpty() || title.isEmpty() || tracks.isEmpty()) {
                System.err.println("Ошибка формата data.txt");
                return;
            }

            WebElement artistField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
            WebElement titleField = driver.findElement(By.name("title"));
            artistField.clear();
            artistField.sendKeys(artist);
            titleField.clear();
            titleField.sendKeys(title);

            List<WebElement> trackFields = driver.findElements(By.cssSelector("input[name^='track']"));
            for (int i = 0; i < tracks.size() && i < trackFields.size(); i++) {
                trackFields.get(i).clear();
                trackFields.get(i).sendKeys(tracks.get(i));
            }

            WebElement a4Radio = driver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            WebElement jewelCaseRadio = driver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!a4Radio.isSelected()) {
                a4Radio.click();
            }
            if (!jewelCaseRadio.isSelected()) {
                jewelCaseRadio.click();
            }

            WebElement submitButton = driver.findElement(By.name("submit"));
            submitButton.click();

            System.out.println("Ожидание загрузки PDF...");
            Path downloadedFile = waitForDownload(downloadDir, 30);
            if (downloadedFile != null) {
                Path target = downloadDir.resolve("cd.pdf");
                Files.move(downloadedFile, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("PDF сохранён: " + target.toAbsolutePath());
            } else {
                System.err.println("PDF не загрузился за 30 секунд");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Path waitForDownload(Path dir, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".pdf"));
            if (files != null && files.length > 0) {
                return files[0].toPath();
            }
            Thread.sleep(500);
        }
        return null;
    }
}
