package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    Controller controller;
    Receiver receiver;

    @Override
    public void start(Stage primaryStage) throws Exception{

        //Parent root = FXMLLoader.load(getClass().getResource("gameWindow.fxml"));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameWindow.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        receiver = new Receiver(controller);
        receiver.start();

        primaryStage.setTitle("One Night JINROU");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    @Override
    public void stop(){ //Application終了時によびだされる
        receiver.exit();    //受信処理終わる
        System.out.println("終了");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
