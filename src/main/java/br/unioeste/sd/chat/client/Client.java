package br.unioeste.sd.chat.client;

import br.unioeste.sd.chat.domain.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1024;

    public static String parseRecipient(String line){
        if (line.startsWith("/all")) {
            return null;
        } else if (line.startsWith("@")) {
            String[] parts = line.split(" ", 2);

            if (parts.length < 2) {
                throw new RuntimeException("Mensagem privada deve conter o destinatário e o conteúdo");
            }

            return parts[0].substring(1);
        } else {
            throw new RuntimeException("Formato inválido. Use \\all ou @nome.");
        }

    }

    public static String parseMessageContent(String line){
        return line.contains(" ") ? line.substring(line.indexOf(' ') + 1) : "";
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
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
                    showMessageOutput(message);
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

            String recipient = parseRecipient(line);
            String content = parseMessageContent(line);

            Message message = Message.builder().sender(username).recipient(recipient).content(content).build();

            out.writeObject(message);

            showOwnMessage(message);
        }
    }

    public static void showMessageOutput(Message message){
        final String recipient = message.getRecipient();
        final String sender = message.getSender();

        final String RESET = "\u001B[0m";
        final String BLUE = "\u001B[34m";
        final String PINK = "\u001B[38;2;255;105;180m";

        String prefix;
        String color;

        if (sender == null) {
            prefix = "[servidor]";
            color = BLUE;
        }
        else if (recipient == null) {
            prefix = "[todos][" + sender + "]";
            color = PINK;
        } else {
            prefix = "[privado][" + sender + "]";
            color = getHexColorCode(sender);
        }

        String messageOutput = color + prefix + (sender == null ? "" : RESET) + ": " + message.getContent() + (sender == null ? RESET : "");

        System.out.println(messageOutput);
    }

    private static String getHexColorCode(String input) {
        int hash = input.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);

        r = 100 + (r % 156);
        g = 100 + (g % 156);
        b = 100 + (b % 156);

        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    private static void showOwnMessage(Message message) {
        final String RESET = "\u001B[0m";
        final String YELLOW = "\u001B[33m";

        String recipient = message.getRecipient() != null ? message.getRecipient() : "todos";

        System.out.println(YELLOW + "Para " + recipient +  RESET  + ": " + message.getContent());
    }
}
