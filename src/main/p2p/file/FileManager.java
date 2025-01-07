package main.p2p.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static final int CHUNK_SIZE = 256 * 1024;
    private final List<byte[]> fileChunks;

    public FileManager(String filePath) throws IOException {
        this.fileChunks = splitFileIntoChunks(filePath);
    }

    private List<byte[]> splitFileIntoChunks(String filePath) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                if (bytesRead < CHUNK_SIZE) {
                    byte[] finalChunk = new byte[bytesRead];
                    System.arraycopy(buffer, 0, finalChunk, 0, bytesRead);
                    chunks.add(finalChunk);
                } else {
                    chunks.add(buffer.clone());
                }
            }
        }
        return chunks;
    }

    public byte[] getChunk(int index) {
        if (index < 0 || index >= fileChunks.size()) {
            throw new IndexOutOfBoundsException("Invalid chunk index.");
        }
        return fileChunks.get(index);
    }

    public int getTotalChunks() {
        return fileChunks.size();
    }

    public void printChunks() {
        for (int i = 0; i < fileChunks.size(); i++) {
            System.out.println("Index " + (i + 1) + ": Chunk Size = " + fileChunks.get(i).length + " bytes");
        }
    }
}