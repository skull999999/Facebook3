package thefacebook.datastructures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FriendGraph {
    private final MyHashTable<String, ArrayList<String>> adjacency = new MyHashTable<>();

    public void addUser(String userId) {
        if (!adjacency.containsKey(userId)) {
            adjacency.put(userId, new ArrayList<>());
        }
    }

    public void addFriendship(String a, String b) {
        addUser(a);
        addUser(b);
        addOneWay(a, b);
        addOneWay(b, a);
    }

    public void removeUser(String userId) {
        for (String other : adjacency.keys()) {
            ArrayList<String> friends = adjacency.get(other);
            if (friends != null) {
                friends.remove(userId);
            }
        }
        adjacency.remove(userId);
    }

    public boolean areFriends(String a, String b) {
        ArrayList<String> friends = adjacency.get(a);
        return friends != null && friends.contains(b);
    }

    public ArrayList<String> friendsOf(String userId) {
        ArrayList<String> friends = adjacency.get(userId);
        return friends == null ? new ArrayList<>() : new ArrayList<>(friends);
    }

    public ArrayList<String> commonFriends(String a, String b) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> aFriends = friendsOf(a);
        ArrayList<String> bFriends = friendsOf(b);
        for (String id : aFriends) {
            if (bFriends.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    // BFS is used to classify first, second, and third degree connections in the social graph.
    public ArrayList<String> connectionRecommendations(String userId) {
        ArrayList<String> result = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        MyHashTable<String, Integer> distance = new MyHashTable<>();
        queue.add(userId);
        distance.put(userId, 0);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int d = distance.get(current);
            if (d >= 3) {
                continue;
            }
            for (String next : friendsOf(current)) {
                if (!distance.containsKey(next)) {
                    distance.put(next, d + 1);
                    queue.add(next);
                    if (d + 1 >= 2 && !areFriends(userId, next)) {
                        result.add(next);
                    }
                }
            }
        }
        sortByCommonFriendCount(userId, result);
        return result;
    }

    public int connectionDegree(String fromUserId, String toUserId) {
        if (fromUserId.equals(toUserId)) {
            return 0;
        }
        Queue<String> queue = new LinkedList<>();
        MyHashTable<String, Integer> distance = new MyHashTable<>();
        queue.add(fromUserId);
        distance.put(fromUserId, 0);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int d = distance.get(current);
            if (d >= 3) {
                continue;
            }
            for (String next : friendsOf(current)) {
                if (!distance.containsKey(next)) {
                    int nextDistance = d + 1;
                    if (next.equals(toUserId)) {
                        return nextDistance;
                    }
                    distance.put(next, nextDistance);
                    queue.add(next);
                }
            }
        }
        return -1;
    }

    public ArrayList<String> connectionsAtDegree(String userId, int targetDegree) {
        ArrayList<String> result = new ArrayList<>();
        if (targetDegree < 1 || targetDegree > 3) {
            return result;
        }

        Queue<String> queue = new LinkedList<>();
        MyHashTable<String, Integer> distance = new MyHashTable<>();
        queue.add(userId);
        distance.put(userId, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDistance = distance.get(current);
            if (currentDistance >= 3) {
                continue;
            }
            for (String next : friendsOf(current)) {
                if (!distance.containsKey(next)) {
                    int nextDistance = currentDistance + 1;
                    distance.put(next, nextDistance);
                    queue.add(next);
                    if (nextDistance == targetDegree) {
                        result.add(next);
                    }
                }
            }
        }
        return result;
    }

    public List<String> allUserIds() {
        return adjacency.keys();
    }

    private void addOneWay(String a, String b) {
        ArrayList<String> friends = adjacency.get(a);
        if (!friends.contains(b)) {
            friends.add(b);
        }
    }

    private void sortByCommonFriendCount(String userId, ArrayList<String> ids) {
        for (int i = 0; i < ids.size() - 1; i++) {
            for (int j = 0; j < ids.size() - i - 1; j++) {
                int left = commonFriends(userId, ids.get(j)).size();
                int right = commonFriends(userId, ids.get(j + 1)).size();
                int leftDegree = connectionDegree(userId, ids.get(j));
                int rightDegree = connectionDegree(userId, ids.get(j + 1));
                if (leftDegree > rightDegree || (leftDegree == rightDegree && left < right)) {
                    String temp = ids.get(j);
                    ids.set(j, ids.get(j + 1));
                    ids.set(j + 1, temp);
                }
            }
        }
    }
}
