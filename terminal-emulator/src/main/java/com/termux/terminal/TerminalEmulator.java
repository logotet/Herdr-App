/*
 * Copyright (C) 2015-2024 The Termux Authors.
 * Licensed under the Apache License, Version 2.0.
 *
 * Modified for Herdr App: this lightweight vendored emulator accepts remote
 * bytes instead of attaching to a local Termux JNI subprocess.
 */
package com.termux.terminal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class TerminalEmulator {
    private static final Pattern CSI = Pattern.compile("\\u001B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern OSC = Pattern.compile("\\u001B\\].*?(\\u0007|\\u001B\\\\)");
    private final StringBuilder currentLine = new StringBuilder();
    private final ArrayList<String> lines = new ArrayList<>();
    private int columns;
    private int rows;

    public TerminalEmulator(int columns, int rows) {
        this.columns = Math.max(2, columns);
        this.rows = Math.max(2, rows);
    }

    public synchronized void resize(int columns, int rows) {
        this.columns = Math.max(2, columns);
        this.rows = Math.max(2, rows);
        trim();
    }

    public synchronized int getColumns() { return columns; }
    public synchronized int getRows() { return rows; }

    public synchronized void append(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        text = OSC.matcher(text).replaceAll("");
        if (text.contains("\u001B[2J")) clear();
        text = CSI.matcher(text).replaceAll("");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\r') {
                currentLine.setLength(0);
            } else if (ch == '\n') {
                pushLine();
            } else if (ch == '\b') {
                if (currentLine.length() > 0) currentLine.deleteCharAt(currentLine.length() - 1);
            } else if (ch >= 0x20 || ch == '\t') {
                currentLine.append(ch == '\t' ? "    " : Character.toString(ch));
                if (currentLine.length() >= columns) pushLine();
            }
        }
        trim();
    }

    public synchronized void clear() {
        lines.clear();
        currentLine.setLength(0);
    }

    private void pushLine() {
        lines.add(currentLine.toString());
        currentLine.setLength(0);
    }

    private void trim() {
        int keep = Math.max(rows * 6, rows + 100);
        while (lines.size() > keep) lines.remove(0);
    }

    public synchronized List<String> getVisibleLines() {
        ArrayList<String> out = new ArrayList<>();
        int start = Math.max(0, lines.size() - Math.max(0, rows - 1));
        out.addAll(lines.subList(start, lines.size()));
        out.add(currentLine.toString());
        while (out.size() < rows) out.add("");
        return out;
    }
}
