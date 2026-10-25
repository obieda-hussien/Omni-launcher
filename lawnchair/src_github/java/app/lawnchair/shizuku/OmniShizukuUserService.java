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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shizuku-backed, read-only performance diagnostics for launcher-owned animation paths.
 *
 * The Binder API deliberately exposes no arbitrary command or settings value. It can inspect
 * global animation values and capture/reset this launcher's gfxinfo counters, but never changes
 * animation duration, display refresh settings, CPU/GPU policy, or thermal settings.
 */
public final class OmniShizukuUserService extends IOmniShizukuService.Stub {
    private static final String PACKAGE_NAME = "app.lawnchair";
    private static final String[] GLOBAL_ANIMATION_KEYS = {
        "window_animation_scale",
        "transition_animation_scale",
        "animator_duration_scale"
    };
    private static final long COMMAND_TIMEOUT_SECONDS = 4L;
    private static final int MAX_CAPTURE_CHARS = 24000;

    public OmniShizukuUserService() {
        // Required: Shizuku constructs this service reflectively.
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public String getStatus() {
        StringBuilder status = new StringBuilder("Omni performance diagnostics (read-only)\n");
        appendCommandValue(status, "Android release", "getprop", "ro.build.version.release");
        appendCommandValue(status, "Android SDK", "getprop", "ro.build.version.sdk");
        appendSetting(status, "Peak refresh rate", "system", "peak_refresh_rate");
        appendSetting(status, "Minimum refresh rate", "system", "min_refresh_rate");
        for (String key : GLOBAL_ANIMATION_KEYS) {
            appendSetting(status, key, "global", key);
        }
        status.append("\nNo animation or display settings are changed by this screen.");
        status.append("\nUse Capture frame stats after reproducing an app-open / return-home hitch.");
        return status.toString();
    }

    @Override
    public String getFrameStats() {
        CommandResult result = runFixedCommand(
                "dumpsys", "gfxinfo", PACKAGE_NAME, "framestats");
        if (result.exitCode != 0) {
            return "Could not capture gfxinfo framestats (exit "
                    + result.exitCode + ").\n" + result.output;
        }
        if (result.output.isEmpty()) {
            return "gfxinfo returned no frame statistics. Open Omni as the current Home app, "
                    + "reproduce the transition, then capture again.";
        }
        return "gfxinfo framestats for " + PACKAGE_NAME + "\n"
                + result.output
                + (result.truncated ? "\n[Output truncated to protect memory.]" : "");
    }

    /**
     * Clears only Android's per-process gfxinfo counters. It does not alter animation settings.
     */
    @Override
    public boolean resetFrameStats() {
        CommandResult result = runFixedCommand(
                "dumpsys", "gfxinfo", PACKAGE_NAME, "reset");
        return result.exitCode == 0;
    }

    private static void appendCommandValue(
            StringBuilder output, String label, String... command) {
        CommandResult result = runFixedCommand(command);
        appendValue(output, label, result);
    }

    private static void appendSetting(
            StringBuilder output, String label, String namespace, String key) {
        // namespace/key values originate only from this class's fixed allowlist / fixed call sites.
        CommandResult result = runFixedCommand("settings", "get", namespace, key);
        appendValue(output, label, result);
    }

    private static void appendValue(
            StringBuilder output, String label, CommandResult result) {
        output.append(label).append(": ");
        if (result.exitCode != 0) {
            output.append("unavailable");
            if (!result.output.isEmpty()) {
                output.append(" (").append(result.output).append(')');
            }
        } else if (result.output.isEmpty() || "null".equals(result.output)) {
            output.append("unset / device default");
        } else {
            output.append(result.output);
        }
        output.append('\n');
    }

    /**
     * Runs an internally defined command only. No command string is accepted from the caller.
     * Output is drained concurrently so a larger gfxinfo response cannot fill the process pipe.
     */
    private static CommandResult runFixedCommand(String... command) {
        StringBuilder output = new StringBuilder();
        AtomicBoolean truncated = new AtomicBoolean(false);
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            final Process running = process;
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        running.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (output) {
                            if (output.length() >= MAX_CAPTURE_CHARS) {
                                truncated.set(true);
                                continue;
                            }
                            if (output.length() > 0) {
                                output.append('\n');
                            }
                            int remaining = MAX_CAPTURE_CHARS - output.length();
                            if (line.length() > remaining) {
                                output.append(line, 0, remaining);
                                truncated.set(true);
                            } else {
                                output.append(line);
                            }
                        }
                    }
                } catch (IOException ignored) {
                    // The command may close its stream while being force-stopped on timeout.
                }
            }, "OmniShizukuDiagnosticsOutput");
            outputReader.setDaemon(true);
            outputReader.start();

            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                outputReader.join(500L);
                return new CommandResult(-1, "command timed out", false);
            }
            outputReader.join(1000L);

            String captured;
            synchronized (output) {
                captured = output.toString().trim();
            }
            if (truncated.get()) {
                captured += "\n[Output truncated]";
            }
            return new CommandResult(process.exitValue(), captured, truncated.get());
        } catch (IOException error) {
            if (process != null) {
                process.destroyForcibly();
            }
            return new CommandResult(-1, "diagnostic command unavailable", false);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return new CommandResult(-1, "diagnostic command interrupted", false);
        }
    }

    private static final class CommandResult {
        final int exitCode;
        final String output;
        final boolean truncated;

        CommandResult(int exitCode, String output, boolean truncated) {
            this.exitCode = exitCode;
            this.output = output;
            this.truncated = truncated;
        }
    }
}
