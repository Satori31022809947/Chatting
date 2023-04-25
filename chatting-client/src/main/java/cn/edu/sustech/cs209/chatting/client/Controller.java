package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Channel;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.PrivateMessage;
import com.vdurmont.emoji.EmojiParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.exit;

public class Controller implements Initializable {

    @FXML
    public Label currentUsername;
    @FXML
    public TextArea inputArea;
    @FXML
    public Label currentOnlineCnt;
    @FXML
    ListView<String> activeUserView;
    @FXML
    ListView<Message> chatContentList;
    @FXML
    ListView<String> chatList;
    String username;
    String password;
    boolean waitNameResponse;
    boolean isNameValid;
    Set<String> activeUser=new HashSet<>();
    Set<String> allUser=new HashSet<>();
    List<Channel>channelList = new ArrayList<>();
    List<PrivateMessage>privateMessageList = new ArrayList<>();
    int sceneNumber;
    boolean isUser;
    boolean isGroup;
    int chatGroupId;
    String chatUsername;

    class ClientWebSocket {
        public boolean showErr;
        public String errTitle;
        public String errContent;
        public boolean online ;
        PrintWriter output  = null;
        Socket socket ;
        public void close() throws IOException {
            socket.close();
        }
        public void sendMessage(String command,String message) throws IOException {
            if (output==null){
                System.out.println("error when send message");
                return ;
            }
            output.write(command+"\nSTART\n"+message+"\nEND\n");
            output.flush();
            System.out.println("Client send message:"+message);
        }
        public void run() throws IOException {
            online=true;
            socket= new Socket("localhost",8888);
            try (
                 Scanner input = new Scanner(socket.getInputStream(),StandardCharsets.UTF_8)
            ) {
                output=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
                while (true) {
                    String command;
                    try {
                        if (input.hasNextLine()) {
                            command = input.nextLine();
                        } else {
                            System.out.println("connection break");
                            break;
                        }
                    } catch (NoSuchElementException e) {
                        System.out.println("connection break");
                        break;
                    }
                    System.out.println(command);
                    if (command == null) {
                        online = false;
                        break; // 服务器断开连接
                    }
                    System.out.println("Received message from server : " + command);
                    //get message
                    List<String> messages = new ArrayList<>();
                    while (true) {
                        String message = input.nextLine();
                        if (message.equals("END")) {
                            break;
                        }
                        if (message.equals("START")) {
                            continue;
                        }
                        messages.add(message);
                    }
                    for (String s : messages) {
                        System.out.println(s);
                    }
                    if (command.equals("Register")) {
                        showErr = true;
                        errTitle = "Register";
                        errContent = messages.get(0);
                        waitNameResponse = false;
                        continue;
                    }
                    if (command.equals("Login")) {
                        showErr = true;
                        errTitle = "Login";
                        errContent = messages.get(0);
                        waitNameResponse = false;
                        if (messages.get(0).equals("Login success")) {
                            isNameValid = true;
                        }
                        continue;
                    }
                    if (command.equals("Online")) {
                        if (messages.size() < 1) {
                            System.out.println("error1");
                            continue;
                        }
                        String name = messages.get(0);
                        activeUser.add(name);
                        Platform.runLater(Controller.this::refreshUser);
                        continue;
                    }
                    if (command.equals("Offline")) {
                        if (messages.size() < 1) {
                            System.out.println("error2");
                            continue;
                        }
                        String name = messages.get(0);
                        activeUser.remove(name);
                        Platform.runLater(Controller.this::refreshUser);
                        continue;
                    }
                    if (command.equals("User")) {
                        if (messages.size() < 1) {
                            System.out.println("error2");
                            continue;
                        }
                        String[] names = messages.get(0).split(" ");
                        allUser.addAll(Arrays.asList(names));
                        Platform.runLater(Controller.this::refreshUser);
                        continue;
                    }
                    if (command.equals("ActiveUser")) {
                        if (messages.size() < 1) {
                            System.out.println("error2.5");
                            continue;
                        }
                        if (!messages.get(0).equals("")) {
                            String[] names = messages.get(0).split(" ");
//                            System.out.println(names.length);
                            for (String s:names){
                                if (s.equals(""))continue;
                                activeUser.add(s);
                            }
                        }
                        Platform.runLater(Controller.this::refreshUser);
                        continue;
                    }
                    if (command.equals("InfoGroup")) {
                        if (messages.size() < 3) {
                            System.out.println("error3");
                            continue;
                        }
                        int id = Integer.parseInt(messages.get(0));
                        String[] names = messages.get(1).split(" ");
                        List<String> groupUsers = Arrays.asList(names);
                        String groupName = messages.get(2);
                        Channel channel = new Channel(groupUsers, groupName, id);
                        channelList.add(channel);
                        continue;
                    }
                    if (command.equals("PMessageGroup")) {
                        if (messages.size() < 4) {
                            System.out.println("error4");
                            continue;
                        }
                        int channelId = Integer.parseInt(messages.get(0));
                        messages.remove(0);
                        long timestamp = Long.parseLong(messages.get(0));
                        messages.remove(0);
                        String sentBy = messages.get(0);
                        messages.remove(0);
                        Message newMessage = new Message(timestamp, sentBy, messages);
                        Platform.runLater(Controller.this::refreshMessage);
                        for (Channel channel : channelList) {
                            if (channel.id == channelId) {
                                channel.messageList.add(newMessage);
                                break;
                            }
                        }
                        continue;
                    }
                    if (command.equals("PMessageUser")) {
                        if (messages.size() < 4) {
                            System.out.println("error5");
                            continue;
                        }
                        long timestamp = Long.parseLong(messages.get(0));
                        messages.remove(0);
                        String sentBy = messages.get(0);
                        messages.remove(0);
                        String sentTo = messages.get(0);
                        messages.remove(0);
                        PrivateMessage newMessage = new PrivateMessage(timestamp, sentBy, sentTo, messages);
                        privateMessageList.add(newMessage);
                        Platform.runLater(Controller.this::refreshMessage);
                    }

                }
                showErr=true;
                errTitle="Connection";
                errContent= "Connection break";
                Platform.runLater(() -> {
                    showError();
                    exit(0);
                });
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("qwq?");
        }
    }
    public void showError(){
        if (client.showErr) {
            // 创建一个错误提示框并显示
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("information");
            alert.setHeaderText(client.errTitle);
            alert.setContentText(client.errContent);
            alert.showAndWait();
            client.showErr=false;
        }
    }

    public void showError(String title,String content){
        // 创建一个错误提示框并显示
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("information");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void refreshUser(){
        currentOnlineCnt.setText("Total User: "+allUser.size()+", Active User: "+activeUser.size());
        ObservableList<String> content=activeUserView.getItems();
        content.setAll(activeUser);
        activeUserView.setItems(content);
        activeUserView.refresh();
    }
    public void refreshMessage(){
        buildChatSelection();
        if (isGroup){
            setGroup(chatGroupId);
        }
        if (isUser){
            setUser(chatUsername);
        }
    }

    ClientWebSocket client;
    public Thread thread;
    public void setConnection(){
        client=new ClientWebSocket();
        thread = new Thread(() -> {
            try{ // 获取输入输出流
                client.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        setConnection();
        sceneNumber=0;
        Dialog<Pair<Integer, String > > dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);

        // 设置对话框按钮
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType, cancelButtonType);

        // 创建两个文本框控件
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        // 将文本框控件添加到对话框界面
        GridPane gridPane = new GridPane();
        gridPane.add(new Label("Username:"), 0, 0);
        gridPane.add(usernameField, 1, 0);
        gridPane.add(new Label("Password:"), 0, 1);
        gridPane.add(passwordField, 1, 1);
        dialog.getDialogPane().setContent(gridPane);

        // 设置按钮响应事件
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        Node registerButton = dialog.getDialogPane().lookupButton(registerButtonType);
        registerButton.setDisable(true);
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
            registerButton.setDisable(newValue.trim().isEmpty());
        });


        // 显示对话框并等待用户响应
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {

                username = usernameField.getText();
                password = passwordField.getText();

                usernameField.clear();
                passwordField.clear();
                return new Pair<>(1,username+"\n"+password+"\n");
            }
            if (dialogButton == registerButtonType) {

                username = usernameField.getText();
                password = passwordField.getText();

                usernameField.clear();
                passwordField.clear();
                return new Pair<>(2,username+"\n"+password+"\n");
            }
            if (dialogButton == cancelButtonType){
                return new Pair<>(0,"");
            }
            return null;
        });

        while (sceneNumber==0) {
            Optional<Pair<Integer, String>> result = dialog.showAndWait();
            // 处理用户响应
            result.ifPresent(usernamePassword -> {
                int type=usernamePassword.getKey();
                if (type==0){
                    Platform.exit();
                    exit(0);
                }
                String info=usernamePassword.getValue();
                waitNameResponse = true;
                isNameValid = false;
                try {
                    if (type==1)
                        client.sendMessage("Login", info);
                    if (type==2)
                        client.sendMessage("Register", info);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                long startTime = System.currentTimeMillis();
                while (waitNameResponse) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime >= 10000) {
                        break;
                    }
                    if (!waitNameResponse) {
                        break;
                    }
                }
                showError();
                if (waitNameResponse) {
                    //fail to get server response
                    client = null;
                    showError("Connection", "failed to connect server response");
                    exit(0);
                }
                if (isNameValid) {
                    isNameValid=false;
                    System.out.println("qwq!");
                    sceneNumber=1;
                    chatContentList.setCellFactory(new MessageCellFactory());
                    chatContentList.setCache(false);
                    buildChatSelection();
                }
            });

        }
    }
    public synchronized void setUser(String user){
        buildChatSelection();
        chatUsername=user;
        isGroup=false;
        isUser=true;
        currentUsername.setText("current user" + user);
        System.out.println("set chat user"+user);
        chatContentList.getItems().clear();
        for (PrivateMessage privateMessage:privateMessageList){
            if (privateMessage.sentTo.equals(user)||privateMessage.sentBy.equals(user)) {
                System.out.println(privateMessage.sentBy+" "+privateMessage.sentTo);
                List<String>data=new ArrayList<>(); data.add(privateMessage.getData());
                chatContentList.getItems().add(new Message(privateMessage.getTimestamp(),privateMessage.sentBy,data));
                System.out.println("data="+data);
            }
        }

        Platform.runLater(chatContentList::refresh);
    }
    public synchronized void buildChatSelection(){
        ObservableList<String> content = chatList.getItems();
        content.clear();
        Map<String,Long > chat = new HashMap<>();
        //add user
        for (PrivateMessage privateMessage:privateMessageList){
            String sendUser=privateMessage.sentBy;
            if (sendUser.equals(username)){
                sendUser=privateMessage.sentTo;
            }
            sendUser="User: "+sendUser;
            if (chat.get(sendUser)!=null){
                chat.put(sendUser,Math.max(chat.get(sendUser),privateMessage.getTimestamp()));
            }
            else {
                chat.put(sendUser,privateMessage.getTimestamp());
            }
        }

        for (Channel channel:channelList){
            if (channel.messageList==null||channel.messageList.isEmpty())continue;
            String sendUser="Channel: "+channel.id+" "+channel.name;
            long timestamp= channel.messageList.get(channel.messageList.size()-1).getTimestamp();
            chat.merge(sendUser, timestamp, Math::max);
        }
        List<String>contentList = chat.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> String.format(entry.getKey() + " - " + Instant.ofEpochMilli(entry.getValue())
                                .atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("HH:mm"))))
                .toList();

        content.addAll(contentList);
        chatList.setItems(content);
        chatList.refresh();

        chatList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                // 在这里处理双击事件
                String selectedItem = chatList.getSelectionModel().getSelectedItem();
                if (selectedItem.startsWith("User")){
                    String userName=selectedItem.split(" ")[1];
                    setUser(userName);
                }
                if (selectedItem.startsWith("Channel")) {
                    int channelId=Integer.parseInt(selectedItem.split(" ")[1]);
                    setGroup(channelId);
                }
            }
        });
        chatList.refresh();
    }
    public synchronized void setGroup(int groupId){
        buildChatSelection();
        isGroup=true;
        isUser=false;
        chatGroupId=groupId;
        StringBuilder groupName= new StringBuilder("group " + groupId + " : ");
        chatContentList.getItems().clear();
        for (Channel channel:channelList){
            if (channel.id==groupId){
                int count=0;
                groupName.append(channel.name).append("  ");
                for (String username:channel.userList){
                    if (count==3){
                        groupName.append(" ... ");
                    }
                    count++;
                    groupName.append(username).append(", ");
                }
                for (Message message:channel.messageList){
                    chatContentList.getItems().add(message);
                }
                break;
            }
        }
        currentUsername.setText("current channel "+groupName);
        chatContentList.refresh();
    }

    @FXML
    public void selectPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();
        List<String>selectUser = new ArrayList<>(allUser.stream().toList());
        selectUser.remove(username);
        userSel.getItems().addAll(selectUser);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        String sendTo = user.toString();
        System.out.println("sendTo" + sendTo);

        setUser(sendTo);
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() throws IOException {
        Dialog<Pair<String, List<String>>> dialog = new Dialog<>();
        dialog.setTitle("Create Group Chat");

        // Set the button types
        ButtonType confirmButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Create the input fields and the checkbox list
        TextField nameField = new TextField();
        nameField.setPromptText("Group chat name");
        ListView<String> userListView = new ListView<>();
        userListView.setPrefSize(200, 250);
        userListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        List<String> selectUser = allUser.stream().filter(user -> !user.equals(username)).toList();
        ObservableList<String> userObservableList = FXCollections.observableArrayList(selectUser);
        userListView.setItems(userObservableList);

        // Set the content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Group name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Select users:"), 0, 1);
        grid.add(userListView, 1, 1);
        dialog.getDialogPane().setContent(grid);

        // Enable/disable confirm button based on the selection and input
        Node confirmButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setDisable(true);
        nameField.textProperty().addListener((observable, oldValue, newValue) -> confirmButton.setDisable(newValue.trim().isEmpty() || userListView.getSelectionModel().getSelectedItems().isEmpty()));
        userListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            confirmButton.setDisable(nameField.getText().trim().isEmpty() || userListView.getSelectionModel().getSelectedItems().isEmpty());
        });

        // Request focus on the name field by default
        Platform.runLater(nameField::requestFocus);

        // Convert the result to a pair of the group name and the selected users list
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                String groupName = nameField.getText().trim();
                List<String> selectedUsers = new ArrayList<>(userListView.getSelectionModel().getSelectedItems());
                return new Pair<>(groupName, selectedUsers);
            }
            return null;
        });

        Optional<Pair<String, List<String>>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Pair<String, List<String>> pair = result.get();
            String groupName = pair.getKey();
            List<String> selectedUsers = pair.getValue();
            selectedUsers.add(username);
            // Do something with the selected group name and users
            StringBuilder content= new StringBuilder();
            for (String s:selectedUsers){
                content.append(s).append(" ");
            }
            content.append("\n").append(groupName).append("\n");
            client.sendMessage("CreateGroup", content.toString());
        }
    }

    @FXML
    public void selectGroupChat() {
        if (channelList==null||channelList.isEmpty())return ;
        AtomicReference<String> group = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        List<String>selectGroup = new ArrayList<>();
        for (Channel channel:channelList){
            selectGroup.add(channel.id+" "+channel.name);
        }

        userSel.getItems().addAll(selectGroup);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            group.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        int id = Integer.parseInt(group.toString().split(" ")[0]);
        System.out.println("sendTo Channel" + id);

        setGroup(id);
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() throws IOException {
        if (isUser){
            System.out.println("qwq 😊 中文");
            String content = EmojiParser.parseToUnicode(inputArea.getText());
            System.out.println(content);
            if (!content.isEmpty())
                client.sendMessage("SendUser",chatUsername+"\n"+content);
            inputArea.clear();
        }
        if (isGroup){
            String content = inputArea.getText();
            if (!content.isEmpty())
                client.sendMessage("SendGroup",chatGroupId+"\n"+content);
            inputArea.clear();
        }
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());
                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");
                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
