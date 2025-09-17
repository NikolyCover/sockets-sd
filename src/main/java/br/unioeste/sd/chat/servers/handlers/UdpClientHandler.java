package br.unioeste.sd.chat.servers.handlers;

import br.unioeste.sd.chat.domain.HandshakeRequest;
import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.domain.UdpClientSession;
import br.unioeste.sd.chat.domain.User;
import br.unioeste.sd.chat.utils.CryptoUtils;
import br.unioeste.sd.chat.utils.MessageUtils;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

public class UdpClientHandler extends Thread {
    private final DatagramSocket socket;
    private final DatagramPacket packet;
    private final Map<User, UdpClientSession> clients;

    public UdpClientHandler(DatagramSocket socket, DatagramPacket packet, Map<User, UdpClientSession> clients) {
        this.socket = socket;
        this.packet = packet;
        this.clients = clients;
    }

    @Override
    public void run() {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais)
        ) {
            Object obj = ois.readObject();
            InetSocketAddress senderAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());

            if (obj instanceof HandshakeRequest) {
                HandshakeRequest request = (HandshakeRequest) obj;

                User user = new User(request.getUsername(), senderAddress.getAddress().getHostAddress());

                byte[] pubKeyBytes = Base64.getDecoder().decode(request.getRsaPublicKey());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey clientPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKeyBytes));
                SecretKey aesKey = CryptoUtils.generateAESKey();
                UdpClientSession session = new UdpClientSession(senderAddress, aesKey);

                String encryptedAESKey = CryptoUtils.encryptRSA(aesKey.getEncoded(), clientPublicKey);
                sendObject(senderAddress, encryptedAESKey);

                synchronized (clients) {
                    clients.put(user, session);
                }

                System.out.println("Sessão segura estabelecida com " + user.getUsername());

                broadcast(null, user.getUsername() + " entrou no chat");

            } else if (obj instanceof Message) {
                Message message = (Message) obj;
                User senderUser = findUserByUsername(message.getSender());
                if (senderUser == null) return;

                UdpClientSession senderSession = clients.get(senderUser);
                if (senderSession == null || senderSession.getSecretKey() == null) return;

                String[] parts = message.getContent().split(":");
                String ciphertext = parts[0];
                byte[] iv = Base64.getDecoder().decode(parts[1]);
                String decryptedContent = CryptoUtils.decryptAES(ciphertext, senderSession.getSecretKey(), iv);
                message.setContent(decryptedContent);

                routeMessage(message, senderSession);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void routeMessage(Message message, UdpClientSession senderSession) throws Exception {
        String recipient = message.getRecipient();
        if (recipient.equals("/all")) {
            broadcast(message.getSender(), message.getContent());
        } else if (recipient.equals("/online")) {
            Message onlineMsg = new Message(null, message.getSender(), MessageUtils.getOnlineUsers(clients.keySet().stream().toList()));
            sendMessage(senderSession, onlineMsg);
        } else {
            UdpClientSession recipientSession = getUdpClientSession(recipient);
            if (recipientSession != null) {
                sendMessage(recipientSession, message);
            } else {
                Message errorMsg = new Message("Servidor", message.getSender(), "Usuário '" + recipient + "' não encontrado.");
                sendMessage(senderSession, errorMsg);
            }
        }
    }

    private void broadcast(String sender, String content) throws Exception {
        Message msg = new Message(sender, null, content);
        synchronized (clients) {
            for (Map.Entry<User, UdpClientSession> entry : clients.entrySet()) {
                if (!entry.getKey().getUsername().equals(sender)) {
                    sendMessage(entry.getValue(), msg);
                }
            }
        }
    }

    private void sendMessage(UdpClientSession session, Message message) throws Exception {
        if (session == null || session.getSecretKey() == null) return;

        String originalContent = message.getContent();

        if (!originalContent.startsWith("Usuários online:")) {
            byte[] iv = CryptoUtils.generateIV();
            String ciphertext = CryptoUtils.encryptAES(originalContent, session.getSecretKey(), iv);
            String payload = ciphertext + ":" + Base64.getEncoder().encodeToString(iv);
            message.setContent(payload);
        }

        sendObject(session.getAddress(), message);
        message.setContent(originalContent);
    }

    private void sendObject(InetSocketAddress address, Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);
    }

    private User findUserByUsername(String username) {
        synchronized (clients) {
            for (User user : clients.keySet()) {
                if (user.getUsername().equals(username)) {
                    return user;
                }
            }
        }
        return null;
    }

    private UdpClientSession getUdpClientSession(String username) {
        synchronized (clients) {
            User user = findUserByUsername(username);
            return (user != null) ? clients.get(user) : null;
        }
    }
}