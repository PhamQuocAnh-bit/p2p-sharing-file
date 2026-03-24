package com.p2p.dto.file;

import java.util.List;

public class FileMetadata {
    private String fileName;
    private long fileSize;
    private int totalChunks;
    private List<String> chunkHashes;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public List<String> getChunkHashes() {
        return chunkHashes;
    }

    public void setChunkHashes(List<String> chunkHashes) {
        this.chunkHashes = chunkHashes;
    }
}
