package com.harbinger.parrot.event;

public class BatEvent {
    public static final int BOS = 0;
    public static final int EOS = 1;
    public int code;

    public BatEvent(int code) {
        this.code = code;
    }
}