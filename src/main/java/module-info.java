module org.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;


    opens org.example.demo to javafx.fxml;
    exports org.example.demo;
    exports org.example.demo.map;
    opens org.example.demo.map to javafx.fxml;
}