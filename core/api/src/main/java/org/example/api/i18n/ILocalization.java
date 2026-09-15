package org.example.api.i18n;

import java.util.ResourceBundle;

public interface ILocalization {
    public String getString(String key);
    public Object getObject(String key);
    public ResourceBundle getBundle();
}
