package com.automation.ui.utilities;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class DataRegistry {

    private static final Logger logger = CustomLogger.getLogger(DataRegistry.class.getName());

    public static class DataException extends RuntimeException {
        public DataException(String message) {
            super(message);
        }
    }

    public static String getDataFilePath(String scenarioUri) {
        // e.g. "classpath:features/create_contacts.feature"
        String path = scenarioUri;
        if (path.contains("classpath:")) {
            path = path.substring(path.indexOf("classpath:") + "classpath:".length());
        }
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        String stem = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
        String dir = slash >= 0 ? path.substring(0, slash) : "";
        String dataDir = dir.replace("/features", "/data").replace("\\features", "\\data");
        return dataDir + "/" + stem + "_data.yml";
    }

    public static Map<String, Map<String, Object>> parseDataFile(String classpathPath) {
        Yaml yaml = new Yaml();
        try (InputStream is = DataRegistry.class.getClassLoader().getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new DataException("Data file not found on classpath: " + classpathPath);
            }
            Object raw = yaml.load(is);
            if (!(raw instanceof Map)) {
                throw new DataException("Top-level of data file must be a mapping: " + classpathPath);
            }
            @SuppressWarnings("unchecked") Map<String, Object> top = (Map<String, Object>) raw;
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : top.entrySet()) {
                if (!(e.getValue() instanceof Map)) {
                    throw new DataException("Dataset '" + e.getKey() + "' must be a mapping in: " + classpathPath);
                }
                @SuppressWarnings("unchecked") Map<String, Object> ds = (Map<String, Object>) e.getValue();
                result.put(e.getKey(), ds);
            }
            return result;

        } catch (DataException de) {
            throw de;

        } catch (Exception e) {
            throw new DataException("Failed to parse data file: " + classpathPath + ": " + e.getMessage());
        }
    }

    public static Map<String, Object> extractDataset(Map<String, Map<String, Object>> data, String datasetName) {
        Map<String, Object> ds = data.get(datasetName);
        if (ds == null) {
            throw new DataException("Dataset '" + datasetName + "' not found. Available: " + String.join(", ", data.keySet()));
        }
        return ds;
    }

    // Parse key: <page>_<field>_<type> - page = first, type = last, field = middle
    static String[] splitKey(String key) {
        String[] parts = key.split("_");
        if (parts.length < 3) {
            throw new DataException("Invalid data key '" + key + "'. Expected '<page>_<field>_<type>'.");
        }
        String page = parts[0];
        String ftype = parts[parts.length - 1];
        String fname = String.join("_", Arrays.copyOfRange(parts, 1, parts.length - 1));
        return new String[]{page, fname, ftype};
    }

    public static void mapDataToFields(UiActions actions, String page, Map<String, Object> dataset) {
        String pageKey = page.replace(" ", "").toLowerCase();

        Map<String, Map<String, Object>> dropDownGroups = new LinkedHashMap<>();
        List<String[]> immediateOps = new ArrayList<>();

        for (Map.Entry<String, Object> entry : dataset.entrySet()) {
            String rawKey = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            String[] parts;
            try {
                parts = splitKey(rawKey);
            }catch (DataException de) {
                logger.warn("Skipping malformed keyL {}", rawKey);
                continue;
            }
            String dsPage = parts[0];
            String fieldName = parts[1];
            String ftype = parts[2];

            if (!dsPage.equals(pageKey)) continue;

            if ("dbutton".equals(ftype) || "dlist".equals(ftype)) {
                if ("dbutton".equals(ftype)) {
                    String base = fieldName;
                    Map<String, Object> grp = dropDownGroups.computeIfAbsent(base, k -> new LinkedHashMap<>());
                    grp.put("opener_name", fieldName);
                    grp.putIfAbsent("menu_name", base + "_menu");
                    grp.put("dbutton", value);
                } else {
                    if(!fieldName.endsWith("_menu")) {
                        throw new DataException("dlist key must end with 'menu': " + rawKey);
                    }
                    String base = fieldName.substring(0, fieldName.length() - 5);
                    Map<String, Object> grp = dropDownGroups.computeIfAbsent(base, k -> new LinkedHashMap<>());
                    grp.put("menu_name", fieldName);
                    grp.putIfAbsent("opener_name", base);
                    grp.put("dlist", value);
                }
            } else {
                immediateOps.add(new String[]{fieldName, ftype, rawKey});
            }
        }

        // Execute immediate ops
        for (String[] op : immediateOps) {
            String fieldName = op[0];
            String ftype = op[1];
            String rawKey = op[2];
            Object value = dataset.get(rawKey);
            switch (ftype) {
                case "input":
                    actions.typeText(page, fieldName, String.valueOf(value), true, Collections.emptyMap());
                    break;
                case "checkbox":
                    if (!(value instanceof Boolean)) {
                        throw new DataException("Checkbox value must be boolean for: " + rawKey);
                    }
                    boolean wantChecked = (Boolean) value;
                    boolean isChecked = actions.isElementSelected(page, fieldName, 10, Collections.emptyMap());
                    if (wantChecked && !isChecked) {
                        actions.click(page, fieldName, 10, Collections.emptyMap());

                    } else if (!wantChecked && isChecked) {
                        actions.click(page, fieldName, 10, Collections.emptyMap());
                    }
                    break;
                case "radio":
                    if (!(value instanceof Boolean)) {
                        throw new DataException("Radio value must be boolean for: " + rawKey);
                    }
                    String suffix = (Boolean) value ? "true" : "false";
                    actions.click(page, fieldName + "_" + suffix, 10, Collections.emptyMap());
                    break;
                case "selectvalue":
                    actions.selectByValue(page, fieldName, String.valueOf(value), 10, Collections.emptyMap());
                    break;
                case "selectindex":
                    int idx;
                    try {
                        idx = Integer.parseInt(String.valueOf(value));
                    } catch (Exception e) {
                        throw new DataException("selectIndex requires integer: " + rawKey);
                    }
                    actions.selectByIndex(page, fieldName, idx, 10, Collections.emptyMap());
                    break;
                case "selecttext":
                    actions.selectByVisibleText(page, fieldName, String.valueOf(value), 10, Collections.emptyMap());
                    break;
                case "assert":
                    // skip
                    break;
                default:
                    if (!fieldName.contains("assert")) {
                        logger.warn("Unsupported field type '{}' for key: {}", ftype, rawKey);
                    }
                    break;
            }
        }

        // Execute dropdown groups
        for(Map.Entry<String, Map<String, Object>> entry : dropDownGroups.entrySet()) {
            Map<String, Object> grp = entry.getValue();
            String openerName = (String) grp.get("opener_name");
            String menuName = (String) grp.get("menu_name");
            Object dbutton = grp.get("dbutton");
            Object dlist = grp.get("dlist");

            if (dbutton == null || dlist == null) {
                throw new DataException("Custom dropdown requires both dbutton and dlist for: " + page + "." + entry.getKey());
            }
            if(!(dbutton instanceof  Boolean)) {
                throw new DataException("dbutton must be boolean for: " + page + "." + openerName);
            }
            if (!(dlist instanceof String)) {
                throw new DataException("dlist must be string for: " + page + "." + menuName);
            }

            boolean clickOpener = (Boolean) dbutton;
            String optionText = (String) dlist;

            if (clickOpener) {
                actions.pickFromCustomDropdown(page, openerName, optionText, page, menuName,
                        10, Collections.emptyMap());
            } else {
                org.openqa.selenium.WebElement menuEl = LocatorRegistry.locate(
                        actions.getDriver(), page, menuName, "visible", 10, Collections.emptyMap());
                String q  = optionText.contains("'") ? "\"" + optionText + "\"" : "'" + optionText + "'";
                String xpath = ".//*[self::li or self::div][normalize-space()=" + q + "]";
                menuEl.findElement(org.openqa.selenium.By.xpath(xpath)).click();
            }

        }
    }



}
