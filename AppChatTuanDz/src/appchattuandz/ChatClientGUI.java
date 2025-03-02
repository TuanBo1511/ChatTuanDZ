/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package appchattuandz;

/**
 *
 * @author dokha
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ChatClientGUI extends JFrame implements ActionListener {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    private JTextArea messageArea;
    private JTextField messageInput;
    private JButton sendButton;
    private JLabel statusLabel;
    private String username;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    // Màu sắc chủ đạo
    private Color backgroundColor = new Color(204, 255, 229);  // Màu xanh lá cây nhạt
    private Color textColor = new Color(0, 0, 128); // Navy
    private Color buttonColor = new Color(0, 153, 102);       // Màu xanh lá cây đậm


    public ChatClientGUI(String username) {
        super("Client Chat");
        this.username = username;

        // Thiết lập JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());
        getContentPane().setBackground(backgroundColor);

        // Message Area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setBackground(Color.WHITE);
        messageArea.setForeground(textColor);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageInput = new JTextField();
        messageInput.setBackground(Color.WHITE);
        messageInput.setForeground(textColor);
        sendButton = new JButton("Gửi");
        sendButton.setBackground(buttonColor);
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(this);
        messageInput.addActionListener(this);

        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBackground(backgroundColor);
        add(inputPanel, BorderLayout.SOUTH);

        // Status Label
        statusLabel = new JLabel("Đang kết nối...");
        statusLabel.setForeground(textColor);
        add(statusLabel, BorderLayout.NORTH);

        // User List
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(Color.WHITE);
        userList.setForeground(textColor);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setPreferredSize(new Dimension(150, 0));
        add(userListScrollPane, BorderLayout.WEST);

        // Kết nối tới Server
        connectToServer();

        setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Gửi username và type tới server
            output.writeObject(username);
            output.writeObject("client");
            output.flush();

            statusLabel.setText("Đã kết nối với máy chủ với tư cách là: " + username);

            // Bắt đầu thread nhận tin nhắn
            new Thread(new MessageReceiver()).start();

        } catch (IOException e) {
            statusLabel.setText("Kết nối thất bại: " + e.getMessage());
            messageArea.append("Kết nối thất bại: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton || e.getSource() == messageInput) {
            sendMessage();
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText();
        String receiver;

        if (userList.getSelectedValue() != null) {
            receiver = userList.getSelectedValue();
        } else {
            receiver = "all";
        }

        if (!messageText.isEmpty()) {
            Message message = new Message(username, receiver, messageText, "client");
            try {
                output.writeObject(message);
                output.flush();
                messageInput.setText("");
            } catch (IOException ex) {
                messageArea.append("Lỗi gửi tin nhắn: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
        }
    }


    // Inner class để nhận tin nhắn từ server
    class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                Object obj;
                while ((obj = input.readObject()) != null) {
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        messageArea.append(message + "\n");
                    } else if (obj instanceof List) {
                        // Nhận danh sách người dùng
                        List<String> userListFromServer = (List<String>) obj;
                        updateUserList(userListFromServer);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                messageArea.append("Mất kết nối đến máy chủ" + "\n");
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void updateUserList(List<String> userListFromServer) {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                userListModel.addElement("all");
                for (String user : userListFromServer) {
                    userListModel.addElement(user);
                }
            });
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Nhập Tên Tài khoản:");
        if (username != null && !username.isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClientGUI(username));
        } else {
            System.exit(0);
        }
    }
}