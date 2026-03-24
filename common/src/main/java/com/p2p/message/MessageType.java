package com.p2p.message;

public enum MessageType {

    // tracker
    REGISTER,
    PEER_LIST,

    // peer
    REQUEST_CHUNK,
    SEND_CHUNK,
    BITFIELD,
    HAVE,

    //file
    FILE_METADATA,



    // system
    ERROR,
    PING,
    PONG
}