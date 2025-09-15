package be.quodlibet.boxable.page;

import be.quodlibet.boxable.PageFooterDetails;
import be.quodlibet.boxable.ReportException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;

/**
 * SSR (Server-Side Rendering) Page Provider that manages pages in a PDF document
 * and provides functionality to add footers with page numbers and dates.
 */
public class SsrPageProvider implements PageProvider<PDPage> {

    private final PDDocument document;
    private final PDRectangle pageSize;
    private final PDFont footerFont;
    private final PageFooterDetails footerDetails;
    private int currentPageIndex = -1;

    /**
     * Creates a new SsrPageProvider with default settings.
     *
     * @param document the PDF document
     * @param pageSize the page size to use for new pages
     */
    public SsrPageProvider(PDDocument document, PDRectangle pageSize) {
        this(document, pageSize, new PDType1Font(Standard14Fonts.FontName.HELVETICA), PageFooterDetails.createDefault());
    }

    /**
     * Creates a new SsrPageProvider with custom font and footer details.
     *
     * @param document the PDF document
     * @param pageSize the page size to use for new pages
     * @param footerFont the font to use for footer text
     * @param footerDetails the footer configuration details
     */
    public SsrPageProvider(PDDocument document, PDRectangle pageSize, PDFont footerFont, PageFooterDetails footerDetails) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (pageSize == null) {
            throw new IllegalArgumentException("Page size cannot be null");
        }
        if (footerFont == null) {
            throw new IllegalArgumentException("Footer font cannot be null");
        }
        if (footerDetails == null) {
            throw new IllegalArgumentException("Footer details cannot be null");
        }
        
        this.document = document;
        this.pageSize = pageSize;
        this.footerFont = footerFont;
        this.footerDetails = footerDetails;
    }

    @Override
    public PDDocument getDocument() {
        return document;
    }

    @Override
    public PDPage createPage() {
        currentPageIndex = document.getNumberOfPages();
        PDPage page = getCurrentPage();
        
        try {
            appendFooter(page);
        } catch (ReportException e) {
            // Log error but don't fail page creation
            // In a real implementation, you might want to use a proper logger
            System.err.println("Failed to append footer to page: " + e.getMessage());
        }
        
        return page;
    }

    @Override
    public PDPage nextPage() {
        if (currentPageIndex == -1) {
            currentPageIndex = document.getNumberOfPages();
        } else {
            currentPageIndex++;
        }

        PDPage page = getCurrentPage();
        
        try {
            appendFooter(page);
        } catch (ReportException e) {
            // Log error but don't fail page navigation
            System.err.println("Failed to append footer to page: " + e.getMessage());
        }
        
        return page;
    }

    @Override
    public PDPage previousPage() {
        currentPageIndex--;
        if (currentPageIndex < 0) {
            currentPageIndex = 0;
        }

        return getCurrentPage();
    }

    /**
     * Gets the current page, creating a new one if necessary.
     * 
     * @return the current page
     */
    private PDPage getCurrentPage() {
        if (currentPageIndex >= document.getNumberOfPages()) {
            PDPage newPage = new PDPage(pageSize);
            document.addPage(newPage);
            return newPage;
        }

        return document.getPage(currentPageIndex);
    }

    /**
     * Appends footer content to the specified page.
     * 
     * @param page the page to append the footer to
     * @throws ReportException if an error occurs while appending the footer
     */
    public void appendFooter(PDPage page) throws ReportException {
        if (page == null) {
            throw new ReportException("Page cannot be null");
        }

        try {
            appendPageFooter(document, page, footerFont, footerDetails);
        } catch (IOException e) {
            throw new ReportException("Failed to append footer to page", e);
        }
    }

    /**
     * Static utility method to append page numbers and footer details to a page.
     * 
     * @param document the PDF document containing the page
     * @param page the page to append the footer to
     * @param font the font to use for the footer text
     * @param footerDetails the footer configuration details
     * @throws IOException if an I/O error occurs
     */
    public static void appendPageFooter(PDDocument document, PDPage page, PDFont font, PageFooterDetails footerDetails) throws IOException {
        if (document == null || page == null || font == null || footerDetails == null) {
            throw new IllegalArgumentException("Document, page, font, and footer details cannot be null");
        }

        PDRectangle mediaBox = page.getMediaBox();
        float pageWidth = mediaBox.getWidth();
        float pageHeight = mediaBox.getHeight();
        
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true)) {
            
            contentStream.setFont(font, footerDetails.getFontSize());
            
            float yPosition = footerDetails.getBottomMargin();
            
            // Add page number if enabled
            if (footerDetails.isIncludePageNumbers()) {
                int pageNumber = document.getPages().indexOf(page) + 1;
                int totalPages = document.getNumberOfPages();
                String pageNumberText = "Page " + pageNumber + " of " + totalPages;
                
                float textWidth = font.getStringWidth(pageNumberText) / 1000 * footerDetails.getFontSize();
                float xPosition = pageWidth - textWidth - 20; // 20 point right margin
                
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, yPosition);
                contentStream.showText(pageNumberText);
                contentStream.endText();
            }
            
            // Add date if enabled
            if (footerDetails.isIncludeDate()) {
                String dateText = footerDetails.getFormattedDate();
                
                contentStream.beginText();
                contentStream.newLineAtOffset(20, yPosition); // 20 point left margin
                contentStream.showText(dateText);
                contentStream.endText();
            }
            
            // Add trademark text if present
            if (footerDetails.getTrademarkText() != null && !footerDetails.getTrademarkText().isEmpty()) {
                String trademarkText = footerDetails.getTrademarkText();
                float textWidth = font.getStringWidth(trademarkText) / 1000 * footerDetails.getFontSize();
                float xPosition = (pageWidth - textWidth) / 2; // Center the text
                
                float trademarkYPosition = yPosition - (footerDetails.getFontSize() + 2);
                
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, trademarkYPosition);
                contentStream.showText(trademarkText);
                contentStream.endText();
            }
        }
    }

    /**
     * Static utility method to append page numbers to a page.
     * 
     * @param document the PDF document containing the page
     * @param page the page to append the page number to
     * @param font the font to use for the page number
     * @param fontSize the font size to use
     * @throws IOException if an I/O error occurs
     */
    public static void appendPageNumbers(PDDocument document, PDPage page, PDFont font, float fontSize) throws IOException {
        PageFooterDetails details = new PageFooterDetails.Builder()
                .withPageNumbers(true)
                .withDate(false)
                .withFontSize(fontSize)
                .build();
        appendPageFooter(document, page, font, details);
    }
}