package org.example.demo3;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        MapView mapView = new MapView();
        mapView.setCenter(new MapPoint(40.7128, -74.0060));
        mapView.addLayer(new CustomLayer());
        mapView.setZoom(12);

        Scene scene = new Scene(new BorderPane(mapView), 800, 600);
        stage.setTitle("GluonHQ Maps");
        stage.setScene(scene);
        stage.show();
    }

    public static void entry(String[] args) {
        launch();
    }
}

