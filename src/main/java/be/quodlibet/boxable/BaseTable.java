package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import be.quodlibet.boxable.page.DefaultPageProvider;
import be.quodlibet.boxable.page.PageProvider;
import be.quodlibet.boxable.utils.FontTextCache;
import be.quodlibet.boxable.utils.FontUtils;

/**
 * Created by dgautier on 3/18/2015.
 */
public class BaseTable extends Table<PDPage> {

    private FontSet fontSet;
    
    // Document-level font text cache for shared performance optimization across all fonts
    private final FontTextCache fontTextCache = new FontTextCache();

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

    /**
     * Gets the document-level font text cache shared by all fonts in this table.
     * This enables performance optimization by sharing cached calculations across
     * different fonts and text operations within the same document.
     * 
     * @return The FontTextCache instance for this table
     */
    public FontTextCache getFontTextCache() {
        return fontTextCache;
    }
    
    /**
     * Clears the document-level font text cache to free memory.
     * This can be useful when processing large amounts of text or when memory usage becomes a concern.
     */
    public void clearFontTextCache() {
        fontTextCache.clearCaches();
    }

}
