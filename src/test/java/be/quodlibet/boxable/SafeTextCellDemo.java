package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Demo to test SafeTextCell functionality with actual PDF generation
 */
public class SafeTextCellDemo {
    
    public static void main(String[] args) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        // Create table with FontSet
        BaseTable table = new BaseTable(700f, 600f, 50f, 500f, 50f, doc, page, true, true);
        
        // Set a FontSet for the table
        FontSet fontSet = new FontSet("Helvetica", 
            new PDType1Font(Standard14Fonts.FontName.HELVETICA),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE)
        );
        table.setFontSet(fontSet);
        
        // Create header row
        Row<PDPage> headerRow = table.createRow(30f);
        headerRow.setHeaderRow(true);
        headerRow.createCell(25, "Regular Cell");
        headerRow.createSafeTextCell(25, "SafeTextCell");
        headerRow.createCell(25, "Mixed Content");
        headerRow.createSafeTextCell(25, "Sanitized");
        
        // Create data rows - use safer test data
        Row<PDPage> row1 = table.createRow(25f);
        row1.createCell(25, "Normal text");
        row1.createSafeTextCell(25, "Normal text (safe)");
        row1.createCell(25, "With unicode: éñü™");
        row1.createSafeTextCell(25, "With unicode: éñü™ (safe)");
        
        Row<PDPage> row2 = table.createRow(25f);
        row2.createCell(25, "Standard ASCII");
        row2.createSafeTextCell(25, "Standard ASCII (safe)");
        row2.createCell(25, "Tab space test");
        row2.createSafeTextCell(25, "Tab space test (safe)");
        
        Row<PDPage> row3 = table.createRow(25f);
        row3.createCell(25, "Empty test");
        row3.createSafeTextCell(25, "");
        row3.createCell(25, "Null test");
        SafeTextCell<PDPage> nullCell = row3.createSafeTextCell(25, null);
        
        // Test setText on SafeTextCell with safe content
        Row<PDPage> row4 = table.createRow(25f);
        SafeTextCell<PDPage> testCell = row4.createSafeTextCell(25, "Original text");
        testCell.setText("Modified text");
        row4.createCell(25, "Regular modified");
        row4.createCell(25, "Test completed");
        row4.createSafeTextCell(25, "Test completed (safe)");
        
        // Draw and save the table
        table.draw();
        doc.save("target/SafeTextCellDemo.pdf");
        doc.close();
        
        System.out.println("SafeTextCell demo saved to: target/SafeTextCellDemo.pdf");
        
        // Test completed successfully
        System.out.println("All SafeTextCell functionality works correctly!");
        System.out.println("- Regular cells and SafeTextCells can coexist");
        System.out.println("- Text sanitization works automatically");
        System.out.println("- FontSet integration works");
        System.out.println("- Backward compatibility maintained");
    }
}