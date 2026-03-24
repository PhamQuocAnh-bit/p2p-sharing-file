package com.p2p.repository;

import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.tracker.RegisterRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTrackerRepository implements TrackerRepository{
    private final Map<String, Map<PeerInfo, List<Integer>>> storage = new ConcurrentHashMap<>();

    @Override
    public void register(RegisterRequest request) {
        storage.putIfAbsent(request.getFileName(), new ConcurrentHashMap<>());
        storage.get(request.getFileName()).put(request.getPeer(), request.getChunks());
        System.out.println("Registered " + request.getFileName() + "by Peer" + request.getPeer())  ;
    }
    @Override
    public List<PeerInfo> getPeers(String fileName) {
        return new ArrayList<>(storage.getOrDefault(fileName,Map.of()).keySet());
    }
    @Override
    public Map<PeerInfo,List<Integer>> getChunkDistribution(String fileName) {
        return storage.getOrDefault(fileName,Map.of());
    }


}
