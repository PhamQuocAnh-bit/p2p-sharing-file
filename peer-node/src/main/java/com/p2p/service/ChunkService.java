package com.p2p.service;

import com.p2p.dto.file.FileMetadata;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkService {
//    private static final String BASE_DIR = "peer-node\\storage";
    private final String BASE_DIR;
    private final Map<String, FileMetadata> metadataMap = new HashMap<>();
    private Map<String, Set<Integer>> localChunks = new ConcurrentHashMap<>();

    public ChunkService(int port) {
        this.BASE_DIR = "peer-node/storage/peer_" + port;
    }
    public ChunkService(String BASE_DIR) {
        this.BASE_DIR = BASE_DIR;
    }

    public void saveChunk(String fileName, int index, byte[] data) {
        try {
            File dir = new File(BASE_DIR + "/" + fileName);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "chunk_" + index);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }

            System.out.println("Saved chunk " + index);
            System.out.println(file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getChunk(String fileName, int index) {
        try {
            File file = new File(BASE_DIR + "/" + fileName + "/chunk_" + index);

            if (!file.exists()) return null;

            return Files.readAllBytes(file.toPath());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public void saveMetadata(FileMetadata metadata) {
        metadataMap.put(metadata.getFileName(), metadata);
    }

    public FileMetadata getMetadata(String fileName) {
        return metadataMap.get(fileName);
    }

    // lay danh sach chunk available
    public List<Integer> getAvailableChunks(String fileName) {
        File dir = new File(BASE_DIR+"/"+fileName);
        List<Integer> chunks = new ArrayList<>();
        if(!dir.exists()) return chunks;
        for(File f : dir.listFiles()) {
            String name = f.getName();
            if(name.startsWith("chunk_")) {
                int index = Integer.parseInt(name.split("_")[1]);
                chunks.add(index);
            }
        }
        return chunks;
    }

    public void addChunk(String fileName, int chunkIndex) {
        localChunks
                .computeIfAbsent(fileName, k -> ConcurrentHashMap.newKeySet())
                .add(chunkIndex);

    }




}