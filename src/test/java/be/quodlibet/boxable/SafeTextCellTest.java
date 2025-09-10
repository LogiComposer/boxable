package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for SafeTextCell functionality
 */
public class SafeTextCellTest {

    @Test
    public void testSafeTextCellCreation() throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        // Create a table
        BaseTable table = new BaseTable(500f, 400f, 50f, 500f, 50f, doc, page, true, true);
        
        // Create a row and add cells
        Row<PDPage> row = table.createRow(50f);
        
        // Test regular cell creation (backward compatibility)
        Cell<PDPage> regularCell = row.createCell(100f, "Regular Cell");
        assertNotNull("Regular cell should be created", regularCell);
        assertEquals("Regular cell text should match", "Regular Cell", regularCell.getText());
        
        // Test SafeTextCell creation
        SafeTextCell<PDPage> safeCell = row.createSafeTextCell(100f, "Safe Cell with unicode: éñ™");
        assertNotNull("SafeTextCell should be created", safeCell);
        assertNotNull("SafeTextCell should have fontAdapter", safeCell.getFontAdapter());
        
        // Test that unsafe characters are handled
        SafeTextCell<PDPage> unsafeCell = row.createSafeTextCell(100f, "Unsafe: \u0000\u0001");
        assertNotNull("SafeTextCell with unsafe chars should be created", unsafeCell);
        // The exact sanitization depends on font capabilities, but it should not crash
        
        doc.close();
    }
    
    @Test
    public void testSafeTextCellTextSanitization() throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        BaseTable table = new BaseTable(500f, 400f, 50f, 500f, 50f, doc, page, true, true);
        Row<PDPage> row = table.createRow(50f);
        
        // Create SafeTextCell and test setText method
        SafeTextCell<PDPage> cell = row.createSafeTextCell(100f, "Initial Text");
        
        // Test setting new text - should be sanitized
        cell.setText("New text with control chars: \u0000\u0001");
        
        // The text should be processed but not crash
        String resultText = cell.getText();
        assertNotNull("Text should not be null after sanitization", resultText);
        
        doc.close();
    }
    
    @Test
    public void testSafeTextCellWithHeaderRow() throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        BaseTable table = new BaseTable(500f, 400f, 50f, 500f, 50f, doc, page, true, true);
        
        // Create header row
        Row<PDPage> headerRow = table.createRow(50f);
        headerRow.setHeaderRow(true);
        
        // Create SafeTextCell in header row
        SafeTextCell<PDPage> headerCell = headerRow.createSafeTextCell(100f, "Header Cell");
        
        assertTrue("Header cell should be marked as header", headerCell.isHeaderCell());
        assertNotNull("Header cell should have fontAdapter", headerCell.getFontAdapter());
        
        doc.close();
    }
    
    @Test
    public void testSafeTextCellVariousConstructors() throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        BaseTable table = new BaseTable(500f, 400f, 50f, 500f, 50f, doc, page, true, true);
        Row<PDPage> row = table.createRow(50f);
        
        // Test different constructor variants
        SafeTextCell<PDPage> cell1 = row.createSafeTextCell(100f, "Test 1");
        SafeTextCell<PDPage> cell2 = row.createSafeTextCell(100f, "Test 2", HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
        
        assertNotNull("Cell1 should be created", cell1);
        assertNotNull("Cell2 should be created", cell2);
        assertEquals("Cell1 should have default alignment", HorizontalAlignment.LEFT, cell1.getAlign());
        assertEquals("Cell2 should have center alignment", HorizontalAlignment.CENTER, cell2.getAlign());
        assertEquals("Cell2 should have middle alignment", VerticalAlignment.MIDDLE, cell2.getValign());
        
        doc.close();
    }
    
    @Test
    public void testBackwardCompatibility() throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        BaseTable table = new BaseTable(500f, 400f, 50f, 500f, 50f, doc, page, true, true);
        Row<PDPage> row = table.createRow(50f);
        
        // Test that all existing methods still work
        Cell<PDPage> cell1 = row.createCell(100f, "Test Cell");
        Cell<PDPage> cell2 = row.createCell(100f, "Test Cell 2", HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM);
        
        assertNotNull("Regular cells should still work", cell1);
        assertNotNull("Regular cells with alignment should still work", cell2);
        assertEquals("Backward compatibility: text should be preserved", "Test Cell", cell1.getText());
        assertEquals("Backward compatibility: alignment should work", HorizontalAlignment.RIGHT, cell2.getAlign());
        
        doc.close();
    }
}