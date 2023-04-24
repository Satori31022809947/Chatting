package cn.edu.sustech.cs209.chatting.server;
import cn.edu.sustech.cs209.chatting.common.Channel;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.PrivateMessage;
import cn.edu.sustech.cs209.chatting.common.User;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {

    private static List<Socket> clients = new ArrayList<>(); // 保存所有客户端的 Socket 连接
    public static Set<String> nameSet= new HashSet<>();
    public static Map<String,String> namePasswordMap = new HashMap<>();
    public static Map<User,Socket> socketUserMap = new HashMap<>();
    public static Set<String> activeUser = new HashSet<>();
    public static List<User> userList = new ArrayList<>();
    public static List<Channel> channelList = new ArrayList<>();
    public static List<PrivateMessage> privateMessageList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // 创建一个服务器端 socket，监听端口 12345
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("Server started.");
        System.out.println(serverSocket);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            clients.add(clientSocket);

            // 使用一个独立的线程处理该客户端的消息
            Thread thread = new Thread(() -> {
                User user=null;
                try // 获取输入输出流
                    (Scanner in = new Scanner(clientSocket.getInputStream());
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    clientSocket ) {
                    // 循环处理客户端发送的消息
                    while (true) {
                        String command;
                        // 读取客户端发送的消息
                        try {
                            if (in.hasNextLine()) {
                                command = in.nextLine();
                            } else {
                                System.out.println("connection break");
                                break;
                            }
                        } catch (NoSuchElementException e) {
                            System.out.println("connection break");
                            break;
                        }
                        if (command == null) {
                            break; // 客户端断开连接
                        }

                        System.out.println("Received message from client " + clientSocket.getInetAddress().getHostAddress() + ": " + command);
                        //get message
                        List<String>messages = new ArrayList<>();
                        while (true){
                            String message = in.nextLine();
                            if (message.equals("END")){
                                break;
                            }
                            if (message.equals("START")){
                                continue;
                            }
                            messages.add(message);
                        }
                        for (String s:messages){
                            System.out.println(s);
                        }
                        //solve message
                        if (command.equals("HelloServer")){
                            continue;
                        }
                        if (command.equals("Register")){
                            if (messages.size()>=2) {
                                String name = messages.get(0);
                                String password = messages.get(1);
                                System.out.println("client Register :" + name);
                                if (nameSet.contains(name)) {
                                    sendToClient(clientSocket, "Register","name exists");
                                    continue;
                                } else {
                                    sendToClient(clientSocket,"Register","register success");
                                    User newUser=new User(name,password);
                                    userList.add(newUser);
                                    nameSet.add(name);
                                    namePasswordMap.put(name,password);
                                    sendToAllClients("User",getUser());
                                    continue;
                                }
                            }
                            sendToClient(clientSocket, "Register","not provide username or password");
                            continue;
                        }
                        if (command.equals("Login")){
                            if (messages.size()>=2) {
                                String name = messages.get(0);
                                String password = messages.get(1);
                                System.out.println("client Login :" + name);
                                if (!nameSet.contains(name)) {
                                    sendToClient(clientSocket, "Login","name not exists");
                                    continue;
                                } else if (!namePasswordMap.get(name).equals(password)) {
                                    sendToClient(clientSocket, "Login","password wrong");
                                    continue;
                                }else {
                                    for (User user1:userList){
                                        if (user1.userName.equals(name)){
                                            user=user1;
                                            break;
                                        }
                                    }
                                    if (user!=null) {
                                        socketUserMap.put(user, clientSocket);
                                        sendToClient(clientSocket,"Login","Login success");
                                        sendToClient(clientSocket,"ActiveUser",getActiveUser());
                                        sendToClient(clientSocket,"User",getUser());
                                        for (Channel channel:channelList){
                                            if (channel.userList.contains(user.userName)){
                                                sendToClient(clientSocket,"InfoGroup", channel.info());
                                                for (Message message:channel.messageList){
                                                    sendToClient(clientSocket,"PMessageGroup",channel.id+"\n"+message.info());
                                                }
                                            }
                                        }
                                        for (PrivateMessage privateMessage:privateMessageList){
                                            if (privateMessage.sentTo.equals(user.userName)||privateMessage.sentBy.equals(user.userName)){
                                                sendToClient(clientSocket,"PMessageUser",privateMessage.info());
                                            }
                                        }
                                        activeUser.add(user.userName);
                                        sendToAllClients("Online", user.userName);
                                        continue;
                                    }
                                    sendToClient(clientSocket, "Login","can't find user?");
                                    continue;
                                }
                            }
                            sendToClient(clientSocket, "Login","not provide username or password");
                            continue;
                        }
                        if (command.equals("CreateGroup")){
                            if (messages.size()<2){
                                System.out.println("error occur when create group");
                                continue;
                            }
                            String[] users=messages.get(0).split(" ");
                            List<User> groupUsers = new ArrayList<>();
                            List<String> names = Arrays.asList(users);
                            Set<String> groupNames = new HashSet<>(names);
                            String groupName = messages.get(1);
                            for (User user1:userList){
                                if (groupNames.contains(user1.userName)){
                                    groupUsers.add(user1);
                                }
                            }
                            if (groupUsers.size()==0){
                                System.out.println("empty group");
                                continue;
                            }
                            Channel channel=new Channel(names,groupName,channelList.size());
                            channelList.add(channel);
                            for (User user1:groupUsers){
                                sendToClient(socketUserMap.get(user1),"InfoGroup",channel.info());
                            }
                        }
                        if (command.equals("SendGroup")){
                            if (messages.size()<1||user==null){
                                System.out.println("error occur when send to user");
                                continue;
                            }
                            int channelId=Integer.parseInt(messages.get(0));
                            messages.remove(0);
                            Long timeStamp = System.currentTimeMillis();
                            Message newMessage = new Message(timeStamp,user.userName,messages );
                            channelList.get(channelId).sendMessage(newMessage);

                            List<User> groupUsers = new ArrayList<>();
                            Set<String> groupNames = new HashSet<>(channelList.get(channelId).userList);
                            for (User user1:userList){
                                if (groupNames.contains(user1.userName)){
                                    groupUsers.add(user1);
                                }
                            }
                            for (User user1:groupUsers){
                                sendToClient(socketUserMap.get(user1),"PMessageGroup",channelId+"\n"+newMessage.info());
                            }
                        }
                        if (command.equals("SendUser")){
                            if (messages.size()<1||user==null){
                                System.out.println("error occur when send to user");
                                continue;
                            }
                            String userName = messages.get(0);
                            messages.remove(0);
                            User sendTo=null;
                            for (User user1:userList){
                                if (user1.userName.equals(userName)){
                                    sendTo=user1;
                                }
                            }
                            if (sendTo==null){
                                System.out.println("sent to user not exists");
                                continue;
                            }
                            Long timeStamp = System.currentTimeMillis();
                            PrivateMessage newMessage = new PrivateMessage(timeStamp,user.userName,sendTo.userName,messages);
                            privateMessageList.add(newMessage);
                            sendToClient(clientSocket,"PMessageUser",newMessage.info());
                            if (socketUserMap.get(sendTo)!=null) {
                                sendToClient(socketUserMap.get(sendTo),"PMessageUser",newMessage.info());
                            }
                        }
                    }
                    clientSocket.close();
                    clients.remove(clientSocket);
                    if (user != null) {
                        sendToAllClients("Offline" ,user.userName);
                        socketUserMap.remove(user);
                        activeUser.remove(user.userName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }
    private static String getActiveUser(){
        String str="";
        StringBuilder strBuilder = new StringBuilder(str);
        for (String s:activeUser){
            strBuilder.append(s + " ");
        }
        str = strBuilder.toString();
        return str;
    }
    // 向所有客户端发送消息
    private static String getUser(){
        String str="";
        StringBuilder strBuilder = new StringBuilder(str);
        for (User s:userList){
            strBuilder.append(s.userName+ " ");
        }
        str = strBuilder.toString();
        return str;
    }
    private static void sendToAllClients(String command,String message) {
        for (Socket client : clients) {
            sendToClient(client,command,message);
        }
    }
    // 向指定客户端发送消息
    private static void sendToClient(Socket client, String command,String message) {
        try {
            if (client==null||client.isClosed())return ;
            PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            System.out.println("command:"+command);
            System.out.println("message:"+message);
            out.print(command+"\nSTART\n"+message+"\nEND\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
