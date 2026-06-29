package thefacebook.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 哈希加密工具静态方法
 * 将输入字符串转为 UTF-8 字节，计算 SHA256 哈希，输出小写16进制字符串
 * @param value 原始明文字符串
 * @return 64位小写SHA256哈希字符串
 */
public class PasswordUtil {
    private PasswordUtil() {
    }


    public static String sha256(String value) {
        try {
            // 获取SHA-256消息摘要实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 明文转UTF-8字节数组并计算哈希摘要字节数组
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            // 遍历每个字节，格式化为两位小写十六进制，不足补0
            for (byte b : encoded) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            // JVM标准环境一定存在SHA-256，异常视为运行时致命错误
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
