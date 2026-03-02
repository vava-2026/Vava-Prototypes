import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class InputItem extends VBox {

    private TextField textField;
    private Label errorLabel;

    public InputItem(String text, String style) {

        // Base elements
        textField = new TextField();
        errorLabel = new Label();
        textField.setPromptText(text);

        // Link design from css file
        textField.getStyleClass().add(style);

        // Error
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);

        // Расположение
        setSpacing(5);
        setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(textField, errorLabel);
    }

    // Получить текст
    public String getText() {
        return textField.getText();
    }

    // Установить текст
    public void setText(String text) {
        textField.setPromptText(text);
    }

    // Show Error
    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        textField.setStyle("-fx-border-color: red;");
    }

    // Hide Error
    public void clearError() {
        errorLabel.setVisible(false);
        textField.setStyle("");
    }

    // Change Position
    public void setPosition(Pos alignment) {
        setAlignment(alignment);
    }

    // Change Spacing
    public void setSpacingBetween(double spacing) {
        setSpacing(spacing);
    }

    // Validation
    public boolean validateRequired() {
        if (getText() == null || getText().trim().isEmpty()) {
            showError("This field is required");
            return false;
        }
        clearError();
        return true;
    }

}