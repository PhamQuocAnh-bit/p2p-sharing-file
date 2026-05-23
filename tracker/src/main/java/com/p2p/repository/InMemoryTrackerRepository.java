package com.p2p.repository;

import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.tracker.RegisterRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTrackerRepository implements TrackerRepository{
    private final Map<String, Map<PeerInfo, List<Integer>>> storage = new ConcurrentHashMap<>();
    private final Set<PeerInfo> onlinePeers = ConcurrentHashMap.newKeySet();
    private final Map<PeerInfo, Long> lastSeen = new ConcurrentHashMap<>();

    @PostConstruct
    public void startCleanupTask() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Chạy mỗi 30 giây
                    long now = System.currentTimeMillis();
                    long TIMEOUT = 100000;
                    for (PeerInfo peer : new ArrayList<>(lastSeen.keySet())) {
                        Long last = lastSeen.get(peer);

                        if (last != null && now - last > TIMEOUT) {

                            System.out.println("REMOVE DEAD PEER: " + peer);

                            lastSeen.remove(peer);
                            onlinePeers.remove(peer);

                            for (Map<PeerInfo, List<Integer>> fileMap : storage.values()) {
                                fileMap.remove(peer);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void register(RegisterRequest request) {
        PeerInfo peer = request.getPeer();
        onlinePeers.add(peer);
        lastSeen.put(peer, System.currentTimeMillis());

        if (request.getFileName() != null && !request.getFileName().isEmpty()) {

            storage.putIfAbsent(request.getFileName(), new ConcurrentHashMap<>());
            storage.get(request.getFileName()).put(peer, request.getChunks());

            System.out.println("Registered file " + request.getFileName()
                    + " by Peer " + peer);
        }
        else {
            System.out.println("Registered Peer " + peer.getIp()+":" + peer.getPort());
        }
    }
    @Override
    public List<PeerInfo> getPeers(String fileName) {
        return new ArrayList<>(storage.getOrDefault(fileName,Map.of()).keySet());
    }
    @Override
    public Map<PeerInfo,List<Integer>> getChunkDistribution(String fileName) {
        return storage.getOrDefault(fileName,Map.of());
    }
    @Override
    public Map<String, Map<PeerInfo, List<Integer>>> getAllFileDistribution() {
        return storage;
    }
    @Override
    public List<PeerInfo> getAllPeers() {
        return new ArrayList<>(onlinePeers);
    }


    public void heartbeat(PeerInfo peer) {
        if (onlinePeers.contains(peer)) {
            lastSeen.put(peer, System.currentTimeMillis());
        } else {
            System.out.println("Heartbeat từ peer chưa register: " + peer);
        }
    }
    public void unregister(PeerInfo peer) {
        lastSeen.remove(peer);
        onlinePeers.remove(peer);

        for (Map<PeerInfo, List<Integer>> fileMap : storage.values()) {
            fileMap.remove(peer);
        }

        System.out.println("PEER REMOVED: " + peer);
    }


}
