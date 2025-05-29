package br.unioeste.sd.chat.servers.handlers;

import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.domain.User;
import br.unioeste.sd.chat.utils.MessageUtils;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class TcpClientHandler extends Thread {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Map<User, ObjectOutputStream> clients;

    private String username;

    public TcpClientHandler(Socket socket, Map<User, ObjectOutputStream> clients) throws IOException {
        this.socket = socket;
        this.clients = clients;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            username = (String) in.readObject();

            synchronized (clients) {
                User user = new User(username, socket.getInetAddress().getHostAddress());

                clients.put(user, out);

                broadcast(null, username + " entrou no chat");
            }

            Message message = null;

            while ((message = (Message) in.readObject()) != null) {
                if(message.getRecipient().equals( "/all")){
                    broadcast(message.getSender(), message.getContent());
                }
                else if(message.getRecipient().equals("/online")){
                    sendPrivate(null, message.getSender(), MessageUtils.getOnlineUsers(clients.keySet().stream().toList()));
                }
                else {
                    sendPrivate(message.getSender(), message.getRecipient(), message.getContent());
                }
            }
        } catch (Exception e) {
            System.out.println("Usuário " + username + " desconectado.");
        } finally {
            try {
                socket.close();
                synchronized (clients) {
                    clients.remove(username);
                    broadcast(null, username + " saiu do chat");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String sender, String content) throws IOException {
        Message msg = new Message(sender, null, content);

        for (Map.Entry<User, ObjectOutputStream> clientOut : clients.entrySet()) {
            User user = clientOut.getKey();
            ObjectOutputStream out = clientOut.getValue();

            if(!user.getUsername().equals(username)){
                out.writeObject(msg);
            }

        }
    }

    private void sendPrivate(String sender, String recipient, String content) throws IOException {
        ObjectOutputStream clientOut = null;

        synchronized (clients) {
            for (Map.Entry<User, ObjectOutputStream> entry : clients.entrySet()) {
                if (entry.getKey().getUsername().equals(recipient)) {
                    clientOut = entry.getValue();
                    break;
                }
            }
        }

        if (clientOut != null) {
            clientOut.writeObject(new Message(sender, recipient, content));
        } else {
            out.writeObject(new Message("Servidor", sender, "Usuário não encontrado"));
        }
    }
}
