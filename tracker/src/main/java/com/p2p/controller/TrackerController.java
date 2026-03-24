package com.p2p.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.dto.tracker.RegisterRequest;
import com.p2p.message.Message;
import com.p2p.service.TrackerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController

@RequestMapping("/tracker")
public class TrackerController {
    @Autowired
    private TrackerService service;
    @Autowired
    private ObjectMapper mapper;
    @PostMapping("/message")
    public ResponseEntity<?> handleMessage(@RequestBody Message<?> message) {
        try {
            switch (message.getType()) {
                case REGISTER :
                    RegisterRequest request =
                            mapper.convertValue(message.getPayload(), RegisterRequest.class);

                    service.register(request);
                    return ResponseEntity.ok("Registered");

                case PEER_LIST:
                    String fileName =
                            mapper.convertValue(message.getPayload(), String.class);
                    return ResponseEntity.ok(service.getPeers(fileName));

                default:
                    return ResponseEntity.badRequest().body("Unknown Message_Type");

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server Error");
        }
    }

}
