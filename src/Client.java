import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

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
                Message msg;
                while ((msg = (Message) in.readObject()) != null) {
                    System.out.println("[" + msg.sender + "]: " + msg.content);
                }
            } catch (Exception e) {
                System.out.println("Desconectado do servidor.");
            }
        }).start();

        while (true) {
            System.out.print("Destinatário (ou 'all'): ");
            String recipient = scanner.nextLine();
            System.out.print("Mensagem: ");
            String content = scanner.nextLine();

            Message msg = new Message(username, recipient, content);
            out.writeObject(msg);
        }
    }
}
