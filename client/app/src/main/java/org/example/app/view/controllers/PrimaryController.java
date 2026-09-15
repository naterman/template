package org.example.app.view.controllers;

import java.util.UUID;

import org.example.fxloader.annotations.FxmlPath;
import org.example.fxloader.impl.AbstractController;
import org.example.map.view.MapViewer;
import org.example.map.view.layers.BasicLayer;

import com.gluonhq.maps.MapPoint;
import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

@FxmlPath("PrimaryView")
public class PrimaryController extends AbstractController {

    @FXML
    private AnchorPane mainView;

    @FXML
    private StackPane stackPane;

    @Inject
    public PrimaryController() {
        System.err.println("Executed Constructor");

    }

    @Override
    public void onInitialize() {
        try {
            final MapViewer mapViewer = new MapViewer();
            mapViewer.setCenter(34.7948027, -86.9937776);

            BasicLayer<Circle> layer = new BasicLayer<>();
            mapViewer.addLayer(layer);

            UUID uuid = layer.add(new Circle(5), 34.7948027, -86.9937776);

            AnchorPane.setTopAnchor(mapViewer, 0d);
            AnchorPane.setLeftAnchor(mapViewer, 0d);
            AnchorPane.setBottomAnchor(mapViewer, 0d);
            AnchorPane.setRightAnchor(mapViewer, 0d);

            stackPane.getChildren().add(mapViewer);

            IO.println("On Window Showed");

            mapViewer.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getButton() != MouseButton.SECONDARY) {
                    return;
                }

                MapPoint position = mapViewer.getMapPosition(event.getX(), event.getY());

                double latitude = position.getLatitude();
                double longitude = position.getLongitude();

                layer.update(uuid, latitude, longitude);

            });

            Label coordinates = new Label("Lat: --\nLon: --");

            mapViewer.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
                MapPoint position = mapViewer.getMapPosition(event.getX(), event.getY());

                coordinates.setText(
                        String.format("Lat: %.6f%nLon: %.6f", position.getLatitude(), position.getLongitude()));
            });

            coordinates.setStyle("""
                    -fx-background-color: rgba(0, 0, 0, 0.75);
                    -fx-text-fill: white;
                    -fx-padding: 8px;
                    -fx-background-radius: 5px;
                    -fx-font-family: monospace;
                    """);

            StackPane.setAlignment(coordinates, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(coordinates, new Insets(0, 10, 10, 0));

            stackPane.getChildren().add(coordinates);

        } catch (Exception ex) {
            System.getLogger(PrimaryController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    @Override
    public Node root() {
        return mainView;
    }

}
