package com.automation.ui.steps;

import com.automation.ui.types.TestContext;
import com.automation.ui.utilities.CustomLogger;
import com.automation.ui.utilities.DataRegistry;
import com.automation.ui.utilities.UiActions;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;

public class GivenTestSteps {

    private static final Logger logger = CustomLogger.getLogger("GivenTestSteps");

    private final TestContext context;

    public GivenTestSteps(TestContext context) {
        this.context = context;
    }

    @Given("I open {string} page of the application")
    public void openApplicationPage(String page) {
        new UiActions(context.driver).openPage(page, 30, "load");
    }

    @Given("Dataset {string} is loaded for the scenario")
    public void loadDataset(String datasetName) {
        if (context.dataFileContent == null || context.dataFileContent.isEmpty()) {
            throw new AssertionError("Data file was not loaded. Ensure 'before_feature' hook loads dataFileContent.");
        }
        try {
            context.dataset = DataRegistry.extractDataset(context.dataFileContent, datasetName);
            context.datasetName = datasetName;
            logger.info("Dataset '{}' loaded successfully for the scenario.", datasetName);

        } catch (DataRegistry.DataException e) {
            throw new AssertionError(e.getMessage());
        }
    }
}