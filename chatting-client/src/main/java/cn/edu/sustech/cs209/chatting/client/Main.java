package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;

public class Main extends Application {

    private Stage primaryStage;
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        Controller controller = fxmlLoader.getController();
        primaryStage.setOnCloseRequest(event -> {
            try {
                controller.client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            controller.thread.interrupt();
        });
        stage.setTitle("Chatting Client");
        stage.show();

    }
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
