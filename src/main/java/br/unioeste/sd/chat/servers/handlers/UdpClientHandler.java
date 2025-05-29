package br.unioeste.sd.chat.servers.handlers;

import br.unioeste.sd.chat.domain.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;

public class UdpClientHandler extends Thread{
    private final DatagramSocket socket;
    private final DatagramPacket receivedPacket;
    private final Map<String, InetSocketAddress> clients;

    public UdpClientHandler(DatagramSocket socket, DatagramPacket receivedPacket, Map<String, InetSocketAddress> clients) {
        this.socket = socket;
        this.receivedPacket = receivedPacket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedPacket.getData(), 0, receivedPacket.getLength());
            ObjectInputStream ois = new ObjectInputStream(bais);

            Message message = (Message) ois.readObject();
            InetSocketAddress senderAddress = new InetSocketAddress(receivedPacket.getAddress(), receivedPacket.getPort());

            clients.putIfAbsent(message.getSender(), senderAddress);

            if (message.getRecipient() == null) {
                for (Map.Entry<String, InetSocketAddress> entry : clients.entrySet()) {
                    sendMessage(entry.getValue(), message);
                }
            } else {
                InetSocketAddress recipientAddress = clients.get(message.getRecipient());

                if (recipientAddress != null) {
                    sendMessage(recipientAddress, message);
                } else {
                    Message error = Message.builder()
                            .sender(null)
                            .content("Usuário \"" + message.getRecipient() + "\" não encontrado.")
                            .build();
                    sendMessage(senderAddress, error);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao processar pacote UDP: " + e.getMessage());
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
    }
}
