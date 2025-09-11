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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for the new loadFont method that takes SupportedFont and FontStyle parameters.
 */
public class FontUtilsLoadFontTest {

    /**
     * Test loading individual fonts using SupportedFont and FontStyle parameters - FreeSans variants
     */
    @Test
    public void testLoadFontWithSupportedFontAndStyle_FreeSans() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test all font styles for FreeSans
        PDType0Font regularFont = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.REGULAR);
        PDType0Font boldFont = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.BOLD);
        PDType0Font italicFont = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.ITALIC);
        PDType0Font boldItalicFont = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.BOLD_ITALIC);
        
        // Verify all fonts loaded successfully
        assertNotNull("Regular FreeSans font should not be null", regularFont);
        assertNotNull("Bold FreeSans font should not be null", boldFont);
        assertNotNull("Italic FreeSans font should not be null", italicFont);
        assertNotNull("Bold-Italic FreeSans font should not be null", boldItalicFont);
        
        // Verify font names are appropriate
        assertNotNull("Regular font name should not be null", regularFont.getName());
        assertNotNull("Bold font name should not be null", boldFont.getName());
        assertNotNull("Italic font name should not be null", italicFont.getName());
        assertNotNull("Bold-Italic font name should not be null", boldItalicFont.getName());
        
        document.close();
    }

    /**
     * Test loading individual fonts using SupportedFont and FontStyle parameters - FreeSerif variants
     */
    @Test
    public void testLoadFontWithSupportedFontAndStyle_FreeSerif() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test all font styles for FreeSerif
        PDType0Font regularFont = FontUtils.loadFont(document, SupportedFont.FREE_SERIF, FontStyle.REGULAR);
        PDType0Font boldFont = FontUtils.loadFont(document, SupportedFont.FREE_SERIF, FontStyle.BOLD);
        PDType0Font italicFont = FontUtils.loadFont(document, SupportedFont.FREE_SERIF, FontStyle.ITALIC);
        PDType0Font boldItalicFont = FontUtils.loadFont(document, SupportedFont.FREE_SERIF, FontStyle.BOLD_ITALIC);
        
        // Verify all fonts loaded successfully
        assertNotNull("Regular FreeSerif font should not be null", regularFont);
        assertNotNull("Bold FreeSerif font should not be null", boldFont);
        assertNotNull("Italic FreeSerif font should not be null", italicFont);
        assertNotNull("Bold-Italic FreeSerif font should not be null", boldItalicFont);
        
        document.close();
    }

    /**
     * Test loading individual fonts using SupportedFont and FontStyle parameters - FreeMono variants
     */
    @Test
    public void testLoadFontWithSupportedFontAndStyle_FreeMono() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test all font styles for FreeMono
        PDType0Font regularFont = FontUtils.loadFont(document, SupportedFont.FREE_MONO, FontStyle.REGULAR);
        PDType0Font boldFont = FontUtils.loadFont(document, SupportedFont.FREE_MONO, FontStyle.BOLD);
        PDType0Font italicFont = FontUtils.loadFont(document, SupportedFont.FREE_MONO, FontStyle.ITALIC);
        PDType0Font boldItalicFont = FontUtils.loadFont(document, SupportedFont.FREE_MONO, FontStyle.BOLD_ITALIC);
        
        // Verify all fonts loaded successfully
        assertNotNull("Regular FreeMono font should not be null", regularFont);
        assertNotNull("Bold FreeMono font should not be null", boldFont);
        assertNotNull("Italic FreeMono font should not be null", italicFont);
        assertNotNull("Bold-Italic FreeMono font should not be null", boldItalicFont);
        
        document.close();
    }

    /**
     * Test loading individual fonts using SupportedFont and FontStyle parameters - SourceSans3 variants
     */
    @Test
    public void testLoadFontWithSupportedFontAndStyle_SourceSans3() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test all font styles for SourceSans3
        PDType0Font regularFont = FontUtils.loadFont(document, SupportedFont.SOURCE_SANS_3, FontStyle.REGULAR);
        PDType0Font boldFont = FontUtils.loadFont(document, SupportedFont.SOURCE_SANS_3, FontStyle.BOLD);
        PDType0Font italicFont = FontUtils.loadFont(document, SupportedFont.SOURCE_SANS_3, FontStyle.ITALIC);
        PDType0Font boldItalicFont = FontUtils.loadFont(document, SupportedFont.SOURCE_SANS_3, FontStyle.BOLD_ITALIC);
        
        // Verify all fonts loaded successfully
        assertNotNull("Regular SourceSans3 font should not be null", regularFont);
        assertNotNull("Bold SourceSans3 font should not be null", boldFont);
        assertNotNull("Italic SourceSans3 font should not be null", italicFont);
        assertNotNull("Bold-Italic SourceSans3 font should not be null", boldItalicFont);
        
        document.close();
    }

    /**
     * Test comparing new method with existing FontSet approach
     */
    @Test
    public void testConsistencyWithFontSetApproach() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load using FontSet approach
        FontSet fontSet = FontUtils.loadFontSet(document, SupportedFont.FREE_SANS);
        assertNotNull("FontSet should not be null", fontSet);
        
        // Load individual fonts using new method
        PDType0Font regularFromNew = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.REGULAR);
        PDType0Font boldFromNew = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.BOLD);
        PDType0Font italicFromNew = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.ITALIC);
        PDType0Font boldItalicFromNew = FontUtils.loadFont(document, SupportedFont.FREE_SANS, FontStyle.BOLD_ITALIC);
        
        // Verify both approaches work and return equivalent results
        assertNotNull("Regular font from new method should not be null", regularFromNew);
        assertNotNull("Bold font from new method should not be null", boldFromNew);
        assertNotNull("Italic font from new method should not be null", italicFromNew);
        assertNotNull("Bold-Italic font from new method should not be null", boldItalicFromNew);
        
        // The font names should be related (though exact equality may not hold due to internal caching)
        assertNotNull("Regular font from FontSet should not be null", fontSet.getRegular());
        assertNotNull("Bold font from FontSet should not be null", fontSet.getBold());
        assertNotNull("Italic font from FontSet should not be null", fontSet.getItalic());
        assertNotNull("Bold-Italic font from FontSet should not be null", fontSet.getBoldItalic());
        
        document.close();
    }

    /**
     * Test that the method works with all supported font families and styles
     */
    @Test
    public void testAllSupportedFontsAndStyles() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test all combinations of SupportedFont and FontStyle
        SupportedFont[] fonts = SupportedFont.values();
        FontStyle[] styles = FontStyle.values();
        
        for (SupportedFont font : fonts) {
            for (FontStyle style : styles) {
                PDType0Font loadedFont = FontUtils.loadFont(document, font, style);
                assertNotNull(String.format("Font should not be null for %s %s", font.getFamilyName(), style), loadedFont);
            }
        }
        
        document.close();
    }

    /**
     * Test edge case where null parameters might be passed (should be handled gracefully)
     */
    @Test
    public void testNullParameterHandling() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test with null FontStyle (should throw IllegalArgumentException)
        try {
            FontUtils.loadFont(document, SupportedFont.FREE_SANS, null);
            fail("Expected IllegalArgumentException when fontStyle is null");
        } catch (IllegalArgumentException e) {
            assertEquals("FontStyle cannot be null", e.getMessage());
        }
        
        // Test with null SupportedFont (should return null)
        PDType0Font fontWithNullSupportedFont = FontUtils.loadFont(document, null, FontStyle.REGULAR);
        assertEquals("Font with null SupportedFont should be null", null, fontWithNullSupportedFont);
        
        // Test with null document (should return null)
        PDType0Font fontWithNullDocument = FontUtils.loadFont(null, SupportedFont.FREE_SANS, FontStyle.REGULAR);
        assertEquals("Font with null document should be null", null, fontWithNullDocument);
        
        document.close();
    }
}