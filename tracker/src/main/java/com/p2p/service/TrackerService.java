package com.p2p.service;


import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.tracker.RegisterRequest;
import com.p2p.repository.TrackerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TrackerService {
    private final TrackerRepository repository;
    public TrackerService(TrackerRepository repository) {
        this.repository = repository;
    }

    public void register(RegisterRequest request) {
        repository.register(request);
    }
    public List<PeerInfo> getPeers(String fileName) {
        return repository.getPeers(fileName);
    }
    public Map<PeerInfo, List<Integer>> getChunkDistribution(String fileName) {
        return repository.getChunkDistribution(fileName);
    }
    public Map<String, Map<PeerInfo, List<Integer>>> getAllFileDistribution() {
        return repository.getAllFileDistribution();
    }
    public void heartbeat(PeerInfo peer) {
        // Cập nhật thời gian cuối cùng peer này gửi heartbeat
        repository.heartbeat(peer);
    }
    public void unregister(PeerInfo peer) {
        repository.unregister(peer);
    }
    public List<PeerInfo> getAllPeers() {
        return repository.getAllPeers();// hoặc map của bạn
    }

}

