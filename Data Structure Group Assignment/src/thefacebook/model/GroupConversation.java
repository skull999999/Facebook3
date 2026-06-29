package thefacebook.model;

import java.util.ArrayList;
import java.util.List;

public class GroupConversation {
    private final String id;
    private String name;
    private final String ownerUserId;
    private final ArrayList<String> memberUserIds = new ArrayList<>();
    private final ArrayList<Message> messages = new ArrayList<>();

    public GroupConversation(String id, String name, String ownerUserId) {
        this.id = id;
        this.name = name;
        this.ownerUserId = ownerUserId;
        addMember(ownerUserId);
    }

    public void addMember(String userId) {
        if (!memberUserIds.contains(userId)) {
            memberUserIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        memberUserIds.remove(userId);
    }

    public boolean hasMember(String userId) {
        return memberUserIds.contains(userId);
    }

    public void addMessage(String senderUserId, String text) {
        if (!hasMember(senderUserId)) {
            throw new IllegalArgumentException("Sender is not a group member.");
        }
        messages.add(Message.now(senderUserId, text));
    }

    public void addLoadedMessage(Message message) {
        messages.add(message);
    }

    public String display() {
        StringBuilder text = new StringBuilder("Group: ").append(name).append(" [").append(id).append("]\n")
                .append("Owner: ").append(ownerUserId).append("\n")
                .append("Members: ").append(memberUserIds).append("\n\n");
        if (messages.isEmpty()) {
            text.append("No group messages yet.");
        } else {
            for (Message message : messages) {
                text.append(message.displayLine()).append("\n");
            }
        }
        return text.toString();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerUserId() { return ownerUserId; }
    public List<String> getMemberUserIds() { return memberUserIds; }
    public List<Message> getMessages() { return messages; }
}
