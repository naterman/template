package org.example.framework.i18n;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class CompositeResourceBundle extends ResourceBundle {

    private final Map<String, Object> values = new HashMap<>();

    public CompositeResourceBundle(Locale locale, String... baseNames) {
        for (String baseName : baseNames) {
            ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);

            for (String key : bundle.keySet()) {
                values.put(key, bundle.getObject(key));
            }
        }
    }

    @Override
    protected Object handleGetObject(String key) {
        return values.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(values.keySet());
    }
}
