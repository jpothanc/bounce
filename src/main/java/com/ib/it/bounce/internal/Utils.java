package com.ib.it.bounce.internal;

import java.time.LocalDate;

public class Utils {
    public static boolean sendEmail(String to, String subject, String body) {
        // Send email
        return true;
    }

    public static  String createCacheKey(String key) {
        String cacheKey = key + LocalDate.now().toString().replace("-", "");
        return cacheKey;
    }
}
