package com.p2p.peer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.peer.PeerList;
import com.p2p.message.Message;
import com.p2p.message.MessageType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PeerListTest {
    // test list peer
    ObjectMapper mapper = new ObjectMapper();
    @Test
    public void testPeerList() throws Exception{
        PeerInfo peer1 = new PeerInfo();
        peer1.setIp("127.0.0.1");
        peer1.setPort(12345);

        PeerInfo peer2 = new PeerInfo();
        peer2.setIp("127.0.0.2");
        peer2.setPort(12346);

        PeerList peerList = new PeerList();
        peerList.setFileName("movie.mp4");
        peerList.setPeers(List.of(peer1, peer2));

        Message<PeerList> listPeer = new Message<>(MessageType.PEER_LIST,peerList);

        String json = mapper.writeValueAsString(listPeer);
        System.out.println("DU lieu gui" + json); // du lieu gui di

        // nhan
        Message<PeerList> msg = mapper.readValue(json, new TypeReference<Message<PeerList>>(){});
        //json nhan


        // so sanh
        assertEquals(MessageType.PEER_LIST,msg.getType());
        assertEquals("movie.mp4",msg.getPayload().getFileName());



    }

}
