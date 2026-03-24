package com.p2p.message;

public class MessageFactory {

    public static <T> Message<T> create(MessageType type, T payload) {
        return new Message<>(type, payload);
    }
}