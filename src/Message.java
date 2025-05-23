import java.io.Serializable;

public class Message implements Serializable {
    public String sender;
    public String recipient;
    public String content;

    public Message(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }
}
