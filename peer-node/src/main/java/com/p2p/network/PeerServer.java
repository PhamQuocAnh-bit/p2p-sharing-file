package com.p2p.network;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.p2p.handler.MessageHandler;
import com.p2p.message.Message;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;



public class PeerServer {
    private int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageHandler handler = new MessageHandler();
    public PeerServer(int port) {
        this.port = port;
    }
    public void start() {
        try(ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handle(socket)).start();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private void handle(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            String json = in.readLine();

            Message<?> message = mapper.readValue(json, Message.class);

            Message<?> response = handler.handle(message);

            out.write(mapper.writeValueAsString(response));
            out.newLine();
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
