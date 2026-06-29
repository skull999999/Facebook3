package thefacebook.model;

import java.util.ArrayList;
import java.util.List;

public class FriendConversation {
    private final String firstUserId;
    private final String secondUserId;
    private final ArrayList<Message> messages = new ArrayList<>();

    public FriendConversation(String firstUserId, String secondUserId) {
        if (firstUserId.compareTo(secondUserId) <= 0) {
            this.firstUserId = firstUserId;
            this.secondUserId = secondUserId;
        } else {
            this.firstUserId = secondUserId;
            this.secondUserId = firstUserId;
        }
    }

    public boolean matches(String userId, String friendId) {
        return hasParticipant(userId) && hasParticipant(friendId);
    }

    public boolean hasParticipant(String userId) {
        return firstUserId.equals(userId) || secondUserId.equals(userId);
    }

    public void addMessage(String senderUserId, String text) {
        if (!hasParticipant(senderUserId)) {
            throw new IllegalArgumentException("Sender is not part of this conversation.");
        }
        messages.add(Message.now(senderUserId, text));
    }

    public void addLoadedMessage(Message message) {
        messages.add(message);
    }

    public String display() {
        StringBuilder text = new StringBuilder("Friend conversation: ")
                .append(firstUserId).append(" and ").append(secondUserId).append("\n");
        if (messages.isEmpty()) {
            text.append("No messages yet.");
        } else {
            for (Message message : messages) {
                text.append(message.displayLine()).append("\n");
            }
        }
        return text.toString();
    }

    public String getFirstUserId() { return firstUserId; }
    public String getSecondUserId() { return secondUserId; }
    public List<Message> getMessages() { return messages; }
}
