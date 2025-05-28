package com.JarHubApp.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TextAreaOutputStreamTest {

    private JTextArea mockTextArea;
    private TextAreaOutputStream textAreaOutputStream;

    @BeforeEach
    void setUp() {
        mockTextArea = mock(JTextArea.class);
        // Mock the document for setCaretPosition if you test auto-scrolling
        JTextPane dummyPaneForDoc = new JTextPane(); // JTextArea doc is PlainDocument
        when(mockTextArea.getDocument()).thenReturn(dummyPaneForDoc.getDocument());

        textAreaOutputStream = new TextAreaOutputStream(mockTextArea);
    }

    @Test
    void testWriteSingleByteThenNewlineFlushes() throws IOException, InterruptedException {
        try (MockedStatic<SwingUtilities> mockedSwingUtilities = Mockito.mockStatic(SwingUtilities.class)) {
            // Capture the Runnable passed to invokeLater
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            // When SwingUtilities.invokeLater is called, capture the argument and run it immediately for test
            mockedSwingUtilities.when(() -> SwingUtilities.invokeLater(runnableCaptor.capture()))
                .thenAnswer(invocation -> {
                    runnableCaptor.getValue().run();
                    return null;
                });

            textAreaOutputStream.write('H');
            textAreaOutputStream.write('i');
            textAreaOutputStream.write('\n'); // This should trigger a flush

            // Verify that mockTextArea.append() was called with "Hi\n"
            verify(mockTextArea, times(1)).append("Hi\n");
        }
    }

    @Test
    void testWriteByteArrayWithNewlineFlushes() throws IOException, InterruptedException {
         try (MockedStatic<SwingUtilities> mockedSwingUtilities = Mockito.mockStatic(SwingUtilities.class)) {
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            mockedSwingUtilities.when(() -> SwingUtilities.invokeLater(runnableCaptor.capture()))
                .thenAnswer(invocation -> {
                    runnableCaptor.getValue().run();
                    return null;
                });

            byte[] data = "Hello\nWorld".getBytes(StandardCharsets.UTF_8);
            textAreaOutputStream.write(data, 0, data.length);

            // It will flush at "Hello\n" and then buffer "World" until next flush/close
            verify(mockTextArea, times(1)).append("Hello\n");

            textAreaOutputStream.close(); // This should flush "World"
            verify(mockTextArea, times(1)).append("World"); //append "World" (the second part)
        }
    }
    
    @Test
    void testWriteMaxBufferFlushes() throws IOException {
        try (MockedStatic<SwingUtilities> mockedSwingUtilities = Mockito.mockStatic(SwingUtilities.class)) {
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            mockedSwingUtilities.when(() -> SwingUtilities.invokeLater(runnableCaptor.capture()))
                .thenAnswer(invocation -> {
                    runnableCaptor.getValue().run();
                    return null;
                });

            StringBuilder expectedString = new StringBuilder();
            for(int i=0; i < 200; i++) { // MAX_BUFFER_BEFORE_FLUSH = 200
                textAreaOutputStream.write('A');
                expectedString.append('A');
            }
            // The 200th 'A' should trigger flush
            verify(mockTextArea, times(1)).append(expectedString.toString());

            textAreaOutputStream.write('B'); // This is buffered
            verify(mockTextArea, times(1)).append(expectedString.toString()); // Still 1 call for append

            textAreaOutputStream.close(); // Flushes 'B'
            verify(mockTextArea, times(1)).append("B");
        }
    }


    @Test
    void testCloseFlushesRemainingBuffer() throws IOException {
        try (MockedStatic<SwingUtilities> mockedSwingUtilities = Mockito.mockStatic(SwingUtilities.class)) {
            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            mockedSwingUtilities.when(() -> SwingUtilities.invokeLater(runnableCaptor.capture()))
                .thenAnswer(invocation -> {
                    runnableCaptor.getValue().run();
                    return null;
                });

            textAreaOutputStream.write('X');
            textAreaOutputStream.write('Y');
            // No flush yet

            verify(mockTextArea, never()).append(anyString());

            textAreaOutputStream.close(); // Should flush "XY"
            verify(mockTextArea, times(1)).append("XY");
        }
    }
}