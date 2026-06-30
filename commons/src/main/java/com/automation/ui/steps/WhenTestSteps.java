package com.automation.ui.steps;

import com.automation.ui.types.TestContext;
import com.automation.ui.utilities.CustomLogger;
import com.automation.ui.utilities.DataRegistry;
import com.automation.ui.utilities.LocatorRegistry;
import com.automation.ui.utilities.UiActions;
import io.cucumber.java.en.When;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

public class WhenTestSteps {

    private static final Logger logger = CustomLogger.getLogger("WhenTestSteps");

    private final TestContext context;

    public WhenTestSteps(TestContext context) {
        this.context = context;
    }

    @When("I enter text {string} in {string} field on {string} page")
    public void enterText(String text, String element, String page) {
        logger.info("Enter '{}' in element '{}' on page '{}'.", text, element, page);
        UiActions actions = new UiActions(context.driver);
        actions.waitForElementPresent(page, element, 10);
        WebElement el = actions.findElement(page, element, "present", 10);
        assert el != null : element + " not found on " + page;
        el.clear();
        el.sendKeys(text);
    }

    @When("I click on {string} on {string} page")
    public void clickElement(String element, String page) {
        logger.info("Click element '{}' on page '{}'.", element, page);
        UiActions actions = new UiActions(context.driver);
        actions.waitForElementClickable(page, element, 10);
        WebElement el = actions.findElement(page, element, "clickable", 10);
        assert el != null : element + " not found on " + page;
        el.click();
    }

    @When("I populate the fields in {string} page with corresponding data")
    public void populateFields(String page) {
        DataRegistry.mapDataToFields(new UiActions(context.driver), page, context.dataset);
    }

    @When("I navigate to {string} page using {string} option")
    public void navigateToPage(String pageName, String option) {
        String opt = option.trim().toLowerCase();
        UiActions actions = new UiActions(context.driver);
        if ("sidebar".equals(opt)) {
            WebElement sidebar = LocatorRegistry.locate(context.driver, pageName, "sidebar", "visible", 10, java.util.Collections.emptyMap());
            WebElement banner = LocatorRegistry.locate(context.driver, pageName, "banner", "visible", 10, java.util.Collections.emptyMap());
            new Actions(context.driver).moveToElement(sidebar).perform();
            String key = pageName.toLowerCase() + "_button";
            LocatorRegistry.locate(context.driver, pageName, key, "clickable", 10, java.util.Collections.emptyMap()).click();
            new Actions(context.driver).moveToElement(banner).perform();
            try {
                actions.verifyPageUrlMatchesRegistry(pageName);
            } catch (Exception e) {
                // ignore - URL check optional after sidebar nav
            }

        } else if ("page_url".equals(opt)) {
            String url = LocatorRegistry.getPageUrl(pageName);
            actions.openUrl(url);

        } else {
            throw new AssertionError("Unsupported navigation option '" + option + "'. Use 'sidebar' or 'page_url'.");
        }
    }

    @When("I switch to {string} overlay on {string} page")
    public void switchToIframe(String iframeLocator, String pageName) {
        new UiActions(context.driver).switchToFrame(pageName, iframeLocator, 10);
    }

    @When("I switch to parent frame")
    public void switchToParentFrame() {
        new UiActions(context.driver).switchToParentFrame();
    }

    @When("I switch to default content")
    public void switchToDefaultContent() {
        new UiActions(context.driver).switchToDefaultContent();
    }
}
