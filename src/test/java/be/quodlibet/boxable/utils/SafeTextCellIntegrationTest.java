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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;

/**
 * Integration test demonstrating the use of PDFontTextAdapter with SafeTextCell.
 * This test shows how to create safe text handling within table cells using the adapter.
 */
public class SafeTextCellIntegrationTest {

    private PDDocument document;
    private PDPage page;
    private PDFont font;
    private PDFontTextAdapter textAdapter;

    @Before
    public void setUp() throws IOException {
        document = new PDDocument();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        textAdapter = new PDFontTextAdapter(font);
    }

    @After
    public void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }

    /**
     * Helper class that demonstrates safe text cell creation using PDFontTextAdapter
     */
    public static class SafeTextCell {
        private final PDFontTextAdapter textAdapter;
        private final Cell<?> cell;

        public SafeTextCell(Cell<?> cell, PDFontTextAdapter textAdapter) {
            this.cell = cell;
            this.textAdapter = textAdapter;
        }

        /**
         * Sets text on the cell after sanitizing it for font compatibility
         */
        public void setSafeText(String text) {
            String sanitizedText = textAdapter.sanitizeText(text);
            cell.setText(sanitizedText);
        }

        /**
         * Sets text that will be truncated to fit within the cell width
         */
        public void setTruncatedText(String text, float maxWidth, float fontSize) {
            String truncatedText = textAdapter.truncateToWidth(text, maxWidth, fontSize);
            cell.setText(truncatedText);
        }

        /**
         * Sets wrapped text that will flow into multiple lines within the cell
         */
        public void setWrappedText(String text, float maxWidth, float fontSize) {
            List<String> lines = textAdapter.wrapText(text, maxWidth, fontSize);
            // For demonstration, we'll use the first line only since Boxable 
            // handles multi-line text differently. In a real implementation,
            // you would integrate with Boxable's Paragraph class for multi-line handling
            String wrappedText = lines.isEmpty() ? "" : lines.get(0) + (lines.size() > 1 ? " [+" + (lines.size()-1) + " more lines]" : "");
            cell.setText(wrappedText);
        }

        /**
         * Gets the underlying cell for additional configuration
         */
        public Cell<?> getCell() {
            return cell;
        }

        /**
         * Gets the text adapter for direct access
         */
        public PDFontTextAdapter getTextAdapter() {
            return textAdapter;
        }
    }

    @Test
    public void testSafeTextCellIntegration() throws IOException {
        // Set up table
        float margin = 10;
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        float yStart = yStartNewPage;
        float bottomMargin = 70;

        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, document, page, true, true);

        // Test 1: Basic sanitization
        Row<PDPage> row1 = table.createRow(20f);
        Cell<PDPage> cell1 = row1.createCell(50, "");
        SafeTextCell safeCell1 = new SafeTextCell(cell1, textAdapter);
        safeCell1.setSafeText("Text with special chars: \u2603\u2665"); // Contains potentially unsupported characters
        cell1.setTextColor(Color.BLACK);
        
        Cell<PDPage> cell2 = row1.createCell(50, "Regular text without special characters");
        cell2.setTextColor(Color.BLACK);

        // Test 2: Text truncation
        Row<PDPage> row2 = table.createRow(20f);
        Cell<PDPage> cell3 = row2.createCell(50, "");
        SafeTextCell safeCell3 = new SafeTextCell(cell3, textAdapter);
        safeCell3.setTruncatedText("This is a very long text that should be truncated to fit in the cell width", 100f, 10f);
        cell3.setTextColor(Color.BLUE);
        
        Cell<PDPage> cell4 = row2.createCell(50, "Normal length text");
        cell4.setTextColor(Color.BLUE);

        // Test 3: Text wrapping
        Row<PDPage> row3 = table.createRow(30f); // Taller row for wrapped text
        Cell<PDPage> cell5 = row3.createCell(50, "");
        SafeTextCell safeCell5 = new SafeTextCell(cell5, textAdapter);
        safeCell5.setWrappedText("This text will be wrapped into multiple lines to fit within the specified width constraints", 120f, 10f);
        cell5.setTextColor(Color.RED);
        
        Cell<PDPage> cell6 = row3.createCell(50, "Another cell with normal content");
        cell6.setTextColor(Color.RED);

        // Test 4: Demonstrate character support checking
        Row<PDPage> row4 = table.createRow(20f);
        Cell<PDPage> cell7 = row4.createCell(100, "");
        SafeTextCell safeCell7 = new SafeTextCell(cell7, textAdapter);
        
        // Build a string showing which characters are supported
        StringBuilder supportTest = new StringBuilder("Character support test: ");
        String testChars = "ABCabc123!@#\u2603\u2665\u03B1\u03B2"; // Mix of ASCII and Unicode
        for (int i = 0; i < testChars.length(); i++) {
            int codePoint = testChars.codePointAt(i);
            boolean supported = textAdapter.canDisplayCharacter(codePoint);
            supportTest.append(testChars.charAt(i)).append(supported ? "✓" : "✗").append(" ");
        }
        safeCell7.setSafeText(supportTest.toString());
        cell7.setTextColor(Color.GREEN);

        // Draw the table
        table.draw();

        // Save the PDF
        File outputFile = new File("target/SafeTextCellIntegrationTest.pdf");
        outputFile.getParentFile().mkdirs();
        document.save(outputFile);
        
        System.out.println("SafeTextCell integration test saved to: " + outputFile.getAbsolutePath());
    }

    @Test
    public void testTextAdapterWithDifferentFontSizes() throws IOException {
        // Set up table
        float margin = 10;
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        float yStart = yStartNewPage;
        float bottomMargin = 70;

        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, document, page, true, true);

        // Test different font sizes and their effects on text handling
        String sampleText = "Sample text for font size testing with various lengths";
        float[] fontSizes = {8f, 10f, 12f, 14f, 16f};
        float maxWidth = 150f;

        for (float fontSize : fontSizes) {
            Row<PDPage> row = table.createRow(20f);
            
            // Original text cell
            Cell<PDPage> originalCell = row.createCell(33, sampleText);
            originalCell.setFontSize(fontSize);
            originalCell.setTextColor(Color.BLACK);
            
            // Truncated text cell
            Cell<PDPage> truncatedCell = row.createCell(33, "");
            SafeTextCell safeTruncatedCell = new SafeTextCell(truncatedCell, textAdapter);
            safeTruncatedCell.setTruncatedText(sampleText, maxWidth, fontSize);
            truncatedCell.setFontSize(fontSize);
            truncatedCell.setTextColor(Color.BLUE);
            
            // Width measurement display
            float originalWidth = textAdapter.getStringWidth(sampleText, fontSize);
            float lineHeight = textAdapter.getLineHeight(fontSize);
            String measurements = String.format("Size:%.0f Width:%.1f Height:%.1f", fontSize, originalWidth, lineHeight);
            Cell<PDPage> measurementCell = row.createCell(34, measurements);
            measurementCell.setFontSize(8f);
            measurementCell.setTextColor(Color.RED);
        }

        // Draw the table
        table.draw();

        // Save the PDF
        File outputFile = new File("target/FontSizeTestIntegration.pdf");
        outputFile.getParentFile().mkdirs();
        document.save(outputFile);
        
        System.out.println("Font size integration test saved to: " + outputFile.getAbsolutePath());
    }

    @Test
    public void testTextAdapterPerformance() throws IOException {
        // Performance test to ensure the adapter doesn't introduce significant overhead
        String testText = "Performance test string with moderate length for timing measurements";
        float fontSize = 12f;
        float maxWidth = 200f;
        
        long startTime, endTime;
        int iterations = 1000;
        
        // Test sanitization performance
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            textAdapter.sanitizeText(testText);
        }
        endTime = System.nanoTime();
        long sanitizeTime = endTime - startTime;
        
        // Test width calculation performance
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            textAdapter.getStringWidth(testText, fontSize);
        }
        endTime = System.nanoTime();
        long widthTime = endTime - startTime;
        
        // Test truncation performance
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            textAdapter.truncateToWidth(testText, maxWidth, fontSize);
        }
        endTime = System.nanoTime();
        long truncateTime = endTime - startTime;
        
        // Test wrapping performance
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            textAdapter.wrapText(testText, maxWidth, fontSize);
        }
        endTime = System.nanoTime();
        long wrapTime = endTime - startTime;
        
        // Output performance results
        System.out.println("PDFontTextAdapter Performance Test Results (" + iterations + " iterations):");
        System.out.println("Sanitization: " + (sanitizeTime / iterations) + " ns/operation");
        System.out.println("Width calculation: " + (widthTime / iterations) + " ns/operation");  
        System.out.println("Truncation: " + (truncateTime / iterations) + " ns/operation");
        System.out.println("Text wrapping: " + (wrapTime / iterations) + " ns/operation");
        
        // Create a simple performance report PDF
        float margin = 10;
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        float yStart = yStartNewPage;
        float bottomMargin = 70;

        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, document, page, true, true);

        // Header row
        Row<PDPage> headerRow = table.createRow(20f);
        headerRow.createCell(50, "Operation").setTextColor(Color.BLUE);
        headerRow.createCell(50, "Avg Time (ns)").setTextColor(Color.BLUE);
        
        // Data rows
        String[][] results = {
            {"Sanitization", String.valueOf(sanitizeTime / iterations)},
            {"Width calculation", String.valueOf(widthTime / iterations)},
            {"Truncation", String.valueOf(truncateTime / iterations)},
            {"Text wrapping", String.valueOf(wrapTime / iterations)}
        };
        
        for (String[] result : results) {
            Row<PDPage> row = table.createRow(15f);
            row.createCell(50, result[0]);
            row.createCell(50, result[1]);
        }
        
        table.draw();
        
        File outputFile = new File("target/PerformanceTestReport.pdf");
        outputFile.getParentFile().mkdirs();
        document.save(outputFile);
        
        System.out.println("Performance test report saved to: " + outputFile.getAbsolutePath());
    }
}