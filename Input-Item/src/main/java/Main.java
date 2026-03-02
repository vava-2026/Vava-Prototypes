import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        InputItem input = new InputItem("Input Item", "input-item");

        VBox root = new VBox(input);
        root.setSpacing(20);
        root.setStyle("-fx-padding: 20;");

        Scene scene = new Scene(root, 400, 200);

        // Linkin CSS
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("InputItem with CSS");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}