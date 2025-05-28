package br.unioeste.sd.chat.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1024;
    static final Map<String, ObjectOutputStream> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Servidor iniciado na porta " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(socket, clients);

            clientHandler.start();
        }
    }
}
