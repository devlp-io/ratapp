package com.samsung.android.usersuite;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShellExecutor {

    public static List<String> run(String cmd) {
        List<String> out = new ArrayList<>();
        if (cmd == null || cmd.isEmpty()) {
            out.add("empty command");
            return out;
        }
        try {
            Process p = Runtime.getRuntime().exec("sh");
            p.getOutputStream().write((cmd + "\n").getBytes());
            p.getOutputStream().write("exit\n".getBytes());
            p.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            long deadline = System.currentTimeMillis() + 4000;
            String line;
            while ((line = reader.readLine()) != null) {
                out.add(line);
                if (out.size() > 500 || System.currentTimeMillis() > deadline) break;
            }
            BufferedReader err = new BufferedReader(
                new InputStreamReader(p.getErrorStream()));
            while ((line = err.readLine()) != null && out.size() < 500) out.add("E| " + line);

            p.waitFor(4, TimeUnit.SECONDS);
            p.destroy();
        } catch (Exception e) {
            out.add("ERR: " + e.getMessage());
        }
        return out;
    }
}
