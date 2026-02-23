package org.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

// my imports
import org.example.demo.map.CountryPolygon;
import org.example.demo.map.Map;
import org.example.demo.map.Point;

import java.io.IOException;
import java.util.List;

public class HelloApplication extends Application {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 20.0;
    private static final double ZOOM_FACTOR = 1.15;

    // view state
    private static double scale = 1.0;
    private static double x = 1.0;
    private static double y = 1.0;

    // dragging state
    private double dragStartX, dragStartY;
    private double dragStartOffsetX, dragStartOffsetY;

    private static String hoveredCountry = "None";

    @Override
    public void start(Stage stage) throws IOException {
        Map map = new Map();
        List<CountryPolygon> polygons = map.getCountryPolygons();

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // initial draw
        drawMap(gc, polygons);

        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragStartOffsetX = x;
                dragStartOffsetY = y;

                var country = map.getCountryByPoint(new Point(screenXToLon(e.getX()), screenYToLat(e.getY())));
                if (country.isPresent()) {
                    System.out.println(country.get().getName());
                }
                else {
                    System.out.println("No country here...");
                }

                canvas.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                x = dragStartOffsetX + (e.getX() - dragStartX);
                y = dragStartOffsetY + (e.getY() - dragStartY);
                // redraw on drag
                drawMap(gc, polygons);
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                canvas.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });

        canvas.setOnMouseMoved(e -> {
            var country = map.getCountryByPoint(new Point(screenXToLon(e.getX()), screenYToLat(e.getY())));
            if (country.isPresent()) {
                hoveredCountry = country.get().getName();
            }
            else {
                hoveredCountry = "None";
            }
            drawMap(gc, polygons);
        });

        canvas.setOnScroll(e -> {
            double mouseX = e.getX();
            double mouseY = e.getY();

            double oldScale = scale;
            if (e.getDeltaY() > 0) {
                scale = Math.min(scale * ZOOM_FACTOR, MAX_SCALE);
            } else {
                scale = Math.max(scale / ZOOM_FACTOR, MIN_SCALE);
            }

            // adjust offset so zoom is centered on mouse cursor
            double scaleChange = scale / oldScale;
            x = mouseX - scaleChange * (mouseX - x);
            y = mouseY - scaleChange * (mouseY - y);

            drawMap(gc, polygons);
        });

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
            stage.setTitle("World Map");
            stage.setScene(scene);
            stage.show();
    }

    private void drawMap(GraphicsContext gc, List<CountryPolygon> polygons) {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // gc.setFill(Color.DARKGREEN);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);

        for (CountryPolygon country : polygons) {
            if (country.getName().equals(hoveredCountry)) {
                gc.setFill(Color.ORANGE);
            }
            else {
                gc.setFill(Color.DARKGREEN);
            }
            drawPolygon(gc, country);
        }
    }

    private void drawPolygon(GraphicsContext gc, CountryPolygon polygon) {
        List<Point> points = polygon.getPoints();
        if (points.isEmpty()) return;

        double[] xPoints = new double[points.size()];
        double[] yPoints = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            xPoints[i] = lonToScreenX(points.get(i).getLon());
            yPoints[i] = latToScreenY(points.get(i).getLat());
        }

        gc.fillPolygon(xPoints, yPoints, points.size());
        gc.strokePolygon(xPoints, yPoints, points.size());
    }

// Map longitude [-180, 180] to canvas X [0, WIDTH]
private double lonToX(double lon) {
    return (lon + 180.0) / 360.0 * WIDTH;
}

// Map latitude [90, -90] to canvas Y [0, HEIGHT] (Y axis is flipped)
private double latToY(double lat) {
    return (90.0 - lat) / 180.0 * HEIGHT;
}

// map longitude [-180,180] to screen X (with pan/zoom)
private double lonToScreenX(double lon) {
    double baseX = (lon + 180) / 360.0 * WIDTH;
    return baseX * scale + x;
}

// map latitude [90,-90] to screen Y (with pan/zoom)
private double latToScreenY(double lat) {
    double latRad = Math.toRadians(lat);
    double merc   = Math.log(Math.tan(Math.PI/4 + latRad/2));
    double baseY  = (1 - (merc + Math.PI) / (2 * Math.PI)) * HEIGHT;
    return baseY * scale + y;
}

private double screenXToLon(double screenX) {
    return ((screenX - x) / scale) / WIDTH * 360 - 180;
}

private double screenYToLat(double screenY) {
    double ny  = 1 - ((screenY - y) / scale) / HEIGHT;
    double merc = ny * 2 * Math.PI - Math.PI;
    return Math.toDegrees(2 * Math.atan(Math.exp(merc)) - Math.PI / 2);
}

public static void main(String[] args) {
        launch();
    }
}
