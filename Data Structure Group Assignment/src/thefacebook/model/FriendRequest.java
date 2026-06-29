package thefacebook.model;

public class FriendRequest {
    private final String fromUserId;
    private final String toUserId;

    public FriendRequest(String fromUserId, String toUserId) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
    }

    public String toCsv() {
        return fromUserId + "|" + toUserId;
    }

    public static FriendRequest fromCsv(String line) {
        String[] p = line.split("\\|", -1);
        return new FriendRequest(p[0], p[1]);
    }

    @Override
    public String toString() {
        return fromUserId + " -> " + toUserId;
    }

    public String getFromUserId() { return fromUserId; }
    public String getToUserId() { return toUserId; }
}
