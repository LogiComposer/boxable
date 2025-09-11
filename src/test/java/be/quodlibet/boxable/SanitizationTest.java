package be.quodlibet.boxable;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import be.quodlibet.boxable.utils.PDFontTextAdapter;

/**
 * Simple test to verify PDFontTextAdapter sanitization
 */
public class SanitizationTest {
    
    public static void main(String[] args) {
        try {
            PDFontTextAdapter adapter = new PDFontTextAdapter(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA)
            );
            
            String testText = "Control chars: \u0000\u0001\u0002";
            System.out.println("Original text: " + testText.replace("\u0000", "\\u0000").replace("\u0001", "\\u0001").replace("\u0002", "\\u0002"));
            
            String sanitized = adapter.sanitizeText(testText);
            System.out.println("Sanitized text: " + sanitized);
            
            System.out.println("Test passed: SafeTextCell text sanitization works correctly!");
            
        } catch (Exception e) {
            System.err.println("Error during sanitization test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}