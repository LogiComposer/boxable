package be.quodlibet.boxable;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class to verify thread safety of the document-level font caching functionality.
 */
public class ThreadSafetyTest {

    /**
     * Test that concurrent access to font loading with the same document is thread-safe.
     */
    @Test
    public void testConcurrentFontLoadingThreadSafety() throws InterruptedException, IOException {
        PDDocument document = new PDDocument();
        String fontPath = "fonts/FreeSans.ttf";
        int numThreads = 10;
        int numLoadsPerThread = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < numLoadsPerThread; j++) {
                        PDType0Font font = FontUtils.loadFont(document, fontPath);
                        assertNotNull("Font should not be null", font);
                        // Small delay to increase chance of race conditions
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue("All threads should complete within 30 seconds", latch.await(30, TimeUnit.SECONDS));
        
        if (errorRef.get() != null) {
            throw new RuntimeException("Thread safety test failed", errorRef.get());
        }
        
        executor.shutdown();
        document.close();
    }

    /**
     * Test that concurrent access with different documents works correctly.
     */
    @Test
    public void testConcurrentMultipleDocuments() throws InterruptedException, IOException {
        String fontPath = "fonts/FreeSans.ttf";
        int numThreads = 5;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                PDDocument document = null;
                try {
                    document = new PDDocument();
                    for (int j = 0; j < 10; j++) {
                        PDType0Font font = FontUtils.loadFont(document, fontPath);
                        assertNotNull("Font should not be null", font);
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    if (document != null) {
                        try {
                            document.close();
                        } catch (IOException e) {
                            // Log but don't fail the test
                            System.err.println("Error closing document: " + e.getMessage());
                        }
                    }
                    latch.countDown();
                }
            });
        }
        
        assertTrue("All threads should complete within 30 seconds", latch.await(30, TimeUnit.SECONDS));
        
        if (errorRef.get() != null) {
            throw new RuntimeException("Multi-document thread safety test failed", errorRef.get());
        }
        
        executor.shutdown();
    }
}