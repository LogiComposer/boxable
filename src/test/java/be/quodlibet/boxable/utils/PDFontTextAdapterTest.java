package be.quodlibet.boxable.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for PDFontTextAdapter
 */
public class PDFontTextAdapterTest {

    private PDDocument document;
    private PDFont type1Font;
    private PDFont type0Font;
    private PDFontTextAdapter type1Adapter;
    private PDFontTextAdapter type0Adapter;
    
    @Before
    public void setUp() throws IOException {
        document = new PDDocument();
        type1Font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        type0Font = FontUtils.loadFont(document, "fonts/FreeSans.ttf");
        
        type1Adapter = new PDFontTextAdapter(type1Font);
        
        // Use type1Font as fallback if type0Font failed to load
        if (type0Font != null) {
            type0Adapter = new PDFontTextAdapter(type0Font);
        } else {
            type0Adapter = new PDFontTextAdapter(type1Font); // fallback for testing
        }
    }
    
    @After
    public void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullFont() {
        new PDFontTextAdapter(null);
    }
    
    @Test
    public void testConstructorWithValidFont() {
        PDFontTextAdapter adapter = new PDFontTextAdapter(type1Font);
        assertNotNull(adapter);
        assertEquals(type1Font, adapter.getFont());
    }
    
    @Test
    public void testSanitizeTextWithNullInput() {
        assertNull(type1Adapter.sanitizeText(null));
    }
    
    @Test
    public void testSanitizeTextWithEmptyInput() {
        assertEquals("", type1Adapter.sanitizeText(""));
    }
    
    @Test
    public void testSanitizeTextWithValidCharacters() {
        String input = "Hello World 123!";
        String result = type1Adapter.sanitizeText(input);
        assertEquals(input, result);
    }
    
    @Test
    public void testSanitizeTextWithUnsupportedCharacters() {
        // Test with characters that might not be supported by Helvetica
        String input = "Hello \u2603 World"; // Contains snowman character
        String result = type1Adapter.sanitizeText(input);
        
        // Result should contain replacement character where unsupported chars were
        assertNotNull(result);
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("World"));
    }
    
    @Test
    public void testCanDisplayCharacter() {
        // Test with common ASCII character
        assertTrue(type1Adapter.canDisplayCharacter('A'));
        assertTrue(type1Adapter.canDisplayCharacter(' '));
        assertTrue(type1Adapter.canDisplayCharacter('1'));
    }
    
    @Test
    public void testGetStringWidthWithNullText() {
        assertEquals(0f, type1Adapter.getStringWidth(null, 12f), 0.01f);
    }
    
    @Test
    public void testGetStringWidthWithEmptyText() {
        assertEquals(0f, type1Adapter.getStringWidth("", 12f), 0.01f);
    }
    
    @Test
    public void testGetStringWidthWithValidText() {
        String text = "Hello";
        float fontSize = 12f;
        float width = type1Adapter.getStringWidth(text, fontSize);
        
        assertTrue("Width should be positive", width > 0);
        
        // Test that longer text has greater width
        float longerWidth = type1Adapter.getStringWidth(text + " World", fontSize);
        assertTrue("Longer text should have greater width", longerWidth > width);
    }
    
    @Test
    public void testGetStringWidthAccuracy() {
        // Test that our adapter width matches FontUtils directly (within 5% tolerance)
        String text = "Sample text for width testing";
        float fontSize = 12f;
        
        float adapterWidth = type1Adapter.getStringWidth(text, fontSize);
        float directWidth = FontUtils.getStringWidth(type1Font, text, fontSize);
        
        float tolerance = Math.abs(directWidth * 0.05f);
        assertEquals("Adapter width should match FontUtils width within 5% tolerance", 
                    directWidth, adapterWidth, tolerance);
    }
    
    @Test
    public void testGetLineHeight() {
        float fontSize = 12f;
        float height = type1Adapter.getLineHeight(fontSize);
        
        assertTrue("Line height should be positive", height > 0);
        
        // Test that line height scales with font size
        float largerHeight = type1Adapter.getLineHeight(fontSize * 2);
        assertTrue("Larger font size should have greater line height", largerHeight > height);
        
        // Verify it matches FontUtils directly
        float directHeight = FontUtils.getHeight(type1Font, fontSize);
        assertEquals("Adapter height should match FontUtils height", directHeight, height, 0.01f);
    }
    
    @Test
    public void testTruncateToWidthWithNullText() {
        assertNull(type1Adapter.truncateToWidth(null, 100f, 12f));
    }
    
    @Test
    public void testTruncateToWidthWithEmptyText() {
        assertEquals("", type1Adapter.truncateToWidth("", 100f, 12f));
    }
    
    @Test
    public void testTruncateToWidthWithTextThatFits() {
        String text = "Short";
        float maxWidth = 1000f; // Very wide
        float fontSize = 12f;
        
        String result = type1Adapter.truncateToWidth(text, maxWidth, fontSize);
        assertEquals(text, result);
    }
    
    @Test
    public void testTruncateToWidthWithTextThatNeedsTruncation() {
        String text = "This is a very long text that will definitely need truncation";
        float maxWidth = 50f; // Very narrow
        float fontSize = 12f;
        
        String result = type1Adapter.truncateToWidth(text, maxWidth, fontSize);
        
        assertNotNull(result);
        assertTrue("Result should end with ellipsis", result.endsWith("..."));
        assertTrue("Result should be shorter than original", result.length() < text.length());
        
        // Verify the truncated text fits within the width
        float resultWidth = type1Adapter.getStringWidth(result, fontSize);
        assertTrue("Truncated text should fit within maxWidth", resultWidth <= maxWidth);
    }
    
    @Test
    public void testTruncateToWidthWithVeryNarrowWidth() {
        String text = "Hello";
        float maxWidth = 5f; // Very narrow, might only fit ellipsis
        float fontSize = 12f;
        
        String result = type1Adapter.truncateToWidth(text, maxWidth, fontSize);
        
        assertNotNull(result);
        // Should at least return ellipsis
        assertTrue("Should contain ellipsis", result.contains("..."));
    }
    
    @Test
    public void testWrapTextWithNullText() {
        List<String> result = type1Adapter.wrapText(null, 100f, 12f);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testWrapTextWithEmptyText() {
        List<String> result = type1Adapter.wrapText("", 100f, 12f);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testWrapTextWithSingleWord() {
        String text = "Hello";
        float maxWidth = 1000f; // Very wide
        float fontSize = 12f;
        
        List<String> result = type1Adapter.wrapText(text, maxWidth, fontSize);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(text, result.get(0));
    }
    
    @Test
    public void testWrapTextWithMultipleWordsOnOneLine() {
        String text = "Hello World";
        float maxWidth = 1000f; // Very wide
        float fontSize = 12f;
        
        List<String> result = type1Adapter.wrapText(text, maxWidth, fontSize);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(text, result.get(0));
    }
    
    @Test
    public void testWrapTextWithMultipleLines() {
        String text = "This is a long sentence that should wrap across multiple lines when constrained to a narrow width";
        float maxWidth = 100f; // Narrow width to force wrapping
        float fontSize = 10f;
        
        List<String> result = type1Adapter.wrapText(text, maxWidth, fontSize);
        
        assertNotNull(result);
        assertTrue("Should have multiple lines", result.size() > 1);
        
        // Verify each line fits within the width constraint
        for (String line : result) {
            float lineWidth = type1Adapter.getStringWidth(line, fontSize);
            assertTrue("Each line should fit within maxWidth", lineWidth <= maxWidth + 1); // +1 for floating point precision
        }
    }
    
    @Test
    public void testWrapTextWithVeryLongWord() {
        String text = "Supercalifragilisticexpialidocious";
        float maxWidth = 50f; // Very narrow
        float fontSize = 12f;
        
        List<String> result = type1Adapter.wrapText(text, maxWidth, fontSize);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue("Long word should be truncated with ellipsis", result.get(0).endsWith("..."));
    }
    
    @Test
    public void testWrapTextPreservesSpacesCorrectly() {
        String text = "Word1 Word2 Word3";
        float maxWidth = 1000f; // Wide enough for all words
        float fontSize = 12f;
        
        List<String> result = type1Adapter.wrapText(text, maxWidth, fontSize);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(text, result.get(0));
    }
    
    @Test
    public void testGetFont() {
        assertEquals(type1Font, type1Adapter.getFont());
        
        if (type0Font != null) {
            assertEquals(type0Font, type0Adapter.getFont());
        }
    }
    
    @Test
    public void testWithDifferentFontTypes() {
        // Test that adapter works with both Type1 and Type0 fonts
        String text = "Test text";
        float fontSize = 12f;
        
        assertNotNull(type1Adapter.sanitizeText(text));
        assertTrue(type1Adapter.getStringWidth(text, fontSize) > 0);
        assertTrue(type1Adapter.getLineHeight(fontSize) > 0);
        
        if (type0Font != null) {
            assertNotNull(type0Adapter.sanitizeText(text));
            assertTrue(type0Adapter.getStringWidth(text, fontSize) > 0);
            assertTrue(type0Adapter.getLineHeight(fontSize) > 0);
        }
    }
    
    @Test
    public void testEdgeCasesWithZeroAndNegativeValues() {
        String text = "Test";
        
        // Test with zero font size
        assertEquals(0f, type1Adapter.getStringWidth(text, 0f), 0.01f);
        assertEquals(0f, type1Adapter.getLineHeight(0f), 0.01f);
        
        // Test with zero width for truncation
        String truncated = type1Adapter.truncateToWidth(text, 0f, 12f);
        assertEquals("...", truncated);
        
        // Test with zero width for wrapping
        List<String> wrapped = type1Adapter.wrapText(text, 0f, 12f);
        assertTrue(wrapped.isEmpty() || wrapped.get(0).equals("..."));
    }
}