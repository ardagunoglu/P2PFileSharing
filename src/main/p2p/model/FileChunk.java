package main.p2p.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static FileChunk readFromStream(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        int fileIdLength = dataInputStream.readInt();
        byte[] fileIdBytes = new byte[fileIdLength];
        dataInputStream.readFully(fileIdBytes);
        String fileId = new String(fileIdBytes);

        int chunkIndex = dataInputStream.readInt();

        int dataLength = dataInputStream.readInt();
        byte[] data = new byte[dataLength];
        dataInputStream.readFully(data);

        return new FileChunk(fileId, chunkIndex, data);
    }

    public byte[] toByteArray() throws IOException {
        try (var outputStream = new java.io.ByteArrayOutputStream();
             var dataOutputStream = new java.io.DataOutputStream(outputStream)) {

            byte[] fileIdBytes = fileId.getBytes();
            dataOutputStream.writeInt(fileIdBytes.length);
            dataOutputStream.write(fileIdBytes);

            dataOutputStream.writeInt(chunkIndex);

            dataOutputStream.writeInt(data.length);
            dataOutputStream.write(data);

            dataOutputStream.flush();
            return outputStream.toByteArray();
        }
    }
}
