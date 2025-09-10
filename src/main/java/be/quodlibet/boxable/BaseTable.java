package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import be.quodlibet.boxable.page.DefaultPageProvider;
import be.quodlibet.boxable.page.PageProvider;
import be.quodlibet.boxable.utils.FontUtils;

/**
 * Created by dgautier on 3/18/2015.
 */
public class BaseTable extends Table<PDPage> {

    private FontSet fontSet;

    public BaseTable(float yStart, float yStartNewPage, float bottomMargin, float width, float margin, PDDocument document, PDPage currentPage, boolean drawLines, boolean drawContent) throws IOException {
        super(yStart, yStartNewPage, 0, bottomMargin, width, margin, document, currentPage, drawLines, drawContent, new DefaultPageProvider(document, currentPage.getMediaBox()));
        this.fontSet = FontUtils.getDefaultFontSet();
    }
    
    public BaseTable(float yStart, float yStartNewPage, float pageTopMargin, float bottomMargin, float width, float margin, PDDocument document, PDPage currentPage, boolean drawLines, boolean drawContent) throws IOException {
        super(yStart, yStartNewPage, pageTopMargin, bottomMargin, width, margin, document, currentPage, drawLines, drawContent, new DefaultPageProvider(document, currentPage.getMediaBox()));
        this.fontSet = FontUtils.getDefaultFontSet();
    }
    
    public BaseTable(float yStart, float yStartNewPage, float pageTopMargin, float bottomMargin, float width, float margin, PDDocument document, PDPage currentPage, boolean drawLines, boolean drawContent, final PageProvider<PDPage> pageProvider) throws IOException {
        super(yStart, yStartNewPage, pageTopMargin, bottomMargin, width, margin, document, currentPage, drawLines, drawContent, pageProvider);
        this.fontSet = FontUtils.getDefaultFontSet();
    }

    @Override
    protected void loadFonts() {
        // Do nothing as we don't have any fonts to load
    }

    /**
     * Gets the current font set for this table.
     * 
     * @return The FontSet being used by this table
     */
    public FontSet getFontSet() {
        return fontSet;
    }

    /**
     * Sets the font set for this table. All rows, cells, and paragraphs
     * created after this call will use the specified font set.
     * 
     * @param fontSet The FontSet to use for this table
     */
    public void setFontSet(FontSet fontSet) {
        if (fontSet == null) {
            throw new IllegalArgumentException("FontSet cannot be null");
        }
        this.fontSet = fontSet;
    }

}
