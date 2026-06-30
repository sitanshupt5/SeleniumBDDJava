package com.automation.ui.steps;

import com.automation.ui.types.TestContext;
import com.automation.ui.utilities.AllureHelpers;
import com.automation.ui.utilities.CustomLogger;
import com.automation.ui.utilities.DataRegistry;
import com.automation.ui.utilities.ReadProperty;
import io.cucumber.java.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class Hooks {

    private static final Logger logger = CustomLogger.getLogger("Hooks");
    private static String appName;
    private static String env;
    private static String runId;

    private final TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @BeforeAll
    public static void beforeAll() {
        appName = System.getProperty("app", "application");
        env = System.getProperty("env", "qa");
        runId = System.getProperty("runId", "");
        String projectRoot = System.getProperty("user.dir");
        String reportsDir = projectRoot + "/reports/" + appName;
        String downloadDir = projectRoot + "/download/" + appName;
        new File(reportsDir).mkdirs();
        new File(downloadDir).mkdirs();
        logger.info("BeforeAll: app={} env={} runId={}", appName, env, runId);
    }

    @Before(order = 1)
    public void beforeFeature(Scenario scenario) {
        String uri = scenario.getUri().toString();
        String dataFilePath = DataRegistry.getDataFilePath(uri);
        logger.info("Loading data file: {}", dataFilePath);
        try {
            context.dataFileContent = DataRegistry.parseDataFile(dataFilePath);
            context.dataFilePath = dataFilePath;

        } catch (DataRegistry.DataException e) {
            logger.warn("Data file not loaded (may not exist): {}", e.getMessage());
            context.dataFileContent = new java.util.LinkedHashMap<>();
        }
    }

    @Before(order = 2)
    public void beforeScenario(Scenario scenario) {
        String projectRoot = System.getProperty("user.dir");
        context.projectRoot = projectRoot;
        context.reportsDir = projectRoot + "/reports/" + appName;
        context.downloadDir = projectRoot + "/download/" + appName;
        context.appName = appName;
        context.baseUrl = ReadProperty.getApplicationUrl();
        context.allureRunId = runId.isEmpty() ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-Hhmmss")) : runId;
        String browser = ReadProperty.getBrowser("chrome");
        boolean headless = ReadProperty.getHeadless(false);

        WebDriver driver;
        if ("firefox".equalsIgnoreCase(browser)) {
            WebDriverManager.firefoxdriver().setup();
            FirefoxOptions opts = new FirefoxOptions();
            if (headless) opts.addArguments("--headless");
            opts.addPreference("browser.download.folderList", 2);
            opts.addPreference("browser.download.dir", context.downloadDir);
            opts.addPreference("browser.download.useDownloadDir", true);
            opts.addPreference("browser.helperApps.neverAsk.saveToDisk",
                    "application/pdf, application/octet-stream");
            driver = new FirefoxDriver(opts);
        } else {
            WebDriverManager.chromedriver().setup();
            ChromeOptions opts = new ChromeOptions();
            if (headless) opts.addArguments("--headless");
            opts.addArguments("--start-maximized", "--no-sandbox", "--disable-dev-shm-usage");
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", context.downloadDir);
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            opts.setExperimentalOption("prefs", prefs);
            driver = new ChromeDriver(opts);
        }

        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(20));
        context.driver = driver;
        logger.info("Started {} for scenario {}", browser, scenario.getName());
    }

    @BeforeStep
    public void beforeStep(Scenario scenario) {
        logger.info("Step: {}", scenario.getName());
    }

    @AfterStep
    public void afterStep(Scenario scenario) {
        if (context.driver == null) return;
        try {
            AllureHelpers.attachText("URL", context.driver.getCurrentUrl());
        } catch (Exception e) {
            // safe - do not fail the test
        }
        try {
            byte[] screenshot = ((org.openqa.selenium.TakesScreenshot) context.driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            AllureHelpers.attachPng("Screenshot - " + scenario.getName(), screenshot);

        } catch (Exception e) {
            // safe - do not fail the test
        }
        if (scenario.isFailed()) {
            try {
                AllureHelpers.attachText("Page Source", context.driver.getPageSource());

            } catch (Exception e) {
                // safe - do not fail the test
            }
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        if (context.driver == null) return;
        try {
            byte[] screenshot = ((org.openqa.selenium.TakesScreenshot) context.driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            AllureHelpers.attachPng("Scenario End - " + scenario.getName(), screenshot);

        } catch (Exception e) {
            // safe - do not fail the test
        }
        if (scenario.isFailed()) {
            try {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9_]", "_");
                String screenshotPath = context.reportsDir + "/" + safeName + "_" + ts + ".png";
                new File(context.reportsDir).mkdirs();
                byte[] png = ((org.openqa.selenium.TakesScreenshot) context.driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(screenshotPath)) {
                    fos.write(png);
                }
                logger.info("Saved failure screenshot: {}", screenshotPath);

            } catch (Exception e) {
                logger.error("Failed to save failure screenshot: {}", e.getMessage());
            }
        }
        try {
            context.driver.quit();
            context.driver = null;
            logger.info("WebDriver closed.");

        } catch (Exception e) {
            // safe
        }
    }
}
