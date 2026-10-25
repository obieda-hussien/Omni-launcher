/*
 * Copyright (C) 2026 Omni Launcher contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package app.lawnchair.shizuku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Small Shizuku UserService exposing only fixed, bounded animation-scale presets.
 *
 * No arbitrary shell command, setting name, or setting value crosses the Binder boundary.
 * This service runs with the identity granted by Shizuku (normally shell on non-root setups).
 */
public final class OmniShizukuUserService extends IOmniShizukuService.Stub {
    private static final String[] ANIMATION_KEYS = {
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale"
    };

    private static final String DEFAULT_SCALE = "1.0";
    private static final String BALANCED_SCALE = "0.75";
    private static final String SNAPPY_SCALE = "0.5";
    private static final long COMMAND_TIMEOUT_SECONDS = 2L;

    public OmniShizukuUserService() {
        // Required: Shizuku constructs the service reflectively.
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public String getStatus() {
        StringBuilder status = new StringBuilder("Global Android animation scales\n");
        for (String key : ANIMATION_KEYS) {
            status.append(key).append(" = ").append(readSetting(key)).append('\n');
        }
        status.append("\nChanges affect system-wide animations, not CPU/GPU performance.");
        return status.toString();
    }

    @Override
    public boolean applyAnimationPreset(int preset) {
        final String value;
        switch (preset) {
            case 0:
                value = DEFAULT_SCALE;
                break;
            case 1:
                value = BALANCED_SCALE;
                break;
            case 2:
                value = SNAPPY_SCALE;
                break;
            default:
                return false;
        }

        // Snapshot current values first. If any write fails, best-effort rollback the earlier ones.
        String[] previous = new String[ANIMATION_KEYS.length];
        for (int i = 0; i < ANIMATION_KEYS.length; i++) {
            previous[i] = readSetting(ANIMATION_KEYS[i]);
            if ("unavailable".equals(previous[i])) {
                return false;
            }
        }

        int completed = 0;
        for (String key : ANIMATION_KEYS) {
            if (!writeSetting(key, value)) {
                for (int i = completed - 1; i >= 0; i--) {
                    String oldValue = previous[i];
                    restoreSetting(ANIMATION_KEYS[i],
                        "null".equals(oldValue) || oldValue.isEmpty() ? DEFAULT_SCALE : oldValue);
                }
                return false;
            }
            completed++;
        }
        return true;
    }

    private static String readSetting(String key) {
        if (!isAllowedKey(key)) {
            return "unavailable";
        }
        CommandResult result = runSettings("get", "global", key);
        if (result.exitCode != 0 || result.output == null || result.output.isEmpty()) {
            return "unavailable";
        }
        return "null".equals(result.output) ? DEFAULT_SCALE : result.output;
    }

    private static boolean restoreSetting(String key, String previousValue) {
        if (!isAllowedKey(key) || !isValidScale(previousValue)) {
            return false;
        }
        return runSettings("put", "global", key, previousValue).exitCode == 0;
    }

    private static boolean isValidScale(String value) {
        try {
            float scale = Float.parseFloat(value);
            return Float.isFinite(scale) && scale >= 0f && scale <= 10f;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private static boolean writeSetting(String key, String value) {
        if (!isAllowedKey(key)
                || !(DEFAULT_SCALE.equals(value)
                    || BALANCED_SCALE.equals(value)
                    || SNAPPY_SCALE.equals(value))) {
            return false;
        }
        return runSettings("put", "global", key, value).exitCode == 0;
    }

    private static boolean isAllowedKey(String key) {
        return Arrays.asList(ANIMATION_KEYS).contains(key);
    }

    private static CommandResult runSettings(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("settings");
        command.addAll(Arrays.asList(arguments));
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new CommandResult(-1, "timed out");
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }
            return new CommandResult(process.exitValue(), output.toString().trim());
        } catch (IOException error) {
            if (process != null) process.destroyForcibly();
            return new CommandResult(-1, "unavailable");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            return new CommandResult(-1, "interrupted");
        }
    }

    private static final class CommandResult {
        final int exitCode;
        final String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
