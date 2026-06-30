package thefacebook.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class User {
    private String id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String passwordHash;
    private LocalDate birthday;
    private String address;
    private String gender;
    private String relationshipStatus;
    private String avatarPath = "";
    // ArrayList is used because hobbies are ordered, displayed often, and can grow or shrink dynamically.
    private final ArrayList<String> hobbies = new ArrayList<>();
    // Stack is used because the newest occupation should appear first, matching profile browsing habits.
    private final Stack<String> careerHistory = new Stack<>();
    private Role role;

    public User(String id, String name, String username, String email, String phone, String passwordHash,
                LocalDate birthday, String address, String gender, String relationshipStatus, Role role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.birthday = birthday;
        this.address = address;
        this.gender = gender;
        this.relationshipStatus = relationshipStatus;
        this.role = role;
    }

    public int getAge() {
        if (birthday == null) {
            return 0;
        }
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    public String toCsv() {
        return join(id, name, username, email, phone, passwordHash, birthday.toString(), address, gender,
                relationshipStatus, role.name(), String.join(";", hobbies), String.join(";", careerHistory), avatarPath);
    }

    public static User fromCsv(String line) {
        String[] p = line.split("\\|", -1);
        User user = new User(p[0], p[1], p[2], p[3], p[4], p[5], LocalDate.parse(p[6]),
                p[7], p[8], p[9], Role.valueOf(p[10]));
        if (p.length > 11 && !p[11].trim().isEmpty()) {
            for (String hobby : p[11].split(";")) {
                user.hobbies.add(hobby);
            }
        }
        if (p.length > 12 && !p[12].trim().isEmpty()) {
            for (String career : p[12].split(";")) {
                user.careerHistory.push(career);
            }
        }
        if (p.length > 13) {
            user.avatarPath = p[13];
        }
        return user;
    }

    private static String join(String... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append('|');
            }
            out.append(values[i].replace("|", " ").replace("\n", " "));
        }
        return out.toString();
    }

    public String publicProfile(int friendCount) {
        return "ID: " + id + "\nName: " + name + "\nUsername: " + username + "\nGender: " + gender
                + "\nAge: " + getAge() + "\nAddress: " + address + "\nRelationship: " + relationshipStatus
                + "\nHobbies: " + hobbies + "\nCareer: " + careerHistory + "\nFriends: " + friendCount
                + "\nAvatar: " + (avatarPath == null || avatarPath.isEmpty() ? "Not set" : avatarPath)
                + "\nRole: " + role;
    }

    /**
     * User 用户实体类 getter/setter、toString 代码解析
     */
    @Override
    public String toString() {
        return name + " (" + username + ", " + id + ")";
    }

    public List<String> getHobbies() { return hobbies; }
    public Stack<String> getCareerHistory() { return careerHistory; }
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getRelationshipStatus() { return relationshipStatus; }
    public void setRelationshipStatus(String relationshipStatus) { this.relationshipStatus = relationshipStatus; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public Role getRole() { return role; }
}
