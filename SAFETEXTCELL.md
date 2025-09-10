# SafeTextCell Documentation

## Overview

`SafeTextCell` is a specialized cell class that extends the standard `Cell` class to provide automatic text sanitization using `PDFontTextAdapter`. This prevents PDF generation failures that can occur when text contains characters unsupported by the current font.

## Features

- **Automatic Text Sanitization**: Replaces unsupported characters with '?' and control characters with spaces
- **FontSet Integration**: Uses the table's FontSet when available, falling back to FontUtils defaults
- **Full Backward Compatibility**: Existing code using regular cells continues to work unchanged
- **Equivalent API**: Provides the same constructor options as regular Cell class

## Usage

### Basic Usage

```java
// Create table and row as usual
BaseTable table = new BaseTable(500f, 400f, 50f, 500f, 50f, doc, page, true, true);
Row<PDPage> row = table.createRow(50f);

// Create SafeTextCell instead of regular cell
SafeTextCell<PDPage> safeCell = row.createSafeTextCell(100f, "Text with unsafe chars: \u0000\u0001");

// The text is automatically sanitized
System.out.println(safeCell.getText()); // "Text with unsafe chars:   "
```

### Available Methods

SafeTextCell provides three creation methods in Row class:

```java
// 1. Basic SafeTextCell with default alignment
SafeTextCell<PDPage> cell1 = row.createSafeTextCell(width, text);

// 2. SafeTextCell with custom alignment  
SafeTextCell<PDPage> cell2 = row.createSafeTextCell(width, text, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);

// 3. SafeTextCell with header width (matches header cell width)
SafeTextCell<PDPage> cell3 = row.createSafeTextCell(text);
```

### Dynamic Text Setting

```java
SafeTextCell<PDPage> cell = row.createSafeTextCell(100f, "Initial text");

// setText automatically sanitizes the new text
cell.setText("New text with problems: \u0000\u0001");
```

### FontSet Integration

SafeTextCell automatically uses fonts from the table's FontSet:

```java
// Set up FontSet for the table
FontSet fontSet = new FontSet("MyFont", regularFont, boldFont, italicFont, boldItalicFont);
table.setFontSet(fontSet);

// SafeTextCell will use fonts from the FontSet
SafeTextCell<PDPage> cell = row.createSafeTextCell(100f, "Text");
```

## When to Use SafeTextCell

### Use SafeTextCell when:
- Processing user input that may contain unsafe characters
- Working with data from external sources (databases, files, APIs)
- Building robust applications that need to handle any text content
- You want to prevent PDF generation failures due to font limitations

### Regular Cell is sufficient when:
- You have full control over the text content
- Text is guaranteed to contain only safe characters
- Performance is critical and you can ensure text safety elsewhere

## Examples

### Example 1: Handling User Input

```java
// This could crash with regular Cell if userInput contains control characters
String userInput = getUserInput(); // May contain \u0000, \u0001, etc.

// SafeTextCell handles it gracefully
SafeTextCell<PDPage> cell = row.createSafeTextCell(200f, userInput);
```

### Example 2: Mixed Cell Types

```java
Row<PDPage> row = table.createRow(30f);

// Use regular cells for safe, controlled content
row.createCell(100f, "Safe Label");

// Use SafeTextCell for potentially unsafe content
row.createSafeTextCell(200f, externalData);

// Both work together seamlessly
```

### Example 3: Header Rows

```java
Row<PDPage> headerRow = table.createRow(40f);
headerRow.setHeaderRow(true);

// SafeTextCell respects header formatting
SafeTextCell<PDPage> headerCell = headerRow.createSafeTextCell(150f, "Header Text");
// Automatically uses bold font from FontSet
```

## Technical Details

### Character Sanitization Rules
- Control characters (except space and tab) → replaced with space
- Unsupported glyphs → replaced with '?'
- Normal characters → passed through unchanged
- Surrogate pairs → handled correctly

### Font Selection Priority
1. Table's FontSet (regular/bold based on isHeaderCell)
2. FontUtils defaults (if available)
3. Built-in Helvetica fonts (fallback)

### Performance
- Sanitization occurs once during setText()
- No performance impact during PDF rendering
- Minimal overhead compared to regular Cell

## Backward Compatibility

SafeTextCell maintains full backward compatibility:

```java
// All existing code continues to work
Cell<PDPage> regularCell = row.createCell(100f, "text");
Cell<PDPage> alignedCell = row.createCell(100f, "text", HorizontalAlignment.CENTER, VerticalAlignment.TOP);

// New SafeTextCell methods are additional, not replacements
SafeTextCell<PDPage> safeCell = row.createSafeTextCell(100f, "text");
```

No existing functionality is changed or removed.