package com.p2p.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.message.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class PeerClient {
    private final ObjectMapper mapper = new ObjectMapper();
    public Message<?> send(String ip, int port , Message<?> msg){
        try (
            Socket socket = new Socket(ip, port);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.write(mapper.writeValueAsString(msg));
            out.newLine();
            out.flush();

            String response = in.readLine();
            return mapper.readValue(response, Message.class);

        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
