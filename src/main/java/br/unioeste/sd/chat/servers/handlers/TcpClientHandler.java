package br.unioeste.sd.chat.servers.handlers;

import br.unioeste.sd.chat.domain.ClientSession; // MODIFICADO
import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.domain.User;
import br.unioeste.sd.chat.utils.CryptoUtils;
import br.unioeste.sd.chat.utils.MessageUtils;

import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors; // MODIFICADO

public class TcpClientHandler extends Thread {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Map<User, ClientSession> clients;
    private SecretKey aesKey;
    private String username;

    public TcpClientHandler(Socket socket, Map<User, ClientSession> clients) throws Exception {
        this.socket = socket;
        this.clients = clients;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            username = (String) in.readObject();

            String pubKeyBase64 = (String) in.readObject();

            byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyBase64);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey clientPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            aesKey = CryptoUtils.generateAESKey();

            String encryptedAESKey = CryptoUtils.encryptRSA(aesKey.getEncoded(), clientPublicKey);

            out.writeObject(encryptedAESKey);
            out.flush();

            synchronized (clients) {
                User user = new User(username, socket.getInetAddress().getHostAddress());
                ClientSession session = new ClientSession(out, aesKey);

                clients.put(user, session);

                broadcast(null, username + " entrou no chat");
            }

            Message message;

            while ((message = (Message) in.readObject()) != null) {
                String[] parts = message.getContent().split(":");
                String ciphertext = parts[0];

                byte[] iv = Base64.getDecoder().decode(parts[1]);

                String decrypted = CryptoUtils.decryptAES(ciphertext, aesKey, iv);

                message.setContent(decrypted);

                if (message.getRecipient().equals("/all")) {
                    broadcast(message.getSender(), message.getContent());
                } else if (message.getRecipient().equals("/online")) {
                    sendPrivate(null, message.getSender(),
                            MessageUtils.getOnlineUsers(clients.keySet().stream().toList()));
                } else {
                    sendPrivate(message.getSender(), message.getRecipient(), message.getContent());
                }
            }
        } catch (Exception e) {
            System.out.println("Usuário " + username + " desconectado.");
        } finally {
            try {
                socket.close();

                clients.remove(username);

                broadcast(null, username + " saiu do chat");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String sender, String content) throws Exception {
        for (Map.Entry<User, ClientSession> clientSession : clients.entrySet()) {
            User user = clientSession.getKey();
            ClientSession session = clientSession.getValue();

            if(!user.getUsername().equals(sender)){
                SecretKey recipientKey = session.getSecretKey();
                ObjectOutputStream recipientOut = session.getOut();

                byte[] iv = CryptoUtils.generateIV();

                String ciphertext = CryptoUtils.encryptAES(content, recipientKey, iv);
                String payload = ciphertext + ":" + Base64.getEncoder().encodeToString(iv);

                recipientOut.writeObject(new Message(sender, null, payload));

                recipientOut.flush();
            }
        }
    }

    private void sendPrivate(String sender, String recipient, String content) throws Exception {
        ClientSession clientSession = null;

        synchronized (clients) {
            for (Map.Entry<User, ClientSession> entry : clients.entrySet()) {
                if (entry.getKey().getUsername().equals(recipient)) {
                    clientSession = entry.getValue();
                    break;
                }
            }
        }

        if (clientSession != null) {
            SecretKey recipientKey = clientSession.getSecretKey();
            ObjectOutputStream recipientOut = clientSession.getOut();

            byte[] iv = CryptoUtils.generateIV();

            String ciphertext = CryptoUtils.encryptAES(content, recipientKey, iv);
            String payload = ciphertext + ":" + Base64.getEncoder().encodeToString(iv);

            recipientOut.writeObject(new Message(sender, recipient, payload));
            recipientOut.flush();
        } else {
            byte[] iv = CryptoUtils.generateIV();

            String ciphertext = CryptoUtils.encryptAES("Usuário não encontrado", this.aesKey, iv);
            String payload = ciphertext + ":" + Base64.getEncoder().encodeToString(iv);

            out.writeObject(new Message("Servidor", sender, payload));
            out.flush();
        }
    }
}