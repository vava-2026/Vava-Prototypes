package org.example.demo3;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

// https://wiki.openstreetmap.org/wiki/Tile_servers

public class CustomLayer extends MapLayer {

    private record Country(String name, List<List<MapPoint>> rings) {}

    private final List<Country> countries = new ArrayList<>();
    private final List<Polygon> polygons = new ArrayList<>();

    // Minimum pixel distance between points — increase to skip more points (faster, less detail)
    private static final double MIN_PIXEL_DIST = 1.5;

    // Toggle this to compare performance
    private static final boolean USE_OPTIMIZED = true;

    public CustomLayer() {
        // Load on background thread so UI doesn't freeze
        Thread.ofVirtual().start(this::loadGeoJson);
    }

    private void loadGeoJson() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    //.uri(URI.create("https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_admin_0_countries.geojson"))
                    .uri(URI.create("https://raw.githubusercontent.com/datasets/geo-countries/master/data/countries.geojson"))
                    .build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JSONObject root = new JSONObject(body);
            JSONArray features = root.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                String name = feature.getJSONObject("properties").optString("ADMIN", "Unknown");
                //String name = feature.getJSONObject("properties").optString("NAME", "Unknown");
                JSONObject geometry = feature.getJSONObject("geometry");
                String type = geometry.getString("type");
                JSONArray coordinates = geometry.getJSONArray("coordinates");

                List<List<MapPoint>> rings = new ArrayList<>();
                if (type.equals("Polygon")) {
                    rings.add(parseRing(coordinates.getJSONArray(0)));
                } else if (type.equals("MultiPolygon")) {
                    for (int j = 0; j < coordinates.length(); j++) {
                        rings.add(parseRing(coordinates.getJSONArray(j).getJSONArray(0)));
                    }
                }
                countries.add(new Country(name, rings));
            }

            // Build polygons back on JavaFX thread
            Platform.runLater(this::buildPolygons);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<MapPoint> parseRing(JSONArray ring) {
        List<MapPoint> points = new ArrayList<>();
        for (int i = 0; i < ring.length(); i++) {
            JSONArray coord = ring.getJSONArray(i);
            points.add(new MapPoint(coord.getDouble(1), coord.getDouble(0)));
        }
        return points;
    }

    private void buildPolygons() {
        for (Country country : countries) {
            // Collect all polygons for this country first
            List<Polygon> countryPolygons = new ArrayList<>();

            for (List<MapPoint> ring : country.rings()) {
                Polygon polygon = new Polygon();
                polygon.setFill(Color.color(0.2, 0.6, 1.0, 0.2));
                polygon.setStroke(Color.STEELBLUE);
                polygon.setStrokeWidth(0.5);
                polygon.setMouseTransparent(false);
                polygon.setUserData(ring);
                countryPolygons.add(polygon);
                polygons.add(polygon);
                getChildren().add(polygon);
            }

            // Attach hover handlers that highlight ALL polygons of this country
            for (Polygon polygon : countryPolygons) {
                polygon.setOnMouseEntered(e -> {
                    for (Polygon p : countryPolygons) {
                        p.setFill(Color.color(1.0, 0.5, 0.0, 0.5));
                        p.setStroke(Color.ORANGE);
                        p.toFront();
                    }
                });
                polygon.setOnMouseExited(e -> {
                    for (Polygon p : countryPolygons) {
                        p.setFill(Color.color(0.2, 0.6, 1.0, 0.2));
                        p.setStroke(Color.STEELBLUE);
                    }
                });
            }
        }
        markDirty();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void layoutLayer() {
        if (USE_OPTIMIZED) {
            layoutLayerOptimized();
        } else {
            layoutLayerNaive();
        }
    }

    private void layoutLayerNaive() {
        for (Polygon polygon : polygons) {
            List<MapPoint> ring = (List<MapPoint>) polygon.getUserData();
            polygon.getPoints().clear();
            for (MapPoint mp : ring) {
                Point2D p = getMapPoint(mp.getLatitude(), mp.getLongitude());
                polygon.getPoints().addAll(p.getX(), p.getY());
            }
        }
    }

    private void layoutLayerOptimized() {
        double width = getScene() != null ? getScene().getWidth() : 800;
        double height = getScene() != null ? getScene().getHeight() : 600;

        for (Polygon polygon : polygons) {
            List<MapPoint> ring = (List<MapPoint>) polygon.getUserData();
            polygon.getPoints().clear();

            // Build simplified point list
            List<Double> pts = new ArrayList<>();
            double lastX = Double.MAX_VALUE, lastY = Double.MAX_VALUE;

            // Track bounding box to cull off-screen polygons
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

            // First pass: convert all points to check visibility
            List<double[]> screenPoints = new ArrayList<>();
            for (MapPoint mp : ring) {
                Point2D p = getMapPoint(mp.getLatitude(), mp.getLongitude());
                screenPoints.add(new double[]{p.getX(), p.getY()});
                if (p.getX() < minX) minX = p.getX();
                if (p.getY() < minY) minY = p.getY();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getY() > maxY) maxY = p.getY();
            }

            // Skip polygon if completely outside viewport
            if (maxX < 0 || minX > width || maxY < 0 || minY > height) {
                polygon.setVisible(false);
                continue;
            }
            polygon.setVisible(true);

            // Second pass: simplify by skipping points too close together
            for (double[] sp : screenPoints) {
                double dx = sp[0] - lastX;
                double dy = sp[1] - lastY;
                if (lastX == Double.MAX_VALUE || Math.sqrt(dx * dx + dy * dy) >= MIN_PIXEL_DIST) {
                    pts.add(sp[0]);
                    pts.add(sp[1]);
                    lastX = sp[0];
                    lastY = sp[1];
                }
            }

            polygon.getPoints().addAll(pts);
        }
    }
}
