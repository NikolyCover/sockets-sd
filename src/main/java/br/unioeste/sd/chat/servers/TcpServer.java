package br.unioeste.sd.chat.servers;

import br.unioeste.sd.chat.domain.User;
import br.unioeste.sd.chat.servers.handlers.TcpClientHandler;

import java.io.*;
import java.net.*;
import java.util.*;

public class TcpServer {
    private static final int PORT = 1024;
    static final Map<User, ObjectOutputStream> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Servidor iniciado na porta " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            TcpClientHandler tcpClientHandler = new TcpClientHandler(socket, clients);

            tcpClientHandler.start();
        }
    }
}
