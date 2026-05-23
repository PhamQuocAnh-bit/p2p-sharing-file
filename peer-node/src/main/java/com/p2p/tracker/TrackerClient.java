package com.p2p.tracker;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.p2p.dto.peer.PeerInfo;
import com.p2p.message.Message;
import com.p2p.message.MessageType;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class TrackerClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String TRACKER_URL = "http://localhost:8080/tracker/message";

    public void register(Object payload) {
        try {
            URL url = new URL(TRACKER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            Message<Object> message =
                    new Message<>(MessageType.REGISTER, payload);

            String json = mapper.writeValueAsString(message);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            int responseCode = conn.getResponseCode();
            System.out.println("Register response : " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Message<?> message) {
        try {
            URL url = new URL(TRACKER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String json = mapper.writeValueAsString(message);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            conn.getInputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<PeerInfo> getPeer(String fileName) {
        try {
            URL url = new URL(TRACKER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            Message<String> message =
                    new Message<>(MessageType.PEER_LIST, fileName);

            String json = mapper.writeValueAsString(message);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            InputStream in = conn.getInputStream();

            JavaType resultType =
                    mapper.getTypeFactory()
                            .constructCollectionType(List.class, PeerInfo.class);

            return mapper.readValue(in, resultType);

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<PeerInfo> getAllPeers() {
        try {
            URL url = new URL(TRACKER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            Message<Object> message =
                    new Message<>(MessageType.GET_ALL_PEERS, null);

            String json = mapper.writeValueAsString(message);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            InputStream in = conn.getInputStream();

            JavaType resultType =
                    mapper.getTypeFactory()
                            .constructCollectionType(List.class, PeerInfo.class);

            return mapper.readValue(in, resultType);

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public Map<PeerInfo, List<Integer>> getChunkDistribution(String fileName) {
        try {
            URL url = new URL(TRACKER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            Message<String> message =
                    new Message<>(MessageType.CHUNK_DISTRIBUTION, fileName);

            String json = mapper.writeValueAsString(message);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            InputStream in = conn.getInputStream();

            TypeFactory tf = mapper.getTypeFactory();

            JavaType listIntegerType =
                    tf.constructCollectionType(List.class, Integer.class);

            JavaType resultType =
                    tf.constructMapType(
                            Map.class,
                            tf.constructType(PeerInfo.class),
                            listIntegerType
                    );

            return mapper.readValue(in, resultType);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    public Map<String, Map<PeerInfo, List<Integer>>> getAllFileDistribution() {
        try {
            URL url = new URL(TRACKER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            Message<Object> message =
                    new Message<>(MessageType.ALL_FILE_DISTRIBUTION, null);

            String json = mapper.writeValueAsString(message);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            InputStream in = conn.getInputStream();

            TypeFactory tf = mapper.getTypeFactory();

            JavaType listIntegerType =
                    tf.constructCollectionType(List.class, Integer.class);

            JavaType peerChunkMapType =
                    tf.constructMapType(
                            Map.class,
                            tf.constructType(PeerInfo.class),
                            listIntegerType
                    );

            JavaType resultType =
                    tf.constructMapType(
                            Map.class,
                            tf.constructType(String.class),
                            peerChunkMapType
                    );

            return mapper.readValue(in, resultType);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }
}