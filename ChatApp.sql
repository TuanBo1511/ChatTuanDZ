-- Tạo database (nếu chưa có)
CREATE DATABASE ChatApp;
GO

USE ChatApp;
GO

-- Bảng users
CREATE TABLE users (
    id INT PRIMARY KEY IDENTITY(1,1),
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    -- Các cột khác như email, status, etc.
);

-- Bảng messages
CREATE TABLE messages (
    id INT PRIMARY KEY IDENTITY(1,1),
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    content VARCHAR(MAX) NOT NULL,
    timestamp DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);