package com.JarHubApp.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeeOutputStream extends OutputStream {
    private final List<OutputStream> streams;

    public TeeOutputStream(OutputStream... streams) {
        this.streams = new ArrayList<>(Arrays.asList(streams));
    }
    
    public TeeOutputStream(List<OutputStream> streams) {
        this.streams = new ArrayList<>(streams);
    }

    @Override
    public void write(int b) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (OutputStream stream : streams) {
            stream.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream stream : streams) {
            stream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        for (OutputStream stream : streams) {
            // We generally don't want the TeeOutputStream to close underlying streams
            // especially System.out/err. Let the owner of the stream manage its lifecycle.
            // However, for file streams specifically added, they should be closed.
            // This simple Tee assumes streams are managed externally or closed together.
            stream.flush(); // Ensure data is written
        }
        // If this TeeOutputStream specifically "owns" some streams, close them here.
        // For this use case, the streams (like FileOutputStream) are managed elsewhere.
    }

    public void addStream(OutputStream stream) {
        this.streams.add(stream);
    }

    public void removeStream(OutputStream stream) {
        this.streams.remove(stream);
    }
}