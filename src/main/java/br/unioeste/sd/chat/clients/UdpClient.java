package br.unioeste.sd.chat.clients;

import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.utils.MessageUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UdpClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1024;

    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Digite seu nome de usuário: ");
        String username = scanner.nextLine();

        sendObject(socket, serverAddress, username);


        new Thread(() -> {
            byte[] buffer = new byte[4096];

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    Message message = (Message) ois.readObject();

                    MessageUtils.showMessageOutput(message);
                } catch (Exception e) {
                    System.out.println("Erro ao receber mensagem.");
                    break;
                }
            }
        }).start();

        while (true) {
            String line = scanner.nextLine();

            System.out.print("\033[F");
            System.out.print("\033[2K");
            System.out.flush();

            try {
                String recipient = MessageUtils.parseRecipient(line);
                String content = MessageUtils.parseMessageContent(line);

                Message message = Message.builder()
                        .sender(username)
                        .recipient(recipient)
                        .content(content)
                        .build();

                sendObject(socket, serverAddress, message);

                MessageUtils.showOwnMessage(message);
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private static void sendObject(DatagramSocket socket, InetAddress address, Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();

        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, UdpClient.SERVER_PORT);
        socket.send(packet);
    }

}
