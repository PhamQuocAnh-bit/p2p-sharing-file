package com.p2p.dto.chunk;

import java.util.List;

public class Bitfield {
    private String fileName;
    private List<Integer> availableChunks;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Integer> getAvailableChunks() {
        return availableChunks;
    }

    public void setAvailableChunks(List<Integer> availableChunks) {
        this.availableChunks = availableChunks;
    }
}
