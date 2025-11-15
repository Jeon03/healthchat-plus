package com.healthchat.backend.service.rag;

import java.nio.ByteBuffer;

public class EmbeddingUtil {

    public static byte[] toBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        for (float v : vector) buffer.putFloat(v);
        return buffer.array();
    }

    public static float[] toFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++)
            vector[i] = buffer.getFloat();
        return vector;
    }
}
