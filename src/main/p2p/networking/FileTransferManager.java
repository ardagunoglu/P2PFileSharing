package main.p2p.networking;

import main.p2p.model.FileChunk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class FileTransferManager {
    private ServerSocket serverSocket;
    private int listeningPort;

    public void startListening(int port, Consumer<FileChunk> onChunkReceived) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                listeningPort = port;
                System.out.println("Listening on port: " + listeningPort);
            } catch (IOException e) {
                System.err.println("Port " + port + " is already in use. Trying a dynamic port...");
                try {
                    serverSocket = new ServerSocket(0);
                    listeningPort = serverSocket.getLocalPort();
                    System.out.println("Dynamic port assigned: " + listeningPort);
                } catch (IOException ex) {
                    System.err.println("Failed to bind to a dynamic port: " + ex.getMessage());
                    return;
                }
            }

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connection accepted from: " + clientSocket.getInetAddress());

                    handleClient(clientSocket, onChunkReceived);
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                    break;
                }
            }
        }).start();
    }

    private void handleClient(Socket clientSocket, Consumer<FileChunk> onChunkReceived) {
        try (var inputStream = clientSocket.getInputStream()) {
            FileChunk chunk = FileChunk.readFromStream(inputStream);
            onChunkReceived.accept(chunk);
            System.out.println("Chunk received: " + chunk.getChunkIndex());
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
                System.out.println("Stopped listening on port: " + listeningPort);
            }
        } catch (IOException e) {
            System.err.println("Error stopping server socket: " + e.getMessage());
        }
    }

    public int getListeningPort() {
        return listeningPort;
    }
}
