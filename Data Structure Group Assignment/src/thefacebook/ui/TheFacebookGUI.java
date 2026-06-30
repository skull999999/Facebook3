package thefacebook.ui;

import thefacebook.datastructures.LinkedListBrowsingHistory;
import thefacebook.model.FriendRequest;
import thefacebook.model.GroupConversation;
import thefacebook.model.Role;
import thefacebook.model.User;
import thefacebook.service.SocialNetwork;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;

public class TheFacebookGUI {
    private final SocialNetwork network;
    private final JFrame frame;
    private final LinkedListBrowsingHistory history = new LinkedListBrowsingHistory();
    private User currentUser;
    private JTextArea output;
    private JLabel avatarLabel;

    public TheFacebookGUI(SocialNetwork network) {
        this.network = network;
        this.frame = new JFrame("TheFacebook");
    }

    public void show() {
        frame.setSize(750, 520);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        showStartPage();
        frame.setVisible(true);
    }

    private void showStartPage() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("TheFacebook 2004", JLabel.CENTER);
        panel.add(title, BorderLayout.NORTH);

        output = new JTextArea();
        output.setEditable(false);
        panel.add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 4));
        JButton login = new JButton("Login");
        JButton register = new JButton("Register");
        JButton search = new JButton("Search Users");
        JButton exit = new JButton("Exit");

        login.addActionListener(e -> login());
        register.addActionListener(e -> register());
        search.addActionListener(e -> searchUsers(false));
        exit.addActionListener(e -> {
            network.saveAll();
            frame.dispose();
        });

        buttons.add(login);
        buttons.add(register);
        buttons.add(search);
        buttons.add(exit);
        panel.add(buttons, BorderLayout.SOUTH);
        setPage(panel);
    }

    private void showHomePage() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Welcome, " + currentUser.getName() + " (" + currentUser.getRole() + ")", JLabel.CENTER);
        panel.add(title, BorderLayout.NORTH);

        output = new JTextArea();
        output.setEditable(false);
        output.setText(currentUser.publicProfile(network.friendCount(currentUser.getId())));
        panel.add(createProfilePanel(currentUser), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(5, 4));
        addButton(buttons, "My Profile", e -> showProfile());
        addButton(buttons, "Edit Profile", e -> editProfile());
        addButton(buttons, "Search Users", e -> searchUsers(true));
        addButton(buttons, "Recommendations", e -> showRecommendations());
        addButton(buttons, "Friend Requests", e -> showRequests());
        addButton(buttons, "My Friends", e -> showFriends());
        addButton(buttons, "Degree Friends", e -> showDegreeFriends());
        addButton(buttons, "Friend Chat", e -> showFriendChat());
        addButton(buttons, "Create Group", e -> createGroupChat());
        addButton(buttons, "Group Chat", e -> showGroupChat());
        addButton(buttons, "Mutual Friends", e -> showMutualFriends());
        addButton(buttons, "History", e -> output.setText(history.showHistory()));
        addButton(buttons, "Back", e -> output.setText(history.back()));
        addButton(buttons, "Forward", e -> output.setText(history.forward()));
        addButton(buttons, "Analysis Report", e -> {
            history.visit("Generated analysis report");
            output.setText(network.generateAnalysisReport());
        });
        if (currentUser.getRole() == Role.ADMIN) {
            addButton(buttons, "Delete User", e -> deleteUser());
        } else {
            addButton(buttons, "Admin Only", e -> message("Only admins can delete users."));
        }
        addButton(buttons, "Logout", e -> {
            history.clear();
            currentUser = null;
            showStartPage();
        });
        addButton(buttons, "Save", e -> {
            network.saveAll();
            message("Data saved.");
        });
        addButton(buttons, "Exit", e -> {
            network.saveAll();
            frame.dispose();
        });

        panel.add(buttons, BorderLayout.SOUTH);
        setPage(panel);
    }

    private void login() {
        JTextField account = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Email or phone:"));
        panel.add(account);
        panel.add(new JLabel("Password:"));
        panel.add(password);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        User user = network.login(account.getText().trim(), new String(password.getPassword()));
        if (user == null) {
            message("Login failed.");
        } else {
            history.clear();
            currentUser = user;
            history.visit("Logged in as " + user.getUsername());
            showHomePage();
        }
    }

    private void register() {
        JTextField name = new JTextField();
        JTextField username = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JPasswordField password = new JPasswordField();
        JPasswordField confirm = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(6, 2));
        panel.add(new JLabel("Name:"));
        panel.add(name);
        panel.add(new JLabel("Username:"));
        panel.add(username);
        panel.add(new JLabel("Email:"));
        panel.add(email);
        panel.add(new JLabel("Phone:"));
        panel.add(phone);
        panel.add(new JLabel("Password:"));
        panel.add(password);
        panel.add(new JLabel("Retype password:"));
        panel.add(confirm);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Register", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String message = network.register(name.getText().trim(), username.getText().trim(),
                    email.getText().trim(), phone.getText().trim(),
                    new String(password.getPassword()), new String(confirm.getPassword()));
            output.setText(message);
        }
    }

    private void showProfile() {
        history.visit("Viewed my profile");
        updateAvatarLabel(currentUser);
        output.setText(currentUser.publicProfile(network.friendCount(currentUser.getId())));
    }

    private void editProfile() {
        JTextField name = new JTextField(currentUser.getName());
        JTextField birthday = new JTextField(currentUser.getBirthday().toString());
        JTextField address = new JTextField(currentUser.getAddress());
        JTextField gender = new JTextField(currentUser.getGender());
        JTextField relationship = new JTextField(currentUser.getRelationshipStatus());
        JTextField hobby = new JTextField();
        JTextField career = new JTextField();
        JTextField avatarPath = new JTextField(currentUser.getAvatarPath());
        JButton chooseAvatar = new JButton("Choose...");
        chooseAvatar.addActionListener(e -> chooseAvatarFile(avatarPath));

        JPanel panel = new JPanel(new GridLayout(8, 2));
        panel.add(new JLabel("Name:"));
        panel.add(name);
        panel.add(new JLabel("Birthday YYYY-MM-DD:"));
        panel.add(birthday);
        panel.add(new JLabel("Address:"));
        panel.add(address);
        panel.add(new JLabel("Gender:"));
        panel.add(gender);
        panel.add(new JLabel("Relationship:"));
        panel.add(relationship);
        panel.add(new JLabel("Add hobby:"));
        panel.add(hobby);
        panel.add(new JLabel("Add career:"));
        panel.add(career);
        panel.add(new JLabel("Avatar image:"));
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.add(avatarPath, BorderLayout.CENTER);
        avatarPanel.add(chooseAvatar, BorderLayout.EAST);
        panel.add(avatarPanel);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Edit Profile", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            currentUser.setName(name.getText().trim());
            currentUser.setBirthday(LocalDate.parse(birthday.getText().trim()));
            currentUser.setAddress(address.getText().trim());
            currentUser.setGender(gender.getText().trim());
            currentUser.setRelationshipStatus(relationship.getText().trim());
            currentUser.setAvatarPath(avatarPath.getText().trim());
            if (!hobby.getText().trim().isEmpty()) {
                currentUser.getHobbies().add(hobby.getText().trim());
            }
            if (!career.getText().trim().isEmpty()) {
                currentUser.getCareerHistory().push(career.getText().trim());
            }
            network.updateUser(currentUser);
            history.visit("Edited my profile");
            showProfile();
            message("Profile updated.");
        } catch (Exception e) {
            message("Invalid input. Birthday must be YYYY-MM-DD.");
        }
    }

    private void searchUsers(boolean canAddFriend) {
        String keyword = JOptionPane.showInputDialog(frame, "Search by name, username, ID, email, or phone:");
        if (keyword == null) {
            return;
        }

        ArrayList<User> users = network.searchUsers(keyword.trim());
        if (currentUser != null) {
            history.visit("Searched users: " + keyword.trim());
        }
        output.setText(formatUsers(users));

        if (canAddFriend && !users.isEmpty()) {
            String id = JOptionPane.showInputDialog(frame, "Enter user ID to send friend request, or leave blank:");
            if (id != null && !id.trim().isEmpty()) {
                output.append("\n" + network.sendFriendRequest(currentUser.getId(), id.trim()));
            }
        }
    }

    private void showRecommendations() {
        history.visit("Viewed friend recommendations");
        ArrayList<User> users = network.friendRecommendations(currentUser.getId());
        output.setText(formatUsers(users));
        if (users.isEmpty()) {
            return;
        }
        String id = JOptionPane.showInputDialog(frame, "Enter user ID to send friend request, or leave blank:");
        if (id != null && !id.trim().isEmpty()) {
            output.append("\n" + network.sendFriendRequest(currentUser.getId(), id.trim()));
        }
    }

    private void showRequests() {
        history.visit("Viewed friend requests");
        java.util.List<FriendRequest> requests = network.requestsFor(currentUser.getId());
        if (requests.isEmpty()) {
            output.setText("No incoming requests.");
            return;
        }

        StringBuilder text = new StringBuilder("Incoming requests:\n");
        for (FriendRequest request : requests) {
            text.append(request.getFromUserId()).append(" wants to add you.\n");
        }
        output.setText(text.toString());

        String fromId = JOptionPane.showInputDialog(frame, "Enter sender ID to respond:");
        if (fromId == null || fromId.trim().isEmpty()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(frame, "Accept this request?", "Friend Request", JOptionPane.YES_NO_OPTION);
        output.append("\n" + network.respondToRequest(currentUser.getId(), fromId.trim(), choice == JOptionPane.YES_OPTION));
    }

    private void showFriends() {
        history.visit("Viewed my friends");
        output.setText(formatUsers(network.friendsOf(currentUser.getId())));
    }

    private void showDegreeFriends() {
        history.visit("Viewed degree friends");
        StringBuilder text = new StringBuilder();
        appendDegreeUsers(text, "First-degree friends", network.connectionsAtDegree(currentUser.getId(), 1));
        appendDegreeUsers(text, "Second-degree friends", network.connectionsAtDegree(currentUser.getId(), 2));
        appendDegreeUsers(text, "Third-degree friends", network.connectionsAtDegree(currentUser.getId(), 3));
        output.setText(text.toString());
    }

    private void showFriendChat() {
        String friendId = JOptionPane.showInputDialog(frame, "Friend user ID:");
        if (friendId == null || friendId.trim().isEmpty()) {
            return;
        }
        String cleanFriendId = friendId.trim();
        history.visit("Opened friend chat with " + cleanFriendId);
        output.setText(network.viewFriendConversation(currentUser.getId(), cleanFriendId));

        String messageText = JOptionPane.showInputDialog(frame, "Type a message, or leave blank to only view:");
        if (messageText != null && !messageText.trim().isEmpty()) {
            output.append("\n" + network.sendFriendMessage(currentUser.getId(), cleanFriendId, messageText));
            output.append("\n\n" + network.viewFriendConversation(currentUser.getId(), cleanFriendId));
            history.visit("Sent friend message to " + cleanFriendId);
        }
    }

    private void createGroupChat() {
        JTextField groupName = new JTextField();
        JTextField memberIds = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Group name:"));
        panel.add(groupName);
        panel.add(new JLabel("Friend IDs, comma separated:"));
        panel.add(memberIds);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Create Group Chat", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String created = network.createGroupConversation(currentUser.getId(), groupName.getText(), splitIds(memberIds.getText()));
        history.visit("Created group chat: " + groupName.getText().trim());
        output.setText(created);
    }

    private void showGroupChat() {
        ArrayList<GroupConversation> groups = network.groupConversationsFor(currentUser.getId());
        output.setText(formatGroups(groups));
        if (groups.isEmpty()) {
            return;
        }
        String groupId = JOptionPane.showInputDialog(frame, "Group ID:");
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        String cleanGroupId = groupId.trim();
        history.visit("Opened group chat " + cleanGroupId);
        output.setText(network.viewGroupConversation(currentUser.getId(), cleanGroupId));

        String messageText = JOptionPane.showInputDialog(frame, "Type a message, or leave blank to only view:");
        if (messageText != null && !messageText.trim().isEmpty()) {
            output.append("\n" + network.sendGroupMessage(currentUser.getId(), cleanGroupId, messageText));
            output.append("\n\n" + network.viewGroupConversation(currentUser.getId(), cleanGroupId));
            history.visit("Sent group message to " + cleanGroupId);
        }
    }

    private void showMutualFriends() {
        String first = JOptionPane.showInputDialog(frame, "First user ID:");
        if (first == null) {
            return;
        }
        String second = JOptionPane.showInputDialog(frame, "Second user ID:");
        if (second == null) {
            return;
        }
        history.visit("Viewed mutual friends for " + first.trim() + " and " + second.trim());
        output.setText(formatUsers(network.commonFriends(first.trim(), second.trim())));
    }

    private void deleteUser() {
        String id = JOptionPane.showInputDialog(frame, "User ID to delete:");
        if (id != null && !id.trim().isEmpty()) {
            history.visit("Deleted user " + id.trim());
            output.setText(network.deleteUser(currentUser, id.trim()));
        }
    }

    private ArrayList<String> splitIds(String ids) {
        ArrayList<String> result = new ArrayList<>();
        for (String id : ids.split(",")) {
            if (!id.trim().isEmpty()) {
                result.add(id.trim());
            }
        }
        return result;
    }

    private String formatGroups(java.util.List<GroupConversation> groups) {
        if (groups.isEmpty()) {
            return "No groups found.";
        }
        StringBuilder text = new StringBuilder("Your groups:\n");
        for (int i = 0; i < groups.size(); i++) {
            GroupConversation group = groups.get(i);
            text.append(i + 1).append(". ")
                    .append(group.getName()).append(" | ")
                    .append(group.getId()).append(" | members: ")
                    .append(group.getMemberUserIds().size()).append("\n");
        }
        return text.toString();
    }

    private void appendDegreeUsers(StringBuilder text, String title, java.util.List<User> users) {
        text.append(title).append(":\n");
        if (users.isEmpty()) {
            text.append("No users found.\n\n");
            return;
        }
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            text.append(i + 1).append(". ")
                    .append(user.getName()).append(" | ")
                    .append(user.getUsername()).append(" | ")
                    .append(user.getId()).append(" | ")
                    .append(user.getEmail()).append(" | Hobbies: ")
                    .append(formatHobbies(user)).append("\n");
        }
        text.append("\n");
    }

    private String formatUsers(java.util.List<User> users) {
        if (users.isEmpty()) {
            return "No users found.";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            text.append(i + 1).append(". ")
                    .append(user.getName()).append(" | ")
                    .append(user.getUsername()).append(" | ")
                    .append(user.getId()).append(" | ")
                    .append(user.getEmail()).append(" | Hobbies: ")
                    .append(formatHobbies(user)).append("\n");
        }
        return text.toString();
    }

    private String formatHobbies(User user) {
        if (user.getHobbies().isEmpty()) {
            return "None";
        }
        return String.join(", ", user.getHobbies());
    }

    private void addButton(JPanel panel, String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        panel.add(button);
    }

    private JPanel createProfilePanel(User user) {
        JPanel profile = new JPanel(new BorderLayout());
        avatarLabel = new JLabel("", JLabel.CENTER);
        updateAvatarLabel(user);
        profile.add(avatarLabel, BorderLayout.WEST);
        profile.add(new JScrollPane(output), BorderLayout.CENTER);
        return profile;
    }

    private void updateAvatarLabel(User user) {
        if (avatarLabel == null) {
            return;
        }
        String avatarPath = user.getAvatarPath();
        if (avatarPath == null || avatarPath.trim().isEmpty() || !new File(avatarPath).exists()) {
            avatarLabel.setIcon(null);
            avatarLabel.setText("<html><center>No<br>Avatar</center></html>");
            return;
        }
        ImageIcon icon = new ImageIcon(avatarPath);
        Image scaled = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        avatarLabel.setText("");
        avatarLabel.setIcon(new ImageIcon(scaled));
    }

    private void chooseAvatarFile(JTextField avatarPath) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            avatarPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void setPage(JPanel panel) {
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(frame, text);
    }
}
