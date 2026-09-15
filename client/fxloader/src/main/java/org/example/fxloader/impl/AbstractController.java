package org.example.fxloader.impl;

import org.example.fxloader.IController;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

@SuppressWarnings("UnnecessaryParentheses")
public abstract class AbstractController implements IController {

    private ChangeListener<Scene> sceneChangeListener;
    private ChangeListener<Window> windowChangeListener;
    private Node rootView;

    @FXML
    protected void initialize() {
        bindLifecycle(root());
        onInitialize();
    }

    private void bindLifecycle(Node root) {
        this.rootView = root;

        sceneChangeListener = (obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                watchWindow(newScene);
            } else {
                onDestroy();
                cleanupListeners();
            }
        };
        root.sceneProperty().addListener(sceneChangeListener);

        if (root.getScene() != null) {
            watchWindow(root.getScene());
        }
    }

    private void watchWindow(Scene scene) {
        if (scene.getWindow() != null) {
            observeStage(scene.getWindow());
        } else {
            windowChangeListener = ((obsWindow, oldWindow, newWindow) -> {
                if (newWindow != null) {

                } else {
                    scene.windowProperty().removeListener(windowChangeListener);
                    windowChangeListener = null;
                }
            });
            scene.windowProperty().addListener(windowChangeListener);
        }
    }

    private void observeStage(Window window) {
        if (window instanceof javafx.stage.Stage stage) {
            if (stage.isShowing()) {
                IO.println("Firing On Window Show");
                onWindowShow();
            } else {
                ChangeListener<Boolean> showingListener = new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                            Boolean newValue) {
                        if (newValue) {
                            stage.showingProperty().removeListener(this);
                            onWindowShow();
                        }
                    }
                };
                stage.showingProperty().addListener(showingListener);
            }
        }
    }

    private void cleanupListeners() {
        if (rootView != null && sceneChangeListener != null) {
            rootView.sceneProperty().removeListener(sceneChangeListener);
            sceneChangeListener = null;
        }
        if (windowChangeListener != null && rootView != null && rootView.getScene() != null) {
            rootView.getScene().windowProperty().removeListener(windowChangeListener);
            windowChangeListener = null;
        }
    }

    @Override
    public void onDestroy() {
        cleanupListeners();
    }
}
