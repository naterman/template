package org.example.fxloader;

import org.example.fxloader.impl.FxLoader;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class FxLoaderModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IFxLoader.class).to(FxLoader.class).in(Scopes.SINGLETON);
    }

}
