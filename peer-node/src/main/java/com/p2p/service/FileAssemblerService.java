package com.p2p.service;

import com.p2p.dto.file.FileMetadata;

import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.List;

public class FileAssemblerService {
    private final ChunkService chunkService;

    public FileAssemblerService(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    public void mergeAndVerify(String fileName, String outputPath) {

        try {
            FileMetadata metadata = chunkService.getMetadata(fileName);

            if (metadata == null) {
                throw new RuntimeException("No metadata found");
            }

            int totalChunks = metadata.getTotalChunks();
            List<String> chunkHashes = metadata.getChunkHashes();

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {

                for (int i = 0; i < totalChunks; i++) {

                    byte[] chunkData = chunkService.getChunk(fileName, i);

                    if (chunkData == null) {
                        throw new RuntimeException("Missing chunk " + i);
                    }

                    // 🔥 VERIFY CHUNK
                    String expected = chunkHashes.get(i);
                    String actual = md5(chunkData);

                    if (!expected.equals(actual)) {
                        throw new RuntimeException("Corrupted chunk " + i);
                    }

                    fos.write(chunkData);
                }
            }

            System.out.println("✅ MERGE DONE: " + outputPath);

        } catch (Exception e) {
            System.out.println("MERGE FAILED");
            e.printStackTrace();
        }
    }

    private String md5(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data);

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
