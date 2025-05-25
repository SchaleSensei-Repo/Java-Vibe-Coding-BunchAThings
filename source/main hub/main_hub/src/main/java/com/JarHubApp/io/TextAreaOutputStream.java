package com.JarHubApp.io;

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class TextAreaOutputStream extends OutputStream {
    private final JTextArea textArea;
    private final StringBuilder sb = new StringBuilder();
    private final int MAX_BUFFER_BEFORE_FLUSH = 200; // Characters or lines

    public TextAreaOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void flush() {
        if (sb.length() > 0) {
            final String text = sb.toString();
            sb.setLength(0); // Clear buffer
            SwingUtilities.invokeLater(() -> {
                textArea.append(text);
                // textArea.setCaretPosition(textArea.getDocument().getLength()); // Auto-scroll
            });
        }
    }

    @Override
    public void write(int b) throws IOException {
        sb.append((char) b);
        if (b == '\n' || sb.length() >= MAX_BUFFER_BEFORE_FLUSH) {
            flush();
        }
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        sb.append(new String(b, off, len));
        if (sb.indexOf("\n") != -1 || sb.length() >= MAX_BUFFER_BEFORE_FLUSH) {
             flush();
        }
    }


    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }
}