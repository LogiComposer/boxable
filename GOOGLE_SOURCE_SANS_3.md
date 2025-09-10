# Google Source Sans 3 Font Support

This document explains how to use Google Source Sans 3 fonts with the Boxable library.

## Overview

The Boxable library now supports Google Source Sans 3 fonts as an alternative to the default Helvetica fonts. This provides better special character support and professional typography for PDF generation.

## Features

- **All Four Variants**: Regular, Bold, Italic, and Bold Italic
- **Font Caching**: Fonts are loaded only once and reused for all PDF generations
- **Seamless Integration**: Works with existing Boxable components without code changes
- **No Breaking Changes**: Existing PDF generation remains unaffected
- **Better Character Support**: Improved rendering of special characters and international text

## Usage

### Basic Usage

To use Google Source Sans 3 fonts, simply call `FontUtils.setSourceSans3FontsAsDefault(document)` before creating tables:

```java
PDDocument doc = new PDDocument();
try {
    // Set Source Sans 3 fonts as default
    FontUtils.setSourceSans3FontsAsDefault(doc);
    
    // Now create tables normally - they will automatically use Source Sans 3
    PDPage page = new PDPage();
    doc.addPage(page);
    
    BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, 
                                   tableWidth, margin, doc, page, true, true);
    
    // All cells will now use Source Sans 3 fonts
    Row<PDPage> row = table.createRow(20f);
    Cell<PDPage> cell = row.createCell(100, "This text uses Source Sans 3!");
    
    table.draw();
    doc.save("output.pdf");
} finally {
    doc.close();
}
```

### Font Variants

The library automatically uses the appropriate font variant based on formatting:

```java
// Regular font
Cell<PDPage> regularCell = row.createCell(25, "Regular text");

// Bold font (using HTML tags)
Cell<PDPage> boldCell = row.createCell(25, "<b>Bold text</b>");

// Italic font (using HTML tags)
Cell<PDPage> italicCell = row.createCell(25, "<i>Italic text</i>");

// Bold Italic font (using HTML tags)
Cell<PDPage> boldItalicCell = row.createCell(25, "<b><i>Bold italic text</i></b>");

// Header cells automatically use bold font
Cell<PDPage> headerCell = row.createCell(100, "Header text");
headerCell.setHeaderCell(true);
```

### Special Characters

Source Sans 3 provides better support for special characters:

```java
Cell<PDPage> specialCell = row.createCell(100, 
    "Special Characters: À Ñ Ü ß € £ ¥ © ® ™ • – — \" \" ' ' « » ¿ ¡");
```

## Font Files

The following font files are included in the library:

- `SourceSans3-Regular.ttf` - Regular variant
- `SourceSans3-Bold.ttf` - Bold variant  
- `SourceSans3-Italic.ttf` - Italic variant
- `SourceSans3-BoldItalic.ttf` - Bold Italic variant

**Note**: The current implementation includes placeholder font files for demonstration. In a production environment, these should be replaced with actual Google Source Sans 3 font files downloaded from [Google Fonts](https://fonts.google.com/specimen/Source+Sans+3).

## Backward Compatibility

The existing font behavior is completely preserved:

- If you don't call `setSourceSans3FontsAsDefault()`, tables will use Helvetica as before
- All existing PDF generation code continues to work without changes
- Performance and rendering remain the same for existing code

## Font Caching

Fonts are automatically cached for performance:

- Font files are loaded from the file system only once per document
- Multiple tables in the same document reuse the same font instances
- This reduces memory usage and improves performance

## Integration with Existing Components

The font system integrates seamlessly with all Boxable components:

- **Cell**: Automatically uses default fonts or falls back to Helvetica
- **Paragraph**: Supports HTML formatting with custom fonts
- **Table**: All cells inherit the default font configuration
- **TableCell**: Works with custom fonts for nested tables

## API Reference

### FontUtils Class

#### `setSourceSans3FontsAsDefault(PDDocument document)`

Sets Google Source Sans 3 fonts as the default fonts for the Boxable library.

**Parameters:**
- `document` - The PDDocument where fonts will be loaded and embedded

**Example:**
```java
FontUtils.setSourceSans3FontsAsDefault(doc);
```

#### `getDefaultfonts()`

Returns the current default font map.

**Returns:** `Map<String, PDFont>` with keys:
- `"font"` - Regular font
- `"fontBold"` - Bold font
- `"fontItalic"` - Italic font
- `"fontBoldItalic"` - Bold Italic font

## Best Practices

1. **Call font setup early**: Set fonts immediately after creating the PDDocument
2. **Reuse documents**: Create multiple tables in the same document to benefit from font caching
3. **Use HTML formatting**: Use `<b>` and `<i>` tags for proper font variant selection
4. **Test with special characters**: Verify that your content renders correctly with the new fonts

## Troubleshooting

### Fonts not loading
- Ensure font files are present in `src/main/resources/fonts/`
- Check that `setSourceSans3FontsAsDefault()` is called before creating tables
- Verify font files are not corrupted

### Characters not rendering
- Confirm the font file supports the characters you're trying to render
- Test with a smaller character set to isolate the issue

### Performance issues
- Use the same PDDocument for multiple tables to benefit from font caching
- Avoid calling `setSourceSans3FontsAsDefault()` multiple times unnecessarily

## License

The font integration code is released under the same license as the Boxable library. Google Source Sans 3 fonts are available under the SIL Open Font License.