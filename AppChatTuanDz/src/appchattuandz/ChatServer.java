/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package appchattuandz;

/**
 *
 * @author dokha
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static ClientHandler adminHandler = null; // Thêm biến cho admin

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Da mo cong nay chu: " + PORT);

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới kết nối: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } finally {
            serverSocket.close();
            // Đóng kết nối cơ sở dữ liệu khi server tắt
            DatabaseManager.closeConnection();
        }
    }

    public static void broadcastMessage(Message message) {
        for (ClientHandler clientHandler : clientHandlers) {
            //Kiểm tra xem client đó có bị chặn hay không
            if (!clientHandler.isBlocked()) {
                clientHandler.sendMessage(message);
            }
        }
    }

    //Hàm gửi tin nhắn đến một client cụ thể
    public static void sendMessageToClient(Message message, String clientUsername) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getUsername().equals(clientUsername)) {
                //Kiểm tra xem client đó có bị chặn hay không
                 if (!clientHandler.isBlocked()) {
                     clientHandler.sendMessage(message);
                     return;
                 } else {
                     System.out.println("Client " + clientUsername + " đã bị chặn, không gửi tin nhắn.");
                     return;
                 }
            }
        }
        System.out.println("Không tìm thấy client " + clientUsername + ".");
    }

    //Hàm gửi tin nhắn đến admin
    public static void sendMessageToAdmin(Message message) {
        if (adminHandler != null) {
            adminHandler.sendMessage(message);
        } else {
            System.out.println("Admin không trực tuyến.");
        }
    }

    //Hàm gửi danh sách client đến tất cả client
    public static void sendUserListToAllClients() {
        for (ClientHandler clientHandler : clientHandlers) {
            List<String> clientUsernames = new ArrayList<>();
            //Liệt kê danh sách các client khác, không bao gồm admin và chính bản thân client
            for (ClientHandler otherClient : clientHandlers) {
                if (!otherClient.getClientType().equals("admin") && !clientHandler.getUsername().equals(otherClient.getUsername())) {
                    clientUsernames.add(otherClient.getUsername());
                }
            }
            clientHandler.sendUserList(clientUsernames);
        }
    }


    // Inner class để xử lý từng client
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private String username;
        private String clientType; // "admin" hoặc "client"
        private boolean isBlocked = false; // Trạng thái bị chặn của client

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(clientSocket.getOutputStream());
                input = new ObjectInputStream(clientSocket.getInputStream());

                // Nhận username và type từ client
                username = (String) input.readObject();
                clientType = (String) input.readObject();

                System.out.println("Client " + username + " (" + clientType + ") logged in.");

                //Gửi danh sách user hiện tại cho client
                sendUserList(getCurrentUserList());

                //Gửi danh sách client đã cập nhật đến admin và client
                sendUserListToAllClients();



                Message message = null;
                String command = null;

                // Lắng nghe tin nhắn từ client
                while (true) {
                    Object received = input.readObject();

                    if (received instanceof String) {
                        command = (String) received;
                        message = null;
                    } else if (received instanceof Message) {
                        message = (Message) received;
                        command = null;
                    } else {
                        System.err.println("Nhận đối tượng không hợp lệ từ client.");
                        continue; // Bỏ qua đối tượng không hợp lệ
                    }

                    //Xử lý command nếu có command
                    if (command != null && command.startsWith("BLOCK")) {
                        String clientToBlock = command.substring(5).trim();
                        System.out.println("Nhận lệnh block cho client: " + clientToBlock + " từ admin");

                        for (ClientHandler client : clientHandlers) {
                            if (client.getUsername().equals(clientToBlock)) {
                                client.setBlocked(true);
                                System.out.println("Client " + clientToBlock + " bị chặn.");
                                break;
                            }
                        }
                    }
                    //Nếu không có command thì kiểm tra message
                    else if (message != null){
                         System.out.println("Nhận tin nhắn: " + message);
                            DatabaseManager.saveMessage(message);

                            if (clientType.equals("admin")) {
                                // Nếu là admin, gửi tin nhắn đến client được chỉ định (nếu có) hoặc broadcast
                                if (message.getReceiver().equals("all")) {
                                    broadcastMessage(message);
                                } else {
                                    sendMessageToClient(message, message.getReceiver());
                                }
                            } else {
                                //Nếu không bị chặn mới cho gửi
                                if(!isBlocked) {
                                    // Nếu là client, gửi tin nhắn đến admin
                                    if (message.getReceiver().equals("admin")) {
                                        sendMessageToAdmin(message);
                                    } else if (message.getReceiver().equals("all")) {
                                        broadcastMessage(message);
                                    } else {
                                        sendMessageToClient(message, message.getReceiver());
                                    }
                                } else {
                                    System.out.println("Client " + username + " đang bị chặn và cố gắng gửi tin nhắn.");
                                }

                            }
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Lỗi xử lý client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    clientHandlers.remove(this);
                    if (clientType.equals("admin")) {
                        adminHandler = null; // Admin disconnected
                        System.out.println("Admin đã đăng xuất.");
                    }
                    System.out.println("Client " + username + " đã đăng xuất.");

                    //Gửi danh sách client đã cập nhật đến admin
                    sendUserListToAllClients();
                } catch (IOException e) {
                    System.err.println("Lỗi đóng socket của client: " + e.getMessage());
                }
            }
        }

        //Lấy danh sách client đang online
        private List<String> getCurrentUserList() {
            List<String> userList = new ArrayList<>();
            //Không thêm admin hay all mặc định nữa
            for (ClientHandler clientHandler : clientHandlers) {
                if (!clientHandler.getUsername().equals(this.username) && !clientHandler.getClientType().equals("admin")) {
                     userList.add(clientHandler.getUsername());
                }

            }
            return userList;
        }


        public String getUsername() {
            return username;
        }

        public String getClientType() {
            return clientType;
        }

        public void sendMessage(Message message) {
            try {
                output.writeObject(message);
                output.flush();
            } catch (IOException e) {
                System.err.println("Lỗi gửi tin nhắn đến client: " + e.getMessage());
            }
        }

        //Hàm gửi danh sách client đến admin
        public void sendUserList(List<String> userList) {
            try {
                output.writeObject(userList);
                output.flush();
            } catch (IOException e) {
                System.err.println("Lỗi gửi danh sách client cho admin: " + e.getMessage());
            }
        }

        //Hàm set trạng thái block của client
        public void setBlocked(boolean blocked) {
            isBlocked = blocked;
        }

        //Hàm kiểm tra trạng thái block của client
        public boolean isBlocked() {
            return isBlocked;
        }

         public void closeConnection() throws IOException {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                    System.out.println("Closed connection for client: " + username);
                }
                if (input != null) input.close();
                if (output != null) output.close();

            } catch (IOException e) {
                System.err.println("Error closing connection for client: " + username + ": " + e.getMessage());
                throw e;
            }
        }
    }
}