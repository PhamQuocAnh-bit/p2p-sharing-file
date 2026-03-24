package com.p2p.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.chunk.RequestChunk;
import com.p2p.dto.chunk.SendChunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTest {
    ObjectMapper mapper = new ObjectMapper();

    // Test request_chunk
    @Test
    void testRequestChunk() throws JsonProcessingException {
        RequestChunk requestChunk = new RequestChunk();
        requestChunk.setFileName("movie.mp4");
        requestChunk.setChunkIndex(3);

        Message<RequestChunk> message =
                new Message<>(MessageType.REQUEST_CHUNK,requestChunk);
        String json = mapper.writeValueAsString(message);
        System.out.println("request chunk json: " + json);

        // mo phong nhan data
        Message<RequestChunk> result =
                mapper.readValue(json, new TypeReference<Message<RequestChunk>>() {
                }); // object

        assertEquals(MessageType.REQUEST_CHUNK,result.getType());
        assertEquals("movie.mp4", result.getPayload().getFileName());
        assertEquals(3, result.getPayload().getChunkIndex());

        System.out.println("Receive " + mapper.writeValueAsString(result));

    }
    // Test SEND_CHunk
    @Test
    void testSendChunk() throws JsonProcessingException {
        SendChunk sendChunk = new SendChunk();
        sendChunk.setFileName("movie.mp4");
        sendChunk.setChunkIndex(3);
        sendChunk.setData("hello".getBytes());

        Message<SendChunk> message =
                new Message<>(MessageType.SEND_CHUNK,sendChunk);
        String json = mapper.writeValueAsString(message);
        System.out.println("send chunk json: " + json);

        //mo phong nhan ve

        Message<SendChunk> result =
                mapper.readValue(json, new TypeReference<Message<SendChunk>>() {});


        // du lieu nhan ve
        System.out.println("Receive " + mapper.writeValueAsString(result));
        assertEquals(MessageType.SEND_CHUNK,result.getType());
        assertEquals("movie.mp4", result.getPayload().getFileName());
        assertEquals(3, result.getPayload().getChunkIndex());
        assertArrayEquals("hello".getBytes(), result.getPayload().getData());
    }


}
