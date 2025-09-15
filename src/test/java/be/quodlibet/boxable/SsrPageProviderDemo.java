package be.quodlibet.boxable;

import be.quodlibet.boxable.page.SsrPageProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SsrPageProviderDemo {

    public static void main(String[] args) {
        try {
            // Create document with SsrPageProvider
            PDDocument document = new PDDocument();
            
            // Create custom footer details
            PageFooterDetails footerDetails = new PageFooterDetails.Builder()
                    .withPageNumbers(true)
                    .withDate(true)
                    .withDateFormatter("yyyy-MM-dd HH:mm:ss")
                    .withTrademarkText("© 2023 SsrPageProvider Demo - Boxable Library")
                    .withFontSize(8f)
                    .withBottomMargin(25f)
                    .build();

            SsrPageProvider pageProvider = new SsrPageProvider(
                    document, 
                    PDRectangle.A4, 
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 
                    footerDetails
            );

            // Create table with the SsrPageProvider
            BaseTable table = new BaseTable(
                    750f, // yStart
                    750f, // yStartNewPage
                    10f,  // pageTopMargin
                    50f,  // pageBottomMargin
                    500f, // width
                    50f,  // margin
                    document,
                    pageProvider.nextPage(),
                    true, // drawLines
                    true, // drawContent
                    pageProvider
            );

            // Add table title
            table.addHeaderRow(createHeaderRow(table));

            // Add many rows to force page breaks and demonstrate footer functionality
            for (int i = 1; i <= 40; i++) {
                Row<PDPage> row = table.createRow(18f);
                Cell<PDPage> cell1 = row.createCell(25f, "Item " + i);
                Cell<PDPage> cell2 = row.createCell(25f, "Description for item " + i);
                Cell<PDPage> cell3 = row.createCell(25f, "Category " + ((i % 5) + 1));
                Cell<PDPage> cell4 = row.createCell(25f, "$" + String.format("%.2f", 10.0 + (i * 2.5)));
                
                // Alternate row colors
                if (i % 2 == 0) {
                    cell1.setFillColor(new Color(240, 240, 240));
                    cell2.setFillColor(new Color(240, 240, 240));
                    cell3.setFillColor(new Color(240, 240, 240));
                    cell4.setFillColor(new Color(240, 240, 240));
                }
            }

            // Draw the table
            table.draw();

            // Save the document
            File outputFile = new File("target/SsrPageProviderDemo.pdf");
            document.save(outputFile);
            document.close();

            System.out.println("SsrPageProvider demonstration PDF created successfully: " + outputFile.getAbsolutePath());
            System.out.println("Document contains " + outputFile.length() + " bytes");
            System.out.println("This PDF demonstrates:");
            System.out.println("- Automatic page footer generation");
            System.out.println("- Page numbering");
            System.out.println("- Date/time stamps");
            System.out.println("- Copyright/trademark text");
            System.out.println("- Multi-page table with footers");

        } catch (IOException e) {
            System.err.println("Error creating SsrPageProvider demonstration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Row<PDPage> createHeaderRow(BaseTable table) {
        Row<PDPage> headerRow = table.createRow(20f);
        Cell<PDPage> headerCell1 = headerRow.createCell(25f, "Item");
        Cell<PDPage> headerCell2 = headerRow.createCell(25f, "Description");
        Cell<PDPage> headerCell3 = headerRow.createCell(25f, "Category");
        Cell<PDPage> headerCell4 = headerRow.createCell(25f, "Price");
        
        // Style header cells
        headerCell1.setFillColor(new Color(100, 150, 200));
        headerCell2.setFillColor(new Color(100, 150, 200));
        headerCell3.setFillColor(new Color(100, 150, 200));
        headerCell4.setFillColor(new Color(100, 150, 200));
        
        headerCell1.setTextColor(Color.WHITE);
        headerCell2.setTextColor(Color.WHITE);
        headerCell3.setTextColor(Color.WHITE);
        headerCell4.setTextColor(Color.WHITE);
        
        return headerRow;
    }
}