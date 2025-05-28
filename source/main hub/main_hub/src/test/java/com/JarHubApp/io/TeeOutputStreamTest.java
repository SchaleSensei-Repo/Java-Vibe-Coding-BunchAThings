package com.JarHubApp.io;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TeeOutputStreamTest {

    @Test
    void testWriteSingleByte() throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        TeeOutputStream tee = new TeeOutputStream(out1, out2);

        tee.write('A');
        tee.flush(); // Important for TeeOutputStream to propagate flush

        assertEquals("A", out1.toString(StandardCharsets.UTF_8));
        assertEquals("A", out2.toString(StandardCharsets.UTF_8));

        tee.close(); // Should also flush
    }

    @Test
    void testWriteByteArray() throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        TeeOutputStream tee = new TeeOutputStream(out1, out2);

        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        tee.write(data);
        tee.flush();

        assertArrayEquals(data, out1.toByteArray());
        assertArrayEquals(data, out2.toByteArray());
        tee.close();
    }

    @Test
    void testWriteByteArrayOffset() throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        TeeOutputStream tee = new TeeOutputStream(out1);

        byte[] data = "Test Data".getBytes(StandardCharsets.UTF_8);
        tee.write(data, 5, 4); // "Data"
        tee.flush();

        assertEquals("Data", out1.toString(StandardCharsets.UTF_8));
        tee.close();
    }

    @Test
    void testAddAndRemoveStream() throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        ByteArrayOutputStream out3 = new ByteArrayOutputStream();

        TeeOutputStream tee = new TeeOutputStream(out1);
        tee.write('X');
        tee.addStream(out2);
        tee.write('Y');
        tee.removeStream(out1);
        tee.addStream(out3);
        tee.write('Z');
        tee.flush();

        assertEquals("XY", out1.toString(StandardCharsets.UTF_8), "out1 should have XY");
        assertEquals("YZ", out2.toString(StandardCharsets.UTF_8), "out2 should have YZ");
        assertEquals("Z", out3.toString(StandardCharsets.UTF_8), "out3 should have Z");
        tee.close();
    }

    @Test
    void testFlushPropagates() throws IOException {
        // Mocking OutputStream to verify flush is called would be better,
        // but for simple cases, ByteArrayOutputStream works.
        // If underlying streams buffer, TeeOutputStream's flush should trigger their flush.
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); // This flushes on write to internal buffer mostly
        FlushCountingOutputStream fcos = new FlushCountingOutputStream(baos);
        TeeOutputStream tee = new TeeOutputStream(fcos);

        tee.write('A');
        assertEquals(0, fcos.getFlushCount(), "Flush should not be called just by Tee's write");
        
        tee.flush();
        assertEquals(1, fcos.getFlushCount(), "Flush should be propagated");
        
        tee.close(); // Close usually implies a flush
         // Depending on TeeOutputStream's close implementation, flush might be called again or not
        // Current TeeOutputStream close() calls flush() on each stream.
        assertTrue(fcos.getFlushCount() >= 1, "Flush should have been called at least once by close or earlier flush");
    }

    // Helper class for testing flush propagation
    private static class FlushCountingOutputStream extends ByteArrayOutputStream {
        private int flushCount = 0;
        private final OutputStream target; // Unused for this simple counter, but good practice

        public FlushCountingOutputStream(OutputStream target) {
            this.target = target;
        }
        
        @Override
        public synchronized void flush() throws IOException {
            super.flush(); // Call super.flush if ByteArrayOutputStream actually does something on flush
            flushCount++;
            // if (target != null) target.flush(); // If wrapping a real stream
        }

        public int getFlushCount() {
            return flushCount;
        }
    }
}