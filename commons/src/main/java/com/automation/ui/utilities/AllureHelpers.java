package com.automation.ui.utilities;

import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;

public class AllureHelpers {

    public static void attachText(String name, String text) {
        Allure.addAttachment(name, "text/plain", text != null ? text : "");
    }

    public static void attachPng(String name, byte[] pngBytes) {
        if (pngBytes != null && pngBytes.length > 0) {
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(pngBytes), ".png");
        }
    }
}