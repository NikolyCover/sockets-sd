package br.unioeste.sd.chat.servers;

import br.unioeste.sd.chat.domain.User;
import br.unioeste.sd.chat.servers.handlers.UdpClientHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class UdpServer {
    private static final int SERVER_PORT = 1048;

    private static final Map<User, InetSocketAddress> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(SERVER_PORT);

        System.out.println("Servidor UDP iniciado na porta " + SERVER_PORT);

        byte[] buffer = new byte[4096];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            UdpClientHandler clientHandler = new UdpClientHandler(socket, packet, clients);

            clientHandler.start();
        }
    }
}
