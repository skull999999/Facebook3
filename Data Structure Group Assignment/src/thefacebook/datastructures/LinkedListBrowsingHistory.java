package thefacebook.datastructures;

import java.util.LinkedList;

public class LinkedListBrowsingHistory {
    private final LinkedList<String> records = new LinkedList<>();
    private int currentIndex = -1;

    /**
     * 访问新页面
     * Visit a new page content
     * @param content 页面名称/链接 page name or url
     */
    public void visit(String content) {
        while (records.size() > currentIndex + 1) {
            records.removeLast();
        }
        records.add(content);
        currentIndex = records.size() - 1;
    }

    /**
     * 格式化输出全部浏览历史，当前页面用->标记
     * Format and return full browsing history, mark current page with ->
     * @return 拼接好的历史字符串 formatted history text
     */
    public String showHistory() {
        if (records.isEmpty()) {
            return "No browsing history in this login session.";
        }
        StringBuilder text = new StringBuilder("Browsing history:\n");
        for (int i = 0; i < records.size(); i++) {
            if (i == currentIndex) {
                text.append("-> ");
            } else {
                text.append("   ");
            }
            text.append(i + 1).append(". ").append(records.get(i)).append("\n");
        }
        return text.toString();
    }

    /**
     * 后退上一页
     * Navigate back to previous page
     * @return 后退结果提示文本 back operation result message
     */
    public String back() {
        if (records.isEmpty()) {
            return "No browsing history in this login session.";
        }
        if (currentIndex <= 0) {
            return "Already at the first record: " + records.get(currentIndex);
        }
        currentIndex--;
        return "Back to: " + records.get(currentIndex);
    }

    /**
     * 前进下一页
     * Navigate forward to next page
     * @return 前进结果提示文本 forward operation result message
     */
    public String forward() {
        if (records.isEmpty()) {
            return "No browsing history in this login session.";
        }
        if (currentIndex >= records.size() - 1) {
            return "Already at the latest record: " + records.get(currentIndex);
        }
        currentIndex++;
        return "Forward to: " + records.get(currentIndex);
    }

    public void clear() {
        records.clear();
        currentIndex = -1;
    }
}
