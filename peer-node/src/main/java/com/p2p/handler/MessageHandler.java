package com.p2p.handler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.chunk.Bitfield;
import com.p2p.dto.chunk.RequestChunk;
import com.p2p.dto.chunk.SendChunk;
import com.p2p.dto.file.FileMetadata;
import com.p2p.dto.peer.Have;
import com.p2p.message.Message;
import com.p2p.message.MessageFactory;
import com.p2p.message.MessageType;
import com.p2p.service.ChunkService;

import java.util.List;

public class MessageHandler {
    private final ChunkService chunkService ;
    public MessageHandler(ChunkService chunkService) {
        this.chunkService = chunkService;
    }
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
            case FILE_METADATA:
                String fileName = mapper.convertValue(message.getPayload(), String.class);

                FileMetadata metadata = chunkService.getMetadata(fileName);

                if (metadata == null) {
                    return MessageFactory.create(MessageType.ERROR, "No metadata");
                }

                return MessageFactory.create(MessageType.FILE_METADATA, metadata);
            case BITFIELD:
                String bfFileName = mapper.convertValue(message.getPayload(), String.class);
                Bitfield bf = new Bitfield();
                bf.setFileName(bfFileName);
                bf.setAvailableChunks(chunkService.getAvailableChunks(bfFileName));
                return MessageFactory.create(MessageType.BITFIELD,bf);
            case HAVE:
                Have have =
                        mapper.convertValue(message.getPayload(), Have.class);
                chunkService.addChunk(have.getFileName(),have.getChunkIndex());
                System.out.println("Recived Have: chunk "+ have.getChunkIndex());
                return MessageFactory.create(MessageType.PONG, "HAVE OK");




            default:
                return MessageFactory.create(MessageType.ERROR,"Unknown type");
        }

    }
}
