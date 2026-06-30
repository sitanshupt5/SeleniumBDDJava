package com.automation.ui.types;

import org.openqa.selenium.WebDriver;

import java.util.Map;

public class TestContext {
    public WebDriver driver;
    public String projectRoot;
    public String reportsDir;
    public String downloadDir;
    public String appName;
    public String baseUrl;
    public Map<String, Map<String, Object>> dataFileContent;
    public String dataFilePath;
    public Map<String, Object> dataset;
    public String datasetName;
    public String allureRunId;
}
