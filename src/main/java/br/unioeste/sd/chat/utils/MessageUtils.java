package br.unioeste.sd.chat.utils;

import br.unioeste.sd.chat.domain.Message;
import br.unioeste.sd.chat.domain.User;
import de.vandermeer.asciitable.AsciiTable;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageUtils {
    public static String parseRecipient(String line) {
        if (line.startsWith("/all")) {
            return "/all";
        } else if (line.startsWith("@")) {
            String[] parts = line.split(" ", 2);
            if (parts.length < 2) {
                throw new RuntimeException("Mensagem privada deve conter o destinatário e o conteúdo");
            }
            return parts[0].substring(1);
        }
        else if (line.startsWith("/online")) {
            return "/online";
        }
        else {
            throw new RuntimeException("Formato inválido. Use /all ou @nome.");
        }
    }

    public static String parseMessageContent(String line) {
        return line.contains(" ") ? line.substring(line.indexOf(' ') + 1) : "";
    }

    public static void showMessageOutput(Message message) {
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
        } else if (recipient == null) {
            prefix = "[todos][" + sender + "]";
            color = PINK;
        } else {
            prefix = "[privado][" + sender + "]";
            color = getHexColorCode(sender);
        }

        String messageOutput = color + prefix + (sender == null ? "" : RESET) + ": " + message.getContent() + (sender == null ? RESET : "");

        System.out.println(messageOutput);
    }

    public static void showOwnMessage(Message message) {
        final String RESET = "\u001B[0m";
        final String YELLOW = "\u001B[33m";

        String recipient = message.getRecipient().equals("/all") ? "todos" : message.getRecipient();

        if(recipient.equals("/online")){
            return;
        }

        System.out.println(YELLOW + "Para " + recipient + RESET + ": " + message.getContent());
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

    public static String getOnlineUsers(List<User> users){
        AsciiTable table = new AsciiTable();

        table.addRule();
        table.addRow("Usuários", "IPs");
        table.addRule();

        for (User user : users) {
            table.addRow(user.getUsername(), user.getIp());
            table.addRule();
        }

        table.addRule();

        return "Listando usuários online...\n" + table.render();
    }
}
