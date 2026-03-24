package com.p2p.dto.tracker;

import com.p2p.dto.peer.PeerInfo;

import java.util.List;

public class RegisterRequest {
    private String fileName;
    private PeerInfo peer;
    private List<Integer> chunks;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public PeerInfo getPeer() {
        return peer;
    }

    public void setPeer(PeerInfo peer) {
        this.peer = peer;
    }

    public List<Integer> getChunks() {
        return chunks;
    }

    public void setChunks(List<Integer> chunks) {
        this.chunks = chunks;
    }
}
