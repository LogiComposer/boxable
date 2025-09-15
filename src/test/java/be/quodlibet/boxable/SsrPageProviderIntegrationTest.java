package be.quodlibet.boxable;

import be.quodlibet.boxable.page.SsrPageProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class SsrPageProviderIntegrationTest {

    private PDDocument document;
    private File outputFile;

    @Before
    public void setUp() {
        document = new PDDocument();
        outputFile = new File("target/SsrPageProviderIntegrationTest.pdf");
        outputFile.deleteOnExit();
    }

    @After
    public void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }

    @Test
    public void testSsrPageProviderWithTable() throws IOException {
        // Create SsrPageProvider with custom footer details
        PageFooterDetails footerDetails = new PageFooterDetails.Builder()
                .withPageNumbers(true)
                .withDate(true)
                .withTrademarkText("© 2023 Test Company")
                .withFontSize(8f)
                .build();

        SsrPageProvider pageProvider = new SsrPageProvider(
                document, 
                PDRectangle.A4, 
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 
                footerDetails
        );

        // Create table with SsrPageProvider
        BaseTable table = new BaseTable(
                500f, // yStart
                500f, // yStartNewPage
                0f,   // pageTopMargin
                50f,  // pageBottomMargin
                500f, // width
                50f,  // margin
                document,
                pageProvider.nextPage(),
                true, // drawLines
                true, // drawContent
                pageProvider
        );

        // Add rows to force page breaks and test footer functionality
        for (int i = 0; i < 30; i++) {
            Row<PDPage> row = table.createRow(20f);
            Cell<PDPage> cell1 = row.createCell(25f, "Row " + (i + 1) + " - Cell 1");
            Cell<PDPage> cell2 = row.createCell(25f, "Row " + (i + 1) + " - Cell 2");
            Cell<PDPage> cell3 = row.createCell(25f, "Row " + (i + 1) + " - Cell 3");
            Cell<PDPage> cell4 = row.createCell(25f, "Row " + (i + 1) + " - Cell 4");
            
            cell1.setFillColor(Color.LIGHT_GRAY);
            cell2.setFillColor(Color.WHITE);
            cell3.setFillColor(Color.LIGHT_GRAY);
            cell4.setFillColor(Color.WHITE);
        }

        // Draw the table - this should trigger footer additions
        table.draw();

        // Verify that multiple pages were created
        assertTrue("Should have multiple pages", document.getNumberOfPages() > 1);

        // Save the document to verify it can be created successfully
        document.save(outputFile);
        assertTrue("Output file should exist", outputFile.exists());
        assertTrue("Output file should not be empty", outputFile.length() > 0);

        System.out.println("Integration test PDF created: " + outputFile.getAbsolutePath());
    }

    @Test
    public void testSsrPageProviderStaticMethods() throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        // Test static page numbers method
        SsrPageProvider.appendPageNumbers(document, page, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);

        // Test static footer method with custom details
        PageFooterDetails details = new PageFooterDetails.Builder()
                .withPageNumbers(true)
                .withDate(true)
                .withDateFormatter("MM/dd/yyyy")
                .build();

        SsrPageProvider.appendPageFooter(document, page, new PDType1Font(Standard14Fonts.FontName.HELVETICA), details);

        // Save to verify no errors
        document.save("target/SsrPageProviderStaticMethodTest.pdf");
    }

    @Test
    public void testPageFooterDetails() {
        // Test builder pattern
        PageFooterDetails details = new PageFooterDetails.Builder()
                .withPageNumbers(false)
                .withDate(true)
                .withTrademarkText("Test Trademark")
                .withFontSize(12f)
                .withBottomMargin(30f)
                .build();

        assertFalse(details.isIncludePageNumbers());
        assertTrue(details.isIncludeDate());
        assertEquals("Test Trademark", details.getTrademarkText());
        assertEquals(12f, details.getFontSize(), 0.001f);
        assertEquals(30f, details.getBottomMargin(), 0.001f);

        // Test date formatting
        assertNotNull(details.getFormattedDate());
        assertFalse(details.getFormattedDate().isEmpty());
    }
}