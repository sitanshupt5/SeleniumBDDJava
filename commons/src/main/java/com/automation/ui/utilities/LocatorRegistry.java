package com.automation.ui.utilities;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

public class LocatorRegistry {

    private static final Logger logger = CustomLogger.getLogger(LocatorRegistry.class.getName());

    public static class PageMeta {
        public final String pageUrl;
        public final String pageTitle;
        public final Pattern urlRegex;

        public PageMeta(String pageUrl, String pageTitle, Pattern urlRegex) {
            this.pageUrl = pageUrl;
            this.pageTitle = pageTitle;
            this.urlRegex = urlRegex;
        }
    }

    // registry: PageName -> elementName -> List<"strategy, selector">
    private static volatile Map<String, Map<String, List<String>>> registry = null;
    private static volatile Map<String, PageMeta> pageMeta = null;
    private static final Object LOCK = new Object();

    private static void ensureLoaded() {
        if (registry == null) {
            synchronized (LOCK) {
                if (registry == null) {
                    scanAll();
                }
            }
        }
    }

    private static void scanAll() {
        Map<String, Map<String, List<String>>> reg = new LinkedHashMap<>();
        Map<String, PageMeta> meta = new LinkedHashMap<>();

        try {
            List<URL> pageFiles = findPageYamls();
            if (pageFiles.isEmpty()) {
                throw new RuntimeException("No page YAML files found on classpath under the 'pages/' directory.");
            }

            Yaml yaml = new Yaml();
            for (URL url : pageFiles) {
                try (InputStream is = url.openStream()){
                    Map<String, Object> data = yaml.load(is);
                    if (data == null) continue;
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        String pageName = entry.getKey();
                        if (!(entry.getValue() instanceof Map)) continue;
                        Map<String, Object> pageBlock = (Map<String, Object>) entry.getValue();
                        String pageUrl = (String) pageBlock.get("page_url");
                        String pageTitle = (String) pageBlock.get("page_title");
                        String urlRegexStr = (String) pageBlock.get("url_regex");
                        Pattern urlRegex = null;
                        if (urlRegexStr != null && !urlRegexStr.isEmpty()) {
                            urlRegex = Pattern.compile(urlRegexStr);
                        }
                        meta.put(pageName, new PageMeta(pageUrl, pageTitle, urlRegex));

                        Object locsObj = pageBlock.get("locators");
                        if (!(locsObj instanceof Map)) continue;
                        Map<String, Object> locs = (Map<String, Object>) locsObj;

                        Map<String, List<String>> normalized = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> locEntry : locs.entrySet()) {
                            String key = locEntry.getKey();
                            Object val = locEntry.getValue();
                            List<String> lines = new ArrayList<>();
                            if (val instanceof String) {
                                lines.add((String) val);
                            } else if (val instanceof  List) {
                                for (Object item : (List<?>) val) {
                                    lines.add(item.toString());
                                }
                            }
                            for (String line : lines) {
                                parseLineStrict(line);
                            }
                            normalized.put(key, lines);
                        }
                        reg.merge(pageName, normalized, (existing, incoming) -> {
                            existing.putAll(incoming);
                            return existing;
                        });
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load page YAML: {}: {}", url, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan page YAML file: " + e.getMessage(), e);
        }

        registry = reg;
        pageMeta = meta;
        logger.info("LocatorRegistry loaded {} pages from classpath.", reg.size());
    }

    private static List<URL> findPageYamls() {
        List<URL> results = new ArrayList<>();
        try {
            ClassLoader cl =  Thread.currentThread().getContextClassLoader();
            String basePath = "pages";
            Enumeration<URL> resources = cl.getResources(basePath);
            while (resources.hasMoreElements()) {
                URL dirUrl = resources.nextElement();
                if ("file".equals(dirUrl.getProtocol())) {
                    java.io.File dir = new java.io.File(dirUrl.toURI());
                    if (dir.isDirectory()) {
                        for (java.io.File f : Objects.requireNonNull(dir.listFiles())) {
                            if (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml")) {
                                results.add(f.toURI().toURL());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning page YAML files: {}", e.getMessage());
        }
        return results;
    }

    static By parseLineStrict(String line) {
        if (line == null || !line.contains(",")) {
            throw  new RuntimeException("Locator line must be 'strategy, selector': " + line);
        }
        int comma  = line.indexOf(',');
        String strat = line.substring(0, comma).trim().toLowerCase();
        String rest = line.substring(comma + 1).trim();
        rest = rest.replaceAll(",\\s*page_load_check\\s*$", "").trim();
        switch (strat) {
            case "xpath":               return By.xpath(rest);
            case "css":
            case "css_selector":        return By.cssSelector(rest);
            case "id":                  return By.id(rest);
            case "name":                return By.name(rest);
            case "class":
            case "class_name":          return By.className(rest);
            case "tag":
            case "tag_name":            return By.tagName(rest);
            case "link_text":           return By.linkText(rest);
            case "partial_link_text":   return By.partialLinkText(rest);
            default: throw new RuntimeException("Unknown locator strategy: " + strat + " in: " + line);
        }
    }

    private static boolean isPageLoadCheck(String line) {return line.toLowerCase().contains("page_load_check");}

    private static String interpolate(String selector, Map<String, String> params) {
        if (selector == null || !selector.contains("{") || params == null) return selector;
        String result = selector;
        for (Map.Entry<String, String> e : params.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    public static String getPageUrl(String page) {
        ensureLoaded();
        PageMeta m =  pageMeta.get(page);
        return m != null ? m.pageUrl : null;
    }

    public static String getPageTitle(String page) {
        ensureLoaded();
        PageMeta m = pageMeta.get(page);
        return m != null ? m.pageTitle : null;
    }

    public static Pattern getUrlRegex(String page) {
        ensureLoaded();
        PageMeta m = pageMeta.get(page);
        return m != null ? m.urlRegex : null;
    }

    public static PageMeta getPageMeta(String page) {
        ensureLoaded();
        return pageMeta.get(page);
    }

    public static List<By> getCandidates(String page, String name, Map<String, String> params) {
        ensureLoaded();
        Map<String, List<String>> pageBlock = registry.get(page);
        if (pageBlock == null) throw new RuntimeException("Unknown page: " + page);
        List<String> lines = pageBlock.get(name);
        if (lines == null || lines.isEmpty()) throw new RuntimeException("Unknown locator '" + name + "' on page '" + page + "'");
        List<By> result = new ArrayList<>();
        for (String line : lines) {
            int c = line.indexOf(',');
            if (c == -1) {
                throw new RuntimeException( "Invalid locator format. Expected 'strategy, locator' but got: " + line);
            }
            String strat = line.substring(0, c).trim().toLowerCase();
            String rest = line.substring(c + 1).trim().replaceAll(",?\\s*page_load_check\\s*$", "").trim();
            String interpolated = interpolate(rest, params);
            By by;
            switch (strat) {
                case "xpath":
                    by = By.xpath(interpolated);
                    break;
                case "css":
                case "css_selector":
                    by = By.cssSelector(interpolated);
                    break;
                case "id":
                    by = By.id(interpolated);
                    break;
                case "name":
                    by = By.name(interpolated);
                    break;
                case "class":
                case "class_name":
                    by = By.className(interpolated);
                    break;
                case "tag":
                case "tag_name":
                    by = By.tagName(interpolated);
                    break;
                case "link_text":
                    by = By.linkText(interpolated);
                    break;
                case "partial_link_text":
                    by = By.partialLinkText(interpolated);
                    break;
                default:
                    throw new RuntimeException("Unknown strategy: " + strat);
            }
            result.add(by);
        }
        return result;
    }

    public static WebElement locate(WebDriver driver, String page, String name, String waitStrategy, int timeoutSec, Map<String, String> params) {
        List<By> candidates = getCandidates(page, name, params != null ? params : Collections.emptyMap());
        Exception lastEx = null;
        for (By by : candidates) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
                WebElement el;
                switch (waitStrategy.toLowerCase()) {
                    case "visible":
                        el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                        break;
                    case "clickable":
                        el = wait.until(ExpectedConditions.elementToBeClickable(by));
                        break;
                    default:
                        el = wait.until(ExpectedConditions.presenceOfElementLocated(by));
                        break;
                }
                return el;

            } catch (Exception e) {
                lastEx = e;
            }
        }
        throw new org.openqa.selenium.TimeoutException("Element '" + page + "." + name + "' not found with any candidate within " + timeoutSec + "s", lastEx);
    }

    public static boolean isPageLoadCheckLocator(String page, String name) {
        ensureLoaded();
        Map<String, List<String>> pageBlock = registry.get(page);
        if (pageBlock == null) return false;
        List<String> lines = pageBlock.get(name);
        if (lines == null) return false;
        return lines.stream().anyMatch(LocatorRegistry::isPageLoadCheck);
    }

    public static Iterable<String> getLocatorNames(String page) {
        ensureLoaded();
        Map<String, List<String>> pageBlock = registry.get(page);
        if (pageBlock == null) return  Collections.emptyList();
        return pageBlock.keySet();
    }
}
