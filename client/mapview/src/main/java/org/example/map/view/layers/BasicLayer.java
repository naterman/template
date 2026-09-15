package org.example.map.view.layers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;
import javafx.util.Pair;

public class BasicLayer<T extends Shape> extends MapLayer {

    private final ObservableMap<UUID, Pair<T, MapPoint>> shapes;

    public BasicLayer() {
        this(FXCollections.observableHashMap());
    }

    public BasicLayer(Map<UUID, Pair<T, MapPoint>> shapes) {
        this.shapes = FXCollections.observableMap(new HashMap<>(shapes));
        this.shapes.addListener((MapChangeListener<UUID, Pair<T, MapPoint>>) change -> {
            boolean added = change.wasAdded();
            Pair<T, MapPoint> pair = added ? change.getValueAdded() : change.getValueRemoved();

            if (pair == null) {
                return;
            }

            getChildren().remove(pair.getKey());

            if (added) {
                getChildren().add(pair.getKey());
            }

            markDirty();
        });
    }

    @Override
    protected void initialize() {
        IO.println("Basic Layer Map Ready...");

    }

    public UUID add(T shape, double latitude, double longtitude) {
        UUID uuid = UUID.randomUUID();
        MapPoint location = new MapPoint(latitude, longtitude);

        Pair<T, MapPoint> pair = new Pair<>(shape, location);
        shapes.put(uuid, pair);

        return uuid;
    }

    public void add(UUID uuid, T shape, double latitude, double longtitude) {
        MapPoint location = new MapPoint(latitude, longtitude);
        Pair<T, MapPoint> pair = new Pair<>(shape, location);
        shapes.put(uuid, pair);
    }

    public void remove(UUID uuid) {
        shapes.computeIfPresent(uuid, (uuidEntry, pairEntry) -> {
            getChildren().removeIf((node) -> node.equals(pairEntry.getKey()));
            return null;
        });
    }

    public void update(UUID uuid, T shape) {
        shapes.computeIfPresent(uuid, (uuidEntry, pairEntry) -> {
            return new Pair<>(shape, pairEntry.getValue());
        });
    }

    public void update(UUID uuid, double latitude, double longtitude) {
        shapes.computeIfPresent(uuid, (uuidEntry, pairEntry) -> {
            MapPoint mapPoint = new MapPoint(latitude, longtitude);

            return new Pair<>(pairEntry.getKey(), mapPoint);
        });
    }

    @Override
    protected void layoutLayer() {
        double zoom = baseMap.zoom().get();
        if (zoom < 5.0) {
            for (Pair<T, MapPoint> entry : shapes.values()) {
                entry.getKey().setVisible(false);
            }
            return; // Skip layout computation entirely if fully zoomed out past threshold
        }

        double scale;
        if (zoom <= 10.0) {
            // Range [3.0 to 10.0] maps to a scale factor from ~0.3 to 1.0
            // (zoom - 3.0) / 7.0 gives a 0.0 to 1.0 progression
            scale = Math.max(0.1, (zoom - 2.0) / 8.0) + 0.5;
        } else {
            // Range [10.0 to 20.0] maps scale from 1.0 to a larger multiplier (e.g., 2.5x
            // max growth)
            // Clamped at zoom 20 using Math.min so it stops growing between 20 and 30
            double effectiveZoom = Math.min(zoom, 20.0);
            scale = 1.0 + (2.5 * (effectiveZoom - 10.0) / 10.0);
        }

        double grandparentwidth = getParent().getScene().getWidth();
        double grandparentheight = getParent().getScene().getHeight();

        for (Pair<T, MapPoint> entry : shapes.values()) {
            Shape shape = entry.getKey();
            MapPoint location = entry.getValue();

            Point2D position = getMapPoint(location.getLatitude(), location.getLongitude());

            double x = position.getX();
            double y = position.getY();

            if (shape != null) {
                double shapeWidth = shape.getBoundsInParent().getWidth();
                double shapeHeight = shape.getBoundsInParent().getHeight();

                // Simple bounding-box check (with a small buffer zone so nodes don't pop
                // abruptly)
                boolean isVisible = (x >= (-1 * shapeWidth / 2) && x <= grandparentwidth + (shapeWidth / 2)
                        && y >= (-1 * shapeHeight / 2) && y <= grandparentheight + (shapeHeight / 2));

                shape.setVisible(isVisible);
                if (isVisible) {
                    shape.setTranslateX(x);
                    shape.setTranslateY(y);

                    shape.setScaleX(scale);
                    shape.setScaleY(scale);
                }
            }
        }
    }
}
