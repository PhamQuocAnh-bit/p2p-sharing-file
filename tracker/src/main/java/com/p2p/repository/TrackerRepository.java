package com.p2p.repository;


import com.p2p.dto.peer.PeerInfo;
import com.p2p.dto.tracker.RegisterRequest;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface TrackerRepository {
    void register(RegisterRequest registerRequest);
    List<PeerInfo> getPeers(String fileName);
    Map<PeerInfo,List<Integer>>  getChunkDistribution(String fileName);


}
