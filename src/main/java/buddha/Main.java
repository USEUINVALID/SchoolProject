package buddha;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;

public final class Main extends Application {

    @Override
    public void start(final Stage stage) {
        stage.setTitle("Image Compressor v0.1-pre-alpha");

        var fileChooser = new FileChooser();

        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg"));

        var openOne = new Button("Open one image...");
        openOne.setOnAction(e -> {
            var file = fileChooser.showOpenDialog(stage);
            if (file == null) return;

            openFile(file);
        });

        var openMany = new Button("Open many images...");
        openMany.setOnAction(event -> {
            var files = fileChooser.showOpenMultipleDialog(stage);
            if (files == null) return;

            files.forEach(this::openFile);
        });

        GridPane.setConstraints(openOne, 0, 0);
        GridPane.setConstraints(openMany, 1, 0);

        var pane = new GridPane();
        pane.setHgap(24);
        pane.setVgap(24);
        pane.getChildren().addAll(openOne, openMany);

        var root = new VBox(48);
        root.getChildren().addAll(pane);
        root.setPadding(new Insets(48, 48, 48, 48));

        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    public void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (Throwable ignored) {}
    }
}