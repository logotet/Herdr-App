/*
 * Copyright (C) 2015-2024 The Termux Authors.
 * Licensed under the Apache License, Version 2.0.
 * Modified for Herdr App remote streams: no local process/JNI is spawned.
 */
package com.termux.view;

import android.os.Handler;
import android.os.Looper;
import com.termux.terminal.TerminalEmulator;
import java.nio.charset.StandardCharsets;

public final class RemoteTerminalSession {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final TerminalEmulator emulator;
    private final TerminalWriteListener writeListener;
    private TerminalUpdateListener updateListener;

    public RemoteTerminalSession(int cols, int rows, TerminalWriteListener writeListener) {
        this.emulator = new TerminalEmulator(cols, rows);
        this.writeListener = writeListener;
    }

    public TerminalEmulator getEmulator() { return emulator; }

    public void setUpdateListener(TerminalUpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    public void appendRemote(byte[] bytes) {
        mainHandler.post(() -> {
            emulator.append(bytes);
            if (updateListener != null) updateListener.onTerminalChanged();
        });
    }

    public void resize(int cols, int rows) {
        emulator.resize(cols, rows);
        if (updateListener != null) updateListener.onTerminalChanged();
    }

    public void write(byte[] bytes) {
        if (writeListener != null) writeListener.onWrite(bytes);
    }

    public void write(String text) {
        write(text.getBytes(StandardCharsets.UTF_8));
    }
}
