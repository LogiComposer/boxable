package be.quodlibet.boxable.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Performance tests for PDFontTextAdapter caching improvements
 */
public class PDFontTextAdapterPerformanceTest {

    private PDDocument document;
    private PDFont font;
    private PDFontTextAdapter adapter;
    
    @Before
    public void setUp() throws IOException {
        document = new PDDocument();
        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        adapter = new PDFontTextAdapter(font);
    }
    
    @After
    public void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }
    
    @Test
    public void testCharacterDisplayCachingPerformance() {
        // Test repeated character validation with caching
        char[] testChars = {'A', 'B', 'C', 'D', 'E', 'a', 'b', 'c', 'd', 'e', '1', '2', '3', ' ', '!', '@'};
        int iterations = 1000;
        
        // First run - populate cache
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (char c : testChars) {
                adapter.canDisplayCharacter(c);
            }
        }
        long firstRunTime = System.nanoTime() - startTime;
        
        // Verify cache is populated
        assertTrue("Character display cache should have entries", adapter.getCharacterDisplayCacheSize() > 0);
        assertEquals("Should have cached all test characters", testChars.length, adapter.getCharacterDisplayCacheSize());
        
        // Second run - should be much faster due to caching
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (char c : testChars) {
                adapter.canDisplayCharacter(c);
            }
        }
        long secondRunTime = System.nanoTime() - startTime;
        
        // Verify consistent results
        for (char c : testChars) {
            assertTrue("Character " + c + " should be displayable", adapter.canDisplayCharacter(c));
        }
        
        // Second run should be significantly faster (at least 50% improvement expected)
        System.out.println("First run (populating cache): " + firstRunTime / 1_000_000.0 + " ms");
        System.out.println("Second run (using cache): " + secondRunTime / 1_000_000.0 + " ms");
        System.out.println("Performance improvement: " + (100.0 * (firstRunTime - secondRunTime) / firstRunTime) + "%");
        
        assertTrue("Cached run should be faster than initial run", secondRunTime < firstRunTime);
    }
    
    @Test
    public void testStringWidthCachingPerformance() {
        // Test repeated string width calculations with caching
        String[] testStrings = {
            "Hello World",
            "The quick brown fox",
            "jumps over the lazy dog",
            "1234567890",
            "!@#$%^&*()",
            "Short",
            "This is a longer string for testing purposes"
        };
        float fontSize = 12f;
        int iterations = 500;
        
        // First run - populate cache
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String text : testStrings) {
                adapter.getStringWidth(text, fontSize);
            }
        }
        long firstRunTime = System.nanoTime() - startTime;
        
        // Verify cache is populated
        assertTrue("String width cache should have entries", adapter.getStringWidthCacheSize() > 0);
        
        // Second run - should be much faster due to caching
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String text : testStrings) {
                adapter.getStringWidth(text, fontSize);
            }
        }
        long secondRunTime = System.nanoTime() - startTime;
        
        // Verify consistent results
        for (String text : testStrings) {
            float width = adapter.getStringWidth(text, fontSize);
            assertTrue("Width should be positive for: " + text, width > 0);
        }
        
        // Second run should be significantly faster
        System.out.println("String width - First run: " + firstRunTime / 1_000_000.0 + " ms");
        System.out.println("String width - Second run: " + secondRunTime / 1_000_000.0 + " ms");
        System.out.println("String width - Performance improvement: " + (100.0 * (firstRunTime - secondRunTime) / firstRunTime) + "%");
        
        assertTrue("Cached string width run should be faster", secondRunTime < firstRunTime);
    }
    
    @Test
    public void testSanitizeTextPerformanceWithRepeatedCharacters() {
        // Test sanitizing text with repeated characters to benefit from caching
        StringBuilder textBuilder = new StringBuilder();
        Random random = new Random(12345); // Use seed for reproducible results
        
        // Create text with repeated characters
        char[] commonChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', ' ', '.', ',', '!', '?'};
        for (int i = 0; i < 1000; i++) {
            textBuilder.append(commonChars[random.nextInt(commonChars.length)]);
        }
        String testText = textBuilder.toString();
        
        // First sanitization - populate cache
        long startTime = System.nanoTime();
        String firstResult = adapter.sanitizeText(testText);
        long firstRunTime = System.nanoTime() - startTime;
        
        // Subsequent sanitizations - should benefit from character caching
        List<Long> subsequentTimes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            startTime = System.nanoTime();
            String result = adapter.sanitizeText(testText);
            long runTime = System.nanoTime() - startTime;
            subsequentTimes.add(runTime);
            
            // Verify consistent results
            assertEquals("Sanitized text should be consistent", firstResult, result);
        }
        
        // Calculate average subsequent time
        long avgSubsequentTime = subsequentTimes.stream().mapToLong(Long::longValue).sum() / subsequentTimes.size();
        
        System.out.println("Text sanitization - First run: " + firstRunTime / 1_000_000.0 + " ms");
        System.out.println("Text sanitization - Average subsequent: " + avgSubsequentTime / 1_000_000.0 + " ms");
        System.out.println("Text sanitization - Performance improvement: " + (100.0 * (firstRunTime - avgSubsequentTime) / firstRunTime) + "%");
        
        // Verify cache utilization
        assertTrue("Character display cache should have entries", adapter.getCharacterDisplayCacheSize() > 0);
        assertTrue("Should have cached common characters", adapter.getCharacterDisplayCacheSize() <= commonChars.length);
    }
    
    @Test
    public void testCacheClearFunctionality() {
        // Populate caches
        adapter.canDisplayCharacter('A');
        adapter.canDisplayCharacter('B');
        adapter.getStringWidth("Hello World", 12f);
        adapter.getStringWidth("Test String", 10f);
        
        // Verify caches have content
        assertTrue("Character cache should have entries", adapter.getCharacterDisplayCacheSize() > 0);
        assertTrue("String width cache should have entries", adapter.getStringWidthCacheSize() > 0);
        assertTrue("Character string cache should have entries", adapter.getCharacterStringCacheSize() > 0);
        
        // Clear caches
        adapter.clearCaches();
        
        // Verify caches are cleared
        assertEquals("Character cache should be empty", 0, adapter.getCharacterDisplayCacheSize());
        assertEquals("String width cache should be empty", 0, adapter.getStringWidthCacheSize());
        assertEquals("Character string cache should be empty", 0, adapter.getCharacterStringCacheSize());
    }
    
    @Test
    public void testCacheMemoryEfficiency() {
        // Test that cache doesn't grow unbounded with unique strings
        int numUniqueStrings = 100;
        float fontSize = 12f;
        
        // Create many unique strings
        for (int i = 0; i < numUniqueStrings; i++) {
            String uniqueString = "String_" + i + "_" + System.nanoTime();
            adapter.getStringWidth(uniqueString, fontSize);
        }
        
        // Verify cache size is reasonable
        int cacheSize = adapter.getStringWidthCacheSize();
        assertTrue("Cache should have entries", cacheSize > 0);
        assertTrue("Cache size should be reasonable", cacheSize <= numUniqueStrings + 10); // Allow some buffer
        
        System.out.println("Created " + numUniqueStrings + " unique strings, cache size: " + cacheSize);
    }
}