package thefacebook.service;

import thefacebook.datastructures.FriendGraph;
import thefacebook.datastructures.MyHashTable;
import thefacebook.model.FriendConversation;
import thefacebook.model.FriendRequest;
import thefacebook.model.GroupConversation;
import thefacebook.model.Message;
import thefacebook.model.Role;
import thefacebook.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SocialNetwork {
    private final CsvStorage storage;
    private final MyHashTable<String, User> usersById = new MyHashTable<>();
    private final MyHashTable<String, User> usersByEmail = new MyHashTable<>();
    private final MyHashTable<String, User> usersByPhone = new MyHashTable<>();
    private final MyHashTable<String, User> usersByUsername = new MyHashTable<>();
    private final FriendGraph graph = new FriendGraph();
    // Queue is used because friend requests should be reviewed in first-in-first-out order.
    private final Queue<FriendRequest> requests = new LinkedList<>();
    private final ArrayList<FriendConversation> friendConversations = new ArrayList<>();
    private final MyHashTable<String, GroupConversation> groupsById = new MyHashTable<>();

    public SocialNetwork(String dataFolder) {
        this.storage = new CsvStorage(dataFolder);
    }
    /**
     * 程序启动加载所有持久化数据
     */
    public void load() {
        storage.ensureFolder();
        List<User> loaded = storage.loadUsers();
        if (loaded.isEmpty()) {
            seedData();
            saveAll();
            return;
        }
        for (User user : loaded) {
            addToIndexes(user);
        }
        for (String[] row : storage.loadFriendships()) {
            if (row.length >= 2) {
                graph.addFriendship(row[0], row[1]);
            }
        }
        requests.addAll(storage.loadRequests());
        loadConversations();
    }

    /**
     * 用户注册业务方法
     * @param name 用户真实姓名
     * @param username 登录用户名
     * @param email 用户邮箱
     * @param phone 用户手机号
     * @param password 原始明文密码
     * @param confirm 确认密码
     * @return 注册结果提示字符串（成功/各类失败提示）
     */
    public String register(String name, String username, String email, String phone, String password, String confirm) {
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        if (!email.contains("@") || phone.length() < 8) {
            return "Email or phone format is invalid.";
        }
        if (usersByEmail.containsKey(email) || usersByPhone.containsKey(phone) || usersByUsername.containsKey(username)) {
            return "Username, email, or phone already exists.";
        }
        String id = "U" + (usersById.values().size() + 1001);
        User user = new User(id, name, username, email, phone, PasswordUtil.sha256(password),
                LocalDate.of(2004, 1, 1), "Not set", "Not set", "Single", Role.USER);
        addToIndexes(user);
        saveAll();
        return "Registered successfully. Your ID is " + id;
    }

    /**
     * 用户登录方法
     * @param emailOrPhone 登录凭证：支持邮箱 或 手机号两种方式登录
     * @param password 用户输入的明文登录密码
     * @return 登录成功返回完整User对象；账号不存在/密码错误则返回null
     */
    public User login(String emailOrPhone, String password) {
        User user = usersByEmail.get(emailOrPhone);
        if (user == null) {
            user = usersByPhone.get(emailOrPhone);
        }
        if (user != null && user.getPasswordHash().equals(PasswordUtil.sha256(password))) {
            return user;
        }
        return null;
    }

    /**
     * 更新用户信息
     * @param user 携带修改后新数据的用户对象
     */
    public void updateUser(User user) {
        rebuildIndexes();
        saveAll();
    }

    /**
     * 发送好友申请
     * @param fromId 发起申请的用户ID
     * @param toId 接收申请的目标用户ID
     * @return 处理结果提示文本
     */
    public String sendFriendRequest(String fromId, String toId) {
        if (fromId.equals(toId)) {
            return "You cannot add yourself.";
        }
        if (usersById.get(toId) == null) {
            return "Target user does not exist.";
        }
        if (graph.areFriends(fromId, toId)) {
            return "You are already friends.";
        }
        for (FriendRequest request : requests) {
            if (request.getFromUserId().equals(fromId) && request.getToUserId().equals(toId)) {
                return "Friend request already sent.";
            }
        }
        requests.add(new FriendRequest(fromId, toId));
        saveAll();
        return "Friend request sent.";
    }

    /**
     * 查询当前用户收到的所有好友申请
     * @param userId 当前登录用户ID
     * @return 该用户作为接收方的全部好友申请列表
     */
    public List<FriendRequest> requestsFor(String userId) {
        ArrayList<FriendRequest> result = new ArrayList<>();
        for (FriendRequest request : requests) {
            if (request.getToUserId().equals(userId)) {
                result.add(request);
            }
        }
        return result;
    }

    /**
     * 处理好友申请（同意/拒绝）
     * @param currentUserId 当前操作的用户ID（好友申请接收人）
     * @param fromUserId 发起好友申请的用户ID
     * @param accept true=同意申请，false=拒绝申请
     * @return 处理结果提示文字
     */
    public String respondToRequest(String currentUserId, String fromUserId, boolean accept) {
        Queue<FriendRequest> kept = new LinkedList<>();
        boolean found = false;
        while (!requests.isEmpty()) {
            FriendRequest request = requests.poll();
            if (request.getFromUserId().equals(fromUserId) && request.getToUserId().equals(currentUserId)) {
                found = true;
                if (accept) {
                    graph.addFriendship(fromUserId, currentUserId);
                }
            } else {
                kept.add(request);
            }
        }
        requests.addAll(kept);
        saveAll();
        if (!found) {
            return "Request not found.";
        }
        return accept ? "Friend request accepted." : "Friend request rejected.";
    }

    /**
     * 根据关键词搜索全部用户
     * @param keyword 用户输入的搜索关键词
     * @return 匹配关键词的用户列表，按姓名升序排序
     */
    public ArrayList<User> searchUsers(String keyword) {
        String lower = keyword.toLowerCase();
        ArrayList<User> result = new ArrayList<>();
        for (User user : usersById.values()) {
            if (contains(user.getName(), lower) || contains(user.getUsername(), lower) || contains(user.getId(), lower)
                    || contains(user.getEmail(), lower) || contains(user.getPhone(), lower)) {
                if (!result.contains(user)) {
                    result.add(user);
                }
            }
        }
        sortByName(result);
        return result;
    }

    /**
     * 获取两个用户的共同好友列表
     * @param a 用户A的ID
     * @param b 用户B的ID
     * @return A、B两人共同好友的用户对象集合，按姓名升序排序
     */
    public ArrayList<User> commonFriends(String a, String b) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.commonFriends(a, b)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        sortByName(result);
        return result;
    }

    /**
     * 获取当前用户的好友推荐列表（推荐可能认识的人）
     * @param userId 当前登录用户ID
     * @return 推荐用户实体集合
     */
    public ArrayList<User> friendRecommendations(String userId) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.connectionRecommendations(userId)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    public int connectionDegree(String fromUserId, String toUserId) {
        return graph.connectionDegree(fromUserId, toUserId);
    }

    public ArrayList<User> connectionsAtDegree(String userId, int degree) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.connectionsAtDegree(userId, degree)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        sortByName(result);
        return result;
    }

    /**
     * 管理员删除指定用户
     * @param actor 当前执行操作的操作者用户对象
     * @param targetId 待删除用户的ID
     * @return 删除操作结果提示文本
     */
    public String deleteUser(User actor, String targetId) {
        if (actor.getRole() != Role.ADMIN) {
            return "Only admins can delete users.";
        }
        User target = usersById.get(targetId);
        if (target == null) {
            return "User not found.";
        }
        usersById.remove(targetId);
        graph.removeUser(targetId);
        removeConversationsFor(targetId);
        rebuildIndexes();
        saveAll();
        return "User deleted: " + target.getName();
    }

    /**
     * 根据用户ID查询用户信息
     * @param id 需要查询的用户唯一ID
     * @return 匹配ID的User对象；无该用户则返回null
     */
    public User getUser(String id) {
        return usersById.get(id);
    }

    /**
     * 获取指定用户的全部好友列表
     * @param userId 目标用户ID
     * @return 该用户所有好友实体集合，按姓名升序排序
     */
    public ArrayList<User> friendsOf(String userId) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.friendsOf(userId)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        sortByName(result);
        return result;
    }

    /**
     * 获取指定用户的好友总数
     * @param userId 目标用户ID
     * @return 用户好友数量（int整型）
     */
    public int friendCount(String userId) {
        return graph.friendsOf(userId).size();
    }

    /**
     * 向好友发送私聊文本消息
     * Send private message to friend
     * @param fromUserId 发送者用户ID sender user ID
     * @param toUserId 接收好友ID receiver friend ID
     * @param text 消息内容 message text
     * @return 操作结果提示字符串 operation result message
     */
    public String sendFriendMessage(String fromUserId, String toUserId, String text) {
        if (isBlank(text)) {
            return "Message cannot be empty.";
        }
        if (usersById.get(toUserId) == null) {
            return "Friend user does not exist.";
        }
        if (!graph.areFriends(fromUserId, toUserId)) {
            return "You can only chat with friends.";
        }
        FriendConversation conversation = findFriendConversation(fromUserId, toUserId);
        if (conversation == null) {
            conversation = new FriendConversation(fromUserId, toUserId);
            friendConversations.add(conversation);
        }
        conversation.addMessage(fromUserId, text.trim());
        saveAll();
        return "Message sent.";
    }

    /**
     * 查看与指定好友的全部私聊记录
     * View full chat history with target friend
     * @param userId 当前操作用户ID current user ID
     * @param friendId 好友ID friend user ID
     * @return 格式化聊天记录文本 formatted chat history string
     */
    public String viewFriendConversation(String userId, String friendId) {
        if (usersById.get(friendId) == null) {
            return "Friend user does not exist.";
        }
        if (!graph.areFriends(userId, friendId)) {
            return "You can only view conversations with friends.";
        }
        FriendConversation conversation = findFriendConversation(userId, friendId);
        if (conversation == null) {
            return new FriendConversation(userId, friendId).display();
        }
        return conversation.display();
    }

    /**
     * 创建新群组，仅可添加自己的好友
     * Create new group chat, only friends can be added as members
     * @param ownerUserId 群主ID group owner ID
     * @param name 群名称 group display name
     * @param memberIds 待加入成员ID列表 list of member user IDs
     * @return 创建成功提示，附带群名与群ID success message with group name & ID
     */
    public String createGroupConversation(String ownerUserId, String name, List<String> memberIds) {
        if (isBlank(name)) {
            return "Group name cannot be empty.";
        }
        String id = "G" + (groupsById.values().size() + 1001);
        GroupConversation group = new GroupConversation(id, name.trim(), ownerUserId);
        for (String memberId : memberIds) {
            String cleanId = memberId.trim();
            if (cleanId.isEmpty() || cleanId.equals(ownerUserId)) {
                continue;
            }
            if (usersById.get(cleanId) == null) {
                return "User does not exist: " + cleanId;
            }
            if (!graph.areFriends(ownerUserId, cleanId)) {
                return "Only your friends can be added to a group: " + cleanId;
            }
            group.addMember(cleanId);
        }
        groupsById.put(id, group);
        saveAll();
        return "Group created: " + group.getName() + " [" + id + "]";
    }

    /**
     * 获取当前用户所有所属群聊，按群名升序排序
     * Get all groups the user joined, sorted alphabetically by group name
     * @param userId 用户ID target user ID
     * @return 已排序群聊列表 sorted ArrayList of GroupConversation
     */
    public ArrayList<GroupConversation> groupConversationsFor(String userId) {
        ArrayList<GroupConversation> result = new ArrayList<>();
        for (GroupConversation group : groupsById.values()) {
            if (group.hasMember(userId)) {
                result.add(group);
            }
        }
        sortGroupsByName(result);
        return result;
    }

    /**
     * 查看指定群组完整聊天历史
     * View full message history of target group
     * @param userId 当前查看者用户ID viewer's user ID
     * @param groupId 目标群组唯一ID target unique group ID
     * @return 格式化聊天记录文本 / 错误提示字符串 formatted chat log or error message
     */
    public String viewGroupConversation(String userId, String groupId) {
        GroupConversation group = groupsById.get(groupId);
        if (group == null) {
            return "Group not found.";
        }
        if (!group.hasMember(userId)) {
            return "You are not a member of this group.";
        }
        return group.display();
    }

    /**
     * 向指定群组发送文本消息
     * Send text message to target group chat
     * @param fromUserId 消息发送者用户ID sender user ID
     * @param groupId 目标群组ID target group ID
     * @param text 用户输入的原始消息文本 raw input message content
     * @return 发送成功提示 / 各类错误提示 success or error message
     */
    public String sendGroupMessage(String fromUserId, String groupId, String text) {
        if (isBlank(text)) {
            return "Message cannot be empty.";
        }
        GroupConversation group = groupsById.get(groupId);
        if (group == null) {
            return "Group not found.";
        }
        if (!group.hasMember(fromUserId)) {
            return "You are not a member of this group.";
        }
        group.addMessage(fromUserId, text.trim());
        saveAll();
        return "Group message sent.";
    }

    /**
     * 生成平台整体数据统计分析报告
     * @return 报告文件存储的完整绝对路径字符串
     */
    public String generateAnalysisReport() {
        ArrayList<String> lines = new ArrayList<>();
        int users = usersById.values().size();
        int admins = 0;
        int normalUsers = 0;
        int male = 0;
        int female = 0;
        int otherGender = 0;
        int totalAge = 0;
        int friendshipEdges = 0;

        for (User user : usersById.values()) {
            if (user.getRole() == Role.ADMIN) {
                admins++;
            } else {
                normalUsers++;
            }
            if ("Male".equalsIgnoreCase(user.getGender())) {
                male++;
            } else if ("Female".equalsIgnoreCase(user.getGender())) {
                female++;
            } else {
                otherGender++;
            }
            totalAge += user.getAge();
            friendshipEdges += graph.friendsOf(user.getId()).size();
        }

        int averageAge = users == 0 ? 0 : totalAge / users;
        int friendships = friendshipEdges / 2;
        lines.add("TheFacebook Data Analysis Report");
        lines.add("Generated date: " + LocalDate.now());
        lines.add("Total accounts: " + users);
        lines.add("Normal users: " + normalUsers);
        lines.add("Admins: " + admins);
        lines.add("Friendship connections: " + friendships);
        lines.add("Pending friend requests: " + requests.size());
        lines.add("Average age: " + averageAge);
        lines.add("Gender - Male: " + male);
        lines.add("Gender - Female: " + female);
        lines.add("Gender - Other/Not set: " + otherGender);
        lines.add("Actionable insight: If pending requests are high, users may need clearer request notifications.");

        return "Report generated at " + storage.writeReport("analysis_report.txt", lines).toAbsolutePath();
    }

    /**
     * 持久化保存全部业务数据
     * 将内存中的用户、好友关系、未处理好友申请统一写入文件存储
     */
    public void saveAll() {
        storage.saveUsers(usersById.values());
        ArrayList<String> friendshipRows = new ArrayList<>();
        for (String a : graph.allUserIds()) {
            for (String b : graph.friendsOf(a)) {
                if (a.compareTo(b) < 0) {
                    friendshipRows.add(a + "|" + b);
                }
            }
        }
        storage.saveFriendships(friendshipRows);
        storage.saveRequests(new ArrayList<>(requests));
        storage.saveFriendMessages(friendConversations);
        storage.saveGroups(groupsById.values());
        storage.saveGroupMessages(groupsById.values());
    }

    /**
     * 工具私有方法：忽略大小写判断原字符串是否包含目标小写关键词
     * @param value 用户对象字段原始值（姓名/用户名/邮箱等）
     * @param lower 已经转为小写的搜索关键词
     * @return 包含返回true；字段为null 或 不包含返回false
     */
    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase().contains(lower);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 根据两个用户ID，查找双方对应的私聊会话
     * Find 1v1 private conversation between two user IDs
     * @param userId 用户A ID
     * @param friendId 用户B ID
     * @return 匹配会话对象；无匹配返回null
     */
    private FriendConversation findFriendConversation(String userId, String friendId) {
        for (FriendConversation conversation : friendConversations) {
            if (conversation.matches(userId, friendId)) {
                return conversation;
            }
        }
        return null;
    }

    /**
     * 从本地存储读取私聊、群聊、群消息历史，加载进内存容器
     * Load private chats, group info, group messages from persistent storage into memory
     */
    private void loadConversations() {
        for (String[] row : storage.loadFriendMessages()) {
            if (row.length >= 5) {
                FriendConversation conversation = findFriendConversation(row[0], row[1]);
                if (conversation == null) {
                    conversation = new FriendConversation(row[0], row[1]);
                    friendConversations.add(conversation);
                }
                conversation.addLoadedMessage(Message.fromParts(row[2], row[3], row[4]));
            }
        }
        for (String[] row : storage.loadGroups()) {
            if (row.length >= 4) {
                GroupConversation group = new GroupConversation(row[0], row[1], row[2]);
                for (String memberId : row[3].split(";")) {
                    if (!memberId.trim().isEmpty()) {
                        group.addMember(memberId.trim());
                    }
                }
                groupsById.put(group.getId(), group);
            }
        }

        for (String[] row : storage.loadGroupMessages()) {
            if (row.length >= 4) {
                GroupConversation group = groupsById.get(row[0]);
                if (group != null) {
                    group.addLoadedMessage(Message.fromParts(row[1], row[2], row[3]));
                }
            }
        }
    }

    /**
     * 删除用户相关所有聊天数据：私聊会话移除、所有群移除该成员
     * Clear all chat data related to target user: remove private chats, kick user from all groups
     * @param userId 需要清理数据的用户ID
     */
    private void removeConversationsFor(String userId) {
        ArrayList<FriendConversation> kept = new ArrayList<>();
        for (FriendConversation conversation : friendConversations) {
            if (!conversation.hasParticipant(userId)) {
                kept.add(conversation);
            }
        }
        friendConversations.clear();
        friendConversations.addAll(kept);
        for (GroupConversation group : groupsById.values()) {
            group.removeMember(userId);
        }
    }

    /**
     * 私有工具方法：将新用户添加到所有内存索引与好友图中
     * @param user 待加入索引的用户对象
     */
    private void addToIndexes(User user) {
        usersById.put(user.getId(), user);
        usersByEmail.put(user.getEmail(), user);
        usersByPhone.put(user.getPhone(), user);
        usersByUsername.put(user.getUsername(), user);
        graph.addUser(user.getId());
    }

    /**
     * 私有工具方法：重建邮箱、手机号、用户名三大内存索引
     * 当用户邮箱/手机号/用户名发生修改、删除用户后调用，同步更新索引缓存
     */
    private void rebuildIndexes() {
        List<User> users = usersById.values();
        usersByEmail.clear();
        usersByPhone.clear();
        usersByUsername.clear();
        for (User user : users) {
            usersByEmail.put(user.getEmail(), user);
            usersByPhone.put(user.getPhone(), user);
            usersByUsername.put(user.getUsername(), user);
        }
    }

    /**
     * 私有工具方法：冒泡排序，按用户姓名字母升序（忽略大小写）排序列表
     * @param users 需要排序的用户集合
     */
    private void sortByName(ArrayList<User> users) {
        for (int i = 0; i < users.size() - 1; i++) {
            for (int j = 0; j < users.size() - i - 1; j++) {
                if (users.get(j).getName().compareToIgnoreCase(users.get(j + 1).getName()) > 0) {
                    User temp = users.get(j);
                    users.set(j, users.get(j + 1));
                    users.set(j + 1, temp);
                }
            }
        }
    }

    private void sortGroupsByName(ArrayList<GroupConversation> groups) {
        for (int i = 0; i < groups.size() - 1; i++) {
            for (int j = 0; j < groups.size() - i - 1; j++) {
                if (groups.get(j).getName().compareToIgnoreCase(groups.get(j + 1).getName()) > 0) {
                    GroupConversation temp = groups.get(j);
                    groups.set(j, groups.get(j + 1));
                    groups.set(j + 1, temp);
                }
            }
        }
    }

    /**
     * 私有初始化种子数据方法：程序启动时自动批量生成32条测试用户、预设好友关系与待处理好友申请
     */
    private void seedData() {
        for (int i = 1; i <= 32; i++) {
            Role role = i <= 2 ? Role.ADMIN : Role.USER;
            String id = role == Role.ADMIN ? "A" + i : "U" + i;
            User user = new User(id, "Clarice User" + i, "user" + i, "user" + i + "@thefacebook.edu",
                    "6012000" + String.format("%03d", i), PasswordUtil.sha256("pass" + i),
                    LocalDate.of(1980 + (i % 20), (i % 12) + 1, (i % 25) + 1),
                    "Harvard House " + ((i % 5) + 1), i % 2 == 0 ? "Female" : "Male",
                    i % 3 == 0 ? "In a relationship" : "Single", role);
            user.getHobbies().add("Coding");
            user.getHobbies().add(i % 2 == 0 ? "Music" : "Basketball");
            user.getCareerHistory().push("Student");
            addToIndexes(user);
        }
        for (int i = 3; i <= 28; i++) {
            graph.addFriendship("U" + i, "U" + (i + 1));
            if (i + 2 <= 32) {
                graph.addFriendship("U" + i, "U" + (i + 2));
            }
        }
        graph.addFriendship("A1", "U3");
        graph.addFriendship("A2", "U4");
        requests.add(new FriendRequest("U5", "U10"));
        requests.add(new FriendRequest("U8", "U10"));
    }
}
