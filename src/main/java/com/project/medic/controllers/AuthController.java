package com.project.medic.controllers;

import com.google.gson.Gson;
import com.project.medic.Run;
import com.project.medic.config.Config;
import com.project.medic.dto.SignInDTO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class AuthController extends Application {

    @FXML
    private Button btnNoConnect;

    @FXML
    private Button buttonAuth;

    @FXML
    private Group groupNoConnect;

    @FXML
    private ImageView imageClose;

    @FXML
    private ImageView imageCollapse;

    @FXML
    private ImageView imageInfo;

    @FXML
    private Label labelInfo;

    @FXML
    private Label labelNoConnect;

    @FXML
    private Label labelRecovery;

    @FXML
    private Label labelRegister;

    @FXML
    private Pane paneAuth;

    @FXML
    private Pane paneInfo;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField textFieldEmail;

    private double offsetPosX;
    private double offsetPosY;

    private Image imageError;
    private Image imageInformation;

    private Timer timerNoConnect;

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(Run.class.getResource("scene/auth.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 700);
        stage.setScene(scene);
        stage.show();

        scene.setOnMousePressed(event -> {
            offsetPosX = stage.getX() - event.getScreenX();
            offsetPosY = stage.getY() - event.getScreenY();
        });
        scene.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + offsetPosX);
            stage.setY(event.getScreenY() + offsetPosY);
        });

    }

    @FXML
    void initialize() throws URISyntaxException {

        // Top panel
        actionWithCloseImg();
        actionWithCollapseImg();

        // For notify
        String path = Objects.requireNonNull(Run.class.getResource("images/info.png")).toURI().toString();
        imageInformation = new Image(path);

        path = Objects.requireNonNull(Run.class.getResource("images/error.png")).toURI().toString();
        imageError = new Image(path);

        // Keyboard
        keyboardOnTextFieldEmail();
        keyboardOnPasswordField();

        // Button auth
        mouseOnButtonAuth();

        // Label register
        mouseOnLabelRegister();

        // Label recovery
        mouseOnLabelRecovery();

        // Check connection
        if (!isConnection()) drawNoConnection();

    }

    /**
     * A function to interact with the minimize button.
     */
    @FXML
    public void actionWithCloseImg() {

        imageClose.setOnMouseEntered(event -> imageClose.setOpacity(1.0));
        imageClose.setOnMouseExited(event -> imageClose.setOpacity(0.5));
        imageClose.setOnMouseClicked(event -> {
            Stage stage = (Stage) imageClose.getScene().getWindow();
            stage.close();

            System.exit(1);
        });

    }

    /**
     * A function to interact with the close button.
     */
    @FXML
    public void actionWithCollapseImg(){

        imageCollapse.setOnMouseEntered(event -> imageCollapse.setOpacity(1.0));
        imageCollapse.setOnMouseExited(event -> imageCollapse.setOpacity(0.5));
        imageCollapse.setOnMouseClicked(event -> {
            Stage stage = (Stage) imageCollapse.getScene().getWindow();
            stage.setIconified(true);
        });

    }

    /**
     * Function to display information.
     * @param text
     * @param error
     */
    @FXML
    public void sendNotify(String text, boolean error){

        imageInfo.setImage(error ? imageError : imageInformation);
        labelInfo.setText(text);
        paneInfo.setVisible(true);

    }

    /**
     * A function to control typing for a text field.
     */
    @FXML
    public void keyboardOnTextFieldEmail(){

        textFieldEmail.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER) authorization();
        });
        textFieldEmail.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if(!newValue.isEmpty() && !newValue.matches("^[a-zA-Z\\d]+$") && !newValue.matches("^[-\\w.]+@([A-z\\d][-A-z\\d]+\\.)+[A-z]{2,4}$")){

                textFieldEmail.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color:red");
                sendNotify("Неверный формат!", false);

            } else {
                textFieldEmail.setStyle("-fx-background-color: white; -fx-text-fill: black");
                paneInfo.setVisible(false);
            }
        });

    }

    /**
     * A function to control typing for a password field.
     */
    @FXML
    public void keyboardOnPasswordField(){

        passwordField.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER) authorization();
        });
        passwordField.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if(!newValue.matches("(?=^.{6,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$")){
                passwordField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color:red");
                sendNotify("Неверный формат пароля!", false);

            } else {
                passwordField.setStyle("-fx-background-color: white; -fx-text-fill: black");
                paneInfo.setVisible(false);
            }
        });

    }

    /**
     * Function to track mouse events for the login button.
     */
    @FXML
    public void mouseOnButtonAuth(){
        buttonAuth.setOnMouseEntered(event -> buttonAuth.setStyle("-fx-background-color: gray"));
        buttonAuth.setOnMouseExited(event -> buttonAuth.setStyle("-fx-background-color: white"));
        buttonAuth.setOnMouseClicked(event -> authorization());
    }

    /**
     * Function for authorization.
     */
    @FXML
    public void authorization(){

        String usernameOrEmail = textFieldEmail.getText();
        String password = passwordField.getText();

        if(!usernameOrEmail.matches("^[a-zA-Z\\d]+$") && !usernameOrEmail.matches("^[-\\w.]+@([A-z\\d][-A-z\\d]+\\.)+[A-z]{2,4}$")) {
            textFieldEmail.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color:red");
            sendNotify("Неверный формат!", false);
            return;
        }

        if(!password.matches("(?=^.{6,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$")){
            passwordField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color:red");
            sendNotify("Неверный формат пароля!", false);
            return;
        }

        if (!isConnection()) {
            drawNoConnection();
            return;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            SignInDTO sign = new SignInDTO(usernameOrEmail, password);
            HttpPost request = new HttpPost(Config.url + Config.host + ":" + Config.port + "/api/auth/signing");
            request.setHeader("content-type", "application/json");
            request.setEntity(new StringEntity(new Gson().toJson(sign), "UTF-8"));

            CloseableHttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                new MainController().start((Stage) buttonAuth.getScene().getWindow());
            } else {
                sendNotify("Неверный логин или пароль!", true);
            }
            response.close();

        } catch (IOException e) {
            sendNotify("Ошибка! Попробуйте позже", true);
        }
    }

    /**
     * Function to track mouse events for the register label.
     */
    @FXML
    public void mouseOnLabelRegister(){
        labelRegister.setOnMouseEntered(event -> labelRegister.setTextFill(Paint.valueOf("#215790")));
        labelRegister.setOnMouseExited(event -> labelRegister.setTextFill(Paint.valueOf("#6198d3")));
        labelRegister.setOnMouseClicked(event -> {
            try {
                new RegisterController().start((Stage) labelRegister.getScene().getWindow());
            } catch (IOException e) {
                sendNotify("Произошла ошибка!", true);
            }
        });
    }

    /**
     * Function to track mouse events for the recovery label.
     */
    @FXML
    public void mouseOnLabelRecovery(){
        labelRecovery.setOnMouseEntered(event -> labelRecovery.setTextFill(Paint.valueOf("#215790")));
        labelRecovery.setOnMouseExited(event -> labelRecovery.setTextFill(Paint.valueOf("#6198d3")));
        labelRecovery.setOnMouseClicked(event -> {
            try {
                new RecoveryController().start((Stage) labelRecovery.getScene().getWindow());
            } catch (IOException e) {
                sendNotify("Произошла ошибка!", true);
            }
        });
    }

    /**
     * A function to check the connection to the server.
     * @return result connect
     */
    public boolean isConnection(){

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(Config.host, Config.port), 10);
            return true;
        } catch (IOException e) {
            drawNoConnection();
            return false;
        }

    }

    /**
     * The function is called when there is no internet connection.
     */
    @FXML
    public void drawNoConnection(){
        groupNoConnect.setVisible(true);
        if(timerNoConnect != null){
            timerNoConnect.cancel();
            timerNoConnect = null;
        }

        timerNoConnect = new Timer();
        timerNoConnect.schedule(new TaskTimer(), 1000, 1000);
    }

    /**
     * The class is used to create a timer.
     */
    private class TaskTimer extends TimerTask{

        private int countdown;

        public TaskTimer(){
            this.countdown = 30;

            btnNoConnect.setOnMouseEntered(event -> btnNoConnect.setTextFill(Paint.valueOf("gray")));
            btnNoConnect.setOnMouseExited(event -> btnNoConnect.setTextFill(Paint.valueOf("white")));

            btnNoConnect.setOnMouseClicked(event -> {
                this.countdown = 30;
                if(isConnection()){
                    groupNoConnect.setVisible(false);
                    timerNoConnect.cancel();
                    timerNoConnect = null;
                }
            });
        }

        @Override
        public void run() {

            this.countdown -= 1;
            if(this.countdown <= 0 && isConnection()) {

                groupNoConnect.setVisible(false);
                timerNoConnect.cancel();
                timerNoConnect = null;

            } else if(this.countdown <= 0) {
                this.countdown = 30;
            }

            Platform.runLater(() -> labelNoConnect.setText(String.format("%02d:%02d", (this.countdown / 60), (this.countdown % 60))));
        }

    }

}
