package com.core.helper;

public class Base64 {
    public static String encode(String content) {
        return java.util.Base64.getEncoder().encodeToString(content.getBytes());
    }

    public static String decode(String content) {
        return new String(java.util.Base64.getDecoder().decode(content));
    }
}