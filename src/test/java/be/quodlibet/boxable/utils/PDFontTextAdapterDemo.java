package be.quodlibet.boxable.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;

/**
 * Comprehensive demonstration of PDFontTextAdapter functionality.
 * This class showcases all the features of the adapter including:
 * - Text sanitization
 * - Character support checking
 * - Width measurement 
 * - Line height calculation
 * - Text truncation with ellipsis
 * - Text wrapping
 */
public class PDFontTextAdapterDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("PDFontTextAdapter Comprehensive Demo");
        System.out.println("====================================");
        
        // Initialize document and font
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        // Create the text adapter
        PDFontTextAdapter adapter = new PDFontTextAdapter(font);
        
        // Demo 1: Text sanitization
        System.out.println("1. Text Sanitization Demo");
        String problematicText = "Text with special chars: \u2603\u2665\u03B1\u03B2 and control chars: \n\t";
        String sanitizedText = adapter.sanitizeText(problematicText);
        System.out.println("Original: " + problematicText);
        System.out.println("Sanitized: " + sanitizedText);
        System.out.println();
        
        // Demo 2: Character support checking
        System.out.println("2. Character Support Demo");
        String testChars = "ABCabc123!@#\u2603\u2665\u03B1\u03B2";
        for (int i = 0; i < testChars.length(); i++) {
            int codePoint = testChars.codePointAt(i);
            boolean supported = adapter.canDisplayCharacter(codePoint);
            System.out.printf("Character '%c' (U+%04X): %s%n", 
                            testChars.charAt(i), codePoint, supported ? "Supported" : "Not supported");
        }
        System.out.println();
        
        // Demo 3: Width measurement
        System.out.println("3. Width Measurement Demo");
        String[] testTexts = {"A", "Hello", "Hello World", "This is a longer text sample"};
        float fontSize = 12f;
        for (String text : testTexts) {
            float width = adapter.getStringWidth(text, fontSize);
            System.out.printf("Text: '%-30s' Width: %.2f points%n", text, width);
        }
        System.out.println();
        
        // Demo 4: Line height calculation
        System.out.println("4. Line Height Calculation Demo");
        float[] fontSizes = {8f, 10f, 12f, 14f, 16f, 18f};
        for (float size : fontSizes) {
            float height = adapter.getLineHeight(size);
            System.out.printf("Font size: %.0f points, Line height: %.2f points%n", size, height);
        }
        System.out.println();
        
        // Demo 5: Text truncation
        System.out.println("5. Text Truncation Demo");
        String longText = "This is a very long text that will need to be truncated to fit within specific width constraints";
        float[] maxWidths = {50f, 100f, 150f, 200f};
        for (float maxWidth : maxWidths) {
            String truncated = adapter.truncateToWidth(longText, maxWidth, fontSize);
            System.out.printf("Max width: %.0f points, Result: '%s'%n", maxWidth, truncated);
        }
        System.out.println();
        
        // Demo 6: Text wrapping
        System.out.println("6. Text Wrapping Demo");
        String wrapText = "This is a long sentence that will be wrapped into multiple lines based on width constraints";
        for (float maxWidth : maxWidths) {
            List<String> lines = adapter.wrapText(wrapText, maxWidth, fontSize);
            System.out.printf("Max width: %.0f points, Lines: %d%n", maxWidth, lines.size());
            for (int i = 0; i < lines.size(); i++) {
                System.out.printf("  Line %d: '%s'%n", i + 1, lines.get(i));
            }
            System.out.println();
        }
        
        // Demo 7: Performance test
        System.out.println("7. Performance Test");
        performanceTest(adapter);
        
        // Demo 8: Create visual PDF demonstration
        System.out.println("8. Creating Visual PDF Demo");
        createPDFDemo(document, page, adapter);
        
        // Save and close
        File outputFile = new File("target/PDFontTextAdapterDemo.pdf");
        outputFile.getParentFile().mkdirs();
        document.save(outputFile);
        document.close();
        
        System.out.println("Demo completed! PDF saved to: " + outputFile.getAbsolutePath());
    }
    
    private static void performanceTest(PDFontTextAdapter adapter) {
        String testText = "Performance test text with reasonable length for timing";
        float fontSize = 12f;
        float maxWidth = 150f;
        int iterations = 1000;
        
        long startTime, endTime;
        
        // Test sanitization
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            adapter.sanitizeText(testText);
        }
        endTime = System.nanoTime();
        System.out.printf("Sanitization: %d ns/operation (avg over %d iterations)%n", 
                         (endTime - startTime) / iterations, iterations);
        
        // Test width calculation
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            adapter.getStringWidth(testText, fontSize);
        }
        endTime = System.nanoTime();
        System.out.printf("Width calculation: %d ns/operation (avg over %d iterations)%n", 
                         (endTime - startTime) / iterations, iterations);
        
        // Test truncation
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            adapter.truncateToWidth(testText, maxWidth, fontSize);
        }
        endTime = System.nanoTime();
        System.out.printf("Text truncation: %d ns/operation (avg over %d iterations)%n", 
                         (endTime - startTime) / iterations, iterations);
        
        // Test wrapping
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            adapter.wrapText(testText, maxWidth, fontSize);
        }
        endTime = System.nanoTime();
        System.out.printf("Text wrapping: %d ns/operation (avg over %d iterations)%n", 
                         (endTime - startTime) / iterations, iterations);
        
        System.out.println();
    }
    
    private static void createPDFDemo(PDDocument document, PDPage page, PDFontTextAdapter adapter) throws IOException {
        float margin = 20;
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        float yStart = yStartNewPage;
        float bottomMargin = 50;

        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, document, page, true, true);

        // Title row
        Row<PDPage> titleRow = table.createRow(25f);
        Cell<PDPage> titleCell = titleRow.createCell(100, "PDFontTextAdapter Feature Demonstration");
        titleCell.setTextColor(Color.BLUE);
        titleCell.setFontSize(16);
        
        // Header row
        Row<PDPage> headerRow = table.createRow(20f);
        headerRow.createCell(25, "Feature").setTextColor(Color.RED);
        headerRow.createCell(35, "Input").setTextColor(Color.RED);
        headerRow.createCell(40, "Output").setTextColor(Color.RED);
        
        // Sanitization demo
        Row<PDPage> row1 = table.createRow(15f);
        row1.createCell(25, "Sanitization");
        String problematicInput = "Special: \u2603\u2665 Control: \n\t";
        row1.createCell(35, "Special: [snowman][heart] Control: [LF][TAB]");
        row1.createCell(40, adapter.sanitizeText(problematicInput));
        
        // Width measurement demo
        Row<PDPage> row2 = table.createRow(15f);
        row2.createCell(25, "Width (12pt)");
        String widthText = "Sample Text";
        row2.createCell(35, widthText);
        row2.createCell(40, String.format("%.2f points", adapter.getStringWidth(widthText, 12f)));
        
        // Line height demo
        Row<PDPage> row3 = table.createRow(15f);
        row3.createCell(25, "Line Height (12pt)");
        row3.createCell(35, "N/A");
        row3.createCell(40, String.format("%.2f points", adapter.getLineHeight(12f)));
        
        // Truncation demo
        Row<PDPage> row4 = table.createRow(15f);
        row4.createCell(25, "Truncation");
        String longText = "This is very long text that needs truncation";
        row4.createCell(35, longText);
        row4.createCell(40, adapter.truncateToWidth(longText, 100f, 10f));
        
        // Wrapping demo
        Row<PDPage> row5 = table.createRow(20f);
        row5.createCell(25, "Text Wrapping");
        String wrapText = "Text that will be wrapped into multiple lines";
        row5.createCell(35, wrapText);
        List<String> wrappedLines = adapter.wrapText(wrapText, 80f, 10f);
        row5.createCell(40, wrappedLines.size() + " lines: " + String.join(" | ", wrappedLines));
        
        // Character support demo
        Row<PDPage> row6 = table.createRow(15f);
        row6.createCell(25, "Char Support");
        row6.createCell(35, "A [snowman] [alpha] [beta]");
        StringBuilder supportResult = new StringBuilder();
        String testChars = "A\u2603\u03B1\u03B2";
        String[] charNames = {"A", "snowman", "alpha", "beta"};
        for (int i = 0; i < testChars.length(); i++) {
            int codePoint = testChars.codePointAt(i);
            supportResult.append(charNames[i])
                        .append(adapter.canDisplayCharacter(codePoint) ? ":YES " : ":NO ");
        }
        row6.createCell(40, supportResult.toString());

        table.draw();
    }
}