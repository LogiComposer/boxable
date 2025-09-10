package be.quodlibet.boxable;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class to verify concurrency improvements and exception handling in FontUtils.
 */
public class FontUtilsConcurrencyTest {

    /**
     * Test that IllegalArgumentException is thrown when fontStyle is null.
     */
    @Test
    public void testNullFontStyleThrowsException() throws IOException {
        PDDocument document = new PDDocument();
        
        try {
            FontUtils.loadFont(document, SupportedFont.FREE_SANS, null);
            fail("Expected IllegalArgumentException when fontStyle is null");
        } catch (IllegalArgumentException e) {
            assertEquals("FontStyle cannot be null", e.getMessage());
        } finally {
            document.close();
        }
    }

    /**
     * Test concurrent font loading to verify thread safety.
     */
    @Test
    public void testConcurrentFontLoading() throws IOException, InterruptedException {
        final int threadCount = 10;
        final int loadsPerThread = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Create documents for each thread to avoid conflicts
        final PDDocument[] documents = new PDDocument[threadCount];
        for (int i = 0; i < threadCount; i++) {
            documents[i] = new PDDocument();
        }
        
        try {
            // Submit tasks
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();
                        
                        // Each thread loads the same font multiple times
                        for (int j = 0; j < loadsPerThread; j++) {
                            PDType0Font font = FontUtils.loadFont(documents[threadIndex], 
                                SupportedFont.FREE_SANS, FontStyle.REGULAR);
                            if (font != null) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for all threads to complete
            assertTrue("All threads should complete within 30 seconds", 
                endLatch.await(30, TimeUnit.SECONDS));
            
            executor.shutdown();
            assertTrue("Executor should shutdown within 5 seconds",
                executor.awaitTermination(5, TimeUnit.SECONDS));
            
            // Verify results
            assertEquals("No failures should occur during concurrent loading", 0, failureCount.get());
            assertEquals("All font loads should succeed", threadCount * loadsPerThread, successCount.get());
            
        } finally {
            // Clean up documents
            for (PDDocument doc : documents) {
                if (doc != null) {
                    doc.close();
                }
            }
        }
    }

    /**
     * Test concurrent loading of different fonts to verify cache isolation.
     */
    @Test
    public void testConcurrentDifferentFontLoading() throws IOException, InterruptedException {
        final int threadCount = 6;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        final PDDocument[] documents = new PDDocument[threadCount];
        final SupportedFont[] fonts = {
            SupportedFont.FREE_SANS, SupportedFont.FREE_SERIF, SupportedFont.FREE_MONO,
            SupportedFont.FREE_SANS, SupportedFont.FREE_SERIF, SupportedFont.FREE_MONO
        };
        final FontStyle[] styles = {
            FontStyle.REGULAR, FontStyle.BOLD, FontStyle.ITALIC,
            FontStyle.BOLD_ITALIC, FontStyle.REGULAR, FontStyle.BOLD
        };
        
        for (int i = 0; i < threadCount; i++) {
            documents[i] = new PDDocument();
        }
        
        try {
            // Submit tasks with different font combinations
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Load a specific font/style combination
                        PDType0Font font = FontUtils.loadFont(documents[threadIndex], 
                            fonts[threadIndex], styles[threadIndex]);
                        if (font != null) {
                            successCount.incrementAndGet();
                            
                            // Load the same font again to test caching
                            PDType0Font font2 = FontUtils.loadFont(documents[threadIndex], 
                                fonts[threadIndex], styles[threadIndex]);
                            if (font2 != null) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            
            assertTrue("All threads should complete within 30 seconds", 
                endLatch.await(30, TimeUnit.SECONDS));
            
            executor.shutdown();
            assertTrue("Executor should shutdown within 5 seconds",
                executor.awaitTermination(5, TimeUnit.SECONDS));
            
            assertEquals("No failures should occur", 0, failureCount.get());
            assertEquals("All font loads should succeed", threadCount * 2, successCount.get());
            
        } finally {
            for (PDDocument doc : documents) {
                if (doc != null) {
                    doc.close();
                }
            }
        }
    }

    /**
     * Test that font cache manager works correctly under concurrent access.
     */
    @Test
    public void testConcurrentCacheAccess() throws IOException, InterruptedException {
        final int threadCount = 8;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // Single document shared across threads to test document-level caching
        final PDDocument sharedDocument = new PDDocument();
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Multiple threads accessing the same document/font combination
                        PDType0Font font = FontUtils.loadFont(sharedDocument, 
                            SupportedFont.FREE_SANS, FontStyle.REGULAR);
                        if (font != null) {
                            successCount.incrementAndGet();
                        }
                        
                        // Test cache size methods
                        int cacheSize = FontUtils.getFontCacheSize(sharedDocument);
                        if (cacheSize >= 0) { // Should be non-negative
                            successCount.incrementAndGet();
                        }
                        
                        int docCacheSize = FontUtils.getDocumentCacheSize();
                        if (docCacheSize >= 0) { // Should be non-negative
                            successCount.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            
            assertTrue("All threads should complete within 30 seconds", 
                endLatch.await(30, TimeUnit.SECONDS));
            
            executor.shutdown();
            assertTrue("Executor should shutdown within 5 seconds",
                executor.awaitTermination(5, TimeUnit.SECONDS));
            
            // Should have successful operations from all threads
            assertTrue("Should have successful operations", successCount.get() > 0);
            
            // Verify the shared document has fonts cached
            assertTrue("Shared document should have cached fonts", 
                FontUtils.getFontCacheSize(sharedDocument) > 0);
            
        } finally {
            sharedDocument.close();
        }
    }
}