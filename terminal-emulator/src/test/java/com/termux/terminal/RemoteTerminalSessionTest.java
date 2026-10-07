package com.termux.terminal;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Herdr App: the remote-backed TerminalSession fed with herdr-style cursor-addressed frames. */
public class RemoteTerminalSessionTest extends TestCase {

    private final List<byte[]> written = new ArrayList<>();
    private final List<int[]> resizes = new ArrayList<>();

    private TerminalSession newSession() {
        return new TerminalSession(100, null, new TerminalSession.RemoteIO() {
            @Override
            public void onWrite(byte[] data) {
                written.add(data);
            }

            @Override
            public void onResize(int cols, int rows) {
                resizes.add(new int[]{cols, rows});
            }
        });
    }

    /** Builds a frame the way herdr does: sync start, clear, one SGR+char per cell, sync end. */
    private static byte[] herdrFrame(String[] lines, int cols, int rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("\033[?2026h\033[?25l\033]8;;\033\\\033[2J\033[1;1H");
        for (int r = 0; r < rows; r++) {
            String line = r < lines.length ? lines[r] : "";
            for (int c = 0; c < cols; c++) {
                char ch = c < line.length() ? line.charAt(c) : ' ';
                sb.append("\033[").append(r + 1).append(';').append(c + 1).append('H');
                sb.append("\033[0;39;49m").append(ch);
            }
        }
        sb.append("\033[0m\033[").append(rows).append(";1H\033[?25h\033[?2026l");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String row(TerminalSession session, int row) {
        TerminalEmulator emulator = session.getEmulator();
        return emulator.getScreen().getSelectedText(0, row, emulator.mColumns, row).trim();
    }

    public void testFrameCreatesEmulatorAtFrameSize() {
        TerminalSession session = newSession();
        session.appendRemote(herdrFrame(new String[]{"claude  done", "> fix the bug"}, 30, 6), 30, 6);

        assertEquals(30, session.getEmulator().mColumns);
        assertEquals(6, session.getEmulator().mRows);
        assertEquals("claude  done", row(session, 0));
        assertEquals("> fix the bug", row(session, 1));
    }

    public void testNextFrameRepaintsAndAdoptsNewSize() {
        TerminalSession session = newSession();
        session.appendRemote(herdrFrame(new String[]{"first"}, 20, 4), 20, 4);
        session.appendRemote(herdrFrame(new String[]{"second", "line two"}, 40, 8), 40, 8);

        assertEquals(40, session.getEmulator().mColumns);
        assertEquals(8, session.getEmulator().mRows);
        assertEquals("second", row(session, 0));
        assertEquals("line two", row(session, 1));
    }

    public void testUpdateSizeReportsRemoteResize() {
        TerminalSession session = newSession();
        session.updateSize(50, 20, 10, 20);
        session.updateSize(60, 25, 10, 20);

        assertEquals(2, resizes.size());
        assertEquals(60, resizes.get(1)[0]);
        assertEquals(25, resizes.get(1)[1]);
        assertEquals(60, session.getEmulator().mColumns);
    }

    public void testViewResizeKeepsFrameSizeButReportsIt() {
        TerminalSession session = newSession();
        session.appendRemote(herdrFrame(new String[]{"wide pc pane"}, 94, 39), 94, 39);
        session.updateSize(45, 30, 10, 20);

        // Frames are cursor-addressed at the PC size; shrinking the grid to the view would garble them.
        assertEquals(94, session.getEmulator().mColumns);
        assertEquals(39, session.getEmulator().mRows);
        assertEquals("wide pc pane", row(session, 0));
        assertEquals(1, resizes.size());
        assertEquals(45, resizes.get(0)[0]);
    }

    public void testInputDroppedUnlessEnabled() {
        TerminalSession session = newSession();
        session.updateSize(20, 5, 10, 20);

        session.write("a");
        assertTrue(written.isEmpty());

        session.setInputEnabled(true);
        session.write("b");
        session.writeCodePoint(false, 'c');
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (byte[] chunk : written) all.write(chunk, 0, chunk.length);
        assertEquals("bc", all.toString());
    }

    public void testEmulatorRepliesOnlySentWhenEnabled() {
        TerminalSession session = newSession();
        session.updateSize(20, 5, 10, 20);
        // Device status report (cursor position); the emulator answers through write().
        session.appendRemote("\033[6n".getBytes(StandardCharsets.UTF_8), 0, 0);
        assertTrue(written.isEmpty());

        session.setInputEnabled(true);
        session.appendRemote("\033[6n".getBytes(StandardCharsets.UTF_8), 0, 0);
        assertEquals(1, written.size());
        assertEquals("\033[1;1R", new String(written.get(0), StandardCharsets.UTF_8));
    }
}
