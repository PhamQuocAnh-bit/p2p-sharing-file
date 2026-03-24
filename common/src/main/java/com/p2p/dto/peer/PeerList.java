package com.p2p.dto.peer;

import java.util.List;

public class PeerList {
    private String fileName;
    private List<PeerInfo> peers;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<PeerInfo> getPeers() {
        return peers;
    }

    public void setPeers(List<PeerInfo> peers) {
        this.peers = peers;
    }
}
