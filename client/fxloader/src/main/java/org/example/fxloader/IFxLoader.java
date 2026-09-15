package org.example.fxloader;

import java.io.IOException;

import org.example.fxloader.impl.Container;
import org.jspecify.annotations.NonNull;

public interface IFxLoader {
    public <T extends IController> Container<T> load(@NonNull Class<T> controller)
            throws IllegalStateException, IllegalArgumentException, IOException;

}
