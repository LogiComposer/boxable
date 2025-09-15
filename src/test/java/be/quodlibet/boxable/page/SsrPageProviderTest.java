package be.quodlibet.boxable.page;

import be.quodlibet.boxable.PageFooterDetails;
import be.quodlibet.boxable.ReportException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SsrPageProviderTest {

    private PDDocument document;
    private SsrPageProvider provider;

    @Before
    public void setUp() {
        document = new PDDocument();
        provider = new SsrPageProvider(document, PDRectangle.A4);
    }

    @After
    public void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }

    @Test
    public void testCreatePage() {
        PDPage page = provider.createPage();
        assertNotNull(page);
        assertEquals(1, document.getNumberOfPages());
    }

    @Test
    public void testNextPage() {
        PDPage firstPage = provider.nextPage();
        assertNotNull(firstPage);
        assertEquals(1, document.getNumberOfPages());

        PDPage secondPage = provider.nextPage();
        assertNotNull(secondPage);
        assertEquals(2, document.getNumberOfPages());
        assertNotSame(firstPage, secondPage);
    }

    @Test
    public void testPreviousPage() {
        // Create a page first
        provider.nextPage();
        provider.nextPage();
        assertEquals(2, document.getNumberOfPages());

        PDPage previousPage = provider.previousPage();
        assertNotNull(previousPage);
        // Should still have 2 pages, just navigated to previous
        assertEquals(2, document.getNumberOfPages());
    }

    @Test
    public void testGetDocument() {
        assertEquals(document, provider.getDocument());
    }

    @Test
    public void testAppendFooter() throws ReportException {
        PDPage page = provider.createPage();
        // This should not throw an exception
        provider.appendFooter(page);
    }

    @Test(expected = ReportException.class)
    public void testAppendFooterWithNullPage() throws ReportException {
        provider.appendFooter(null);
    }

    @Test
    public void testStaticAppendPageFooter() throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PageFooterDetails details = PageFooterDetails.createDefault();

        // This should not throw an exception
        SsrPageProvider.appendPageFooter(document, page, new PDType1Font(Standard14Fonts.FontName.HELVETICA), details);
    }

    @Test
    public void testStaticAppendPageNumbers() throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        // This should not throw an exception
        SsrPageProvider.appendPageNumbers(document, page, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDocument() {
        new SsrPageProvider(null, PDRectangle.A4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullPageSize() {
        new SsrPageProvider(document, null);
    }
}