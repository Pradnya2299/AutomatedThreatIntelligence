package com.northwind;

import java.security.MessageDigest;

public class InsecureHash {
    public static byte[] digest(byte[] input) throws Exception {
        return MessageDigest.getInstance("MD5").digest(input);
    }
}
