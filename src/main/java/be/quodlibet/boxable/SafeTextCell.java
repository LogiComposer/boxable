package be.quodlibet.boxable;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import be.quodlibet.boxable.utils.FontUtils;
import be.quodlibet.boxable.utils.PDFontTextAdapter;

/**
 * SafeTextCell
 * ------------
 * A Boxable Cell that automatically sanitizes text
 * using PDFontTextAdapter (unsupported characters → '?').
 */
public class SafeTextCell<T extends PDPage> extends Cell<T> {

    private final PDFontTextAdapter fontAdapter;

    /**
     * <p>
     * Constructs a SafeTextCell with the default alignment
     * {@link VerticalAlignment#TOP} {@link HorizontalAlignment#LEFT}.
     * </p>
     *
     * @param row
     * @param width
     * @param text
     * @param isCalculated
     * @see SafeTextCell#SafeTextCell(Row, float, String, boolean, HorizontalAlignment,
     *      VerticalAlignment)
     */
    SafeTextCell(Row<T> row, float width, String text, boolean isCalculated) {
        this(row, width, text, isCalculated, HorizontalAlignment.LEFT, VerticalAlignment.TOP);
    }

    /**
     * <p>
     * Constructs a SafeTextCell.
     * </p>
     *
     * @param row
     *            The parent row
     * @param width
     *            absolute width in points or in % of table width (depending on
     *            the parameter {@code isCalculated})
     * @param text
     *            The text content of the cell
     * @param isCalculated
     *            If {@code true}, the width is interpreted in % to the table
     *            width
     * @param align
     *            The {@link HorizontalAlignment} of the cell content
     * @param valign
     *            The {@link VerticalAlignment} of the cell content
     * @see SafeTextCell#SafeTextCell(Row, float, String, boolean)
     */
    SafeTextCell(Row<T> row, float width, String text, boolean isCalculated, HorizontalAlignment align,
            VerticalAlignment valign) {
        super(row, width, null, isCalculated, align, valign); // Pass null text to avoid double processing
        
        // Get font from table's fontSet if available, otherwise use defaults
        PDFont primaryFont = getSelectedFont();
        this.fontAdapter = new PDFontTextAdapter(primaryFont);
        
        // Now set the text (which will be sanitized)
        setText(text);
    }

    /**
     * Gets the appropriate font for this cell from the table's FontSet or defaults
     */
    private PDFont getSelectedFont() {
        Table<T> table = getRow().getTable();
        
        // Try to get font from table's FontSet first
        if (table != null && table.getFontSet() != null) {
            if (isHeaderCell()) {
                return table.getFontSet().getBold();
            } else {
                return table.getFontSet().getRegular();
            }
        }
        
        // Fall back to FontUtils defaults if available
        if (!FontUtils.getDefaultfonts().isEmpty()) {
            if (isHeaderCell()) {
                return FontUtils.getDefaultfonts().get("fontBold");
            } else {
                return FontUtils.getDefaultfonts().get("font");
            }
        }
        
        // Final fallback to built-in fonts
        if (isHeaderCell()) {
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        } else {
            return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
    }

    @Override
    public void setText(String text) {
        // Always sanitize text before setting
        String sanitized = fontAdapter.sanitizeText(text);
        super.setText(sanitized);
    }

    /**
     * Gets the PDFontTextAdapter used by this cell
     * 
     * @return The font adapter instance
     */
    public PDFontTextAdapter getFontAdapter() {
        return fontAdapter;
    }
}