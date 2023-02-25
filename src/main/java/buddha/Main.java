package buddha;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;

public final class Main extends Application {

    public final String buttonStyle =
            """
            -fx-background-color: linear-gradient(#ff5400, #be1d00);
            -fx-background-radius: 30;
            -fx-background-insets: 0;
            -fx-text-fill: white;
            """;

    @Override
    public void start(final Stage stage) {
        stage.setTitle("Image Compressor v0.1-pre-alpha");

        var fileChooser = new FileChooser();

        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg"));

        var openOne = new Button("Open one image...");
        openOne.setPrefSize(480, 240);

        openOne.setStyle(buttonStyle);
        openOne.setOnAction(event -> {
            var file = fileChooser.showOpenDialog(stage);
            if (file == null) return;

            openFile(file);
        });

        var openMany = new Button("Open many images...");
        openMany.setPrefSize(480, 240);

        openMany.setStyle(buttonStyle);
        openMany.setOnAction(event -> {
            var files = fileChooser.showOpenMultipleDialog(stage);
            if (files == null) return;

            files.forEach(this::openFile);
        });

        var root = new VBox(40, openOne, openMany);
        root.setPadding(new Insets(40, 40, 40, 40));

        stage.setScene(new Scene(root, 560, 320));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    public void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (Throwable ignored) {
        }
    }
}