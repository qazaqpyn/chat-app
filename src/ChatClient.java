/*
1. Put the fxml file together with the java files.
2. Delete the attribute about the controller from the root control.
3. Delete all xmlns attributes from the root control.
4. Add xmlns:fx="http://javafx.com/fxml" as an attribute of the root control.
5. Save the changes of the fxml file.
*/


import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;

import static java.awt.SystemColor.desktop;

public class ChatClient extends Application {
    String chatUserReceiver = "";
    static HashMap<String, ArrayList<Node>> messages = new HashMap<>();
    ArrayList<String> users = new ArrayList<String>();
    ObservableList<Node> childrenMessage;
    ObservableList<Node> childrenUsers;
    ObservableList<Node> childrenUser;

    ObservableList<Node> childrenLogin;
    int msgIndex = 0;
    String serverIP = "127.0.0.1";
    int port = Integer.parseInt("12345");

    Socket socket = new Socket(serverIP, port);
    DataInputStream in = new DataInputStream(socket.getInputStream());
    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    Scanner scanner = new Scanner(System.in);

    @FXML
    private VBox listPane, login, user;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField txtInput, txtUsername;
    @FXML
    private Button btnEmoji;
    @FXML private PasswordField txtPassword;

    @FXML private Button btnLogin;

    public String username, password;
    private Desktop desktop = Desktop.getDesktop();

    public ChatClient() throws IOException {
    }


    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("./ChatUI2.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Chat");
        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(500);
        primaryStage.show();
        btnEmoji.setOnMouseClicked(event -> {
            openFiles(primaryStage);
        });
    }

    @FXML
    protected void initialize() throws IOException {
        childrenMessage = login.getChildren();
        childrenUsers = listPane.getChildren();
        childrenUser = user.getChildren();

        login.heightProperty().addListener(event -> {
            scrollPane.setVvalue(1);
        });

        txtInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER"))
                sendMessage();
        });
        btnLogin.setOnMouseClicked(event->{
            username = txtUsername.getText();
            password = txtPassword.getText();
            try {
                if(login(out, in, username, password)){
                    childrenMessage.clear();
                    //get list of users in server database
                    storeListOfUsers();
                    addListOfUserTo();
                    Thread t = new Thread(()-> {
                        try {
                            while(true) {
                                String receivedUser = this.receiveString(in);
                                String receivedMessage = this.receiveString(in);
                                this.print(receivedUser + ": " + receivedMessage + "\n");
                                Platform.runLater(() -> {
                                    Node mesNode = messageNode(receivedMessage, false);
                                    //check if opened chatroom is with received user's message if yes add it to chatroom children
                                    if(receivedUser.equals(chatUserReceiver)){
                                        childrenMessage.add(mesNode);
                                    }
                                    //storeMessage
                                    storeMessage(receivedUser, mesNode);
                                });

                            }
                        } catch (IOException ex) {
                            print("Connection drop ");
                        }
                    });
                    t.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    private void readBufferForMessage(){

    }

    private void storeListOfUsers() throws IOException {
        String usersString = receiveString(in);
        String[] usersArray = usersString.split(",");
        for(int i = 0; i<usersArray.length; i++){
            users.add(usersArray[i]);
            messages.put(usersArray[i],new ArrayList<Node>());
        }
    }


    private void addListOfUserTo(){
        for(int i = 0; i<users.size();i++){
            childrenUsers.add(stringNodes(users.get(i)));
        }
    }

    private Node stringNodes(String name){
        HBox box = new HBox();
        box.paddingProperty().setValue(new Insets(10, 10, 10, 10));
        Label label = new Label(name);
        label.setWrapText(true);
        label.setOnMouseClicked(event -> {
            openChat(name);
        });
        box.getChildren().add(label);
        return box;
    }



    private void openChat(Object user){
        chatUserReceiver = String.valueOf(user);
        childrenUser.clear();
        childrenUser.add(stringNodes("Chat Room with "+ chatUserReceiver));

        //change user chatroom text
        childrenMessage.clear();
        ArrayList<Node> listOfMessages = messages.get(user);
        for(int i = 0; i<listOfMessages.size(); i++){
            childrenMessage.add(listOfMessages.get(i));
        }
    }

    private Node messageNode(String text, boolean alignToRight) {
        HBox box = new HBox();
        box.paddingProperty().setValue(new Insets(10, 10, 10, 10));

        if (alignToRight)
            box.setAlignment(Pos.BASELINE_RIGHT);
        Label label = new Label(text);
        label.setWrapText(true);
        box.getChildren().add(label);


        return box;
    }

    private Node imageNode(String imagePath, boolean alignToRight) {
        try {
            HBox box = new HBox();
            box.paddingProperty().setValue(new Insets(10, 10, 10, 10));

            if (alignToRight)
                box.setAlignment(Pos.BASELINE_RIGHT);
            FileInputStream in = new FileInputStream(imagePath);
            ImageView imageView = new ImageView(new Image(in));
            imageView.setFitWidth(50);
            imageView.setPreserveRatio(true);
            box.getChildren().add(imageView);
            return box;
        } catch (IOException ex) {
            ex.printStackTrace();
            return messageNode("!!! Fail to display an image !!!", alignToRight);
        }
    }

    private void sendMessage() {
        Platform.runLater(() -> {
            String text = txtInput.getText();
            txtInput.clear();
            Node mesNode = messageNode(text, true);
            childrenMessage.add(mesNode);
            //storeMessage
            storeMessage(chatUserReceiver, mesNode);
            try {
                sendString(chatUserReceiver, out);
                sendString(text, out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void openFiles(Stage stage) {
        Platform.runLater(() -> {
            final FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    readFile(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    private void readFile(File file) throws IOException {
        System.out.println("name : " + file.getName());
        System.out.println("size (bytes) : " + file.length());
        System.out.println("absolute path? : " + file.isAbsolute());
        System.out.println("exists? : " + file.exists());
        System.out.println("hidden? : " + file.isHidden());
        System.out.println("dir? : " + file.isDirectory());
        System.out.println("file? : " + file.isFile());
        System.out.println("modified (timestamp) : " + file.lastModified());
        System.out.println("readable? : " + file.canRead());
        System.out.println("writable? : " + file.canWrite());
        System.out.println("executable? : " + file.canExecute());
        System.out.println("parent : " + file.getParent());
        System.out.println("absolute file : " + file.getAbsoluteFile());
        System.out.println("absolute path : " + file.getAbsolutePath());
        System.out.println("canonical file : " + file.getCanonicalFile());
        System.out.println("canonical path : " + file.getCanonicalPath());

    }


    public static void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public static void storeMessage(String user, Node messagesNode){
        messages.get(user).add(messagesNode);
    }

    public static void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
    }

    public static String receiveString(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        String str = "";
        int len = in.readInt();
        while (len > 0) {
            int l = in.read(buffer, 0, Math.min(len, buffer.length));
            str += new String(buffer, 0, l);
            len -= l;
        }
        return str;
    }

    public static boolean login(DataOutputStream out, DataInputStream in, String username, String password) throws IOException {
        sendString(username, out);
        sendString(password, out);
        String receivedMessage = receiveString(in);
        print(receivedMessage);
        if(receivedMessage.equals("Y")){
            return true;
        }else{
            return false;
        }
    }

    public static void main(String[] args) throws IOException {

        launch(args);
    }
}