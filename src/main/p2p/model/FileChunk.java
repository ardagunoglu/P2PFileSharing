package main.p2p.model;

import java.util.Arrays;

public class FileChunk {
    private final String fileId;
    private final int chunkIndex;
    private final byte[] data;

    public FileChunk(String fileId, int chunkIndex, byte[] data) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.data = Arrays.copyOf(data, data.length);
    }

    public String getFileId() {
        return fileId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public byte[] getData() {
        return data;
    }
}
