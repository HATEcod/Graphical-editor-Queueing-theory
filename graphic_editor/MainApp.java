package org.example.graphic_editor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            BorderPane root = new BorderPane();
            MainController controller = new MainController();

            // Ліворуч панель інструментів
            root.setLeft(controller.getToolbox());
            // По центру робоча зона
            root.setCenter(controller.getEditorPane());
            // Потім дороблю знизу – панель із кнопками симуляції та статистики
            // root.setBottom(controller.getBottomBar());

            Scene scene = new Scene(root, 1200, 700);
            primaryStage.setTitle("Графічний редактор (СМО з рівномірним розподілом)");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
