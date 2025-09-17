package br.unioeste.sd.chat.clients;

import br.unioeste.sd.chat.domain.HandshakeRequest;
import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.utils.CryptoUtils;
import br.unioeste.sd.chat.utils.MessageUtils;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Scanner;

public class UdpClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1048;
    private static volatile SecretKey aesKey;

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
        Scanner scanner = new Scanner(System.in);

        KeyPair rsaKeys = CryptoUtils.generateRSAKeyPair();
        System.out.print("Digite seu nome de usuário: ");
        String username = scanner.nextLine();

        //System.out.println("Enviando solicitação de sessão segura...");
        String publicKeyB64 = Base64.getEncoder().encodeToString(rsaKeys.getPublic().getEncoded());
        HandshakeRequest request = new HandshakeRequest(username, publicKeyB64);
        sendObject(socket, serverAddress, request);

        //System.out.println("Aguardando estabelecimento de sessão segura...");
        byte[] buffer = new byte[4096];
        DatagramPacket aesPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(aesPacket);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(aesPacket.getData(), 0, aesPacket.getLength());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            String encryptedAES = (String) ois.readObject();
            byte[] aesBytes = CryptoUtils.decryptRSA(encryptedAES, rsaKeys.getPrivate());
            aesKey = CryptoUtils.bytesToAES(aesBytes);
        }
        System.out.println("Sessão segura estabelecida!");

        new Thread(() -> {
            byte[] receiveBuffer = new byte[4096];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(packet);

                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Message message = (Message) ois.readObject();

                    if (message.getContent() != null && message.getContent().contains(":")) {
                        try {
                            String[] parts = message.getContent().split(":");
                            String ciphertext = parts[0];
                            byte[] iv = Base64.getDecoder().decode(parts[1]);
                            String decrypted = CryptoUtils.decryptAES(ciphertext, aesKey, iv);
                            message.setContent(decrypted);
                        } catch (Exception e) {
                            System.out.println("Falha ao descriptografar mensagem: " + message.getContent());
                        }
                    }

                    MessageUtils.showMessageOutput(message);
                } catch (Exception e) {
                    System.out.println("Erro ao receber mensagem: " + e.getMessage());
                    break;
                }
            }
        }).start();

        while (true) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue;

            System.out.print("\033[F"); // Move cursor para cima
            System.out.print("\033[2K"); // Apaga a linha

            try {
                String recipient = MessageUtils.parseRecipient(line);
                String content = MessageUtils.parseMessageContent(line);

                byte[] iv = CryptoUtils.generateIV();
                String ciphertext = CryptoUtils.encryptAES(content, aesKey, iv);
                String payload = ciphertext + ":" + Base64.getEncoder().encodeToString(iv);

                Message message = new Message(username, recipient, payload);
                sendObject(socket, serverAddress, message);

                message.setContent(content);
                MessageUtils.showOwnMessage(message);
            } catch (Exception e) {
                System.out.println("Erro ao enviar mensagem: " + e.getMessage());
            }
        }
    }

    private static void sendObject(DatagramSocket socket, InetAddress address, Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, SERVER_PORT);
        socket.send(packet);
    }
}