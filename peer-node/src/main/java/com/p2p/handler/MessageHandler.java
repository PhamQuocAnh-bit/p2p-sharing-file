package com.p2p.handler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.chunk.RequestChunk;
import com.p2p.dto.chunk.SendChunk;
import com.p2p.message.Message;
import com.p2p.message.MessageFactory;
import com.p2p.message.MessageType;
import com.p2p.service.ChunkService;

public class MessageHandler {
    private final ChunkService chunkService = new ChunkService();
    private final ObjectMapper mapper = new ObjectMapper();
    public Message<?> handle(Message<?> message) {
        switch (message.getType()) {
            case PING:
                System.out.println("Received PING");
                return MessageFactory.create(MessageType.PONG,"OK");

            case REQUEST_CHUNK:
                RequestChunk req = mapper.convertValue(message.getPayload(), RequestChunk.class);
                byte[] data = chunkService.getChunk(req.getFileName(), req.getChunkIndex());
                if(data == null) return MessageFactory.create(MessageType.ERROR,"Chunk not found");
                SendChunk chunk = new SendChunk();
                chunk.setFileName(req.getFileName());
                chunk.setChunkIndex(req.getChunkIndex());
                chunk.setData(data);
                return MessageFactory.create(MessageType.SEND_CHUNK,chunk);
            default:
                return MessageFactory.create(MessageType.ERROR,"Unknown type");
        }

    }
}
