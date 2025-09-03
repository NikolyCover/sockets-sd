package br.unioeste.sd.chat.servers.handlers;

import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.domain.User;
import br.unioeste.sd.chat.utils.MessageUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;

public class UdpClientHandler extends Thread {
    private final DatagramSocket socket;
    private final DatagramPacket packet;
    private final Map<User, InetSocketAddress> clients;

    private String username;

    public UdpClientHandler(DatagramSocket socket, DatagramPacket packet, Map<User, InetSocketAddress> clients) {
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

            if (obj instanceof String objMessage) {
                this.username = objMessage;

                synchronized (clients) {
                    InetSocketAddress userAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                    User user = new User(username, userAddress.getAddress().getHostAddress());

                    clients.put(user, userAddress);
                    broadcast(null, user + " entrou no chat");
                }

            } else if (obj instanceof Message message) {
                String sender = message.getSender();
                String recipient = message.getRecipient();

                if(recipient == null){
                    broadcast(null, message.getContent());
                }
                else if (recipient.equals("/all")) {
                    broadcast(message.getSender(), message.getContent());
                } else if (recipient.equals("/online")) {
                    Message onlineMsg = Message.builder()
                            .sender(null)
                            .recipient(sender)
                            .content(MessageUtils.getOnlineUsers(clients.keySet().stream().toList()))
                            .build();

                    InetSocketAddress address = getInetSocketAddress(sender);
                    sendMessage(address, onlineMsg);
                } else {
                    InetSocketAddress recipientAddress = getInetSocketAddress(recipient);

                    if (recipientAddress != null) {
                        sendMessage(recipientAddress, message);
                    } else {
                        Message error = Message.builder()
                                .sender(null)
                                .recipient(sender)
                                .content("Usuário \"" + recipient + "\" não encontrado.")
                                .build();

                        InetSocketAddress senderAddress = getInetSocketAddress(sender);
                        sendMessage(senderAddress, error);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Usuário " + username + " desconectado.");
        }
//        } finally {
//            try {
//                socket.close();
//                synchronized (clients) {
//                    removeClientByUsername(username);
//                    broadcast(null, username + " saiu do chat");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void broadcast(String sender, String content) throws IOException {
        Message msg = new Message(sender, null, content);

        for (Map.Entry<User, InetSocketAddress> clientOut : clients.entrySet()) {
            User user = clientOut.getKey();
            InetSocketAddress out = clientOut.getValue();

            if(!user.getUsername().equals(sender)){
                sendMessage(out, msg);
            }

        }
    }

    private void sendMessage(InetSocketAddress address, Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.flush();

        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
        socket.send(packet);

        System.out.println("Mensagem enviada para: " + address);
    }

    private InetSocketAddress getInetSocketAddress(String recipientUsername) {
        synchronized (clients) {
            for (Map.Entry<User, InetSocketAddress> entry : clients.entrySet()) {
                if (entry.getKey().getUsername().equals(recipientUsername)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private void removeClientByUsername(String username) {
        synchronized (clients) {
            User userToRemove = null;

            for (User user : clients.keySet()) {
                if (user.getUsername().equals(username)) {
                    userToRemove = user;
                    break;
                }
            }

            if (userToRemove != null) {
                clients.remove(userToRemove);
                System.out.println("Usuário removido: " + username);
            }
        }
    }
}
