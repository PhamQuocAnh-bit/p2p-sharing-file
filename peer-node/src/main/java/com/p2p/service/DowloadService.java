package com.p2p.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.chunk.Bitfield;
import com.p2p.dto.chunk.RequestChunk;
import com.p2p.dto.chunk.SendChunk;
import com.p2p.dto.file.FileMetadata;
import com.p2p.dto.peer.Have;
import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.peer.PeerList;
import com.p2p.dto.tracker.RegisterRequest;
import com.p2p.message.Message;
import com.p2p.message.MessageType;
import com.p2p.network.PeerClient;
import com.p2p.tracker.TrackerClient;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.*;
import java.util.function.Consumer;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class DowloadService {
    private PeerClient peerClient = new PeerClient();
    private ObjectMapper mapper = new ObjectMapper();
    private ChunkService chunkService;
    private int myPort;


    private Consumer<String> progressCallback;

    public void setProgressCallback(Consumer<String> progressCallback) {
        this.progressCallback = progressCallback;
    }

    private void updateProgress(String text) {
        System.out.println(text);

        if (progressCallback != null) {
            progressCallback.accept(text);
        }
    }

    public  DowloadService(ChunkService chunkService, int myPort) {
        this.chunkService = chunkService;
        this.myPort = myPort;
    }
    public void dowload(String fileName , List<PeerInfo> peers, List<Integer> selectedChunks ) {
        if(peers.isEmpty()) {
            System.out.println("No peer");
            return;
        }
        PeerInfo peer = peers.get(0);

//        // lay chunk aviable
//        Message<String> bitReq =
//                new Message<>(MessageType.BITFIELD, fileName);
//
//        Message<?> bitRes =
//                peerClient.send(peer.getIp(), peer.getPort(), bitReq);
//
//        if (bitRes == null || bitRes.getType() != MessageType.BITFIELD) {
//            System.out.println("Cannot get BITFIELD");
//            return;
//        }
//
//        Bitfield bitfield =
//                mapper.convertValue(bitRes.getPayload(), Bitfield.class);
//
//        List<Integer> availableChunks = bitfield.getAvailableChunks();
//
//        System.out.println("Peer has chunks: " + availableChunks);


        // 1.get metadata
        Message<String> mesRequest =
                new Message<>(MessageType.FILE_METADATA,fileName);
        Message<?> metaRes =
                peerClient.send(peer.getIp(), peer.getPort(), mesRequest);
        if(metaRes == null || metaRes.getType()!= MessageType.FILE_METADATA) {
            System.out.println("Cannot get Metadata");
            return;
        }
        FileMetadata metadata =
                mapper.convertValue(metaRes.getPayload(),FileMetadata.class);
        chunkService.saveMetadata(metadata);
        int totalChunks = metadata.getTotalChunks();
        System.out.println("Total Chunks" + totalChunks);
        // 2.build bitfield map

        Map<Integer, List<PeerInfo>> chunkMap =
                buildChunkPeerMap(fileName, peers);

        if (chunkMap.isEmpty()) {
            System.out.println("No available chunks in network");
            return;
        }

        // 3.select chunk

        List<Integer> chunksToDownload = new ArrayList<>();

        if (selectedChunks == null || selectedChunks.isEmpty()) {
            System.out.println("Mode: FULL DOWNLOAD");
            for (int i = 0; i < totalChunks; i++) {
                if (chunkMap.containsKey(i)) {
                    chunksToDownload.add(i);
                }
            }

        } else {

            System.out.println("Mode: PARTIAL DOWNLOAD");
            for(int c : selectedChunks) {
                if(c>=0 && c< totalChunks && chunkMap.containsKey(c)) {
                    chunksToDownload.add(c);
                }
            }
            if (chunksToDownload.isEmpty()) {
                System.out.println("No valid chunks to download");
                return;
            }
        }
        // rarest first
        chunksToDownload.sort((a, b) -> {
            int cmp = Integer.compare(
                    chunkMap.get(a).size(),
                    chunkMap.get(b).size()
            );
            if (cmp != 0) return cmp;

            // nếu cùng độ hiếm => random
            return ThreadLocalRandom.current().nextInt(-1, 2);
        });

        System.out.println("Download order (rarest-first): " + chunksToDownload);

        // 4. multi-thread dowload

//        int THREADS = Math.min(10, peers.size());
//        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
//        for(int chunkIndex : chunksToDownload) {
//
//            executor.submit(() -> {
////                try {
////                    PeerInfo selectPeer =
////                            peers.get(chunkIndex % peers.size());
////                    RequestChunk req = new RequestChunk();
////                    req.setFileName(fileName);
////                    req.setChunkIndex(chunkIndex);
////
////                    Message<RequestChunk> msg =
////                            new Message<>(MessageType.REQUEST_CHUNK, req);
////
////                    Message<?> response =
////                            peerClient.send(selectPeer.getIp(), selectPeer.getPort(), msg);
////
////                    if (response != null && response.getType() == MessageType.SEND_CHUNK) {
////
////                        SendChunk send =
////                                mapper.convertValue(response.getPayload(), SendChunk.class);
////
////                        chunkService.saveChunk(fileName, chunkIndex, send.getData());
////
////
////                        System.out.println("Downloaded chunk " + chunkIndex+" from "+ selectPeer.getPort());
////                    } else {
////                        System.out.println("Failed chunk " + chunkIndex + " from "+ selectPeer.getPort());
////                    }
////
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////            });
////
////
////        }
//        executor.shutdown();
//        try {
//            executor.awaitTermination(10, TimeUnit.MINUTES);
//
//        }
//        catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("DONE DOWLOAD");
//
//
//
//    }
        // 4. multi-thread dowload
        int THREADS = Math.min(10, chunksToDownload.size());
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int chunkIndex : chunksToDownload) {
            Future<Boolean> future =
                    executor.submit(() -> downloadChunk(fileName, chunkIndex, chunkMap, peers));

            futures.add(future);
        }

        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean allSuccess = true;

        for (Future<Boolean> future : futures) {
            try {
                if (!future.get()) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                allSuccess = false;
            }
        }

        if (allSuccess) {
            updateProgress("DOWNLOAD SUCCESS");
        } else {
            updateProgress("DOWNLOAD FAILED");
        }

        FileAssemblerService assembler =
                new FileAssemblerService(chunkService);

        String outputPath = "output_" + fileName;

        if (allSuccess && hasAllChunks(fileName, totalChunks)) {
            assembler.mergeAndVerify(fileName, outputPath);
        } else {
            updateProgress("Not enough valid chunks, skip merge");
        }


    }
    private boolean hasAllChunks(String fileName, int totalChunks) {
        List<Integer> chunks = chunkService.getAvailableChunks(fileName);

        for (int i = 0; i < totalChunks; i++) {
            if (!chunks.contains(i)) {
                System.out.println("Missing chunk " + i);
                return false;
            }
        }

        return true;
    }


    //5. dowload  1 chunk

    private boolean downloadChunk(String fileName,
                               int chunkIndex,
                               Map<Integer,List<PeerInfo>> chunkMap,
                               List<PeerInfo> peers) {
        List<PeerInfo> peerList = chunkMap.get(chunkIndex);
        if(peerList == null || peerList.isEmpty()) {
            System.out.println("No Peer has chunk " + chunkIndex);
            return false;
        }
        // random peer
        List<PeerInfo> candidates = new ArrayList<>(peerList);
        Collections.shuffle(candidates);
        for(PeerInfo peer : candidates){
            try {
                RequestChunk req = new RequestChunk();
                req.setFileName(fileName);
                req.setChunkIndex(chunkIndex);

                Message<RequestChunk> msg =
                        new Message<>(MessageType.REQUEST_CHUNK, req);

//                Message<?> response =
//                        peerClient.send(peer.getIp(), peer.getPort(), msg);
//
//                if (response != null && response.getType() == MessageType.SEND_CHUNK) {
//
//                    SendChunk send =
//                            mapper.convertValue(response.getPayload(), SendChunk.class);
//
//
//
//                    chunkService.saveChunk(fileName, chunkIndex, send.getData());
//
//                    registerChunkToTracker(fileName, chunkIndex);
//
//
//                    sendHaveChunk(fileName,chunkIndex,peers);
//
//
//
//                    System.out.println("Downloaded chunk " + chunkIndex +
//                            " from " + peer.getPort());
//
//                    return; // SUCCESS → stop retry
//
//                }

                // old + thong ke toc do dowload
                long startTime = System.nanoTime();

                final boolean[] downloading = {true};

                Thread fakeProgress = new Thread(() -> {

                    int fakePercent = 0;

                    while (downloading[0] && fakePercent < 95) {

                        try {
                            Thread.sleep(120);
                        } catch (Exception ignored) {}

                        fakePercent += 5;

                        updateProgress(
                                "Downloading chunk " + chunkIndex +
                                        " from peer " + peer.getPort() +
                                        " | Progress: " + fakePercent + "%" +
                                        " | Speed: calculating..."
                        );
                    }
                });

                fakeProgress.start();

                Message<?> response =
                        peerClient.send(peer.getIp(), peer.getPort(), msg);

                long endTime = System.nanoTime();

                downloading[0] = false;
                try {
                    fakeProgress.join();
                } catch (Exception ignored) {}

                if (response != null && response.getType() == MessageType.SEND_CHUNK) {

                    SendChunk send =
                            mapper.convertValue(response.getPayload(), SendChunk.class);

                    byte[] data = send.getData();

                    double seconds =
                            (endTime - startTime) / 1_000_000_000.0;

                    double sizeKB =
                            data.length / 1024.0;

                    double speedKBps =
                            sizeKB / seconds;

                    FileMetadata metadata = chunkService.getMetadata(fileName);

                    if (metadata == null) {
                        updateProgress("No metadata found, cannot verify chunk " + chunkIndex);
                        return false;
                    }

                    String expectedHash =
                            metadata.getChunkHashes().get(chunkIndex);

                    String actualHash =
                            md5(data);

                    updateProgress(
                            "Checking integrity for chunk "
                                    + chunkIndex
                    );

                    updateProgress(
                            "Expected hash: "
                                    + expectedHash
                    );

                    updateProgress(
                            "Actual hash:   "
                                    + actualHash
                    );

                    if (!expectedHash.equals(actualHash)) {

                        updateProgress(
                                "Integrity FAILED for chunk "
                                        + chunkIndex
                                        + " from peer "
                                        + peer.getPort()
                                        + " | retry another peer..."
                        );

                        continue;
                    }

                    updateProgress(
                            "Integrity OK for chunk "
                                    + chunkIndex
                    );

                    chunkService.saveChunk(fileName, chunkIndex, data);

                    registerChunkToTracker(fileName, chunkIndex);

                    sendHaveChunk(fileName, chunkIndex, peers);

                    updateProgress(String.format(
                            "Downloaded + verified chunk %d from peer %d | Progress: 100%% | Speed: %.2f KB/s",
                            chunkIndex,
                            peer.getPort(),
                            speedKBps
                    ));

                    return true;
                }

            }
            catch (Exception e) {
                System.out.println("Retry chunk " + chunkIndex + " .....");
            }
        }
        updateProgress("FAILED chunk " + chunkIndex);
        return false;
    }
    private String md5(byte[] data) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("MD5");

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



    // lay bitfield tu tat ca cac peer
    private Map<Integer, List<PeerInfo>> buildChunkPeerMap(String fileName, List<PeerInfo> peers){
        Map<Integer,List<PeerInfo>> chunkMap = new ConcurrentHashMap<>();
        for(PeerInfo peer : peers) {
            try{
                Message<String> bitReq =
                        new Message<>(MessageType.BITFIELD, fileName);
                Message<?> bitRes =
                        peerClient.send(peer.getIp(), peer.getPort() ,bitReq);
                if (bitRes == null || bitRes.getType() != MessageType.BITFIELD) {
                    System.out.println("Cannot get BITFIELD from " + peer.getPort());
                    continue;
                }
                Bitfield bf =
                        mapper.convertValue(bitRes.getPayload(), Bitfield.class);

                List<Integer> chunks = bf.getAvailableChunks();

                System.out.println("Peer " + peer.getPort() + " has: " + chunks);

                for (int chunkIndex : chunks) {
                    chunkMap.putIfAbsent(chunkIndex, new ArrayList<>());
                    chunkMap.get(chunkIndex).add(peer);
                }

            } catch (Exception e) {
                System.out.println("Error getting bitfield from "+ peer.getPort());
            }
        }
        return chunkMap;

    }

    // gui have de thong bao
    private  void sendHaveChunk(String fileName, int chunkIndex,List<PeerInfo> peers) {
        Have have = new Have();
        have.setFileName(fileName);
        have.setChunkIndex(chunkIndex);

        Message<Have> msg =
                new Message<>(MessageType.HAVE,have);
        for (PeerInfo peer : peers) {
            if (peer.getPort() == myPort) continue;
            try {
                peerClient.send(peer.getIp(), peer.getPort(), msg);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void registerChunkToTracker(String fileName, int chunkIndex) {

        try {
            TrackerClient trackerClient = new TrackerClient();

            PeerInfo me = new PeerInfo();
            me.setIp("127.0.0.1");
            me.setPort(myPort);

            List<Integer> chunks = new ArrayList<>();
            chunks.add(chunkIndex); // chỉ gửi chunk mới

            RegisterRequest request = new RegisterRequest();
            request.setFileName(fileName);
            request.setPeer(me);
            request.setChunks(chunks);

            trackerClient.register(request);

            System.out.println("Registered chunk " + chunkIndex + " to tracker");

        } catch (Exception e) {
            System.out.println("Failed to register chunk " + chunkIndex);
        }
    }



}
