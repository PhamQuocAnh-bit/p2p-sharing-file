package com.p2p.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrackerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testRegister() throws Exception {

        String json = """
        {
          "type": "REGISTER",
          "payload": {
            "fileName": "file.txt",
            "peer": {
              "ip": "127.0.0.1",
              "port": 8080
            },
            "chunks": [1,2,3]
          }
        }
        """;

        mockMvc.perform(post("/tracker/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("Registered"));
    }

    @Test
    void testGetPeers() throws Exception {

        // 1. REGISTER trước
        String registerJson = """
        {
          "type": "REGISTER",
          "payload": {
            "fileName": "file.txt",
            "peer": {
              "ip": "127.0.0.1",
              "port": 8080
            },
            "chunks": [1,2,3]
          }
        }
        """;

        mockMvc.perform(post("/tracker/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        // 2. GET PEERS
        String getPeersJson = """
        {
          "type": "PEER_LIST",
          "payload": "file.txt"
        }
        """;

        mockMvc.perform(post("/tracker/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPeersJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ip").value("127.0.0.1"))
                .andExpect(jsonPath("$[0].port").value(8080));
    }

    @Test
    void testUnknownType() throws Exception {

        String json = """
        {
          "type": "UNKNOWN",
          "payload": "test"
        }
        """;

        mockMvc.perform(post("/tracker/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}