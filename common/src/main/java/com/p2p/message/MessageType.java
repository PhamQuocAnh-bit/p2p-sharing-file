package com.p2p.message;

public enum MessageType {

    // tracker
    REGISTER,
    PEER_LIST,
    HEARTBEAT,
    UNREGISTER,

    // peer
    REQUEST_CHUNK,
    SEND_CHUNK,
    BITFIELD,
    HAVE,
    GET_ALL_PEERS,

    //file
    FILE_METADATA,





    // system
    ERROR,
    PING,
    PONG,
    //search
    CHUNK_DISTRIBUTION,
    ALL_FILE_DISTRIBUTION,

}