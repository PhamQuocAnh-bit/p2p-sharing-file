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

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.*;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class DowloadService {
    private PeerClient peerClient = new PeerClient();
    private ObjectMapper mapper = new ObjectMapper();
    private ChunkService chunkService;
    private int myPort;

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
            // ✅ MODE FULL
            System.out.println("Mode: FULL DOWNLOAD");



            for (int i = 0; i < totalChunks; i++) {
                if (chunkMap.containsKey(i)) {
                    chunksToDownload.add(i);
                }
            }

        } else {
            // ✅ MODE PARTIAL
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

        for (int chunkIndex : chunksToDownload) {

            executor.submit(() -> downloadChunk(fileName, chunkIndex, chunkMap,peers));
        }

        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("DONE DOWNLOAD");
        FileAssemblerService assembler =
                new FileAssemblerService(chunkService);

        String outputPath = "output_" + fileName;

        assembler.mergeAndVerify(fileName, outputPath);

    }


    //5. dowload  1 chunk

    private void downloadChunk(String fileName,
                               int chunkIndex,
                               Map<Integer,List<PeerInfo>> chunkMap,
                               List<PeerInfo> peers) {
        List<PeerInfo> peerList = chunkMap.get(chunkIndex);
        if(peerList == null || peerList.isEmpty()) {
            System.out.println("No Peer has chunk " + chunkIndex);
            return ;
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

                Message<?> response =
                        peerClient.send(peer.getIp(), peer.getPort(), msg);

                if (response != null && response.getType() == MessageType.SEND_CHUNK) {

                    SendChunk send =
                            mapper.convertValue(response.getPayload(), SendChunk.class);



                    chunkService.saveChunk(fileName, chunkIndex, send.getData());

                    registerChunkToTracker(fileName, chunkIndex);


                    sendHaveChunk(fileName,chunkIndex,peers);



                    System.out.println("Downloaded chunk " + chunkIndex +
                            " from " + peer.getPort());

                    return; // SUCCESS → stop retry

                }

            }
            catch (Exception e) {
                System.out.println("Retry chunk " + chunkIndex + " .....");
            }
        }
        System.out.println("FAILED chunk " + chunkIndex);
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
