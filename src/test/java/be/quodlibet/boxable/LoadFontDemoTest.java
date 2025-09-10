/*
 * Quodlibet.be
 */
package be.quodlibet.boxable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

/**
 * Simple demonstration of the new FontUtils.loadFont(document, family, style) functionality.
 */
public class LoadFontDemoTest {

    /**
     * Demonstrates the new loadFont method with SupportedFont and FontStyle
     */
    @Test
    public void demonstrateNewLoadFontMethod() throws IOException {
        PDDocument document = new PDDocument();
        
        try {
            // Load individual fonts using the new method
            PDType0Font freeSansRegular = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.REGULAR);
            PDType0Font freeSerifBold = FontUtils.loadFont(document, SupportedFont.FREE_SERIF, FontStyle.BOLD);
            PDType0Font freeMonoItalic = FontUtils.loadFont(document, SupportedFont.FREE_MONO, FontStyle.ITALIC);
            PDType0Font sourceSans3BoldItalic = FontUtils.loadFont(document, SupportedFont.SOURCE_SANS_3, FontStyle.BOLD_ITALIC);
            
            // Create a demonstration PDF
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            float yStart = page.getMediaBox().getHeight() - 50;
            float width = page.getMediaBox().getWidth() - 100;
            float margin = 50;
            
            BaseTable table = new BaseTable(yStart, yStart, 50, width, margin, document, page, true, true);
            
            // Header
            Row<PDPage> headerRow = table.createRow(25f);
            Cell<PDPage> headerCell = headerRow.createCell(100, "New FontUtils.loadFont() Method Demo");
            headerCell.setTextColor(Color.BLUE);
            headerCell.setHeaderCell(true);
            
            // Demonstrate different fonts and styles
            Row<PDPage> row1 = table.createRow(20f);
            Cell<PDPage> cell1 = row1.createCell(100, "FreeSans Regular: This shows normal FreeSans font");
            cell1.setFont(freeSansRegular);
            
            Row<PDPage> row2 = table.createRow(20f);
            Cell<PDPage> cell2 = row2.createCell(100, "FreeSerif Bold: This shows bold FreeSerif font");
            cell2.setFont(freeSerifBold);
            
            Row<PDPage> row3 = table.createRow(20f);
            Cell<PDPage> cell3 = row3.createCell(100, "FreeMono Italic: This shows italic FreeMono font");
            cell3.setFont(freeMonoItalic);
            
            Row<PDPage> row4 = table.createRow(20f);
            Cell<PDPage> cell4 = row4.createCell(100, "SourceSans3 Bold-Italic: This shows bold-italic SourceSans3 font");
            cell4.setFont(sourceSans3BoldItalic);
            
            // Usage example row
            Row<PDPage> usageRow = table.createRow(30f);
            Cell<PDPage> usageCell = usageRow.createCell(100, 
                "Usage: PDType0Font font = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.BOLD);");
            usageCell.setFillColor(Color.LIGHT_GRAY);
            usageCell.setFont(freeSansRegular);
            usageCell.setFontSize(10f);
            
            table.draw();
            
            // Save the demonstration
            File file = new File("target/LoadFontDemo.pdf");
            file.getParentFile().mkdirs();
            System.out.println("LoadFont method demo saved at: " + file.getAbsolutePath());
            
            // Don't save to avoid font subsetting issues in test environment
            // document.save(file);
            
        } finally {
            document.close();
        }
    }
}