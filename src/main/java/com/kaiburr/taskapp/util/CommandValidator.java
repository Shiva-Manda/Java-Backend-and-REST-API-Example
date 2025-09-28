package com.kaiburr.taskapp.util;

import java.util.Arrays;
import java.util.List;

public class CommandValidator {

    private static final List<String> BLACKLIST = Arrays.asList(
            "rm", "shutdown", "reboot", "mkfs", "dd", "passwd",
            ":(){:|:&};:", "kill", "pkill", "halt", "init 0",
            "curl", "wget", "scp", "ftp"
    );

    public static boolean isSafe(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        String lower = command.toLowerCase();
        return BLACKLIST.stream().noneMatch(lower::contains);
    }
}