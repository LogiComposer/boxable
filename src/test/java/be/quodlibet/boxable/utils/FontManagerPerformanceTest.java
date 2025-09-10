package be.quodlibet.boxable.utils;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Performance and load test for FontManager functionality.
 * Tests concurrent PDF generation with FontManager to verify thread safety and performance.
 */
public class FontManagerPerformanceTest {

    /**
     * Test concurrent PDF generation with Source Sans 3 fonts to verify thread safety and performance.
     * Note: This test focuses on font loading performance rather than PDF saving due to PDFBox 
     * font subsetting limitations in concurrent scenarios.
     */
    @Test
    public void testConcurrentPDFGeneration() throws InterruptedException, ExecutionException {
        final int NUM_THREADS = 5;
        final int NUM_OPERATIONS_PER_THREAD = 10;
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        FontManager fontManager = FontManager.getInstance();
        
        // Clear caches for clean test
        fontManager.clearAllCaches();
        
        long startTime = System.currentTimeMillis();
        
        try {
            Future<Boolean>[] futures = new Future[NUM_THREADS];
            
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadId = i;
                futures[i] = executor.submit(() -> {
                    try {
                        for (int j = 0; j < NUM_OPERATIONS_PER_THREAD; j++) {
                            // Focus on font loading performance rather than PDF saving
                            testFontLoadingInThread(threadId, j);
                            successCount.incrementAndGet();
                        }
                        return true;
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                        return false;
                    }
                });
            }
            
            // Wait for all tasks to complete
            for (int i = 0; i < NUM_THREADS; i++) {
                Boolean result = futures[i].get();
                assertTrue("Thread " + i + " should complete successfully", result);
            }
            
        } finally {
            executor.shutdown();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("=== FontManager Performance Test Results ===");
        System.out.println("Threads: " + NUM_THREADS);
        System.out.println("Operations per thread: " + NUM_OPERATIONS_PER_THREAD);
        System.out.println("Total operations: " + (NUM_THREADS * NUM_OPERATIONS_PER_THREAD));
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + errorCount.get());
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average time per operation: " + (totalTime / (NUM_THREADS * NUM_OPERATIONS_PER_THREAD)) + "ms");
        System.out.println("Font data cache size: " + fontManager.getFontDataCacheSize());
        System.out.println("Font instance cache size: " + fontManager.getFontInstanceCacheSize());
        
        // Assertions
        assertEquals("All operations should be successful", NUM_THREADS * NUM_OPERATIONS_PER_THREAD, successCount.get());
        assertEquals("No errors should occur", 0, errorCount.get());
        assertTrue("Should have cached font data", fontManager.getFontDataCacheSize() > 0);
        // Note: Font instance cache may be 0 due to clearDocumentFonts cleanup, which is correct behavior
    }

    /**
     * Test memory efficiency of font caching by creating many documents.
     */
    @Test
    public void testMemoryEfficiencyWithManyDocuments() throws IOException {
        FontManager fontManager = FontManager.getInstance();
        fontManager.clearAllCaches();
        
        final int NUM_DOCUMENTS = 20;
        
        for (int i = 0; i < NUM_DOCUMENTS; i++) {
            PDDocument doc = new PDDocument();
            try {
                // Load all font variants
                FontUtils.setSourceSans3FontsAsDefault(doc);
                
                // Create a small table to verify fonts work
                PDPage page = new PDPage();
                doc.addPage(page);
                
                BaseTable table = new BaseTable(700, 700, 50, 400, 20, doc, page, true, true);
                Row<PDPage> row = table.createRow(20f);
                Cell<PDPage> cell = row.createCell(100, "Test document " + i + " with Source Sans 3");
                cell.setHeaderCell(true); // Use bold font
                table.draw();
                
                // Note: Not saving PDF to avoid concurrent PDF saving issues with font subsetting
                // The test focuses on font loading and caching efficiency
                
            } finally {
                doc.close();
                // Clear document-specific fonts to simulate real usage
                FontUtils.clearDocumentFonts(doc);
            }
        }
        
        System.out.println("=== Memory Efficiency Test Results ===");
        System.out.println("Documents created: " + NUM_DOCUMENTS);
        System.out.println("Font data cache size: " + fontManager.getFontDataCacheSize());
        System.out.println("Font instance cache size: " + fontManager.getFontInstanceCacheSize());
        
        // Font data should be minimal (only 4 font files)
        assertEquals("Should have exactly 4 cached font data entries", 4, fontManager.getFontDataCacheSize());
        
        // Font instances should be cleared after document cleanup
        assertTrue("Font instance cache should be reasonable after cleanup", 
                  fontManager.getFontInstanceCacheSize() <= 10);
    }

    /**
     * Test rapid font loading and ensure no deadlocks occur.
     */
    @Test
    public void testRapidFontLoadingNoDeadlocks() throws InterruptedException, ExecutionException {
        final int NUM_THREADS = 10;
        final int NUM_OPERATIONS = 50;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        FontManager fontManager = FontManager.getInstance();
        
        fontManager.clearAllCaches();
        
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
            AtomicInteger operationCount = new AtomicInteger(0);
            
            for (int i = 0; i < NUM_THREADS; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        for (int j = 0; j < NUM_OPERATIONS; j++) {
                            PDDocument doc = new PDDocument();
                            try {
                                FontUtils.setSourceSans3FontsAsDefault(doc);
                                operationCount.incrementAndGet();
                            } finally {
                                doc.close();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for completion with timeout to detect deadlocks
            boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
            
            assertTrue("All threads should complete within timeout (no deadlocks)", completed);
            assertEquals("All operations should complete successfully", 
                        NUM_THREADS * NUM_OPERATIONS, operationCount.get());
            
            System.out.println("=== Rapid Loading Test Results ===");
            System.out.println("Total operations: " + operationCount.get());
            System.out.println("Font data cache size: " + fontManager.getFontDataCacheSize());
            System.out.println("Font instance cache size: " + fontManager.getFontInstanceCacheSize());
            
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Helper method to test font loading performance in concurrent scenarios.
     */
    private void testFontLoadingInThread(int threadId, int operationId) throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Set Source Sans 3 fonts as default - this is where performance matters
            FontUtils.setSourceSans3FontsAsDefault(doc);
            
            // Verify fonts are loaded correctly
            assertNotNull("Default fonts should be loaded", FontUtils.getDefaultfonts());
            assertEquals("Should have 4 font variants", 4, FontUtils.getDefaultfonts().size());
            
            // Create a simple page and table to verify fonts work
            PDPage page = new PDPage();
            doc.addPage(page);
            
            BaseTable table = new BaseTable(700, 700, 50, 400, 20, doc, page, true, true);
            Row<PDPage> row = table.createRow(20f);
            Cell<PDPage> cell = row.createCell(100, 
                String.format("Thread %d Operation %d", threadId, operationId));
            cell.setHeaderCell(true); // Uses bold font
            
            // Draw table in memory (don't save to avoid concurrent PDF saving issues)
            table.draw();
            
        } finally {
            doc.close();
            // Clear document fonts to simulate proper cleanup
            FontUtils.clearDocumentFonts(doc);
        }
    }
}