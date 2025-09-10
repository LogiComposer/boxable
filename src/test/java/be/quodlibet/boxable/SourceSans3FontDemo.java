package be.quodlibet.boxable;

import be.quodlibet.boxable.utils.FontUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

/**
 * Demo test showing how to use Google Source Sans 3 fonts with Boxable.
 * This creates a PDF demonstrating all font variants.
 */
public class SourceSans3FontDemo {

    @Test
    public void createSourceSans3FontDemo() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // IMPORTANT: Set Source Sans 3 fonts as default BEFORE creating tables
            FontUtils.setSourceSans3FontsAsDefault(doc);
            
            PDPage page = new PDPage();
            doc.addPage(page);
            
            // Create table with Source Sans 3 fonts
            float margin = 20;
            float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
            float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
            float yStart = yStartNewPage;
            float bottomMargin = 50;
            
            BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true, true);
            
            // Header demonstrating custom fonts work automatically
            Row<PDPage> headerRow = table.createRow(25f);
            Cell<PDPage> headerCell = headerRow.createCell(100, "Google Source Sans 3 Font Demo");
            headerCell.setHeaderCell(true); // Uses bold font automatically
            headerCell.setFillColor(Color.DARK_GRAY);
            headerCell.setTextColor(Color.WHITE);
            headerCell.setFontSize(14);
            table.addHeaderRow(headerRow);
            
            // Font variants row
            Row<PDPage> variantsRow = table.createRow(40f);
            
            Cell<PDPage> regularCell = variantsRow.createCell(25, "Regular Font: This text uses Source Sans 3 Regular variant.");
            regularCell.setFontSize(10);
            
            Cell<PDPage> boldCell = variantsRow.createCell(25, "<b>Bold Font: This text uses Source Sans 3 Bold variant.</b>");
            boldCell.setFontSize(10);
            
            Cell<PDPage> italicCell = variantsRow.createCell(25, "<i>Italic Font: This text uses Source Sans 3 Italic variant.</i>");
            italicCell.setFontSize(10);
            
            Cell<PDPage> boldItalicCell = variantsRow.createCell(25, "<b><i>Bold Italic: This text uses Source Sans 3 Bold Italic variant.</i></b>");
            boldItalicCell.setFontSize(10);
            
            // Special characters row  
            Row<PDPage> specialRow = table.createRow(30f);
            Cell<PDPage> specialCell = specialRow.createCell(100, 
                "Special Characters: À Á Â Ã Ä Å Æ Ç È É Ê Ë Ì Í Î Ï Ð Ñ Ò Ó Ô Õ Ö × Ø Ù Ú Û Ü Ý Þ ß € £ ¥ © ® ™");
            specialCell.setFontSize(9);
            specialCell.setFillColor(Color.LIGHT_GRAY);
            
            // Mixed formatting row
            Row<PDPage> mixedRow = table.createRow(50f);
            Cell<PDPage> mixedCell = mixedRow.createCell(100, 
                "<p><b>Google Source Sans 3</b> provides excellent readability and supports a wide range of characters. " +
                "This demo shows <i>all four variants</i> working seamlessly with Boxable's existing architecture.</p>" +
                "<p>Key benefits:</p>" +
                "<ul>" +
                "<li><b>Better special character support</b></li>" +
                "<li><i>Professional typography</i></li>" +
                "<li>Cached font loading (loaded only once)</li>" +
                "<li><b><i>No breaking changes to existing code</i></b></li>" +
                "</ul>");
            mixedCell.setFontSize(9);
            
            table.draw();
            
            // Save the demo PDF
            File file = new File("target/GoogleSourceSans3-Demo.pdf");
            file.getParentFile().mkdirs();
            doc.save(file);
            
            System.out.println("Google Source Sans 3 demo PDF created at: " + file.getAbsolutePath());
            System.out.println("This demonstrates all four font variants working with the Boxable library.");
            
        } finally {
            doc.close();
        }
    }
}