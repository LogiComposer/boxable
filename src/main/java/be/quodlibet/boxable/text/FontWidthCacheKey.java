package be.quodlibet.boxable.text;

import org.apache.pdfbox.pdmodel.font.PDFont;

public class FontWidthCacheKey {

    private final PDFont font;
    private final String text;
    private final float fontSize;

    public FontWidthCacheKey(PDFont font, String text, float fontSize) {
        this.font = font;
        this.text = text;
        this.fontSize = fontSize;
    }

    public PDFont getFont() {
        return font;
    }

    public String getText() {
        return text;
    }

    public float getFontSize() {
        return fontSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FontWidthCacheKey that = (FontWidthCacheKey) o;
        return Float.compare(that.fontSize, fontSize) == 0 &&
                font.equals(that.font) &&
                text.equals(that.text);
    }

    @Override
    public int hashCode() {
        int result = font.hashCode();
        result = 31 * result + text.hashCode();
        result = 31 * result + Float.hashCode(fontSize);
        return result;
    }
}
