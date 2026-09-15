package org.example.fxloader.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.example.api.i18n.ILocalization;
import org.example.fxloader.IController;
import org.example.fxloader.IFxLoader;
import org.example.fxloader.annotations.FxmlPath;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import javafx.fxml.FXMLLoader;

@Singleton
public class FxLoader implements IFxLoader {

    private final Injector injector;
    private ILocalization localization;

    /**
     * Construct FxLoader with injected Guice Injector
     *
     * @param injector
     *            guice injector
     */
    @Inject
    public FxLoader(Injector injector, ILocalization localization) {
        this.injector = injector;
        this.localization = localization;
    }

    /**
     * Load Container for given Controller
     *
     * @param <T>
     *            instance of IController
     * @param controller
     *            to resolve FxmlPath for view from.
     * @return Container with View and Controller instances loaded from FXMLLoader.
     * @throws IllegalStateException
     *             if class is not annotated with FxmlPath
     * @throws IllegalArgumentException
     *             if path does not exist
     * @throws IOException
     *             if errors loading file from resources
     */
    @Override
    public <T extends IController> Container<T> load(@NonNull Class<T> controller)
            throws IllegalStateException, IllegalArgumentException, IOException {
        String path = resolve(controller);

        URL location = controller.getResource(path + ".fxml");
        if (location == null) {
            String exception = String.format("FXML File: %s; does not exist.%n", path);
            throw new IllegalArgumentException(exception);
        }

        FXMLLoader loader = new FXMLLoader(location, localization.getBundle());
        loader.setControllerFactory(injector::getInstance);

        return new Container<>(loader.load(), loader.getController());
    }

    private <T extends IController> String resolve(@NonNull Class<T> controller) throws IllegalStateException {
        @Nullable
        FxmlPath annotation = controller.getAnnotation(FxmlPath.class);

        Optional<FxmlPath> view = Optional.ofNullable(annotation);
        if (view.isEmpty()) {
            throw new IllegalStateException("Missing @FxmlPath");
        }

        return view.get().value();
    }

}
