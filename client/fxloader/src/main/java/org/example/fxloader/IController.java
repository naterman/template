package org.example.fxloader;

import javafx.scene.Node;

public interface IController {

    abstract void onInitialize();
    abstract Node root();
    default void onWindowShow() {
    }
    default void onDestroy() {
    }
}
