package be.quodlibet.boxable.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.quodlibet.boxable.FontSet;
import be.quodlibet.boxable.FontStyle;
import be.quodlibet.boxable.SupportedFont;

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

	private static final Map<String, PDFont> defaultFonts = new HashMap<>();

	private FontUtils() {
	}

	/**
	 * Loads the {@link PDType0Font} to be embedded in the specified {@link PDDocument}.
	 * Font file data is cached to avoid frequent loading from files, and PDType0Font objects
	 * are cached per document to avoid unnecessary object creation.
	 *
	 * @param document {@link PDDocument} where fonts will be loaded
	 * @param fontPath Font path which will be loaded
	 * @return The loaded {@link PDType0Font}, or null if loading fails
	 */
	public static PDType0Font loadFont(PDDocument document, String fontPath) {
		if (document == null || fontPath == null) {
			logger.warn("Document and fontPath cannot be null");
			return null;
		}

		// Attempt to retrieve the font from the document-level cache
		PDType0Font cachedFont = FontCacheManager.getCachedFont(document, fontPath);
		if (cachedFont != null) {
			return cachedFont;
		}

		try {
			// Load font data, either from cache or file
			byte[] fontData = getFontData(fontPath);
			if (fontData == null) {
				return null;
			}

			// Create and cache the PDType0Font
			return createAndCacheFont(document, fontPath, fontData);
		} catch (IOException e) {
			logger.warn("Cannot load given external font: " + fontPath, e);
			return null;
		}
	}

	private static byte[] getFontData(String fontPath) throws IOException {
		byte[] fontData = FontCacheManager.getCachedFontData(fontPath);
		if (fontData == null) {
			try (InputStream fontStream = FontUtils.class.getClassLoader().getResourceAsStream(fontPath)) {
				if (fontStream == null) {
					logger.warn("Cannot find font file: " + fontPath);
					return null;
				}
				fontData = readStreamToByteArray(fontStream);
				FontCacheManager.cacheFontData(fontPath, fontData);
			}
		} else {
			logger.debug("Using cached font data for: " + fontPath);
		}
		return fontData;
	}

	private static PDType0Font createAndCacheFont(PDDocument document, String fontPath, byte[] fontData) throws IOException {
		try (InputStream fontStream = new ByteArrayInputStream(fontData)) {
			PDType0Font font = PDType0Font.load(document, fontStream);
			if (font != null) {
				FontCacheManager.cacheFont(document, fontPath, font);
			}
			return font;
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
	 * Clears the font cache for a specific document. This can be useful for explicit cleanup
	 * when you know a document is no longer needed, though the WeakHashMap should handle
	 * automatic cleanup when documents are garbage collected.
	 * </p>
	 * 
	 * @param document the PDDocument for which to clear the font cache
	 */
	public static void clearDocumentFontCache(PDDocument document) {
		FontCacheManager.clearDocumentFontCache(document);
	}

	/**
	 * <p>
	 * Gets the number of documents currently in the font cache. Useful for monitoring and testing.
	 * </p>
	 * 
	 * @return the number of documents with cached fonts
	 */
	public static int getDocumentCacheSize() {
		return FontCacheManager.getDocumentCacheSize();
	}

	/**
	 * <p>
	 * Gets the number of fonts cached for a specific document. Useful for monitoring and testing.
	 * </p>
	 * 
	 * @param document the PDDocument to check
	 * @return the number of fonts cached for this document, or 0 if none
	 */
	public static int getFontCacheSize(PDDocument document) {
		return FontCacheManager.getFontCacheSize(document);
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
	 * @return A new {@link FontSet} containing all font variants
	 */
	public static FontSet loadFontSet(PDDocument document, String familyName,
			String regularPath, String boldPath, String italicPath, String boldItalicPath) {
		PDType0Font regular = loadFont(document, regularPath);
		PDType0Font bold = loadFont(document, boldPath);
		PDType0Font italic = loadFont(document, italicPath);
		PDType0Font boldItalic = loadFont(document, boldItalicPath);
		
		if (regular == null || bold == null || italic == null || boldItalic == null) {
			logger.warn("Failed to load one or more font variants for family: " + familyName);
			return null;
		}
		
		return new FontSet(familyName, regular, bold, italic, boldItalic);
	}

	/**
	 * <p>
	 * Loads a complete font set using a supported font family enum.
	 * This provides a convenient way to load fonts without specifying individual file paths.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where fonts will be loaded
	 * @param supportedFont
	 *            The {@link SupportedFont} enum representing the font family to load
	 * @return A new {@link FontSet} containing all font variants, or null if loading fails
	 */
	public static FontSet loadFontSet(PDDocument document, SupportedFont supportedFont) {
		return loadFontSet(document, 
						   supportedFont.getFamilyName(),
						   supportedFont.getRegularPath(),
						   supportedFont.getBoldPath(),
						   supportedFont.getItalicPath(),
						   supportedFont.getBoldItalicPath());
	}

	/**
	 * <p>
	 * Loads a specific font variant for a supported font family and style.
	 * This provides a convenient way to load a single font without loading a complete FontSet.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where the font will be loaded
	 * @param supportedFont
	 *            The {@link SupportedFont} enum representing the font family
	 * @param fontStyle
	 *            The {@link FontStyle} enum representing the desired font style
	 * @return The loaded {@link PDType0Font} for the specified family and style, or null if loading fails
	 */
	public static PDType0Font loadFont(PDDocument document, SupportedFont supportedFont, FontStyle fontStyle) {
		if (document == null) {
			logger.warn("Document cannot be null");
			return null;
		}
		
		if (supportedFont == null) {
			logger.warn("SupportedFont cannot be null");
			return null;
		}
		
		if (fontStyle == null) {
			throw new IllegalArgumentException("FontStyle cannot be null");
		}
		
		String fontPath;
		
		switch (fontStyle) {
			case REGULAR:
				fontPath = supportedFont.getRegularPath();
				break;
			case BOLD:
				fontPath = supportedFont.getBoldPath();
				break;
			case ITALIC:
				fontPath = supportedFont.getItalicPath();
				break;
			case BOLD_ITALIC:
				fontPath = supportedFont.getBoldItalicPath();
				break;
			default:
				logger.warn("Unknown font style: " + fontStyle + ", defaulting to regular");
				fontPath = supportedFont.getRegularPath();
				break;
		}
		
		return loadFont(document, fontPath);
	}

	/**
	 * <p>
	 * Creates a FontSet from the current default fonts. If no default fonts are set,
	 * uses Standard14Fonts (Helvetica variants).
	 * </p>
	 * 
	 * @return A {@link FontSet} containing the default font variants
	 */
	public static FontSet getDefaultFontSet() {
		if (defaultFonts.isEmpty()) {
			// Use Standard14Fonts as fallback
			PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
			PDType1Font boldItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
			
			return new FontSet("Helvetica", regular, bold, italic, boldItalic);
		} else {
			PDFont regular = defaultFonts.get("font");
			PDFont bold = defaultFonts.get("fontBold");
			PDFont italic = defaultFonts.get("fontItalic");
			PDFont boldItalic = defaultFonts.get("fontBoldItalic");
			
			// Check if any of the default fonts are null - if so, fall back to Standard14Fonts
			if (regular == null || bold == null || italic == null || boldItalic == null) {
				logger.warn("Some default fonts are null, falling back to Standard14Fonts");
				PDType1Font regularFallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
				PDType1Font boldFallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
				PDType1Font italicFallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
				PDType1Font boldItalicFallback = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
				
				return new FontSet("Helvetica", regularFallback, boldFallback, italicFallback, boldItalicFallback);
			}
			
			return new FontSet("Default", regular, bold, italic, boldItalic);
		}
	}

	public static void clearDefaultFonts() {
		defaultFonts.clear();
	}
}
