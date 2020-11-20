package com.harbinger.parrot.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class CommonUtil {
    public static short[] bytesToShort(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    public static byte[] shortToBytes(short[] shorts) {
        if (shorts == null) {
            return null;
        }
        byte[] bytes = new byte[shorts.length * 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);

        return bytes;
    }

    public static void main(String[] args) {
        byte[] ba = {21, 32, 45, 98, 46, 85};
        short[] sa = bytesToShort(ba);
        byte[] bb = shortToBytes(sa);

        System.out.println("ba=" + Arrays.toString(ba) + ",sa=" + Arrays.toString(sa) + ",bb=" + Arrays.toString(bb));
    }
}