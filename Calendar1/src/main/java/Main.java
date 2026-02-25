import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.view.CalendarView;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        Calendar calendar = new Calendar("My Calendar");
        CalendarSource calendarSource = new CalendarSource("My Calendars");
        calendarSource.getCalendars().add(calendar);

        CalendarView calendarView = new CalendarView();
        calendarView.getCalendarSources().add(calendarSource);

        Scene scene = new Scene(calendarView, 900, 600);

        primaryStage.setTitle("Simple Scheduler");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}