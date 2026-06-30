package com.automation.ui.utilities;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

public class UiActions {

    private static Logger logger = CustomLogger.getLogger(UiActions.class.getName());

    private final WebDriver driver;
    private final int defaultTimeout;

    public UiActions(WebDriver driver) {
        this.driver = driver;
        this.defaultTimeout = ReadProperty.getWaitTime10Sec();
    }

    public WebDriver getDriver() {return driver;}

    public void verifyPageLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout)).until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    public void openUrl(String url) {
        logger.info("Open URL: {}", url);
        driver.get(url);
        verifyPageLoaded();
    }

    public void openPage(String page, int timeout, String waitFor) {
        String url = LocatorRegistry.getPageUrl(page);
        if (url == null || url.isEmpty()) {
            throw new AssertionError("No page_url configured in YAML for page: " + page);
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            String base = ReadProperty.getApplicationUrl();
            url = base != null ? base + url : url;
        }
        driver.get(url);
        if ("load".equalsIgnoreCase(waitFor)) {
            new WebDriverWait(driver, Duration.ofSeconds(timeout)).until((WebDriver d) -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        }
        logger.info("{} page loaded.", page);
    }

    public void openPage(String page, int timeout) {
        openPage(page, timeout, "load");
    }

    public void navigateBack() {
        logger.info("Navigate: back");
        driver.navigate().back();
        verifyPageLoaded();
    }

    public void navigateForward() {
        logger.info("Navigate: forward");
        driver.navigate().forward();
        verifyPageLoaded();
    }

    public void navigateRefresh() {
        logger.info("Navigate: refresh");
        driver.navigate().refresh();
        verifyPageLoaded();
    }
// --- Keyed Waits ---

    public void waitForElementPresent(String page, String name, int timeout, Map<String, String> params) {
        LocatorRegistry.locate(driver, page, name, "present", timeout, params);
    }

    public void waitForElementPresent(String page, String name, int timeout) {
        waitForElementPresent(page, name, timeout, Collections.emptyMap());
    }

    public void waitForElementVisible(String page, String name, int timeout, Map<String, String> params) {
        LocatorRegistry.locate(driver, page, name, "visible", timeout, params);
    }

    public void waitForElementVisible(String page, String name, int timeout) {
        waitForElementVisible(page, name, timeout, Collections.emptyMap());
    }

    public void waitForElementClickable(String page, String name, int timeout, Map<String, String> params) {
        LocatorRegistry.locate(driver, page, name, "clickable", timeout, params);
    }

    public void waitForElementClickable(String page, String name, int timeout) {
        waitForElementClickable(page, name, timeout, Collections.emptyMap());
    }
// --- Keyed Finds ---

    public WebElement findElement(String page, String name, String wait, int pollFrequency, int timeout, Map<String, String> params) {
        List<By> candidates = LocatorRegistry.getCandidates(page, name, params);
        Exception lastEx = null;
        for (By by : candidates) {
            try {
                WebDriverWait wdw = new WebDriverWait(driver, Duration.ofSeconds(timeout));
                switch (wait.toLowerCase()) {
                    case "visible":
                        return wdw.until(ExpectedConditions.visibilityOfElementLocated(by));
                    case "clickable":
                        return wdw.until(ExpectedConditions.elementToBeClickable(by));
                    default:
                        return wdw.until(ExpectedConditions.presenceOfElementLocated(by));
                }

            } catch (Exception e) {
                lastEx = e;
            }
        }
        throw new TimeoutException("Element '" + page + "." + name + "' not found within " + timeout + "s", lastEx);
    }

    public WebElement findElement(String page, String name, String wait, int timeout, Map<String, String> params) {
        return findElement(page, name, wait, 0, timeout, params);
    }

    public WebElement findElement(String page, String name, String wait, int timeout) {
        return findElement(page, name, wait, timeout, Collections.emptyMap());
    }

    public WebElement findElement(String page, String name) {
        return findElement(page, name, "present", defaultTimeout, Collections.emptyMap());
    }

    public List<WebElement> findAllElements(String page, String name, int timeout, Map<String, String> params) {
        List<By> candidates = LocatorRegistry.getCandidates(page, name, params);
        Exception lastEx = null;
        for (By by : candidates) {
            try {
                List<WebElement> elems = new WebDriverWait(driver, Duration.ofSeconds(timeout)).until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
                if (!elems.isEmpty()) return elems;

            } catch (Exception e) {
                lastEx = e;
            }
        }
        throw new TimeoutException("No elements found for '" + page + "." + name + "'", lastEx);
    }

    public List<WebElement> findAllElements(String page, String name, int timeout) {
        return findAllElements(page, name, timeout, Collections.emptyMap());
    }
// --- Element Actions ---

    public void click(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = LocatorRegistry.locate(driver, page, name, "clickable", timeout, params);
        logger.info("Click: {}.{}", page, name);
        el.click();
    }

    public void click(String page, String name, int timeout) {
        click(page, name, timeout, Collections.emptyMap());
    }

    public void click(String page, String name) {
        click(page, name, defaultTimeout, Collections.emptyMap());
    }

    public void clickJs(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        logger.info("Click via JS: {}.{}", page, name);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click()", el);
    }

    public void clickJs(String page, String name, int timeout) {
        clickJs(page, name, timeout, Collections.emptyMap());
    }

    public void doubleClick(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        logger.info("Double-click: {}.{}", page, name);
        new Actions(driver).moveToElement(el).doubleClick().perform();
    }

    public void doubleClick(String page, String name, int timeout) {
        doubleClick(page, name, timeout, Collections.emptyMap());
    }

    public void typeText(String page, String name, String text, boolean clear, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", defaultTimeout, params);
        logger.info("Type into {}.{}: '{}' (clear={})", page, name, text, clear);
        if (clear) el.clear();
        el.sendKeys(text);
    }

    public void typeText(String page, String name, String text, boolean clear) {
        typeText(page, name, text, clear, Collections.emptyMap());
    }

    public String getText(String page, String name, String wait, int pollFrequency, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, wait, pollFrequency, timeout, params);
        String txt = el.getText();
        logger.info("Get text {}.{} -> '{}'", page, name, txt);
        return txt;
    }

    public String getText(String page, String name, String wait, int timeout) {
        return getText(page, name, wait, 0, timeout, Collections.emptyMap());
    }
// --- Checkbox / Radio / Select ---

    public boolean isElementSelected(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        boolean sel = el.isSelected();
        logger.info("Is selected {}.{} -> {}", page, name, sel);
        return sel;
    }

    public boolean isElementSelected(String page, String name, int timeout) {
        return isElementSelected(page, name, timeout, Collections.emptyMap());
    }

    public void selectCheckbox(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        if (!el.isSelected()) {
            el.click();
            logger.info("Selected checkbox {}.{}", page, name);
        }
    }

    public void unselectCheckbox(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        if (el.isSelected()) {
            el.click();
            logger.info("Unselected checkbox {}.{}", page, name);
        }
    }

    public void selectByVisibleText(String page, String name, String text, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        new Select(el).selectByVisibleText(text);
    }

    public void selectByVisibleText(String page, String name, String text, int timeout) {
        selectByVisibleText(page, name, text, timeout, Collections.emptyMap());
    }

    public void selectByValue(String page, String name, String value, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        new Select(el).selectByValue(value);
    }

    public void selectByValue(String page, String name, String value, int timeout) {
        selectByValue(page, name, value, timeout, Collections.emptyMap());
    }

    public void selectByIndex(String page, String name, int index, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "visible", timeout, params);
        new Select(el).selectByIndex(index);
    }

    public void selectByIndex(String page, String name, int index, int timeout) {
        selectByIndex(page, name, index, timeout, Collections.emptyMap());
    }

    public void pickFromCustomDropdown(String openerPage, String openerName, String optionText, String menuPage, String menuName, int timeout, Map<String, String> params) {
        scrollToElement(openerPage, openerName, timeout, params);
        WebElement opener = LocatorRegistry.locate(driver, openerPage, openerName, "clickable", timeout, params);
        opener.click();
        logger.info("Expanded dropdown {}.{}", openerPage, openerName);
        WebElement menuScope = LocatorRegistry.locate(driver, menuPage, menuName, "visible", timeout, params);
        logger.info("Parsing through the {} dropdown menu.", menuName);
        String q = optionText.contains("'") ? "\"" + optionText + "\"" : "'" + optionText + "'";
        String xpath = ".//*[self::li or self::div]" + "[normalize-space()=" + q + "]";
        WebElement optionEl = new WebDriverWait(driver, Duration.ofSeconds(timeout)).until(d -> menuScope.findElement(By.xpath(xpath)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", optionEl);
        optionEl.click();
        logger.info("'{}' option selected from {} dropdown.", optionText, menuName);
    }

    public void pickFromCustomDropdown(String openerPage, String openerName, String optionText, String menuPage, String menuName) {
        pickFromCustomDropdown(openerPage, openerName, optionText, menuPage, menuName, defaultTimeout, Collections.emptyMap());
    }

    public void selectFromMenu(String menuPage, String menuName, String optionText, int timeout, Map<String, String> params) {
        WebElement menuEl = LocatorRegistry.locate(driver, menuPage, menuName, "visible", timeout, params);
        String q = optionText.contains("'") ? "\"" + optionText + "\"" : "'" + optionText + "'";
        String xpath = ".//*[self::li or self::div]" + "[normalize-space()=" + q + "]";
        WebElement optionEl = menuEl.findElement(By.xpath(xpath));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", optionEl);
        optionEl.click();
        logger.info("'{}' selected from menu {}.", optionText, menuName);
    }

    public void selectFromMenu(String menuPage, String menuName, String optionText, int timeout) {
        selectFromMenu(menuPage, menuName, optionText, timeout, Collections.emptyMap());
    }
// --- Scrolling & Highlighting ---

    public void scrollPage(String to) {
        if ("top".equalsIgnoreCase(to)) {
            logger.info("Scroll page -> top");
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");

        } else {
            logger.info("Scroll page -> end");
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        }
    }

    public void scrollToTop() {
        scrollPage("top");
    }

    public void scrollToEnd() {
        scrollPage("end");
    }

    public void scrollToElement(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "present", timeout, params);
        logger.info("Scroll to element {}.{}", page, name);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    public void scrollToElement(String page, String name, int timeout) {
        scrollToElement(page, name, timeout, Collections.emptyMap());
    }

    public void highlightElement(String page, String name, String color, int timeout, Map<String, String> params) {
        try {
            WebElement el = findElement(page, name, "visible", timeout, params);
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.border='3px solid " + color + "';", el);

        } catch (Exception e) {
            logger.warn("Could not highlight element (script error).");
        }
    }

    public void highlightElement(String page, String name, String color, int timeout) {
        highlightElement(page, name, color, timeout, Collections.emptyMap());
    }
// --- Frames / Windows / Alerts ---

    public void switchToFrame(String page, String name, int timeout, Map<String, String> params) {
        WebElement el = findElement(page, name, "present", timeout, params);
        logger.info("Switch to frame {}.{}", page, name);
        driver.switchTo().frame(el);
    }

    public void switchToFrame(String page, String name, int timeout) {
        switchToFrame(page, name, timeout, Collections.emptyMap());
    }

    public void switchToParentFrame() {
        logger.info("Switch to parent frame");
        driver.switchTo().parentFrame();
    }

    public void switchToDefaultContent() {
        logger.info("Switch to default content");
        driver.switchTo().defaultContent();
    }

    public void switchToNewWindow() {
        logger.info("Window handles (before): {}", driver.getWindowHandles());
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
        }
        logger.info("Switched to last window handle");
    }

    public void switchToWindow(int index) {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        logger.info("Window handles: {}", handles);
        if (index < 0 || index >= handles.size()) {
            throw new IndexOutOfBoundsException("Window index " + index + " out of range; have " + handles.size());
        }
        driver.switchTo().window(handles.get(index));
        logger.info("Switched to window[{}]", index);
    }

    public void acceptAlert() {
        new WebDriverWait(driver, Duration.ofSeconds(ReadProperty.getWaitTime5Sec())).until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        logger.info("Alert accepted");
    }

    public void dismissAlert() {
        new WebDriverWait(driver, Duration.ofSeconds(ReadProperty.getWaitTime5Sec())).until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().dismiss();
        logger.info("Alert dismissed");
    }

    public String getAlertText() {
        new WebDriverWait(driver, Duration.ofSeconds(ReadProperty.getWaitTime5Sec())).until(ExpectedConditions.alertIsPresent());
        String txt = driver.switchTo().alert().getText();
        logger.info("Alert text: '{}'", txt);
        return txt;
    }
// --- Low-Level ---

    public String getPageSource() {
        return driver.getPageSource();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public void quit() {
        driver.quit();
    }
// --- Assertions ---

    public void assertTitleIs(String expected, int timeout) {
        logger.info("Assert title equals: '{}' (timeout={}s)", expected, timeout);
        new WebDriverWait(driver, Duration.ofSeconds(timeout)).until(ExpectedConditions.titleIs(expected));
    }

    public boolean verifyPageHeaderIfPresent(String page, String wait, int timeout) {
        try {
            String actual = getText(page, "page_heading", wait, timeout);
            if (!normaliseText(actual).equals(normaliseText(page))) {
                throw new AssertionError("Header text mismatch for page '" + page + "'. " + "Expected: '" + page + "'. " + "Actual: '" + actual + "'");
            }
            return true;

        } catch (TimeoutException | org.openqa.selenium.NoSuchElementException e) {
            return false;

        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Unknown locator") || e.getMessage().contains("Unknown page"))) {
                return false;
            }
            throw e;
        }
    }

    public boolean verifyPageHeaderIfPresent(String page, String wait) {
        return verifyPageHeaderIfPresent(page, wait, 2);
    }

    public void verifyPageUrlMatchesRegistry(String page, boolean strict, int timeout, int pollFrequency) {
        LocatorRegistry.PageMeta meta = LocatorRegistry.getPageMeta(page);
        if (meta == null) {
            throw new AssertionError("Page '" + page + "' not found in locator registry.");
        }
        if (meta.urlRegex != null) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(timeout)).pollingEvery(Duration.ofSeconds(pollFrequency > 0 ? pollFrequency : 1)).until((WebDriver d) -> meta.urlRegex.matcher(d.getCurrentUrl()).find());
                logger.info("URL regex match OK for page '{}'.", page);
                return;

            } catch (TimeoutException e) {
                throw new AssertionError("URL did not match regex for page '" + page + "' within " + timeout + "s. " + "Regex: " + meta.urlRegex.pattern() + " Actual: " + driver.getCurrentUrl());
            }
        }
        String expectedUrl = meta.pageUrl;
        if (expectedUrl == null || expectedUrl.isEmpty()) {
            throw new AssertionError("No page_url/url_regex configured for page '" + page + "'.");
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeout)).pollingEvery(Duration.ofSeconds(pollFrequency > 0 ? pollFrequency : 1)).until((WebDriver d) -> strict ? d.getCurrentUrl().equals(expectedUrl) : d.getCurrentUrl().startsWith(expectedUrl));
            logger.info("URL match OK for page '{}' ({})", page, strict ? "strict" : "prefix");

        } catch (TimeoutException e) {
            String mode = strict ? "exactly" : "to start with";
            throw new AssertionError("URL did not match for page '" + page + "' within " + timeout + "s. " + "Expected " + mode + ": " + expectedUrl + " Actual: " + driver.getCurrentUrl());
        }
    }

    public void verifyPageUrlMatchesRegistry(String page, boolean strict, int timeout) {
        verifyPageUrlMatchesRegistry(page, strict, timeout, 1);
    }

    public void verifyPageUrlMatchesRegistry(String page) {
        verifyPageUrlMatchesRegistry(page, false, defaultTimeout, 1);
    }

    public void assertAllLocatorsPresent(String page, String wait, int timeout, Map<String, String> params) {
        for (String name : LocatorRegistry.getLocatorNames(page)) {
            if (name.contains("menu")) continue;
            if (LocatorRegistry.isPageLoadCheckLocator(page, name)) {
                waitForElementPresent(page, name, timeout, params);
            }
        }
    }

    public void assertAllLocatorsPresent(String page, String wait, int timeout) {
        assertAllLocatorsPresent(page, wait, timeout, Collections.emptyMap());
    }

    public void assertAllLocatorsPresent(String page) {
        assertAllLocatorsPresent(page, "present", defaultTimeout, Collections.emptyMap());
    }

    public void assertMessageText(String page, String name, String expected, String wait, int pollFrequency, int timeout) {
        String actual = getText(page, name, wait, pollFrequency, timeout, Collections.emptyMap());
        if (!normaliseText(actual).equals(normaliseText(expected))) {
            throw new AssertionError("Message text mismatch for " + page + "." + name + ".\n" + "Expected: '" + expected + "'\n" + "Actual:   '" + actual + "'");
        }
    }

    public void assertMessageText(String page, String name, String expected, String wait, int timeout) {
        assertMessageText(page, name, expected, wait, 0, timeout);
    }

    public void assertMessageTextFromDataset(String page, String name, Map<String, Object> dataset, String datasetName, String wait, int timeout) {
        if (dataset == null || dataset.isEmpty()) {
            throw new AssertionError("No dataset loaded. Ensure you ran: " + "Given Dataset '<NAME>' is loaded for the scenario.");
        }
        String pageKey = page.replace(" ", "").toLowerCase();
        String dataKey = pageKey + "_assert_" + name;
        if (!dataset.containsKey(dataKey)) {
            throw new AssertionError("Expected message key '" + dataKey + "' not found in dataset '" + (datasetName != null ? datasetName : "(unknown)") + "'.");
        }
        String expected = String.valueOf(dataset.get(dataKey));
        assertMessageText(page, name, expected, wait, 0, timeout);
    }

    private String normaliseText(String s) {
        return (s == null ? "" : s).replaceAll("\\s+", " ").strip();
    }
}
