package br.unioeste.sd.chat.server;

import br.unioeste.sd.chat.domain.Message;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Map<String, ObjectOutputStream> clients;

    private String username;

    public ClientHandler(Socket socket, Map<String, ObjectOutputStream> clients) throws IOException {
        this.socket = socket;
        this.clients = clients;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            username = (String) in.readObject();

            synchronized (clients) {
                clients.put(username, out);

                broadcast(null, username + " entrou no chat");
            }

            Message message = null;

            while ((message = (Message) in.readObject()) != null) {
                if(message.getRecipient() == null){
                    broadcast(message.getSender(), message.getContent());
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

        for (Map.Entry<String, ObjectOutputStream> clientOut : clients.entrySet()) {
            String key = clientOut.getKey();
            ObjectOutputStream out = clientOut.getValue();

            if(!key.equals(username)){
                out.writeObject(msg);
            }

        }
    }

    private void sendPrivate(String sender, String recipient, String content) throws IOException {
        ObjectOutputStream clientOut = clients.get(recipient);
        if (clientOut != null) {
            clientOut.writeObject(new Message(sender, recipient, content));
        } else {
            out.writeObject(new Message("Servidor", sender, "Usuário não encontrado"));
        }
    }
}
