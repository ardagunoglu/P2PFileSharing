package main.p2p.networking;

import main.p2p.model.FileChunk;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferManager {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void sendFileChunk(String peerIP, int port, FileChunk chunk) {
        executor.submit(() -> {
            try (Socket socket = new Socket(peerIP, port);
                 OutputStream out = socket.getOutputStream();
                 ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
                objectOut.writeObject(chunk);
                System.out.println("Sent chunk: " + chunk.getChunkIndex() + " to " + peerIP);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void startListening(int port, FileChunkHandler handler) {
        executor.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         InputStream in = clientSocket.getInputStream();
                         ObjectInputStream objectIn = new ObjectInputStream(in)) {
                        FileChunk chunk = (FileChunk) objectIn.readObject();
                        handler.handleChunk(chunk);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FunctionalInterface
    public interface FileChunkHandler {
        void handleChunk(FileChunk chunk);
    }
}
