package com.smartxplorer.bestsystemlottery.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static Map<String, String> extractTicketDate(String text) {
        Map<String, String> result = new HashMap<>();

        Pattern pattern = Pattern.compile("T\\.NO\\.\\s*:\\s*(\\d+)\\s*Date\\s*:\\s*(\\d{2}-\\d{2}-\\d{4})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            result.put("ticket", matcher.group(1));
            result.put("date", matcher.group(2));
        }

        return result;
    }

    public static Map<String, String> regexDataScan(String text) {
        Map<String, String> result = new HashMap<>();

        Pattern pattern = Pattern.compile("(\\d{8})(\\d{2})(\\d{2})(\\d{4})");

        Matcher matcher = pattern.matcher(text);

        if (matcher.matches()) {
            result.put("ticket", matcher.group(1));
            result.put("date", matcher.group(2) + "/" + matcher.group(3) + "/" + matcher.group(4));
        }

        return result;
    }
}
