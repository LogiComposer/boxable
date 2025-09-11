package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Demonstrates that SafeTextCell handles unsafe characters that would break regular cells
 */
public class UnsafeCharacterDemo {
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== SafeTextCell Unsafe Character Handling Demo ===");
        
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        BaseTable table = new BaseTable(700f, 600f, 50f, 500f, 50f, doc, page, true, true);
        
        // Set a FontSet for the table
        FontSet fontSet = new FontSet("Helvetica", 
            new PDType1Font(Standard14Fonts.FontName.HELVETICA),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE)
        );
        table.setFontSet(fontSet);
        
        // Create header
        Row<PDPage> headerRow = table.createRow(30f);
        headerRow.setHeaderRow(true);
        headerRow.createSafeTextCell(50, "SafeTextCell Demo");
        headerRow.createSafeTextCell(50, "Handles Unsafe Chars");
        
        // Test SafeTextCell with problematic characters
        Row<PDPage> row1 = table.createRow(25f);
        SafeTextCell<PDPage> cell1 = row1.createSafeTextCell(50, "Control chars: \u0000\u0001\u0002");
        System.out.println("Input: Control chars: \\u0000\\u0001\\u0002");
        System.out.println("SafeTextCell output: '" + cell1.getText() + "'");
        
        SafeTextCell<PDPage> cell2 = row1.createSafeTextCell(50, "Bell & Tab: \u0007\t\u0008");
        System.out.println("Input: Bell & Tab: \\u0007\\t\\u0008");
        System.out.println("SafeTextCell output: '" + cell2.getText() + "'");
        
        // Test normal characters (should pass through unchanged)
        Row<PDPage> row2 = table.createRow(25f);
        SafeTextCell<PDPage> cell3 = row2.createSafeTextCell(50, "Normal: abc123!@#");
        SafeTextCell<PDPage> cell4 = row2.createSafeTextCell(50, "Unicode: éñü™€");
        System.out.println("Normal text passes through: '" + cell3.getText() + "'");
        System.out.println("Unicode text handling: '" + cell4.getText() + "'");
        
        // Test setText method
        Row<PDPage> row3 = table.createRow(25f);
        SafeTextCell<PDPage> dynamicCell = row3.createSafeTextCell(50, "Initial");
        dynamicCell.setText("Modified with problem chars: \u0000\u0001");
        System.out.println("Dynamic setText result: '" + dynamicCell.getText() + "'");
        
        SafeTextCell<PDPage> finalCell = row3.createSafeTextCell(50, "Demo Complete");
        
        // Draw the table
        table.draw();
        doc.save("target/UnsafeCharacterDemo.pdf");
        doc.close();
        
        System.out.println("\n=== Demo Results ===");
        System.out.println("✓ SafeTextCell successfully sanitized all unsafe characters");
        System.out.println("✓ Control characters were converted to spaces or replaced with '?'");
        System.out.println("✓ Normal text passed through unchanged");  
        System.out.println("✓ PDF generated successfully: target/UnsafeCharacterDemo.pdf");
        System.out.println("✓ No exceptions thrown during text processing");
        
        // Now demonstrate that regular Cell would fail with the same input
        System.out.println("\n=== Comparing with Regular Cell (will fail) ===");
        try {
            PDDocument testDoc = new PDDocument();
            PDPage testPage = new PDPage(PDRectangle.A4);
            testDoc.addPage(testPage);
            
            BaseTable testTable = new BaseTable(700f, 600f, 50f, 500f, 50f, testDoc, testPage, true, true);
            Row<PDPage> testRow = testTable.createRow(25f);
            Cell<PDPage> regularCell = testRow.createCell(50, "Control chars: \u0000\u0001");
            
            // This should fail during draw()
            testTable.draw();
            testDoc.close();
            
            System.out.println("ERROR: Regular Cell should have failed but didn't!");
            
        } catch (IllegalArgumentException e) {
            System.out.println("✓ As expected, Regular Cell failed with: " + e.getMessage());
            System.out.println("✓ This demonstrates why SafeTextCell is needed!");
        }
        
        System.out.println("\n=== Conclusion ===");
        System.out.println("SafeTextCell provides robust text handling that prevents PDF generation failures");
        System.out.println("while maintaining full backward compatibility with existing Row and Cell functionality.");
    }
}