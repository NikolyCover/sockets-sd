package br.unioeste.sd.chat.clients;

import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.utils.MessageUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class TcpClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1024;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite seu nome de usuário: ");
        String username = scanner.nextLine();
        out.writeObject(username);

        new Thread(() -> {
            try {
                Message message = null;

                while ((message = (Message) in.readObject()) != null) {
                    MessageUtils.showMessageOutput(message);
                }
            } catch (Exception e) {
                System.out.println("Desconectado do servidor.");
            }
        }).start();

        while (true) {
            String line = scanner.nextLine();

            System.out.print("\033[F");
            System.out.print("\033[2K");
            System.out.flush();

            String recipient = MessageUtils.parseRecipient(line);
            String content = MessageUtils.parseMessageContent(line);

            Message message = Message.builder().sender(username).recipient(recipient).content(content).build();

            out.writeObject(message);

            MessageUtils.showOwnMessage(message);
        }
    }
}
