# SeleniumBDDJava

A Java-based BDD test automation framework for UI testing, built on **Selenium 4**, **Cucumber 7**(JUnit 4 runner), and
**Allure Reports**. Page objects and test data are defined in YAML, keeping locator and data management fully separate
from test logic. Shared framework code live in the `commons` Gradle subproject; application-specific features, data
and pages live in the `application` subproject.

---

## Project Structure

```
SeleniumBDDJava/
├── application/                               # Test application subproject (one per app under test)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/automation/ui/
│       │   └── Runner.java                   (Cucumber JUnit 4 runner - @RunWith + @CucumberOptions)
│       └── resources/
│           ├── features/                     (*.feature files)
│           │   ├── cogmento_login.feature
│           │   └── create_contacts.feature
│           │
│           ├── data/                         (<feature_name>_data.yml - one per feature file)
│           │   ├── cogmento_login_data.yml
│           │   └── create_contacts_data.yml
│           │
│           └── pages/                        (<page_name>_page.yml - auto-discovered at startup)
│               ├── login_page.yml
│               ├── landing_page.yml
│               ├── home_page.yml
│               ├── contacts_page.yml
│               ├── create_contacts_page.yml
│               └── contact_details_page.yml
│
├── commons/                                  # Shared framework code
│   ├── build.gradle                          (all third-party dependencies declared here)
│   └── src/main/
│       ├── java/com/automation/ui/
│       │   ├── steps/
│       │   │   ├── Hooks.java               (@BeforeAll / @Before / @BeforeStep / @AfterStep / @After)
│       │   │   ├── GivenTestSteps.java      (@Given step implementations)
│       │   │   ├── WhenTestSteps.java       (@When step implementations)
│       │   │   └── ThenTestSteps.java       (@Then step implementations)
│       │   │
│       │   ├── types/
│       │   │   └── TestContext.java         (PicoContainer-injected shared state between steps)
│       │   │
│       │   └── utilities/
│       │       ├── UiActions.java           (Selenium interaction API: click, type, wait, verify)
│       │       ├── LocatorRegistry.java     (Loads YAML pages, resolves locators with fallback)
│       │       ├── DataRegistry.java        (Loads YAML data, maps keys to form fields)
│       │       ├── ReadProperty.java        (Reads configuration from config.ini)
│       │       ├── CustomLogger.java        (SLF4J/Logback logger factory wrapper)
│       │       ├── AllureHelpers.java       (Allure attachment helpers: text, PNG)
│       │       └── ExcelUtils.java          (Apache POI helper for reading/writing .xlsx files)
│       │
│       └── resources/
│           └── logback.xml                  (console + file appender; log file named by runId)
│
├── configuration/
│   ├── config.ini                           (on commons classpath; env URLs, wait times, driver config)
│   ├── pipeline-config.env                 (CI/CD project identifiers - fill in once before Bootstrap)
│   │
│   └── cloudformation/
│       ├── oidc-provider.yml               (registers Bitbucket as OIDC provider in AWS IAM)
│       ├── oidc-role.yml                   (IAM role assumed by Bitbucket pipeline steps)
│       └── s3-reports.yml                  (public S3 static website bucket for Allure reports)
│
├── webApp/                                  # Report portal (uploaded to S3 once during Bootstrap)
│   ├── index.html
│   └── js/s3BucketListing.js
│
├── reports/                                 # Generated at runtime
│   └── <app>/
│       ├── allure-results/
│       └── allure-report/
│
├── download/                                # Files downloaded during test runs
├── logs/                                    # Log files: logs/test_<RUN_ID>.log
│
├── build.gradle                             (root; applies java plugin to all subprojects, Java 11)
├── settings.gradle                          (includes 'application' and 'commons')
├── gradle.properties                        (sets org.gradle.java.home to Java 11)
├── gradlew / gradlew.bat                    (Gradle wrapper)
├── run_tests.sh                             (Unix entry point: wraps gradlew + allure generate)
├── run_tests.bat                            (Windows entry point: same as above for cmd.exe)
├── deploy-cfn.sh                            (idempotent cloudformation deploy helper)
└── bitbucket-pipelines.yml
```

`application` depends on `commons` (`implementation project(':commons')`). All reusable framework
code lives in `commons`; application-specific features, data and pages live in `application`.

---

## Running Tests

**Prerequisites:** Java 11 and a Chrome or Firefox browser installed.  
No manual ChromeDriver or GeckoDriver download is needed. `WebDriverManager` resolves and caches the matching driver binary automatically.

The included Gradle wrapper (`gradlew` / `gradlew.bat`) downloads Gradle 8.8 on first use.  
`allure` CLI must be on `PATH` if you want to generate the HTML report locally.

**Run tests (Unix):**

```bash
# Run all @Sample scenarios (default)
./run_tests.sh --app application --tags '@Sample' --env qa --headless false

# Run a specific tag
./run_tests.sh --tags '@CreateContact'

# Combine tags
./run_tests.sh --tags '@Sample and not @wip'

# Run headless
./run_tests.sh --headless true --tags '@Sample'

# Run against a specific environment
./run_tests.sh --app application --env qa
```

**Run tests (Windows):**

```bat
run_tests.bat --app application --tags "@Sample" --env qa --headless false
```

**Run via Gradle directly (targets the `application` subproject):**

```bash
./gradlew :application:test \
    -Dcucumber.filter.tags="@Sample" \
    -Denv="qa" \
    -Dheadless="false"
```
| Argument     | Gradle system property      | Default               | Description                                                               |
|--------------|-----------------------------|-----------------------|---------------------------------------------------------------------------|
| `--app`      | `-Dapp`                     | `application`         | Subproject folder name; controls the report output directory              |
| `--tags`     | `-Dcucumber.filter.tags`    | `@Sample`             | Cucumber tag filter expression                                            |
| `--env`      | `-Denv`                     | `qa`                  | Environment name (`qa` / `dev`); selects base URL from `config.ini`       |
| `--headless` | `-Dheadless`                | `false`               | `true` / `false`; runs Chrome/Firefox without a window                    |
| *(auto)*     | `-DrunId`                   | `yyyyMMdd_HHmmss`     | Log file suffix; auto-generated by `run_tests.sh` if omitted              |

`run_tests.sh` and `run_tests.bat` automatically generate the Allure HTML report after the test run under `reports/<app>/allure-report/`.

**Generate Allure report manually:**

```bash
allure generate reports/application/allure-results --clean -o reports/application/allure-report
allure open reports/application/allure-report
```

---

## Flow of a Test

The following sequence describes what happens from the moment `run_tests.sh` is invoked to the final Allure report.

```
run_tests.sh
│
├── Sets env vars: APP_NAME, ENV, TAGS, RUN_ID (timestamp)
├── Creates output dirs: reports/, logs/, download/
├── Calls: ./gradlew :application:test -Dapp -Dcucumber.filter.tags -Denv -Dheadless -DrunId
│
└── Gradle test task
    │
    ├── Locates Runner.java in application/src/main/java/com/automation/ui/
    │   (testClassesDirs is wired to main sourceSet so JUnit 4 picks it up)
    │
    └── JUnit 4 / Cucumber lifecycle (Hooks.java)
        │
        ├── @BeforeAll (once per test run)
        │   Reads system properties: app, env, runId.
        │   Creates reports/<app>/ and download/<app>/ directories.
        │
        ├── @Before(order=1) (once per scenario)
        │   Derives data file path from scenario URI:
        │       features/foo.feature → data/foo_data.yml
        │   Calls DataRegistry.parseDataFile() → context.dataFileContent
        │   (warns and sets empty map if no data file exists)
        │
        ├── @Before(order=2) (once per scenario)
        │   Populates context fields: projectRoot, reportsDir, downloadDir,
        │   appName, baseUrl (from ReadProperty), allureRunId.
        │
        │   Creates Chrome or Firefox WebDriver via WebDriverManager.
        │       Chrome: --headless=new, --start-maximized, download prefs set.
        │       Firefox: --headless, download prefs set.
        │
        │   Sets page load timeout to 60 s.
        │   Attaches driver to context.driver.
        │
        ├── @BeforeStep (before every step)
        │   Logs the scenario name via CustomLogger (SLF4J → Logback).
        │
        ├── [Step executes - see Step Execution below]
        │
        ├── @AfterStep (after every step)
        │   Attaches current URL to Allure as plain text.
        │   Captures screenshot → attaches to Allure as PNG.
        │   On failure: attaches full page source to Allure.
        │
        ├── @After (once per scenario)
        │   Captures final screenshot → attaches to Allure.
        │   On failure: saves PNG to disk at
        │       reports/<app>/<scenario_name>_<timestamp>.png
        │   Calls driver.quit() and sets context.driver = null.
        │
        └── (after all scenarios)
            run_tests.sh calls: allure generate ... --clean -o ...
            Opens: reports/<app>/allure-report/index.html
```

### Step Execution

Each Gherkin step maps to a Java method annotated with `@Given`, `@When`, or `@Then` in `commons/src/main/java/com/automation/ui/steps/`.
The `TestContext` object is injected into each step class by PicoContainer and carries `driver`, `dataset`, and other shared state.

The following traces the `Verify Login Happy Path` scenario end to end.

```
Given Dataset "HappyPathLogin" is loaded for the scenario
└── GivenTestSteps.loadDataset("HappyPathLogin")
    └── DataRegistry.extractDataset(context.dataFileContent, "HappyPathLogin")
        → context.dataset = {
              login_username_input: "sitanshupt5@gmail.com",
              login_password_input: "Bapuna10@"
          }

And I open "Login" page of the application
└── GivenTestSteps.openApplicationPage("Login")
    └── new UiActions(context.driver).openPage("Login", 30, "load")
        └── LocatorRegistry.getPageUrl("Login") → "https://ui.cogmento.com/"
            driver.get(url)
            WebDriverWait until document.readyState == "complete"

When I populate the fields in "Login" page with corresponding data
└── WhenTestSteps.populateFields("Login")
    └── DataRegistry.mapDataToFields(actions, "Login", context.dataset)
        Iterates dataset keys where page prefix == "login":
            login_username_input (type=input)
                → actions.typeText("Login", "username", "sitanshupt5@gmail.com")

            login_password_input (type=input)
                → actions.typeText("Login", "password", "Bapuna10@")

And I click on "login_button" on "Login" page
└── WhenTestSteps.clickElement("login_button", "Login")
    └── actions.waitForElementClickable("Login", "login_button", 10)
        └── LocatorRegistry.locate() tries candidate locators in YAML order:
            1. "xpath, //div[text()='Login']" → found → el.click()
            2. "css, .ui.fluid.large.blue.submit.button"
               (fallback, not reached)

Then I verify navigation to "Landing" page
└── ThenTestSteps.verifyNavigation("Landing")
    └── actions.verifyPageLoaded()
        → wait until document.readyState == "complete"

        actions.verifyPageUrlMatchesRegistry("Landing", false, 10, 1)
            → URL starts with "https://ui.cogmento.com" ✓

        actions.verifyPageHeaderIfPresent("Landing", "present", 10)
            → attempts getText("Landing", "page_heading")
               → not defined → skip

        actions.assertAllLocatorsPresent("Landing", "present", 10)
            → waits for all locators flagged page_load_check:
                search_box, banner, sidebar, contacts_button, ... ✓
```

---

## Test Case Creation

### 1. Define the Page Object (YAML)

Create `application/src/main/resources/pages/<page_name>_page.yml`. The top-level key is the **page name** used verbatim in all step sentences.

```yaml
MyPage:
  page_url: "https://example.com/my-page"
  page_title: "My Page Title"          # optional; available for title assertions
  url_regex: "example\\.com/my-page"   # optional; used instead of exact URL prefix match

  locators:
    some_input:
      - "xpath, //input[@id='some-input']"
      - "css, input#some-input"        # fallback tried if first locator fails

    submit_button:
      - "xpath, //button[text()='Submit']"

    result_message:
      - "xpath, //div[@class='result']"
```

Mark locators that should be verified on page load by appending `, page_load_check`:

```yaml
page_heading:
  - "xpath, //h1[@class='header'], page_load_check"
```

`LocatorRegistry` scans the `pages/` classpath directory at startup (lazy, thread-safe singleton) and builds a registry of all pages
automatically, no registration step needed.

### 2. Create the Test Data File (YAML)

Create `application/src/main/resources/data/<feature_name>_data.yml`. The filename must match the corresponding feature file stem (e.g.
`create_contacts.feature` → `create_contacts_data.yml`).

Each top-level key is a **dataset name** passed to the `Dataset` step. Keys inside each dataset follow the naming convention
`<page>_<field>_<type>` (see Standardizations).

```yaml
MyScenarioDataset:
  mypage_some_input_input: "Hello World"
  mypage_assert_result_message: "Success"
```

### 3. Write the Feature File

Create `application/src/main/resources/features/<feature_name>.feature`.

```gherkin
Feature: Verify My Page functionality

@MyTag
Scenario: Verify successful submission
    Given Dataset "MyScenarioDataset" is loaded for the scenario
    And I open "MyPage" page of the application
    When I populate the fields in "MyPage" page with corresponding data
    And I click on "submit_button" on "MyPage" page
    Then I verify navigation to "ResultPage" page
    And I verify "result_message" text on "MyPage" page

@MyTag
Scenario Outline: Verify submission with <Scenario>
    Given Dataset "<Dataset>" is loaded for the scenario
    And I open "MyPage" page of the application
    When I populate the fields in "MyPage" page with corresponding data
    And I click on "submit_button" on "MyPage" page
    Then I verify "result_message" text on "MyPage" page
    Examples:
    | Scenario      | Dataset           |
    | valid input   | ValidInputData    |
    | invalid input | InvalidInputData  |
```

### 4. Available Step Sentences

#### Given

| Step                                                | Description                                                                                         |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `Given Dataset "{name}" is loaded for the scenario` | Loads a named dataset from the feature's data YAML into `context.dataset`                           |
| `Given I open "{page}" page of the application`     | Navigates to the `page_url` defined in the page YAML; waits for `document.readyState == "complete"` |

#### When

| Step                                                                  | Description                                                                  |
|-----------------------------------------------------------------------|------------------------------------------------------------------------------|
| `When I populate the fields in "{page}" page with corresponding data` | Bulk-populates all fields on the page using `context.dataset`                |
| `When I enter text "{text}" in "{element}" field on "{page}" page`    | Clears and types text into a specific element                                |
| `When I click on "{element}" on "{page}" page`                        | Waits for the element to be clickable then clicks it                         |
| `When I navigate to "{page_name}" page using "{option}" option`       | Navigates via `sidebar` (hover + click) or `page_url` (direct URL)           |
| `When I switch to "{iframe_locator}" overlay on "{page_name}" page`   | Switches Selenium context into an iframe identified by the given locator key |
| `When I switch to parent frame`                                       | Switches to the parent frame                                                 |
| `When I switch to default content`                                    | Exits all iframes back to the main document                                  |

#### Then

| Step                                                                             | Description                                                                                          |
|----------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `Then I verify navigation to "{page}" page`                                      | Verifies `document.readyState`, URL match, optional page heading, and all `page_load_check` locators |
| `Then I verify "{message}" text on "{page_name}" page`                           | Verifies element text against the `<page>_assert_<message>` value in the loaded dataset              |
| `Then I verify that the text: %{expected} {type} matches the current page title` | Verifies `driver.getTitle()` if `{type}` is `exactly` or `partially`                                 |

---

## Standardizations

### Data Key Naming Convention

All keys in `*_data.yml` files follow the strict format:

```
<page_name>_<field_name>_<field_type>
```

- **`page_name`**: Lowercase version of the YAML page key with spaces removed  
  (e.g. `"Login"` → `"login"`, `"CreateNewContact"` → `"createnewcontact"`).
- **`field_name`**: Exact locator key from the page YAML (can contain underscores).
- **`field_type`**: One of the supported types below.

`DataRegistry.splitKey()` splits on `_`, taking the first segment as `page`, the last segment as `type`, and everything in between as `field`.

### Supported Field Types

| Type          | Value     | Behavior                                                                                        |
|---------------|-----------|-------------------------------------------------------------------------------------------------|
| `input`       | `String`  | Clears the field and types the value                                                            |
| `checkbox`    | `boolean` | Checks or unchecks the checkbox to match the target state                                       |
| `radio`       | `boolean` | Clicks `<field>_true` or `<field>_false` locator                                                |
| `selectValue` | `String`  | Selects `<option value="...">` in a native `<select>`                                           |
| `selectIndex` | `int`     | Selects by 0-based index in a native `<select>`                                                 |
| `selectText`  | `String`  | Selects by visible text in a native `<select>`                                                  |
| `dbutton`     | `boolean` | Custom dropdown opener. `true` = click to open                                                  |
| `dlist`       | `String`  | Custom dropdown option text to select                                                           |
| `assert`      | `String`  | Expected text verified by the `I verify "{message}" text` step; skipped during field population |

**Custom dropdown pairing (`dbutton` + `dlist`):** Both keys must be present for the same dropdown
base. The `dlist` key must end with `_menu`.

```yaml
# Correct pairing for a "status" dropdown:
mypage_status_dropdown_dbutton: true
mypage_status_dropdown_menu_dlist: "Active"
```

Corresponding locator keys in the page YAML:

```yaml
MyPage:
  locators:
    status_dropdown:  # opener - matches dbutton field_name
      - "xpath, //div[@id='status-trigger']"

    status_dropdown_menu:  # menu container - must end with _menu
      - "xpath, //div[@id='status-menu']"
```

### Page YAML Conventions

- Top-level key = page name. Must match **exactly** what is passed in step sentences.
- Locator format: `"<strategy>, <selector>"` where strategy and selector are separated by a comma.
- Supported strategies:
    - `xpath`
    - `css` / `css_selector`
    - `id`
    - `name`
    - `class` / `class_name`
    - `tag` / `tag_name`
    - `link_text`
    - `partial_link_text`
- Multiple locators per element are listed as a YAML sequence. They are tried in order with automatic fallback. The first one that resolves
  within the timeout wins.
- Append `, page_load_check` to any locator that should be verified by the `I verify navigation to` step.
- `url_regex` (optional): if present, URL verification uses `Pattern.matcher(currentUrl).find()` instead of a prefix match against
  `page_url`. Use this for pages with dynamic URL segments (e.g. UUIDs in path).

### Tagging Conventions

| Tag                                           | Purpose                                                                                    |
|-----------------------------------------------|--------------------------------------------------------------------------------------------|
| `@wip`                                        | Work in progress. Excluded from all runs by default (`tags = "not @wip"` in `Runner.java`) |
| `@Sample`                                     | Smoke/sample scenarios included in default runs                                            |
| Feature-specific tags (e.g. `@CreateContact`) | Used to target a specific feature or scenario group                                        |

The default tag filter (`not @wip`) is set in `Runner.java` via `@CucumberOptions(tags = "not @wip")`.
At runtime this is overridden by the `-Dcucumber.filter.tags` system property passed through
`run_tests.sh`, `run_tests.bat`, or the Gradle command line.

### Assertion Keys in Data Files

Assertion values used by the `I verify "{message}" text` step must include `assert` between
the page prefix and the locator name:

```yaml
login_assert_error_message_text: "Invalid login"
```

The `message` parameter in the step (`"error_message_text"`) maps directly to the locator key in the page YAML. The expected text is read
from `<page>_assert_<message>` in the loaded dataset.

`DataRegistry.assertMessageTextFromDataset()` constructs the lookup key as `<pageKey>_assert_<message>` and looks it up in
`context.dataset`.

### Configuration

`configuration/config.ini` is on the `commons` classpath (added via `srcDirs` in `commons/build.gradle`) and is parsed at class-load time by
`ReadProperty`.

```ini
[environment]
type = qa                    # Default environment; overridden by -Denv at runtime

[common_info]
qa_baseURL = https://ui.cogmento.com/
dev_baseURL = https://ui.cogmento.com/

[wait]
sec10 = 10
sec5 = 5

[driver configuration]
browser = chrome             # chrome or firefox; overridden by -Dbrowser at runtime
headless = false             # overridden by -Dheadless at runtime
```

All values in `config.ini` can be overridden at runtime by passing the corresponding `-D<property>` system property to Gradle. System
properties always take precedence over `config.ini` values.

---

## CI/CD Setup (Bitbucket Pipelines + AWS)

This section walks through setting up automated test execution on Bitbucket  Pipelines with reports
hosted on AWS S3. Follow the steps in order ─ each section builds on the previous one.

---

### Pre-Requisites

You need the following before running any pipeline.

#### 1. AWS Account

You need an AWS account and an IAM user with permission to create the infrastructure.

**Create an IAM user:**
1. Log in to the [AWS Console](https://console.aws.amazon.com)
2. Got to **IAM** → **Users** → **Create user**
3. Give it a name (e.g. `bitbucket-bootstrap`)
4. On the **Permissions** page, choose **Attach policies directly**
5. Attach these managed policies:
    - `IAMFullAccess`
    - `AmazonS3FullAccess`
    - `AWSCloudFormationFullAccess`
6. Finish creating the user
7. Go to the user → **Security credentials** → **Create access key**
8. Choose **Other** → create → **download or copy both values**:
    - Access key ID
    - Secret access key

> These values are used only during the one-time Bootstrap step. You can delete them from Bitbucket after Bootstrap is complete.

#### 2. Bitbucket Workspace Slug and UUID

You need two identifiers for your Bitbucket workspace:

**Workspace slug** ─ visible in the URL when you are in your workspace:
```
https://bitbucket.org/{WORKSPACE_SLUG}/
```

**Workspace UUID** ─ found in Bitbucket settings:
1. Go to your Bitbucket workspace
2. Click **Settings** (bottom of the left sidebar)
3. Click **Workspace details**
4. Copy the **Workspace UUID** (looks like `{39c79d5f-e07d-4bc0-9107-bf39b0c24f41}`, including the curly braces)

#### 3. Your Repository Slug

The repo slug is the last part of your repository URL:
```
https://bitbucket.org/{workspace}/{REPO_SLUG}/
```

---

### Step 1 ─ Fill in the Config File

Open `configuration/pipeline-config.env` and replace every placeholder with your real values.

```
WORKSPACE_SLUG      → Your Bitbucket workspace slug
WORKSPACE_UUID      → Your Bitbucket workspace UUID (with curly braces)
REPO_SLUG           → Your Bitbucket repository slug
AWS_REGION          → The AWS region you want to deploy to (e.g. ap-south-1)
S3_BUCKET_NAME      → A unique name for your S3 bucket (e.g. mycompany-allure-reports)
STACK_PREFIX        → A short prefix for your AWS resource names
```

**S3 bucket name rules:** lowercase letters, numbers and hyphens only. No spaces or underscores.
Must be globally unique ─ if the name is already taken by any other AWS account, Bootstrap will fail.
Add something specific like your company name to make it unique.

---

### Step 2 ─ Set BitBucket Repository Variables

Repository variables are like a secure notepad inside Bitbucket. Sensitive values like AWS keys
go here ─ never in files that are commited to the repository.

**How to add repository variables:**
1. Go to your Bitbucket repository
2. Click **Repository settings** (bottom of the left sidebar)
3. Click **Repository variables** (under Pipelines)
4. For each variable below, click **Add variable**, enter the Name and Value, and click **Add**

**Variables to add before Bootstrap:**

| Name | Value | Secured? |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | Your IAM access key ID | Yes (tick the checkbox) |
| `AWS_SECRET_ACCESS_KEY` | Your IAM secret access key | Yes (tick the checkbox) |
| `AWS_DEFAULT_REGION` | Your AWS region (e.g. `ap-south-1`) | No |

> Ticking **Secured** hides the value from logs and from other users.

---

### Step 3 ─ Run the Bootstrap Pipeline

The Bootstrap pipeline create all AWS infrastructure and upload the report portal.
It only need to be run once.

**How to trigger it:**
1. Go to your Bitbucket repository
2. Click **Pipelines** in the left sidebar
3. Click **Run pipeline** (top right)
4. Under **Pipeline**, select **Bootstrap**
5. Click **Run**

The pipeline will:
- Deploy 3 AWS CloudFormation stacks (OIDC provider, IAM role, S3 bucket)
- Upload the report portal website to your S3 bucket
- Print your **Role ARN** and **Portal URL** at the end of the log

**At the end of the Bootstrap log, look for this section:**
```
================================================================
BOOTSTRAP COMPLETE
ACTION REQUIRED ─ Save this as a Bitbucket repository variable:
  Name : BITBUCKET_AWS_ROLE_ARN
  Value: arn:aws:iam::123456789012:role/myproject-bitbucket-oidc-role

Your report portal URL (bookmark this):
  http://mycompany-allure-reports.s3-website-ap-south-1.amazonaws.com
================================================================
```

---

### Step 4 ─ Save the Role ARN

1. Copy the **Value** shown next to ` BITBUCKET_AWS_ROLE_ARN` in the Bootstrap log
2. Go back to **Repository settings** → **Repository variables**
3. Add a new variable:

| Name | Value | Secured? |
|---|---|---|
| `BITBUCKET_AWS_ROLE_ARN` | The ARN you just copied | Yes |

After this step, the `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` variable are no longer needed
and can be deleted from Repository variables.

---

### Step 5 ─ Bookmark the Portal URL

Copy the URL printed under **"Your report portal URL"** in the Bootstrap log and bookmark it in
your browser. This is where all test reports will be published.

---

### Running Tests

Once Bootstrap is complete, you can run tests any time using the **Run-Tests** pipeline.

**How to trigger it:**
1. Go to **Pipelines** → **Run pipeline**
2. Select **Run-Tests**
3. Fill in the parameters (or leave them at their defaults):

| Parameter | Default | What it means |
|---|---|---|
| `APP` | `application` | The app folder to test. Must match a folder name in the repository root (e.g. `application`) |
| `ENV` | `qa` | The environment to test against. `qa` and `dev` are configured in `configuration/config.ini`. |
| `TAGS` | `@Sample` | Which test scenarios to run. Must match a tag from a `.feature` file (e.g. `@CreateContact`, `@Sample`). |

**Finding available tags:**
Open any `.feature` file under `application/features/`. Tags appear above `Scenario:` lines,
starting with `@`:
```gherkin
@CreateContact
Scenario: Create a new contact
```

4. Click **Run**

The pipeline will run the tests, generate the Allure report and upload it to S3 under:
```
APP / YYYY-MM-DD / BUILD_NUMBER /
```
The build number matches the pipeline run number shown in Bitbucket.

---

### Viewing Reports

1. Open the portal URL you bookmarked in Step 5
2. You will see a list of app directories (e.g. `application/`)
3. Click oan app directory to see a list of dates
4. Click a data to see a list of pipeline build numbers
5. Click a build number to open the fill Allure report directly

**Understanding the Allure report:**
- **Overview**  ─ total pass/fail count and pie char for this run
- **Trend**     ─ pass/fail history across multiple runs (appears from the second run onward)
- **Suites**    ─ breakdown by scenario and feature file
- **Timeline**  ─ time taken per scenario
- Click any failed scenario to see the step that failed, screenshot and error message

---

### Re-Deploying Infrastructure

If you ever change ` pipeline-config.env` or the CloudFormation templates, run the **Deploy-Infra**
pipeline to apply the changes. This uses OIDC (no static keys needed).

---

### Troubleshooting:

| Symptom                                           | Cause                                                                       | Fix                                                                                                                                             |
|---------------------------------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Bootstrap fails: `AWS_ACCESS_KEY_ID not set`      | Repo variable not added                                                     | Add `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in Repository variables                                                                     |
| Run-Tests fails: `BITBUCKET_AWS_ROLE_ARN not set` | Step 4 not completed                                                        | Copy the Role ARN from the Bootstrap log and add it as a repo variable                                                                          |
| Run-Tests fails at OIDC step with `AccessDenied`  | Role ARN is wrong or repo/workspace mismatch                                | Re-check that `WORKSPACE_SLUG`, `WORKSPACE_UUID`, and `REPO_SLUG` in `pipeline-config.env` are correct, then re-run Bootstrap                   |
| Portal URL shows blank page or XML                | Bootstrap did not upload the WebApp                                         | Re-run the Bootstrap pipeline                                                                                                                   |
| Trend graphs not showing                          | Expected on first run, no history exists yet                                | Run tests a second time. Trends appear from run 2 onward                                                                                        |
| App directory not visible in portal               | No test run has been completed for that app yet                             | Run the Run-Tests pipeline with the correct `app` value                                                                                         |
| S3 bucket name already taken                      | Another AWS account owns that name                                          | Choose a more unique name in `pipeline-config.env` and re-run Bootstrap                                                                         |
| `No page YAML files found on classpath`           | Pages directory not on classpath                                            | Confirm `application/src/main/resources/pages/` exists and `application/build.gradle` includes resources `src/main/resources` in the sourceSet. |
| `Dataset 'X' not found`                           | Dataset name in feature file does not match a top-level key in `*_data.yml` | Check spelling; key in YAML and `Dataset` step must match exactly (case-sensitive)                                                              |
| `Unknown page: X`                                 | Page name in step does not match any top-level key in any `*_page.yml`      | Check spelling and PascalCase; the top-level YAML key must match the string passed in step sentence exactly.                                    | 


