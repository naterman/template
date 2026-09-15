package org.example.fxloader;

import javafx.scene.layout.Pane;

public interface IContainer<T> {
    Pane view();
    T controller();
}
