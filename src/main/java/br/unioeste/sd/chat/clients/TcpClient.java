package br.unioeste.sd.chat.clients;

import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.utils.CryptoUtils;
import br.unioeste.sd.chat.utils.MessageUtils;

import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Scanner;

public class TcpClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1024;

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        Scanner scanner = new Scanner(System.in);

        KeyPair rsaKeys = CryptoUtils.generateRSAKeyPair();

        System.out.print("Digite seu nome de usuário: ");
        String username = scanner.nextLine();

        out.writeObject(username);
        out.writeObject(Base64.getEncoder().encodeToString(rsaKeys.getPublic().getEncoded()));
        out.flush();

        String encryptedAES = (String) in.readObject();

        byte[] aesBytes = CryptoUtils.decryptRSA(encryptedAES, rsaKeys.getPrivate());

        SecretKey aesKey = CryptoUtils.bytesToAES(aesBytes);

        System.out.println("Sessão segura estabelecida!");

        new Thread(() -> {
            try {
                Message message;

                while ((message = (Message) in.readObject()) != null) {

                    String[] parts = message.getContent().split(":");
                    String ciphertext = parts[0];

                    byte[] iv = Base64.getDecoder().decode(parts[1]);

                    String decrypted = CryptoUtils.decryptAES(ciphertext, aesKey, iv);

                    message.setContent(decrypted);

                    MessageUtils.showMessageOutput(message);
                }
            } catch (Exception e) {
                 e.printStackTrace();
                System.out.println("Desconectado do servidor.");
            }
        }).start();

        while (true) {
            String line = scanner.nextLine();
            String recipient = MessageUtils.parseRecipient(line);
            String content = MessageUtils.parseMessageContent(line);

            byte[] iv = CryptoUtils.generateIV();

            String ciphertext = CryptoUtils.encryptAES(content, aesKey, iv);

            String payload = ciphertext + ":" + Base64.getEncoder().encodeToString(iv);

            out.writeObject(new Message(username, recipient, payload));
            out.flush();

            MessageUtils.showOwnMessage(new Message(username, recipient, content));
        }
    }
}