package org.example.style;

public class CustomStyle {

    public static String getDefaultStylesheet() {
        return CustomStyle.class.getResource("/css/default.css").toString();
    }
}
