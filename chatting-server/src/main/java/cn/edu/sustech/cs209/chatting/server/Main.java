package cn.edu.sustech.cs209.chatting.server;
import java.io.*;
import java.net.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Main {

    private static List<Socket> clients = new ArrayList<>(); // 保存所有客户端的 Socket 连接

    public static void main(String[] args) throws IOException {

        // 创建一个服务器端 socket，监听端口 12345
        ServerSocket serverSocket = new ServerSocket(12345);

        System.out.println("Server started.");

        // 循环接受客户端连接
        while (true) {
            Socket clientSocket = serverSocket.accept();
            clients.add(clientSocket);

            // 使用一个独立的线程处理该客户端的消息
            Thread thread = new Thread(() -> {
                try // 获取输入输出流
                    (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    clientSocket ) {

                    // 循环处理客户端发送的消息
                    while (true) {
                        // 读取客户端发送的消息
                        String message = in.readLine();

                        if (message == null) {
                            break; // 客户端断开连接
                        }

                        System.out.println("Received message from client " + clientSocket.getInetAddress().getHostAddress() + ": " + message);

                        // 判断消息是否为指令，如果是则向指定客户端发送消息
                        if (message.startsWith("sendTo ")) {
                            String[] parts = message.split(" ");
                            String clientId = parts[1];
                            String content = parts[2];
                            sendToClient(clientId, content);
                        } else {
                            // 否则向所有客户端发送消息
                            sendToAllClients(message);
                        }
                    }
                    clientSocket.close();
                    clients.remove(clientSocket);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        }
    }

    // 向所有客户端发送消息
    private static void sendToAllClients(String message) {
        for (Socket client : clients) {
            try {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                out.println(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 向指定客户端发送消息
    private static void sendToClient(String clientId, String message) {
        for (Socket client : clients) {
            if (client.getInetAddress().getHostAddress().equals(clientId)) {
                try {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                    out.println(message);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
