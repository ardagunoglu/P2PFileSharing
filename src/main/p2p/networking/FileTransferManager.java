package main.p2p.networking;

import main.p2p.model.FileChunk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class FileTransferManager {
    private ServerSocket serverSocket;

    public void startListening(int port, Consumer<FileChunk> onChunkReceived) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Listening for file transfers on port: " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connection accepted from: " + clientSocket.getInetAddress());
                    handleClient(clientSocket, onChunkReceived);
                }
            } catch (IOException e) {
                System.err.println("Error starting file transfer listener: " + e.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket clientSocket, Consumer<FileChunk> onChunkReceived) {
        try (var inputStream = clientSocket.getInputStream()) {
            FileChunk chunk = FileChunk.readFromStream(inputStream);
            onChunkReceived.accept(chunk);
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    public void stopListening() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Stopped file transfer listener.");
            }
        } catch (IOException e) {
            System.err.println("Error stopping file transfer listener: " + e.getMessage());
        }
    }
}
