/*
 * Copyright (C) 2015-2024 The Termux Authors.
 * Licensed under the Apache License, Version 2.0.
 * Modified for Herdr App remote streams.
 */
package com.termux.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TerminalView extends View implements TerminalUpdateListener {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RemoteTerminalSession session;
    private float fontSizeSp = 14f;
    private boolean inputEnabled;

    public TerminalView(Context context) { super(context); init(); }
    public TerminalView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(Color.rgb(12, 12, 15));
        paint.setColor(Color.rgb(232, 232, 232));
        paint.setTypeface(Typeface.MONOSPACE);
        setFontSize(fontSizeSp);
    }

    public void setRemoteSession(RemoteTerminalSession session) {
        if (this.session != null) this.session.setUpdateListener(null);
        this.session = session;
        if (session != null) session.setUpdateListener(this);
        invalidate();
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
        if (enabled) requestFocus();
    }

    public void setFontSize(float sp) {
        fontSizeSp = Math.max(8f, Math.min(28f, sp));
        paint.setTextSize(fontSizeSp * getResources().getDisplayMetrics().scaledDensity);
        invalidate();
    }

    public int estimateColumns() {
        float w = Math.max(1f, paint.measureText("M"));
        return Math.max(20, (int)(getWidth() / w));
    }

    public int estimateRows() {
        Paint.FontMetrics fm = paint.getFontMetrics();
        float h = Math.max(1f, fm.descent - fm.ascent);
        return Math.max(8, (int)(getHeight() / h));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (session == null) return;
        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineHeight = fm.descent - fm.ascent;
        float y = -fm.ascent + 4;
        List<String> lines = session.getEmulator().getVisibleLines();
        for (String line : lines) {
            canvas.drawText(line, 8, y, paint);
            y += lineHeight;
            if (y > getHeight() + lineHeight) break;
        }
    }

    @Override public void onTerminalChanged() { invalidate(); }

    @Override public boolean onCheckIsTextEditor() { return inputEnabled; }

    @Override public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        return new BaseInputConnection(this, true) {
            @Override public boolean commitText(CharSequence text, int newCursorPosition) {
                writeText(text.toString());
                return true;
            }
            @Override public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                writeBytes(new byte[]{0x7f});
                return true;
            }
            @Override public boolean sendKeyEvent(KeyEvent event) {
                return TerminalView.this.dispatchKeyEvent(event);
            }
        };
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!inputEnabled || session == null) return super.onKeyDown(keyCode, event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER: writeText("\r"); return true;
            case KeyEvent.KEYCODE_DEL: writeBytes(new byte[]{0x7f}); return true;
            case KeyEvent.KEYCODE_ESCAPE: writeBytes(new byte[]{0x1b}); return true;
            case KeyEvent.KEYCODE_TAB: writeText("\t"); return true;
            case KeyEvent.KEYCODE_DPAD_UP: writeText("\u001B[A"); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: writeText("\u001B[B"); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: writeText("\u001B[C"); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: writeText("\u001B[D"); return true;
            default:
                int unicode = event.getUnicodeChar();
                if (unicode != 0) { writeText(Character.toString((char) unicode)); return true; }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void writeText(String text) { writeBytes(text.getBytes(StandardCharsets.UTF_8)); }
    private void writeBytes(byte[] bytes) { if (session != null) session.write(bytes); }
}
