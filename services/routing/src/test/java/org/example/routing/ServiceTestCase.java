package org.example.routing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

public class ServiceTestCase {

    private Path basePath;

    @BeforeEach
    public void setup() {
        String dataFolder = System.getProperty("graphhopper.data");
        basePath = Path.of(dataFolder);
    }

    @Test
    public void testPath() {
        assertThat(basePath.toFile()).exists();
        assertThat(basePath.toFile()).isDirectory();
    }

    @Test
    public void testServiceSetup() {
        BlockingQueue<String> queue = mock();
        Service service = new Service(queue);
        service.run();
    }

    @Test
    public void testReadGeopkgSetup() {
        BlockingQueue<String> queue = mock();
        Service service = new Service(queue);
        service.run();
        try {
            File alabama = basePath.resolve("cb_2025_us_all_500k.gpkg").toFile();
            assertThat(alabama).exists();
            List<List<GHPoint>> list = service.readWithNgaLibrary(alabama);
            assertThat(list).isNotNull();
            assertThat(list).isNotEmpty();
            for (List<GHPoint> sublist : list) {
                for (GHPoint point : sublist) {
                    GHPoint3D point3D = service.getEvel(point.getLat(), point.getLon());
                    System.err.println("3D Point: " + point3D);
                }
            }

        } catch (Exception e) {
            System.err.println("Error:" + e.getMessage());
        }
    }

    @Test
    public void testKml() {
        try (InputStream input = new FileInputStream(basePath.resolve("cb_2025_01_cousub_500k.kml").toFile())) {
            // Instantiate the schema configuration and parser
            // KMLConfiguration configuration = new KMLConfiguration();
            // Parser parser = new Parser(configuration);

            // // Parse the KML file into a SimpleFeature tree
            // Object rootFeature = parser.parse(input);

            // if (rootFeature instanceof SimpleFeature simpleFeature) {
            // for (Object object : simpleFeature.getProperties()) {
            // System.err.println(object);
            // }

            // }

            // if (rootFeature instanceof SimpleFeatureCollection collection) {
            // try (SimpleFeatureIterator iterator = collection.features()) {
            // while (iterator.hasNext()) {
            // SimpleFeature feature = iterator.next();

            // // Extract the County Name
            // // Note: KML attributes are mapped to standard SimpleFeature attributes.
            // // The standard KML <name> tag is usually mapped to "name"
            // String name = (String) feature.getAttribute("name");
            // System.out.println("Processing county: " + name);

            // // Extract the JTS Geometry
            // Geometry geom = (Geometry) feature.getDefaultGeometry();

            // // 4. Pass the geometry to your extraction logic
            // if (geom != null) {
            // // You can reuse the exact same extractBoundaries() method
            // // you wrote earlier for the Shapefiles!
            // // List<List<MapPoint>> points = extractBoundaries(feature);
            // }
            // }
            // }
            // }

            // Extract feature information
            // System.out.println("Root Feature Name: " + rootFeature.getAttribute("name"));

            // Object object = rootFeature.getDefaultGeometry();
            // System.err.println(object);

            Map<String, List<List<GHPoint>>> countyShapes = new HashMap<>();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false); // Ignore namespaces for easier tag querying
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(input);

            NodeList placemarks = doc.getElementsByTagName("Placemark");

            for (int i = 0; i < placemarks.getLength(); i++) {
                Element placemark = (Element) placemarks.item(i);

                // 1. Extract the actual NAME from SimpleData
                String countyName = extractSimpleDataValue(placemark, "NAME");
                if (countyName == null) {
                    countyName = "Unknown_County_" + i;
                }

                // 2. Find all Polygons within this Placemark
                List<List<GHPoint>> polygons = new ArrayList<>();
                NodeList coordinateNodes = placemark.getElementsByTagName("coordinates");

                for (int j = 0; j < coordinateNodes.getLength(); j++) {
                    String rawCoords = coordinateNodes.item(j).getTextContent().trim();
                    polygons.add(parseKmlCoordinateString(rawCoords));
                }

                countyShapes.put(countyName, polygons);
            }

            for (Map.Entry<String, List<List<GHPoint>>> count : countyShapes.entrySet()) {
                System.err.println("Processing: " + count.getKey());
                for (List<GHPoint> count2 : count.getValue()) {
                    System.err.println(count2);

                }

            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getLocalizedMessage());
        }
    }

    private static String extractSimpleDataValue(Element placemark, String targetName) {
        NodeList simpleDataNodes = placemark.getElementsByTagName("SimpleData");
        for (int k = 0; k < simpleDataNodes.getLength(); k++) {
            Element simpleData = (Element) simpleDataNodes.item(k);
            if (targetName.equals(simpleData.getAttribute("name"))) {
                return simpleData.getTextContent().trim();
            }
        }
        return null;
    }

    private static List<GHPoint> parseKmlCoordinateString(String rawCoords) {
        List<GHPoint> mapPoints = new ArrayList<>();

        // KML separates points with whitespace (spaces, tabs, newlines)
        String[] points = rawCoords.split("\\s+");

        for (String point : points) {
            if (point.isEmpty())
                continue;

            // KML point format: longitude,latitude,altitude
            String[] lonLatAlt = point.split(",");
            if (lonLatAlt.length >= 2) {
                try {
                    double lon = Double.parseDouble(lonLatAlt[0]);
                    double lat = Double.parseDouble(lonLatAlt[1]);
                    // Gluon expects Latitude, Longitude
                    mapPoints.add(new GHPoint(lat, lon));
                } catch (NumberFormatException e) {
                    // Skip malformed coordinate pairs
                }
            }
        }
        return mapPoints;
    }

}
