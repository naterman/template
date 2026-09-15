package org.example.fxloader.impl;

import org.example.fxloader.IContainer;

import javafx.scene.layout.Pane;

public record Container<T>(Pane view, T controller) implements IContainer<T> {
}
