package thefacebook.ui;

import thefacebook.datastructures.BrowsingHistory;
import thefacebook.model.FriendRequest;
import thefacebook.model.Role;
import thefacebook.model.User;
import thefacebook.service.SocialNetwork;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final SocialNetwork network;
    private final Scanner scanner = new Scanner(System.in);
    private final BrowsingHistory history = new BrowsingHistory();

    public ConsoleUI(SocialNetwork network) {
        this.network = network;
    }

    /**
     * 程序入口主循环方法，启动控制台菜单交互界面
     */
    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== TheFacebook 2004 ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Search users");
            System.out.println("4. Exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    login();
                    break;
                case 2:
                    register();
                    break;
                case 3:
                    searchUsers(null);
                    break;
                case 4:
                    network.saveAll();
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        System.out.println("Goodbye from TheFacebook.");
    }

    /**
     * 控制台登录交互流程
     * 接收用户输入账号密码，调用业务层校验，登录成功进入用户功能菜单，失败直接返回主菜单
     */
    private void login() {
        String emailOrPhone = readText("Email or phone: ");
        String password = readText("Password: ");
        User user = network.login(emailOrPhone, password);
        if (user == null) {
            System.out.println("Login failed.");
            return;
        }
        history.clear();
        history.visit("Logged in as " + user.getUsername());
        userMenu(user);
    }

    /**
     * 控制台注册交互流程
     * 收集用户填写的全部注册信息，调用业务层完成注册校验与创建账号，打印注册结果提示
     */
    private void register() {
        System.out.println("\n--- Register Account ---");
        String name = readText("Name: ");
        String username = readText("Username: ");
        String email = readText("Email: ");
        String phone = readText("Phone: ");
        String password = readText("Password: ");
        String confirm = readText("Retype password: ");
        System.out.println(network.register(name, username, email, phone, password, confirm));
    }

    /**
     * 登录成功后的用户主页菜单控制台交互
     * 根据当前登录用户角色（管理员/普通用户）展示差异化菜单，循环提供各类社交、个人、管理功能
     * @param current 当前登录的用户对象
     */
    private void userMenu(User current) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n=== Home: " + current.getName() + " (" + current.getRole() + ") ===");
            System.out.println("1. View my account");
            System.out.println("2. Edit my account");
            System.out.println("3. Search and view users");
            System.out.println("4. Friend recommendations");
            System.out.println("5. Friend requests");
            System.out.println("6. My friends");
            System.out.println("7. Mutual friends");
            System.out.println("8. Traceback history");
            System.out.println("9. Generate data analysis report");
            if (current.getRole() == Role.ADMIN) {
                System.out.println("10. Admin: delete user");
                System.out.println("11. Logout");
            } else {
                System.out.println("10. Logout");
            }
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    viewProfile(current, current);
                    break;
                case 2:
                    editProfile(current);
                    break;
                case 3:
                    searchUsers(current);
                    break;
                case 4:
                    recommendations(current);
                    break;
                case 5:
                    manageRequests(current);
                    break;
                case 6:
                    listFriends(current);
                    break;
                case 7:
                    mutualFriends();
                    break;
                case 8:
                    System.out.println(history.back());
                    break;
                case 9:
                    System.out.println(network.generateAnalysisReport());
                    break;
                case 10:
                    if (current.getRole() == Role.ADMIN) {
                        deleteUser(current);
                    } else {
                        loggedIn = false;
                    }
                    break;
                case 11:
                    if (current.getRole() == Role.ADMIN) {
                        loggedIn = false;
                    } else {
                        System.out.println("Invalid choice.");
                    }
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        history.clear();
    }

    /**
     * 控制台查看用户资料页面
     * 记录浏览历史，展示用户公开主页信息，并计算、展示当前登录人与目标用户的人脉关系度数
     * @param viewer 当前浏览者（登录用户）
     * @param target 被查看资料的目标用户
     */
    private void viewProfile(User viewer, User target) {
        history.visit("Viewed profile: " + target.getUsername());
        int degree = network.connectionDegree(viewer.getId(), target.getId());
        System.out.println("\n--- Profile ---");
        System.out.println(target.publicProfile(network.friendCount(target.getId())));
        if (viewer.getId().equals(target.getId())) {
            System.out.println("Connection: This is your account.");
        } else if (degree == -1) {
            System.out.println("Connection: Not connected within 3 degrees.");
        } else {
            System.out.println("Connection: " + degree + ordinalSuffix(degree) + " degree.");
        }
    }

    /**
     * 控制台个人资料编辑菜单交互
     * 记录编辑操作历史，循环提供多字段修改入口，每次修改后实时同步更新用户数据
     * @param user 当前登录待编辑资料的用户
     */
    private void editProfile(User user) {
        history.visit("Edited profile");
        boolean editing = true;
        while (editing) {
            System.out.println("\n--- Edit Account ---");
            System.out.println("1. Name");
            System.out.println("2. Username");
            System.out.println("3. Email");
            System.out.println("4. Phone");
            System.out.println("5. Birthday");
            System.out.println("6. Address");
            System.out.println("7. Gender");
            System.out.println("8. Relationship status");
            System.out.println("9. Hobbies");
            System.out.println("10. Career history");
            System.out.println("11. Back");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    user.setName(readText("New name: "));
                    break;
                case 2:
                    user.setUsername(readText("New username: "));
                    break;
                case 3:
                    user.setEmail(readText("New email: "));
                    break;
                case 4:
                    user.setPhone(readText("New phone: "));
                    break;
                case 5:
                    updateBirthday(user);
                    break;
                case 6:
                    user.setAddress(readText("New address: "));
                    break;
                case 7:
                    chooseGender(user);
                    break;
                case 8:
                    chooseRelationship(user);
                    break;
                case 9:
                    editHobbies(user);
                    break;
                case 10:
                    editCareer(user);
                    break;
                case 11:
                    editing = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            network.updateUser(user);
        }
    }

    /**
     * 修改用户生日的子交互方法
     * 读取用户输入日期字符串并解析为LocalDate，格式错误则捕获异常提示
     * @param user 当前编辑资料的用户对象
     */
    private void updateBirthday(User user) {
        try {
            user.setBirthday(LocalDate.parse(readText("Birthday (YYYY-MM-DD): ")));
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
        }
    }

    /**
     * 选择性别子交互方法
     * 封装性别可选列表，调用通用选择工具方法完成性别修改
     * @param user 当前编辑资料的用户对象
     */
    private void chooseGender(User user) {
        ArrayList<String> options = new ArrayList<>(Arrays.asList("Female", "Male", "Other", "Prefer not to say"));
        chooseFromArrayList("Gender", options, user::setGender);
    }

    /**
     * 修改感情状态子交互方法
     * 预设感情状态选项列表，调用通用选择工具完成状态更新
     * @param user 当前编辑资料的用户对象
     */
    private void chooseRelationship(User user) {
        ArrayList<String> options = new ArrayList<>(Arrays.asList("Single", "In a relationship", "It's complicated", "Married"));
        chooseFromArrayList("Relationship", options, user::setRelationshipStatus);
    }

    /**
     * 编辑用户爱好的子交互方法
     * 展示当前已有爱好，提供推荐爱好列表，支持手动输入新增爱好，空输入结束编辑
     * @param user 当前编辑资料的用户对象
     */
    private void editHobbies(User user) {
        System.out.println("Current hobbies: " + user.getHobbies());
        ArrayList<String> options = new ArrayList<>(Arrays.asList("Coding", "Music", "Basketball", "Reading", "Gaming", "Art"));
        System.out.println("Suggested hobbies stored in ArrayList:");
        printNumbered(options);
        String hobby = readText("Type a hobby to add, or leave blank to stop: ");
        if (!hobby.trim().isEmpty()) {
            user.getHobbies().add(hobby);
        }
    }

    /**
     * 编辑用户职业履历栈的子交互方法
     * 展示当前履历（栈结构，最新内容在前），输入新经历压入栈，空输入退出编辑
     * @param user 当前正在编辑资料的用户对象
     */
    private void editCareer(User user) {
        System.out.println("Career stack, latest shown first: " + user.getCareerHistory());
        String career = readText("Push latest job/school experience, or leave blank to stop: ");
        if (!career.trim().isEmpty()) {
            user.getCareerHistory().push(career);
        }
    }

    /**
     * 用户搜索控制台交互逻辑
     * 记录搜索操作历史，支持姓名/用户名/ID/邮箱/手机号多维度检索，展示匹配用户，可查看他人主页、发送好友申请
     * @param current 当前登录用户；若为null代表主菜单未登录状态下的搜索
     */
    private void searchUsers(User current) {
        history.visit("Searched users");
        String keyword = readText("Search by name, username, ID, email, or phone: ");
        ArrayList<User> results = network.searchUsers(keyword);
        if (results.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUsers(results);
        if (current == null) {
            return;
        }
        int choice = readInt("Open result number, or 0 to cancel: ");
        if (choice > 0 && choice <= results.size()) {
            User target = results.get(choice - 1);
            viewProfile(current, target);
            if (!current.getId().equals(target.getId())) {
                String add = readText("Send friend request? (y/n): ");
                if (add.equalsIgnoreCase("y")) {
                    System.out.println(network.sendFriendRequest(current.getId(), target.getId()));
                }
            }
        }
    }

    /**
     * 查看好友推荐控制台交互流程
     * 记录浏览推荐的操作历史，拉取系统推荐用户列表，展示人脉度数与共同好友数量，支持直接发送好友申请
     * @param current 当前登录用户
     */
    private void recommendations(User current) {
        history.visit("Viewed friend recommendations");
        ArrayList<User> users = network.friendRecommendations(current.getId());
        if (users.isEmpty()) {
            System.out.println("No recommendations yet.");
            return;
        }
        System.out.println("\n--- Friend Recommendations ---");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            int degree = network.connectionDegree(current.getId(), user.getId());
            int mutuals = network.commonFriends(current.getId(), user.getId()).size();
            System.out.println((i + 1) + ". " + user.getName() + " [" + user.getId() + "] - "
                    + degree + ordinalSuffix(degree) + " degree, " + mutuals + " mutual friend(s)");
        }
        int choice = readInt("Send request to number, or 0 to cancel: ");
        if (choice > 0 && choice <= users.size()) {
            System.out.println(network.sendFriendRequest(current.getId(), users.get(choice - 1).getId()));
        }
    }

    /**
     * 控制台处理收到的好友申请交互方法
     * 记录查看申请操作历史，加载所有发给当前登录用户的待处理好友请求，支持选择申请同意/拒绝
     * @param current 当前登录用户
     */
    private void manageRequests(User current) {
        history.visit("Reviewed friend requests");
        List<FriendRequest> requests = network.requestsFor(current.getId());
        if (requests.isEmpty()) {
            System.out.println("No incoming requests.");
            return;
        }
        for (int i = 0; i < requests.size(); i++) {
            FriendRequest request = requests.get(i);
            User from = network.getUser(request.getFromUserId());
            System.out.println((i + 1) + ". " + (from == null ? request.getFromUserId() : from.getName())
                    + " [" + request.getFromUserId() + "]");
        }
        int choice = readInt("Choose request, or 0 to cancel: ");
        if (choice <= 0 || choice > requests.size()) {
            return;
        }
        String answer = readText("Accept? (y/n): ");
        FriendRequest request = requests.get(choice - 1);
        System.out.println(network.respondToRequest(current.getId(), request.getFromUserId(), answer.equalsIgnoreCase("y")));
    }

    /**
     * 控制台查看当前用户好友列表交互方法
     * 记录查看好友列表操作历史，查询并打印当前登录用户全部好友
     * @param current 当前登录用户
     */
    private void listFriends(User current) {
        history.visit("Viewed friend list");
        ArrayList<User> friends = network.friendsOf(current.getId());
        if (friends.isEmpty()) {
            System.out.println("You have no friends yet.");
            return;
        }
        printUsers(friends);
    }

    /**
     * 控制台查询两位用户共同好友交互方法
     * 记录查询共同好友操作历史，输入两个用户ID，查询并展示二者交集好友
     */
    private void mutualFriends() {
        history.visit("Checked mutual friends");
        String first = readText("First user ID: ");
        String second = readText("Second user ID: ");
        ArrayList<User> mutuals = network.commonFriends(first, second);
        if (mutuals.isEmpty()) {
            System.out.println("No mutual friends found.");
            return;
        }
        System.out.println("--- Mutual Friends ---");
        printUsers(mutuals);
    }

    /**
     * 管理员删除用户控制台交互方法
     * 仅管理员账号可进入，输入目标用户ID，调用业务层执行删除并打印操作结果
     * @param admin 当前执行操作的管理员用户对象
     */
    private void deleteUser(User admin) {
        String targetId = readText("Target user ID to delete: ");
        System.out.println(network.deleteUser(admin, targetId));
    }

    /**
     * 函数式接口：用于接收用户选中的字符串选项，执行对应更新逻辑
     */
    private interface SelectionHandler {
        void accept(String value);
    }

    /**
     * 通用列表选择工具方法
     * 打印带编号的选项列表，接收用户数字选择，合法则回调处理器传入选中文本，非法则提示
     * @param label 选项分类标题（如Gender、Relationship）
     * @param options 可选文本集合
     * @param handler 回调处理器，接收选中字符串执行业务赋值
     */
    private void chooseFromArrayList(String label, ArrayList<String> options, SelectionHandler handler) {
        System.out.println(label + " options stored in ArrayList:");
        printNumbered(options);
        int choice = readInt("Choose: ");
        if (choice > 0 && choice <= options.size()) {
            handler.accept(options.get(choice - 1));
        } else {
            System.out.println("Invalid option.");
        }
    }

    /**
     * 批量打印用户列表工具方法
     * 循环遍历用户集合，按统一格式输出序号、姓名、用户名、用户ID、邮箱
     * @param users 需要打印的用户集合
     */
    private void printUsers(ArrayList<User> users) {
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            System.out.println((i + 1) + ". " + user.getName() + " | " + user.getUsername()
                    + " | " + user.getId() + " | " + user.getEmail());
        }
    }

    /**
     * 通用带序号打印字符串列表工具
     * 遍历字符串集合，从1开始输出序号+对应文本
     * @param values 待打印的字符串列表
     */
    private void printNumbered(List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            System.out.println((i + 1) + ". " + values.get(i));
        }
    }

    /**
     * 控制台读取输入文本工具方法
     * 打印提示文字，读取一行输入并去除首尾空格后返回
     * @param prompt 输入提示语
     * @return 去除首尾空格后的输入字符串
     */
    private String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * 控制台读取整数输入工具方法
     * 循环读取输入，若输入无法转为数字则持续提示重新输入，直到获取合法整数
     * @param prompt 输入提示文字
     * @return 用户输入的合法整数
     */
    private int readInt(String prompt) {
        while (true) {
            String text = readText(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    /**
     * 获取数字英文序数后缀工具方法
     * 根据数字返回 st / nd / rd / th，用于人脉度数展示（1st、2nd、3rd、4th...）
     * @param value 待处理数字
     * @return 对应序数后缀字符串
     */
    private String ordinalSuffix(int value) {
        if (value == 1) {
            return "st";
        }
        if (value == 2) {
            return "nd";
        }
        if (value == 3) {
            return "rd";
        }
        return "th";
    }
}
