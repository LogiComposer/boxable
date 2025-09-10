package be.quodlibet.boxable.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Utility methods for fonts
 * </p>
 * 
 * @author hstimac
 * @author mkuehne
 */
public final class FontUtils {

	private final static Logger logger = LoggerFactory.getLogger(FontUtils.class);

	private static final class FontMetrics {
		private final float ascent;

		private final float descent;

		private final float height;

		public FontMetrics(final float height, final float ascent, final float descent) {
			this.height = height;
			this.ascent = ascent;
			this.descent = descent;
		}
	}

	/**
	 * <p>
	 * {@link HashMap} for caching {@link FontMetrics} for designated
	 * {@link PDFont} because {@link FontUtils#getHeight(PDFont, float)} is
	 * expensive to calculate and the results are only approximate.
	 */
	private static final Map<String, FontMetrics> fontMetrics = new HashMap<>();

	/**
	 * <p>
	 * {@link HashMap} for caching font file data to avoid frequent loading from files.
	 * The key is the font path and the value is the font file data as byte array.
	 */
	private static final Map<String, byte[]> fontDataCache = new HashMap<>();

	private static final Map<String, PDFont> defaultFonts = new HashMap<>();

	private FontUtils() {
	}

	/**
	 * <p>
	 * Loads the {@link PDType0Font} to be embedded in the specified
	 * {@link PDDocument}. Font file data is cached to avoid frequent loading from files.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where fonts will be loaded
	 * @param fontPath
	 *            font path which will be loaded
	 * @return The read {@link PDType0Font}
	 */
	public static final PDType0Font loadFont(PDDocument document, String fontPath) {
		try {
			// Check if font data is already cached
			byte[] fontData = fontDataCache.get(fontPath);
			
			if (fontData == null) {
				// Load font data from file and cache it
				try (InputStream fontStream = FontUtils.class.getClassLoader().getResourceAsStream(fontPath)) {
					if (fontStream == null) {
						logger.warn("Cannot find font file: " + fontPath);
						return null;
					}
					
					fontData = readStreamToByteArray(fontStream);
					fontDataCache.put(fontPath, fontData);
					logger.debug("Cached font data for: " + fontPath);
				}
			} else {
				logger.debug("Using cached font data for: " + fontPath);
			}
			
			// Create PDType0Font from cached data
			return PDType0Font.load(document, new ByteArrayInputStream(fontData));
		} catch (IOException e) {
			logger.warn("Cannot load given external font: " + fontPath, e);
			return null;
		}
	}

	/**
	 * <p>
	 * Helper method to read an InputStream into a byte array.
	 * </p>
	 * 
	 * @param inputStream the InputStream to read
	 * @return byte array containing the stream data
	 * @throws IOException if reading fails
	 */
	private static byte[] readStreamToByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[8192];
		int bytesRead;
		
		while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, bytesRead);
		}
		
		return buffer.toByteArray();
	}

	/**
	 * <p>
	 * Retrieving {@link String} width depending on current font size. The width
	 * of the string in 1/1000 units of text space.
	 * </p>
	 * 
	 * @param font
	 *            The font of text whose width will be retrieved
	 * @param text
	 *            The text whose width will be retrieved
	 * @param fontSize
	 *            The font size of text whose width will be retrieved
	 * @return text width
	 */
	public static float getStringWidth(final PDFont font, final String text, final float fontSize) {
		try {
			return font.getStringWidth(text) / 1000 * fontSize;
		} catch (final IOException e) {
			// turn into runtime exception
			throw new IllegalStateException("Unable to determine text width", e);
		}
	}

	/**
	 * <p>
	 * Calculate the font ascent distance.
	 * </p>
	 * 
	 * @param font
	 *            The font from which calculation will be applied
	 * @param fontSize
	 *            The font size from which calculation will be applied
	 * @return Positive font ascent distance
	 */
	public static float getAscent(final PDFont font, final float fontSize) {
		final String fontName = font.getName();
		if (!fontMetrics.containsKey(fontName)) {
			createFontMetrics(font);
		}

		return fontMetrics.get(fontName).ascent * fontSize;
	}

	/**
	 * <p>
	 * Calculate the font descent distance.
	 * </p>
	 * 
	 * @param font
	 *            The font from which calculation will be applied
	 * @param fontSize
	 *            The font size from which calculation will be applied
	 * @return Negative font descent distance
	 */
	public static float getDescent(final PDFont font, final float fontSize) {
		final String fontName = font.getName();
		if (!fontMetrics.containsKey(fontName)) {
			createFontMetrics(font);
		}

		return fontMetrics.get(fontName).descent * fontSize;
	}

	/**
	 * <p>
	 * Calculate the font height.
	 * </p>
	 * 
	 * @param font
	 *            {@link PDFont} from which the height will be calculated.
	 * @param fontSize
	 *            font size for current {@link PDFont}.
	 * @return {@link PDFont}'s height
	 */
	public static float getHeight(final PDFont font, final float fontSize) {
		final String fontName = font.getName();
		if (!fontMetrics.containsKey(fontName)) {
			createFontMetrics(font);
		}

		return fontMetrics.get(fontName).height * fontSize;
	}

	/**
	 * <p>
	 * Create basic {@link FontMetrics} for current font.
	 * <p>
	 * 
	 * @param font
	 *            The font from which calculation will be applied <<<<<<< HEAD
	 * @throws IOException
	 *             If reading the font file fails ======= >>>>>>> using FreeSans
	 *             as default font and added new free fonts
	 */
	private static void createFontMetrics(final PDFont font) {
		final float base = font.getFontDescriptor().getXHeight() / 1000;
		final float ascent = font.getFontDescriptor().getAscent() / 1000 - base;
		final float descent = font.getFontDescriptor().getDescent() / 1000;
		fontMetrics.put(font.getName(), new FontMetrics(base + ascent - descent, ascent, descent));
	}

	public static void addDefaultFonts(final PDFont font, final PDFont fontBold, final PDFont fontItalic,
			final PDFont fontBoldItalic) {
		defaultFonts.put("font", font);
		defaultFonts.put("fontBold", fontBold);
		defaultFonts.put("fontItalic", fontItalic);
		defaultFonts.put("fontBoldItalic", fontBoldItalic);
	}

	public static Map<String, PDFont> getDefaultfonts() {
		return defaultFonts;
	}

	public static void setSansFontsAsDefault(PDDocument document) {
		defaultFonts.put("font", loadFont(document, "fonts/FreeSans.ttf"));
		defaultFonts.put("fontBold", loadFont(document, "fonts/FreeSansBold.ttf"));
		defaultFonts.put("fontItalic", loadFont(document, "fonts/FreeSansOblique.ttf"));
		defaultFonts.put("fontBoldItalic", loadFont(document, "fonts/FreeSansBoldOblique.ttf"));
	}

	/**
	 * <p>
	 * Sets Source Sans 3 fonts as the default fonts for the document.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where Source Sans 3 fonts will be set as default
	 */
	public static void setSourceSans3FontsAsDefault(PDDocument document) {
		defaultFonts.put("font", loadFont(document, "fonts/SourceSans3-Regular.ttf"));
		defaultFonts.put("fontBold", loadFont(document, "fonts/SourceSans3-Bold.ttf"));
		defaultFonts.put("fontItalic", loadFont(document, "fonts/SourceSans3-It.ttf"));
		defaultFonts.put("fontBoldItalic", loadFont(document, "fonts/SourceSans3-BoldIt.ttf"));
	}

	/**
	 * <p>
	 * Loads a complete font set (regular, bold, italic, bold-italic) from the specified paths.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where fonts will be loaded
	 * @param familyName
	 *            The name of the font family
	 * @param regularPath
	 *            Path to the regular font file
	 * @param boldPath
	 *            Path to the bold font file
	 * @param italicPath
	 *            Path to the italic font file
	 * @param boldItalicPath
	 *            Path to the bold-italic font file
	 * @return A new {@link be.quodlibet.boxable.FontSet} containing all font variants
	 */
	public static final be.quodlibet.boxable.FontSet loadFontSet(PDDocument document, String familyName, 
			String regularPath, String boldPath, String italicPath, String boldItalicPath) {
		PDType0Font regular = loadFont(document, regularPath);
		PDType0Font bold = loadFont(document, boldPath);
		PDType0Font italic = loadFont(document, italicPath);
		PDType0Font boldItalic = loadFont(document, boldItalicPath);
		
		if (regular == null || bold == null || italic == null || boldItalic == null) {
			logger.warn("Failed to load one or more font variants for family: " + familyName);
			return null;
		}
		
		return new be.quodlibet.boxable.FontSet(familyName, regular, bold, italic, boldItalic);
	}

	/**
	 * <p>
	 * Creates a FontSet from the current default fonts. If no default fonts are set,
	 * uses Standard14Fonts (Helvetica variants).
	 * </p>
	 * 
	 * @return A {@link be.quodlibet.boxable.FontSet} containing the default font variants
	 */
	public static final be.quodlibet.boxable.FontSet getDefaultFontSet() {
		if (defaultFonts.isEmpty()) {
			// Use Standard14Fonts as fallback
			org.apache.pdfbox.pdmodel.font.PDType1Font regular = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
			org.apache.pdfbox.pdmodel.font.PDType1Font bold = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
			org.apache.pdfbox.pdmodel.font.PDType1Font italic = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_OBLIQUE);
			org.apache.pdfbox.pdmodel.font.PDType1Font boldItalic = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
			
			return new be.quodlibet.boxable.FontSet("Helvetica", regular, bold, italic, boldItalic);
		} else {
			PDFont regular = defaultFonts.get("font");
			PDFont bold = defaultFonts.get("fontBold");
			PDFont italic = defaultFonts.get("fontItalic");
			PDFont boldItalic = defaultFonts.get("fontBoldItalic");
			
			// Check if any of the default fonts are null - if so, fall back to Standard14Fonts
			if (regular == null || bold == null || italic == null || boldItalic == null) {
				logger.warn("Some default fonts are null, falling back to Standard14Fonts");
				org.apache.pdfbox.pdmodel.font.PDType1Font regularFallback = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
				org.apache.pdfbox.pdmodel.font.PDType1Font boldFallback = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
				org.apache.pdfbox.pdmodel.font.PDType1Font italicFallback = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_OBLIQUE);
				org.apache.pdfbox.pdmodel.font.PDType1Font boldItalicFallback = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
				
				return new be.quodlibet.boxable.FontSet("Helvetica", regularFallback, boldFallback, italicFallback, boldItalicFallback);
			}
			
			return new be.quodlibet.boxable.FontSet("Default", regular, bold, italic, boldItalic);
		}
	}
}
