package com.friendmod;

public class ChatMessage {
    public String from;
    public String to;
    public String message;
    public long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String from, String to, String message, long timestamp) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.timestamp = timestamp;
    }
}
