package com.automation.ui.steps;

import com.automation.ui.utilities.CustomLogger;
import com.automation.ui.utilities.UiActions;
import com.automation.ui.types.TestContext;
import io.cucumber.java.en.Then;
import org.slf4j.Logger;

public class ThenTestSteps {

    private static final Logger logger = CustomLogger.getLogger("ThenTestSteps");

    private final TestContext context;

    public ThenTestSteps(TestContext context) {
        this.context = context;
    }

    @Then("I verify that the text: {string} {word} matches the current page title")
    public void verifyPageTitle(String expectedTitle, String assertionType) {
        String currentTitle = context.driver.getTitle();
        System.out.println(currentTitle);
        if ("exactly".equals(assertionType)) {
            assert expectedTitle.equals(currentTitle) : "Page title does not match exactly. Expected: " + expectedTitle + "\tActual: " + currentTitle;
            logger.info("Page title matches exactly. Expected: {}\tActual: {}", expectedTitle, currentTitle);

        } else if ("partially".equals(assertionType)) {
            assert currentTitle.contains(expectedTitle) : "Page title does not match partially. Expected: " + expectedTitle + "\tActual: " + currentTitle;
            logger.info("Page title matches partially. Expected: {}\tActual: {}", expectedTitle, currentTitle);

        } else {
            throw new IllegalArgumentException("Invalid assertion type: " + assertionType);
        }
    }

    @Then("I verify navigation to {string} page")
    public void verifyNavigation(String page) {
        UiActions actions = new UiActions(context.driver);
        actions.verifyPageLoaded();
        logger.info("{} page load complete.", page);
        actions.verifyPageUrlMatchesRegistry(page, false, 10, 1);
        actions.verifyPageHeaderIfPresent(page, "present", 10);
        actions.assertAllLocatorsPresent(page, "present", 10);
        logger.info("Navigation to {} page successfully completed.", page);
    }

    @Then("I verify {string} text on {string} page")
    public void verifyMessageText(String message, String pageName) {
        new UiActions(context.driver).assertMessageTextFromDataset(pageName, message, context.dataset, context.datasetName, "visible", 10);
    }
}
