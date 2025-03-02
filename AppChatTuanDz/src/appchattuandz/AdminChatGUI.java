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
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class AdminChatGUI extends JFrame implements ActionListener {

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

    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JButton blockButton; // Nút Block

    // Màu sắc chủ đạo
    private Color backgroundColor = new Color(230, 230, 250); // Lavender
    private Color buttonColor = new Color(0, 153, 102);       // Màu xanh lá cây đậm
    private Color textColor = new Color(0, 0, 128); // Navy

    public AdminChatGUI(String username) {
        super("Admin Chat");
        this.username = username;

        // Thiết lập JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
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
        statusLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        // Client List
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setBackground(Color.WHITE);
        clientList.setForeground(textColor);
        JScrollPane clientListScrollPane = new JScrollPane(clientList);
        clientListScrollPane.setPreferredSize(new Dimension(200, 0));

        //Block button
        blockButton = new JButton("Chặn");
        blockButton.setBackground(new Color(220,20,60));
        blockButton.setForeground(Color.WHITE);
        blockButton.addActionListener(this);

        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BorderLayout());
        westPanel.setBackground(backgroundColor);
        westPanel.add(clientListScrollPane, BorderLayout.CENTER);
        westPanel.add(blockButton,BorderLayout.SOUTH);
        add(westPanel, BorderLayout.WEST);

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
            output.writeObject("admin");
            output.flush();

            statusLabel.setText("Đã kết nối với máy chủ với tư cách là Quản trị viên: " + username);

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
        } else if (e.getSource() == blockButton) {
            blockClient();
        }
    }

    private void sendMessage() {
        String messageText = messageInput.getText();
        String receiver;
        if (clientList.getSelectedValue() != null) {
            receiver = clientList.getSelectedValue();
        } else {
            receiver = "all";
        }

        if (!messageText.isEmpty()) {
            Message message = new Message(username, receiver, messageText, "admin");
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

    //Hàm block
    private void blockClient() {
        String selectedClient = clientList.getSelectedValue();
        if (selectedClient != null && !selectedClient.equals("all")) {
                try {
                    output.writeObject("BLOCK " + selectedClient); // Gửi lệnh block đến server
                    output.flush();
                    messageArea.append("Bạn đã chặn client: " + selectedClient + "\n"); // Hiển thị thông báo
                } catch (IOException ex) {
                    messageArea.append("Lỗi gửi lệnh block: " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                }

        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một client để block.");
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
                        // Nhận danh sách client từ Server
                        List<String> clientListFromServer = (List<String>) obj;
                        updateClientList(clientListFromServer);
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

        private void updateClientList(List<String> clientListFromServer) {
            SwingUtilities.invokeLater(() -> {
                clientListModel.clear();
                clientListModel.addElement("all");
                for (String client : clientListFromServer) {
                    clientListModel.addElement(client);
                }
            });
        }


    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Nhập Tên Máy Chủ:");
        if (username != null && !username.isEmpty()) {
            SwingUtilities.invokeLater(() -> new AdminChatGUI(username));
        } else {
            System.exit(0);
        }
    }
}