import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Map<String, ObjectOutputStream> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        public void run() {
            try {
                username = (String) in.readObject();
                synchronized (clients) {
                    clients.put(username, out);
                    broadcast("Servidor", "all", username + " entrou no chat");
                }

                Message msg;
                while ((msg = (Message) in.readObject()) != null) {
                    if (msg.recipient.equals("all")) {
                        broadcast(msg.sender, "all", msg.content);
                    } else {
                        sendPrivate(msg.sender, msg.recipient, msg.content);
                    }
                }
            } catch (Exception e) {
                System.out.println("Usuário " + username + " desconectado.");
            } finally {
                try {
                    socket.close();
                    synchronized (clients) {
                        clients.remove(username);
                        broadcast("Servidor", "all", username + " saiu do chat");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcast(String sender, String recipient, String content) throws IOException {
            Message msg = new Message(sender, recipient, content);
            for (ObjectOutputStream clientOut : clients.values()) {
                clientOut.writeObject(msg);
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
}
