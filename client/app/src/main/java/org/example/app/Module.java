package org.example.app;

import org.example.fxloader.FxLoaderModule;

import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new FxLoaderModule());
    }

}
