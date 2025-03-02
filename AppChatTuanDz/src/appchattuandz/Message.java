/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package appchattuandz;

/**
 *
 * @author dokha
 */
import java.io.Serializable;

//Class đại diện cho tin nhắn, cần Serializable để truyền qua mạng
public class Message implements Serializable {
    private String sender;   // Người gửi
    private String receiver; // Người nhận
    private String content;  // Nội dung tin nhắn
    private String type;     // Loại tin nhắn ("admin" hoặc "client")

    public Message(String sender, String receiver, String content, String type) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
    }

    // Các getter và setter cho các thuộc tính (bạn cần tự thêm vào)

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("[%s -> %s] (%s): %s", sender, receiver, type, content);
    }
}