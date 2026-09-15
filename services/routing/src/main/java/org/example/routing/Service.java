package org.example.routing;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.jspecify.annotations.Nullable;

import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperConfig;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.sf.Geometry;
import mil.nga.sf.LineString;
import mil.nga.sf.MultiPolygon;
import mil.nga.sf.Point;

public class Service implements Runnable {

    private GraphHopper hopper;

    public Service(BlockingQueue<String> queue) {

    }

    @Override
    public void run() {
        IO.println("Running Service...");

        Path basePath = Path.of(System.getProperty("graphhopper.data"));
        Path cacheFolder = basePath.resolve("cache");

        GraphHopperConfig config = new GraphHopperConfig();
        config.putObject("graph.elevation.provider", "srtm");
        config.putObject("graph.elevation.cache_dir", cacheFolder.resolve("elevation").toAbsolutePath().toString());
        config.putObject("graph.encoded_values", "car_access, car_average_speed, road_access, road_environment, "
                + "max_speed, ferry_speed, road_class, average_slope");

        config.putObject("import.osm.ignored_highways", "service");
        config.setProfiles(List.of(new Profile("car").setCustomModel(GHUtility.loadCustomModelFromJar("car.json"))));
        config.setCHProfiles(List.of(new CHProfile("car")));

        config.putObject("graph.location", cacheFolder.toAbsolutePath().toString());

        hopper = new GraphHopper();
        hopper.setOSMFile(basePath.resolve("us-south-260828.osm.pbf").toAbsolutePath().toString());
        hopper.init(config);
        hopper.importOrLoad();

    }

    public GHPoint3D getEvel(double lat, double lon) {
        ElevationProvider provider = hopper.getElevationProvider();
        double elev = provider.getEle(lat, lon);
        return new GHPoint3D(lat, lon, elev);
    }

    public @Nullable List<List<GHPoint>> readWithNgaLibrary(File unzippedGpkgFile) {
        List<List<GHPoint>> allPoints = new ArrayList<>();

        // 1. Open the GeoPackage natively
        try (GeoPackage geoPackage = GeoPackageManager.open(unzippedGpkgFile)) {

            // 2. Get a list of all vector feature tables in the database
            List<String> featureTables = geoPackage.getFeatureTables();
            for (String table : featureTables) {
                System.err.println("Table: " + table);
            }
            if (featureTables.isEmpty())
                return null;

            // 3. Open the first table (e.g., roads, boundaries, etc.)
            for (String table : featureTables) {
                System.err.println("Table: " + table);
            }
            FeatureDao featureDao = geoPackage.getFeatureDao("cb_2025_us_place_500k");

            // 4. Query all rows in the table
            // FeatureResultSet featureResultSet = featureDao.queryForAll();
            String whereClause = "STUSPS = ? AND NAME = ?";

            // 2. Define the matching arguments as strings
            String[] whereArgs = new String[]{"AL", "Athens"};

            // 3. Execute the query using your FeatureDao instance
            FeatureResultSet featureResultSet = featureDao.query(whereClause, whereArgs);
            while (featureResultSet.moveToNext()) {
                FeatureRow featureRow = featureResultSet.getRow();

                System.err.println(featureRow.getValue("name"));

                // 5. Extract the geometry wrapper
                GeoPackageGeometryData geometryData = featureRow.getGeometry();

                if (geometryData != null && !geometryData.isEmpty()) {
                    // Extract the actual geometric shape
                    Geometry geometry = geometryData.getGeometry();

                    // You can now pass this geometry to the extraction method below
                    allPoints.addAll(extractNgaBoundaries(geometry));

                }
            }

        }
        return allPoints;
    }

    public List<List<GHPoint>> extractNgaBoundaries(Geometry geometry) {
        List<List<GHPoint>> mapShapes = new ArrayList<>();

        System.err.println("Geo Type: " + geometry.getGeometryType());

        if (null != geometry.getGeometryType())
            switch (geometry.getGeometryType()) {
                case MULTIPOLYGON -> {
                    MultiPolygon multiPolygon = (MultiPolygon) geometry;
                    for (mil.nga.sf.Polygon polygon : multiPolygon.getGeometries()) {
                        mapShapes.add(parseNgaPolygon(polygon));
                    }
                }
                case POLYGON -> mapShapes.add(parseNgaPolygon((mil.nga.sf.Polygon) geometry));
                case POINT -> {
                    Point point = (Point) geometry;
                    List<GHPoint> pointtemp = new ArrayList<>();
                    pointtemp.add(new GHPoint(point.getY(), point.getX()));
                    mapShapes.add(pointtemp);
                }
                default -> {
                }
            }

        return mapShapes;
    }

    private List<GHPoint> parseNgaPolygon(mil.nga.sf.Polygon polygon) {
        List<GHPoint> mapPoints = new ArrayList<>();

        // An NGA Polygon consists of one exterior ring and zero or more inner hole
        // rings.
        // We grab the exterior ring for the outer boundary.
        // org.locationtech.jts.geom.LinearRing exteriorRing =
        // polygon.getExteriorRing();
        LineString lineString = polygon.getExteriorRing();
        for (Point point : lineString.getPoints()) {
            // Point X is Longitude, Point Y is Latitude
            mapPoints.add(new GHPoint(point.getY(), point.getX()));
        }

        return mapPoints;
    }

}
