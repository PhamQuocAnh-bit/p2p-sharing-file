package com.p2p.service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

public class ChunkService {
    private static final String BASE_DIR = "peer-node\\storage";

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




}
