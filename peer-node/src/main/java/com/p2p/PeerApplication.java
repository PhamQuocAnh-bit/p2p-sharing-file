package com.p2p;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.chunk.RequestChunk;
import com.p2p.dto.chunk.SendChunk;
import com.p2p.dto.file.FileMetadata;
import com.p2p.message.Message;
import com.p2p.message.MessageType;
import com.p2p.network.PeerClient;
import com.p2p.network.PeerServer;
import com.p2p.service.ChunkService;
import com.p2p.service.FileService;

public class PeerApplication {
    public static void main(String[] args) throws InterruptedException,Exception{
        // start sever ( peer A)
        PeerServer server = new PeerServer(5000);
        new Thread(server::start).start();

        // delay
        Thread.sleep(2000);
        // peer B
        PeerClient client = new PeerClient();

//        Message<String> ping = new Message<>(MessageType.PING, "hello");
//        Message<?> response = client.send("127.0.0.1",5000,ping);
//        System.out.println("Response" + response.getType());

        // test request chunk
//        ChunkService chunkService = new ChunkService();
//
//        // tạo chunk giả
//        chunkService.saveChunk("test.txt", 1, "Hello P2P".getBytes());
//
//        // request
//        RequestChunk req = new RequestChunk();
//        req.setFileName("test.txt");
//        req.setChunkIndex(0);
//
//        Message<RequestChunk> message =
//                new Message<>(MessageType.REQUEST_CHUNK, req);
//
//        Message<?> response =
//                client.send("127.0.0.1", 5000, message);
//        // reponse
//        ObjectMapper mapper = new ObjectMapper();
//
//        if (response != null && response.getType() == MessageType.SEND_CHUNK) {
//
//            SendChunk chunk =
//                    mapper.convertValue(response.getPayload(), SendChunk.class);
//
//            System.out.println("DATA: " + new String(chunk.getData()));
//        }
//        if (response == null) {
//            System.out.println("No response");
//        }
//        else if (response.getType() == MessageType.ERROR) {
//            System.out.println("ERROR: " + response.getPayload());
//        }
//        else if (response.getType() == MessageType.SEND_CHUNK) {
//
//            SendChunk chunk =
//                    mapper.convertValue(response.getPayload(), SendChunk.class);
//
//            System.out.println("DATA: " + new String(chunk.getData()));
//        }

        //** Test Split file **//
        FileService fileService = new FileService();
        String filePath = "D:/PTIT/HocKi2Nam4/CacHeThongPhanTan/test-p2p/test.pdf";

        FileMetadata metadata = fileService.splitFile(filePath);

        System.out.println("FileName: " + metadata.getFileName());
        System.out.println("FileSize: " + metadata.getFileSize());
        System.out.println("TotalChunks: " + metadata.getTotalChunks());
        System.out.println("ChunkHashes: " + metadata.getChunkHashes());






    }

}
