package com.p2p.service;

import com.p2p.dto.file.FileMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final int CHUNK_SIZE = 100 * 1024; // 1mb
    private final ChunkService chunkService = new ChunkService();

    public FileMetadata splitFile(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()) throw new IOException("File not found" + filePath);

        long fileSize = file.length();
        int totalChunks = (int) Math.ceil(fileSize * 1.0 / CHUNK_SIZE);

        List<String> chunkHashes = new ArrayList<String>();
        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[CHUNK_SIZE];
        for(int i=0;i<totalChunks;i++){
            int read = fis.read(buffer);
            byte[] chunkDaTa = new byte[read];
            System.arraycopy(buffer, 0,chunkDaTa, 0, read);

            // hashchunk
            String hash = md5(chunkDaTa);
            chunkHashes.add(hash);
            chunkService.saveChunk(file.getName(),i,chunkDaTa);

        }
        fis.close();
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(file.getName());
        metadata.setFileSize(fileSize);
        metadata.setTotalChunks(totalChunks);
        metadata.setChunkHashes(chunkHashes);

        return metadata;
    }
    private String md5(byte[] data){
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder() ;
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
