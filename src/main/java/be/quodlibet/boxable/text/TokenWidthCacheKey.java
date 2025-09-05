package be.quodlibet.boxable.text;

import org.apache.pdfbox.pdmodel.font.PDFont;

public class TokenWidthCacheKey {

    private final PDFont font;
    private final Token token;

    public TokenWidthCacheKey(PDFont font, Token token) {
        this.font = font;
        this.token = token;
    }

    public PDFont getFont() {
        return font;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenWidthCacheKey that = (TokenWidthCacheKey) o;
        return font.equals(that.font) &&
                token.equals(that.token);
    }

    @Override
    public int hashCode() {
        int result = font.hashCode();
        result = 31 * result + token.hashCode();
        return result;
    }
}
