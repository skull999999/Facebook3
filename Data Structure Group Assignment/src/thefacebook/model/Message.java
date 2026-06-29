package thefacebook.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String senderUserId;
    private final LocalDateTime sentAt;
    private final String text;

    public Message(String senderUserId, LocalDateTime sentAt, String text) {
        this.senderUserId = senderUserId;
        this.sentAt = sentAt;
        this.text = text;
    }

    public static Message now(String senderUserId, String text) {
        return new Message(senderUserId, LocalDateTime.now(), text);
    }

    public static Message fromParts(String senderUserId, String sentAt, String text) {
        return new Message(senderUserId, LocalDateTime.parse(sentAt), text);
    }

    public String displayLine() {
        return "[" + sentAt.format(FORMATTER) + "] " + senderUserId + ": " + text;
    }

    public String getSenderUserId() { return senderUserId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getText() { return text; }
}
