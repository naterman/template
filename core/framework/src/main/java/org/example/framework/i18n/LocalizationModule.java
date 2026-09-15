package org.example.framework.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import org.example.api.i18n.ILocalization;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class LocalizationModule extends AbstractModule {

    private final Locale locale;
    private final String[] bundles;

    public LocalizationModule(Locale locale, String... bundles) {
        this.locale = locale;
        this.bundles = bundles;
    }

    @Provides
    @Singleton
    public ILocalization providLocalization() {
        ResourceBundle bundle = new CompositeResourceBundle(locale, bundles);
        return new Localization(bundle);
    }

}
