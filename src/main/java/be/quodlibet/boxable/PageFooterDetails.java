package be.quodlibet.boxable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contains configuration details for page footers including date format,
 * trademark text, and formatting options.
 */
public class PageFooterDetails {

    private final DateTimeFormatter dateFormatter;
    private final String trademarkText;
    private final boolean includePageNumbers;
    private final boolean includeDate;
    private final float fontSize;
    private final float bottomMargin;

    /**
     * Builder class for creating PageFooterDetails instances.
     */
    public static class Builder {
        private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private String trademarkText = null;
        private boolean includePageNumbers = true;
        private boolean includeDate = true;
        private float fontSize = 8f;
        private float bottomMargin = 20f;

        public Builder withDateFormatter(DateTimeFormatter dateFormatter) {
            this.dateFormatter = dateFormatter;
            return this;
        }

        public Builder withDateFormatter(String pattern) {
            this.dateFormatter = DateTimeFormatter.ofPattern(pattern);
            return this;
        }

        public Builder withTrademarkText(String trademarkText) {
            this.trademarkText = trademarkText;
            return this;
        }

        public Builder withPageNumbers(boolean includePageNumbers) {
            this.includePageNumbers = includePageNumbers;
            return this;
        }

        public Builder withDate(boolean includeDate) {
            this.includeDate = includeDate;
            return this;
        }

        public Builder withFontSize(float fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder withBottomMargin(float bottomMargin) {
            this.bottomMargin = bottomMargin;
            return this;
        }

        public PageFooterDetails build() {
            return new PageFooterDetails(this);
        }
    }

    private PageFooterDetails(Builder builder) {
        this.dateFormatter = builder.dateFormatter;
        this.trademarkText = builder.trademarkText;
        this.includePageNumbers = builder.includePageNumbers;
        this.includeDate = builder.includeDate;
        this.fontSize = builder.fontSize;
        this.bottomMargin = builder.bottomMargin;
    }

    public DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    public String getTrademarkText() {
        return trademarkText;
    }

    public boolean isIncludePageNumbers() {
        return includePageNumbers;
    }

    public boolean isIncludeDate() {
        return includeDate;
    }

    public float getFontSize() {
        return fontSize;
    }

    public float getBottomMargin() {
        return bottomMargin;
    }

    /**
     * Formats the current date using the configured formatter.
     * 
     * @return formatted date string
     */
    public String getFormattedDate() {
        return LocalDateTime.now().format(dateFormatter);
    }

    /**
     * Creates a default PageFooterDetails instance.
     * 
     * @return default configuration
     */
    public static PageFooterDetails createDefault() {
        return new Builder().build();
    }
}