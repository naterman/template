package org.example.framework.i18n;

import java.util.ResourceBundle;

import org.example.api.i18n.ILocalization;

import com.google.inject.Inject;

public class Localization implements ILocalization {

    private final ResourceBundle bundle;

    @Inject
    public Localization(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public String getString(String key) {
        return bundle.getString(key);
    }

    @Override
    public Object getObject(String key) {
        return bundle.getObject(key);
    }

    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }
}
