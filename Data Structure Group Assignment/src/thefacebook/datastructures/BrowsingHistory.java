package thefacebook.datastructures;
/**
 * 浏览历史记录类，基于双向链表存储用户会话操作记录
 * 支持访问新页面、后退、清空全部历史
 */
public class BrowsingHistory {
    /**
     * 双向链表内部节点静态内部类
     * 存储单条操作记录，同时保存前驱、后继节点指针
     */
    private static class Node {
        String action;
        Node previous;
        Node next;

        Node(String action) {
            this.action = action;
        }
    }

    private Node current;


    /**
     * 访问新操作/页面，新增一条浏览历史
     * @param action 新的操作名称
     */
    public void visit(String action) {
        Node node = new Node(action);
        if (current != null) {
            current.next = node;
            node.previous = current;
        }
        current = node;
    }

    /**
     * 执行后退操作，返回后退提示文本
     * @return 后退结果提示字符串
     */
    public String back() {
        if (current == null) {
            return "No browsing history in this login session.";
        }
        String leaving = current.action;
        current = current.previous;
        if (current == null) {
            return "Back from: " + leaving + "\nNow at login menu.";
        }
        return "Back from: " + leaving + "\nNow at: " + current.action;
    }

    /**
     * 清空本次登录全部浏览历史
     * 直接置空游标，链表失去引用被GC回收
     */
    public void clear() {
        current = null;
    }
}
