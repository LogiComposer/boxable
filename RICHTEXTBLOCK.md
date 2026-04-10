# RichTextBlock Usage Guide

`RichTextBlock` is a bounded rich-text area that positions mixed content — styled text, images, lists, and headings — within a fixed rectangle on a PDF page. It is built on top of [Apache PDFBox](https://pdfbox.apache.org/) and is part of the Boxable library.

---

## Table of Contents

- [Overview](#overview)
- [Key Classes](#key-classes)
- [Quick Start](#quick-start)
- [Builder API Reference](#builder-api-reference)
- [Text Styling](#text-styling)
  - [Plain Text](#plain-text)
  - [Bold, Italic, and Underline](#bold-italic-and-underline)
  - [Mixed Style Combinations](#mixed-style-combinations)
  - [Colored Text](#colored-text)
- [Text Alignment](#text-alignment)
  - [Left, Right, Center, and Justify](#left-right-center-and-justify)
- [Block-Level Header](#block-level-header)
- [Content Headers (Header1 and Header2)](#content-headers-header1-and-header2)
- [Block-Level Images](#block-level-images)
  - [From File (JPG/PNG)](#from-file-jpgpng)
  - [From InputStream](#from-inputstream)
  - [From Base64 String](#from-base64-string)
- [Inline Images](#inline-images)
- [Lists](#lists)
  - [Bulleted Lists](#bulleted-lists)
  - [Numbered Lists](#numbered-lists)
  - [Lists with Mixed Formatting](#lists-with-mixed-formatting)
- [Paragraph Breaks](#paragraph-breaks)
- [Overflow Handling](#overflow-handling)
- [Content Height Estimation](#content-height-estimation)
- [Chaining Multiple Blocks](#chaining-multiple-blocks)
- [Side-by-Side Blocks (Column Layout)](#side-by-side-blocks-column-layout)
- [Landscape Orientation](#landscape-orientation)
- [Rendering on an Existing Page](#rendering-on-an-existing-page)
- [Rendering on a New Page](#rendering-on-a-new-page)
- [Text Wrapping Behaviour](#text-wrapping-behaviour)
- [Debug Border](#debug-border)
- [Complete Example](#complete-example)

---

## Overview

`RichTextBlock` uses a **Builder pattern** for fluent, readable construction. Content is modelled as a list of `ContentElement` objects (text, images, headers, lists) rendered sequentially inside a bounded rectangle. The block tracks a vertical cursor; when content exceeds the block height, remaining elements are skipped and an optional overflow indicator (`…`) is shown.

### Architecture

```
RichTextBlock
├── Block-level header (optional, underlined)
└── Content elements (rendered in order)
    ├── TextContentElement    — styled text paragraphs
    ├── HeaderContentElement  — inline H1 / H2 headings
    ├── ImageContentElement   — block-level images
    └── ListContentElement    — bulleted or numbered lists
```

Each `TextContentElement` contains one or more `RichTextLine` objects, and each line contains one or more `LineElement` instances — either `RichTextSegment` (styled text) or `InlineImageSegment` (small images that flow inline with text).

---

## Key Classes

| Class | Description |
|-------|-------------|
| `RichTextBlock` | The bounded container; owns position, size, header, and content list |
| `RichTextBlock.Builder` | Fluent builder for constructing a `RichTextBlock` |
| `ContentElement` | Interface for renderable content (text, image, header, list) |
| `TextContentElement` | Renders one or more styled text lines with word wrapping |
| `ImageContentElement` | Renders a block-level image (scaled to fit) |
| `HeaderContentElement` | Renders an inline heading (H1 or H2) within the content flow |
| `ListContentElement` | Renders a bulleted or numbered list |
| `RichTextLine` | One logical line: an ordered list of segments + alignment + list decoration |
| `RichTextSegment` | An immutable run of styled text (font size, bold/italic/underline, color) |
| `InlineImageSegment` | A small image that flows on the same line as text |
| `TextStyle` | Enum: `BOLD`, `ITALIC`, `UNDERLINE` (combinable via `EnumSet`) |
| `TextAlignment` | Enum: `LEFT`, `CENTER`, `RIGHT`, `JUSTIFY` |
| `TextType` | Enum: `BODY` (10pt), `HEADER1` (18pt bold), `HEADER2` (14pt bold) |
| `ListType` | Enum: `NONE`, `BULLETED`, `NUMBERED` |
| `RenderContext` | Mutable rendering state passed during a render pass |

---

## Quick Start

```java
import be.quodlibet.boxable.Standard14FontFamily;
import be.quodlibet.boxable.richtext.*;
import be.quodlibet.boxable.utils.FontUtils;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.Collections;
import java.util.EnumSet;

try (PDDocument doc = new PDDocument()) {
    PDRectangle pageSize = PDRectangle.A4;
    PDPage page = new PDPage(pageSize);
    doc.addPage(page);

    PageContentStreamOptimized stream = new PageContentStreamOptimized(
            new PDPageContentStream(doc, page));

    RichTextLine paragraph = new RichTextLine(Collections.singletonList(
            new RichTextSegment("Hello, RichTextBlock!",
                    EnumSet.noneOf(TextStyle.class), 12f)),
            ListType.NONE, 0, TextAlignment.LEFT);

    RichTextBlock block = RichTextBlock.builder()
            .at(40, 30)                       // top-left position (x, y from top)
            .size(500, 300)                    // width × height in points
            .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA),
                    14, "My First Block", TextAlignment.CENTER)
            .addContent(new TextContentElement(paragraph))
            .build();

    block.render(doc, stream, pageSize.getHeight());
    stream.close();

    doc.save("QuickStart.pdf");
}
```

---

## Builder API Reference

| Builder Method | Description | Default |
|----------------|-------------|---------|
| `.at(float x, float y)` | Top-left position (logical top-down coordinates) | `(0, 0)` |
| `.size(float width, float height)` | Block dimensions in points | `400 × 300` |
| `.blockPadding(float padding)` | Internal padding between block edge and content | `4` pt |
| `.header(FontSet, float fontSize, String text, TextAlignment)` | Block-level header (rendered with underline) | No header |
| `.addContent(ContentElement)` | Appends a content element | — |
| `.addAllContent(List<ContentElement>)` | Appends all content elements from a list | — |
| `.showOverflowIndicator(boolean)` | Show `…` when content overflows the block | `true` |
| `.drawBorder(boolean)` | Draw a light-gray debug border around the block | `false` |
| `.build()` | Creates the immutable `RichTextBlock` | — |

---

## Text Styling

### Plain Text

```java
RichTextSegment plain = new RichTextSegment(
        "Plain body text.",
        EnumSet.noneOf(TextStyle.class), 10f);
```

### Bold, Italic, and Underline

```java
// Bold
new RichTextSegment("Bold text", EnumSet.of(TextStyle.BOLD), 10f);

// Italic
new RichTextSegment("Italic text", EnumSet.of(TextStyle.ITALIC), 10f);

// Underline
new RichTextSegment("Underlined text", EnumSet.of(TextStyle.UNDERLINE), 10f);
```

### Mixed Style Combinations

Styles are combined using `EnumSet`:

```java
// Bold + Italic
new RichTextSegment("Bold-Italic",
        EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 11f);

// Bold + Underline
new RichTextSegment("Bold-Underline",
        EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 11f);

// Italic + Underline
new RichTextSegment("Italic-Underline",
        EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 11f);

// All three: Bold + Italic + Underline
new RichTextSegment("All Three",
        EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 11f);
```

Multiple segments with different styles can be placed on the same line:

```java
RichTextLine mixedLine = new RichTextLine(Arrays.asList(
        new RichTextSegment("Normal ", EnumSet.noneOf(TextStyle.class), 10f),
        new RichTextSegment("Bold+Italic ", EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 10f),
        new RichTextSegment("Bold+Underline ", EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 10f),
        new RichTextSegment("Italic+Underline ", EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 10f),
        new RichTextSegment("All Three", EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 10f)
), ListType.NONE, 0, TextAlignment.LEFT);
```

### Colored Text

Supply a `java.awt.Color` as the fourth constructor argument:

```java
new RichTextSegment("Red text", EnumSet.of(TextStyle.BOLD), 10f, Color.RED);
```

If omitted, the color defaults to `Color.BLACK`.

---

## Text Alignment

### Left, Right, Center, and Justify

Alignment is set per `RichTextLine`:

```java
// Left aligned (default)
new RichTextLine(segments, ListType.NONE, 0, TextAlignment.LEFT);

// Right aligned
new RichTextLine(segments, ListType.NONE, 0, TextAlignment.RIGHT);

// Center aligned
new RichTextLine(segments, ListType.NONE, 0, TextAlignment.CENTER);

// Justified (last visual line falls back to left alignment)
new RichTextLine(segments, ListType.NONE, 0, TextAlignment.JUSTIFY);
```

You can combine multiple alignment styles within a single block by adding each paragraph as a separate `TextContentElement`:

```java
RichTextBlock block = RichTextBlock.builder()
        .at(40, 30).size(500, 750)
        .blockPadding(12f)
        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                "Multi-Paragraph Alignment Demo", TextAlignment.CENTER)
        .addContent(new TextContentElement(leftParagraph))
        .addContent(new TextContentElement(rightParagraph))
        .addContent(new TextContentElement(centerParagraph))
        .drawBorder(true)
        .build();
```

---

## Block-Level Header

The block-level header is set via the builder and is rendered first (bold, underlined). It is separate from the content list.

```java
RichTextBlock.builder()
        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                "Quarterly Report Summary", TextAlignment.CENTER)
        // ...
        .build();
```

The header supports `LEFT`, `CENTER`, and `RIGHT` alignment and uses the font family from the provided `FontSet`. `TextAlignment.JUSTIFY` is not currently applied for block-level headers and falls back to left alignment. Long header text is automatically word-wrapped within the block width.

---

## Content Headers (Header1 and Header2)

Unlike the block-level header, `HeaderContentElement` is an inline content element that can appear **anywhere** in the content list — allowing multiple headings at different levels within a single block.

```java
// Header1 (18pt bold by default)
ContentElement h1 = new HeaderContentElement.Builder("Chapter One: Introduction", TextType.HEADER1)
        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN))
        .alignment(TextAlignment.LEFT)
        .build();

// Header2 (14pt bold by default)
ContentElement h2 = new HeaderContentElement.Builder("Section 1.1: Background", TextType.HEADER2)
        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN))
        .alignment(TextAlignment.LEFT)
        .build();
```

The `HeaderContentElement.Builder` supports:
- `.fontSize(float)` — override the default font size
- `.fontFamily(FontSet)` — set the font family (defaults to Helvetica)
- `.alignment(TextAlignment)` — horizontal alignment (defaults to LEFT)

You can mix headers and body text to create structured documents:

```java
RichTextBlock block = RichTextBlock.builder()
        .at(40, 30).size(500, 700)
        .addContent(h1)
        .addContent(new TextContentElement(bodyParagraph1))
        .addContent(h2)
        .addContent(new TextContentElement(bodyParagraph2))
        .addContent(h2b)
        .addContent(new TextContentElement(bodyParagraph3))
        .drawBorder(true)
        .build();
```

### TextType Reference

| TextType | Default Font Size | Bold |
|----------|------------------|------|
| `BODY` | 10 pt | No |
| `HEADER1` | 18 pt | Yes |
| `HEADER2` | 14 pt | Yes |

Use `TextType.BODY.getDefaultFontSize()` when constructing body text segments to stay consistent with the type system.

---

## Block-Level Images

`ImageContentElement` renders a standalone image that occupies its own vertical space within the block. The image is automatically scaled to fit the available width while maintaining its aspect ratio.

### From File (JPG/PNG)

```java
File jpgFile = new File("path/to/image.jpg");

// Proportionally auto-scaled to block width
ContentElement image = new ImageContentElement.Builder(jpgFile)
        .alignment(TextAlignment.CENTER)
        .build();

// Explicit size override (width × height in points)
ContentElement sized = new ImageContentElement.Builder(jpgFile)
        .size(200, 60)
        .alignment(TextAlignment.CENTER)
        .cacheKey("my-jpg")
        .build();
```

### From InputStream

```java
try (InputStream pngStream = getClass().getResourceAsStream("/image.png")) {
    ContentElement image = new ImageContentElement.Builder(pngStream)
            .size(200, 60)
            .alignment(TextAlignment.CENTER)
            .cacheKey("my-png")
            .build();
    // Use the image element — do NOT use the stream after this point
}
```

> **Note:** The caller is responsible for closing the stream. The builder reads the stream fully into a `BufferedImage` but does not close it.

### From Base64 String

Both raw Base64 and data-URI format are supported:

```java
// Raw Base64
ContentElement pngImage = ImageContentElement.Builder.fromBase64(pngBase64String)
        .alignment(TextAlignment.CENTER)
        .cacheKey("b64-png")
        .build();

// Data-URI format (e.g., "data:image/jpeg;base64,/9j/4AAQ...")
ContentElement jpgImage = ImageContentElement.Builder.fromBase64(jpgDataUri)
        .size(220, 70)
        .alignment(TextAlignment.CENTER)
        .cacheKey("b64-jpg")
        .build();
```

### ImageContentElement.Builder Options

| Method | Description | Default |
|--------|-------------|---------|
| `.size(float w, float h)` | Explicit dimensions (overrides auto-scaling) | Proportional to block width |
| `.alignment(TextAlignment)` | Horizontal alignment | `LEFT` |
| `.cacheKey(String)` | Custom cache key (avoids re-encoding the same image) | Auto-generated |
| `.spacingBefore(float pts)` | Vertical spacing before the image | `5` pt |
| `.spacingAfter(float pts)` | Vertical spacing after the image | `5` pt |
| `.quality(float q)` | JPEG encoding quality `(0, 1]`; use `1.0` for lossless PNG | `1.0` |

---

## Inline Images

`InlineImageSegment` embeds a small image on the same line as text — like an icon or chart badge. It is treated as an atomic, unsplittable unit during word wrapping (similar to a single word).

```java
InlineImageSegment pngInline = InlineImageSegment.fromFile(
        new File("chart.png"), 36, 14);  // widthPt, heightPt

InlineImageSegment jpgInline = InlineImageSegment.fromFile(
        new File("icon.jpg"), 36, 14);
```

### Inline Image Placement

```java
// Image in the middle of text
RichTextLine middleLine = new RichTextLine(Arrays.asList(
        new RichTextSegment("Revenue grew by ", EnumSet.noneOf(TextStyle.class), 10f),
        pngInline,
        new RichTextSegment(" compared to last quarter.", EnumSet.noneOf(TextStyle.class), 10f)
), ListType.NONE, 0, TextAlignment.LEFT);

// Image at the start
RichTextLine startLine = new RichTextLine(Arrays.asList(
        jpgInline,
        new RichTextSegment(" This text follows the image.", EnumSet.noneOf(TextStyle.class), 10f)
), ListType.NONE, 0, TextAlignment.LEFT);

// Image at the end
RichTextLine endLine = new RichTextLine(Arrays.asList(
        new RichTextSegment("This text precedes an image: ", EnumSet.noneOf(TextStyle.class), 10f),
        pngInline
), ListType.NONE, 0, TextAlignment.LEFT);

// Multiple images side-by-side with text
RichTextLine sideBySide = new RichTextLine(Arrays.asList(
        pngInline,
        new RichTextSegment(" between ", EnumSet.of(TextStyle.BOLD), 10f),
        jpgInline,
        new RichTextSegment(" and more ", EnumSet.noneOf(TextStyle.class), 10f),
        pngInline
), ListType.NONE, 0, TextAlignment.LEFT);

// Image-only line (no text)
RichTextLine imageOnly = new RichTextLine(Arrays.asList(
        pngInline, jpgInline, pngInline
), ListType.NONE, 0, TextAlignment.CENTER);
```

### Inline Image Factory Methods

| Method | Description |
|--------|-------------|
| `InlineImageSegment.of(BufferedImage, widthPt, heightPt)` | From a `BufferedImage` |
| `InlineImageSegment.fromFile(File, widthPt, heightPt)` | From a file (PNG, JPEG, etc.) |
| `InlineImageSegment.fromStream(InputStream, widthPt, heightPt)` | From an `InputStream` |
| `InlineImageSegment.fromBase64(String, widthPt, heightPt)` | From a Base64 or data-URI string |

---

## Lists

### Bulleted Lists

```java
List<RichTextLine> bulletItems = Arrays.asList(
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("First item", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0),
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("Second item", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0),
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("Third item", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0));

ContentElement bulletList = new ListContentElement(ListType.BULLETED, bulletItems);
```

### Numbered Lists

```java
List<RichTextLine> numberedItems = Arrays.asList(
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("Step one", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0),
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("Step two", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0));

ContentElement numberedList = new ListContentElement(ListType.NUMBERED, numberedItems);
```

### Lists with Mixed Formatting

List items can contain mixed-style segments:

```java
List<RichTextLine> styledBullets = Arrays.asList(
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("Plain bullet item",
                        EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0),
        new RichTextLine(Collections.singletonList(
                new RichTextSegment("Bold bullet item",
                        EnumSet.of(TextStyle.BOLD), 10f)),
                ListType.NONE, 0),
        new RichTextLine(Arrays.asList(
                new RichTextSegment("Mixed: ",
                        EnumSet.noneOf(TextStyle.class), 10f),
                new RichTextSegment("bold+italic",
                        EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 10f),
                new RichTextSegment(" in a bullet",
                        EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0));

ContentElement list = new ListContentElement(ListType.BULLETED, styledBullets);
```

---

## Paragraph Breaks

To add vertical space between content elements (paragraph breaks), insert a spacer `TextContentElement` containing blank lines:

```java
private static TextContentElement paragraphBreak(int lines, float fontSize) {
    List<RichTextLine> spacerLines = new ArrayList<>();
    for (int i = 0; i < lines; i++) {
        spacerLines.add(new RichTextLine(
                Collections.singletonList(
                        new RichTextSegment(" ", EnumSet.noneOf(TextStyle.class), fontSize)),
                ListType.NONE, 0, TextAlignment.LEFT));
    }
    return new TextContentElement(spacerLines);
}
```

Usage:

```java
RichTextBlock.builder()
        .addContent(new TextContentElement(paragraph1))
        .addContent(paragraphBreak(1, 10f))     // 1 blank line at 10pt line height
        .addContent(new TextContentElement(paragraph2))
        // ...
```

---

## Overflow Handling

When content exceeds the block's declared height, the block stops rendering and (by default) shows an ellipsis (`…`) at the bottom.

```java
RichTextBlock.Builder builder = RichTextBlock.builder()
        .at(50, 50).size(495, 200)           // small height
        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                "Overflow Demonstration", TextAlignment.CENTER)
        .showOverflowIndicator(true)         // show "…" (default)
        .drawBorder(true);

for (int i = 1; i <= 20; i++) {
    RichTextLine line = new RichTextLine(Collections.singletonList(
            new RichTextSegment("Line " + i + ": Lorem ipsum dolor sit amet...",
                    EnumSet.noneOf(TextStyle.class), 9f)),
            ListType.NONE, 0, TextAlignment.LEFT);
    builder.addContent(new TextContentElement(line));
}

RichTextBlock block = builder.build();
block.renderOnNewPage(doc, PDRectangle.A4, false);
```

Set `.showOverflowIndicator(false)` to silently truncate without the ellipsis.

---

## Content Height Estimation

Use `estimateContentHeight()` to measure how tall a block's content is, then size the block to exactly fit:

```java
RichTextBlock.Builder b = RichTextBlock.builder()
        .at(x, y)
        .size(width, 9999f)                  // temporary large height
        .blockPadding(8f)
        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 13,
                "Auto-sized Block", TextAlignment.LEFT)
        .addContent(new TextContentElement(paragraph));

float estimatedHeight = b.build().estimateContentHeight();
RichTextBlock block = b.size(width, estimatedHeight + 10f).build();  // add a small buffer
```

---

## Chaining Multiple Blocks

The `render()` method returns the final PDF Y-coordinate where content ended. Use this to stack blocks vertically without overlap:

```java
float pageHeight = pageSize.getHeight();
float currentTopDownY = 30f;               // starting Y from top of page
float blockGap = 15f;                      // gap between blocks

// Block 1
RichTextBlock.Builder b1 = RichTextBlock.builder()
        .at(blockX, currentTopDownY)
        .size(blockWidth, 9999f)
        .addContent(new TextContentElement(introLine))
        .drawBorder(true);
float h1 = b1.build().estimateContentHeight();
RichTextBlock block1 = b1.size(blockWidth, h1 + 10f).build();

float pdfY1 = block1.render(doc, stream, pageHeight);

// Position Block 2 directly below Block 1
currentTopDownY = pageHeight - pdfY1 + blockGap;

RichTextBlock.Builder b2 = RichTextBlock.builder()
        .at(blockX, currentTopDownY)
        .size(blockWidth, 9999f)
        .addContent(new TextContentElement(detailLine))
        .drawBorder(true);
float h2 = b2.build().estimateContentHeight();
RichTextBlock block2 = b2.size(blockWidth, h2 + 10f).build();

float pdfY2 = block2.render(doc, stream, pageHeight);
```

---

## Side-by-Side Blocks (Column Layout)

Multiple blocks can be rendered independently on the same page at different X positions:

```java
// Left column
RichTextBlock mainBlock = RichTextBlock.builder()
        .at(30, 30).size(370, 500)
        .header(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN), 16,
                "Quarterly Report Summary", TextAlignment.CENTER)
        .addContent(new TextContentElement(paragraph))
        .addContent(new ImageContentElement.Builder(jpgFile)
                .size(200, 60).alignment(TextAlignment.CENTER).build())
        .addContent(new ListContentElement(ListType.BULLETED, bullets))
        .drawBorder(true)
        .build();

// Right column
RichTextBlock sideBlock = RichTextBlock.builder()
        .at(420, 30).size(340, 500)
        .blockPadding(10f)
        .header(FontUtils.getFontSet(Standard14FontFamily.COURIER), 12,
                "Side Notes", TextAlignment.LEFT)
        .addContent(new TextContentElement(sideParagraph))
        .drawBorder(true)
        .build();

// Render both on the same page and stream
mainBlock.render(doc, stream, pageHeight);
sideBlock.render(doc, stream, pageHeight);
```

---

## Landscape Orientation

Create a landscape page by swapping width and height of the page rectangle:

```java
PDRectangle landscape = new PDRectangle(
        PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
PDPage page = new PDPage(landscape);
doc.addPage(page);
```

Or use the convenience method `renderOnNewPage`:

```java
block.renderOnNewPage(doc, PDRectangle.A4, true);  // true = landscape
```

Both A3, A4, and LETTER page sizes are supported in either orientation. The block automatically respects the page dimensions.

---

## Rendering on an Existing Page

Use `render()` to draw on a page you already created:

```java
PDPage page = new PDPage(PDRectangle.A4);
doc.addPage(page);

PageContentStreamOptimized stream = new PageContentStreamOptimized(
        new PDPageContentStream(doc, page));
try {
    float finalY = block.render(doc, stream, PDRectangle.A4.getHeight());
    // finalY is the PDF Y-coordinate where content ended
} finally {
    stream.close();
}
```

---

## Rendering on a New Page

Use `renderOnNewPage()` to let the block create and manage its own page:

```java
float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
// false = portrait, true = landscape
```

This convenience method creates a new page, renders the block, and closes the content stream automatically.

---

## Text Wrapping Behaviour

Text is automatically wrapped at word boundaries to fit within the block's inner width (block width minus padding on both sides).

- **Normal wrapping:** Text splits at word boundaries when a word doesn't fit on the current line
- **Character-level break:** If a single word is wider than the available width, it is broken character by character (with at least one character per line to prevent infinite loops)
- **Multi-segment wrapping:** Style boundaries are preserved across line breaks — if a bold segment wraps to the next line, the continuation remains bold

```java
// Narrow block (150pt) forces wrapping
RichTextBlock narrowBlock = RichTextBlock.builder()
        .at(30, 30).size(150, 400)
        .blockPadding(8f)
        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 11,
                "Narrow Block", TextAlignment.CENTER)
        .addContent(new TextContentElement(longTextLine))
        .drawBorder(true)
        .build();
```

---

## Debug Border

Enable `.drawBorder(true)` to render a light-gray border around the block bounds. This is useful during development to visualize block positioning and overlap detection:

```java
RichTextBlock block = RichTextBlock.builder()
        .at(40, 30).size(500, 300)
        .drawBorder(true)     // shows a light-gray rectangle
        // ...
        .build();
```

---

## Complete Example

This comprehensive example combines headers, body text with mixed formatting, bulleted and numbered lists, and block-level images into a single document:

```java
try (PDDocument doc = new PDDocument()) {
    PDRectangle landscape = new PDRectangle(
            PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
    PDPage page = new PDPage(landscape);
    doc.addPage(page);

    PageContentStreamOptimized stream = new PageContentStreamOptimized(
            new PDPageContentStream(doc, page));

    // Mixed-style paragraph with bold emphasis
    RichTextLine paragraph = new RichTextLine(Arrays.asList(
            new RichTextSegment("Revenue grew by ", EnumSet.noneOf(TextStyle.class), 10f),
            new RichTextSegment("25%", EnumSet.of(TextStyle.BOLD), 10f),
            new RichTextSegment(" compared to last quarter.", EnumSet.noneOf(TextStyle.class), 10f)
    ), ListType.NONE, 0, TextAlignment.JUSTIFY);

    // Confidential subtitle (italic + underline, centered)
    RichTextLine subtitle = new RichTextLine(Collections.singletonList(
            new RichTextSegment("-- Confidential --",
                    EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 9f)),
            ListType.NONE, 0, TextAlignment.CENTER);

    // Date line (right-aligned)
    RichTextLine dateLine = new RichTextLine(Collections.singletonList(
            new RichTextSegment("Date: 2026-03-11",
                    EnumSet.noneOf(TextStyle.class), 8f)),
            ListType.NONE, 0, TextAlignment.RIGHT);

    // Bulleted list
    List<RichTextLine> bullets = Arrays.asList(
            new RichTextLine(Collections.singletonList(
                    new RichTextSegment("North America: strong performance",
                            EnumSet.of(TextStyle.ITALIC), 9f)),
                    ListType.NONE, 0),
            new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Europe: steady growth",
                            EnumSet.noneOf(TextStyle.class), 9f)),
                    ListType.NONE, 0),
            new RichTextLine(Collections.singletonList(
                    new RichTextSegment("APAC: emerging opportunities",
                            EnumSet.of(TextStyle.BOLD), 9f)),
                    ListType.NONE, 0));

    // Numbered list
    List<RichTextLine> numbered = Arrays.asList(
            new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Expand into APAC markets",
                            EnumSet.of(TextStyle.UNDERLINE), 9f)),
                    ListType.NONE, 0),
            new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Increase R&D budget by 15%",
                            EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 9f)),
                    ListType.NONE, 0));

    File jpgFile = new File("path/to/image.jpg");

    RichTextBlock block = RichTextBlock.builder()
            .at(30, 30).size(370, 500)
            .header(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN), 16,
                    "Quarterly Report Summary", TextAlignment.CENTER)
            .addContent(new TextContentElement(paragraph))
            .addContent(new TextContentElement(subtitle))
            .addContent(new TextContentElement(dateLine))
            .addContent(new ImageContentElement.Builder(jpgFile)
                    .size(200, 60).alignment(TextAlignment.CENTER).build())
            .addContent(new ListContentElement(ListType.BULLETED, bullets))
            .addContent(new ListContentElement(ListType.NUMBERED, numbered))
            .showOverflowIndicator(true)
            .drawBorder(true)
            .build();

    block.render(doc, stream, landscape.getHeight());
    stream.close();

    doc.save("ComprehensiveReport.pdf");
}
```
