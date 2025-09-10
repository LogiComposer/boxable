package be.quodlibet.boxable.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.concurrent.ConcurrentHashMap;
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

	private static final Map<String, byte[]> fontFileCache = new ConcurrentHashMap<>();

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
	 * <p>
	 * Loads the {@link PDType0Font} to be embedded in the specified
	 * {@link PDDocument}.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where fonts will be loaded
	 * @param fontFilePath
	 *            font path which will be loaded
	 * @return The read {@link PDType0Font}
	 */
	public static PDType0Font loadFont(PDDocument document, String fontFilePath) throws IOException {
		byte[] fontBytes = fontFileCache.computeIfAbsent(fontFilePath, path -> {
			try (InputStream is = FontUtils.class.getClassLoader().getResourceAsStream(path)) {
				if (is == null) {
					throw new RuntimeException(new FileAlreadyExistsException("Font file not found: " + path));
				}
				return readInputStreamToByteArray(is);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load font file: " + path, e);
			}
		});
		return PDType0Font.load(document, new ByteArrayInputStream(fontBytes));
	}

	private static byte[] readInputStreamToByteArray(InputStream is) throws IOException {
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			byte[] temp = new byte[1024];
			int bytesRead;
			while ((bytesRead = is.read(temp)) != -1) {
				buffer.write(temp, 0, bytesRead);
			}
			return buffer.toByteArray();
		}
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
		try {
			defaultFonts.put("font", loadFont(document, "fonts/FreeSans.ttf"));
			defaultFonts.put("fontBold", loadFont(document, "fonts/FreeSansBold.ttf"));
			defaultFonts.put("fontItalic", loadFont(document, "fonts/FreeSansOblique.ttf"));
			defaultFonts.put("fontBoldItalic", loadFont(document, "fonts/FreeSansBoldOblique.ttf"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load SansFonts: " + e.getMessage(), e);
		}
	}

	/**
	 * <p>
	 * Sets Google Source Sans 3 fonts as the default fonts for the Boxable library.
	 * This method loads and caches all four variants: Regular, Bold, Italic, and Bold Italic.
	 * The fonts are loaded only once and reused for all subsequent PDF generation.
	 * </p>
	 * 
	 * @param document
	 *            {@link PDDocument} where fonts will be loaded and embedded
	 */
	public static void setSourceSans3FontsAsDefault(PDDocument document) {
		try {
			defaultFonts.put("font", loadFont(document, "fonts/SourceSans3-Regular.ttf"));
			defaultFonts.put("fontBold", loadFont(document, "fonts/SourceSans3-Bold.ttf"));
			defaultFonts.put("fontItalic", loadFont(document, "fonts/SourceSans3-Italic.ttf"));
			defaultFonts.put("fontBoldItalic", loadFont(document, "fonts/SourceSans3-BoldItalic.ttf"));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load SourceSans3Fonts: " + e.getMessage(), e);
		}
	}
}
