# SsrPageProvider Implementation

This document describes the implementation of the SsrPageProvider class and its integration with the Boxable library.

## Overview

The SsrPageProvider (Server-Side Rendering Page Provider) is a new implementation of the PageProvider interface that adds automatic footer functionality to PDF pages. It provides:

- Page management for PDDocument instances
- Automatic footer generation with page numbers, dates, and custom text
- Integration with Table.java's drawRow method for automatic footer insertion
- Static utility methods for standalone footer operations

## Classes Implemented

### 1. SsrPageProvider

**Location**: `be.quodlibet.boxable.page.SsrPageProvider`

A PageProvider implementation that automatically adds footers to pages when they are created or navigated to.

**Key Features**:
- Implements PageProvider<PDPage> interface
- Automatic footer insertion during page operations
- Configurable through PageFooterDetails
- Custom font support for footer text
- Error handling through ReportException

**Usage Example**:
```java
// Create custom footer details
PageFooterDetails footerDetails = new PageFooterDetails.Builder()
    .withPageNumbers(true)
    .withDate(true)
    .withTrademarkText("© 2023 My Company")
    .withFontSize(8f)
    .build();

// Create SsrPageProvider
SsrPageProvider pageProvider = new SsrPageProvider(
    document, 
    PDRectangle.A4, 
    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 
    footerDetails
);

// Use with BaseTable
BaseTable table = new BaseTable(
    750f, 750f, 10f, 50f, 500f, 50f,
    document, pageProvider.nextPage(),
    true, true, pageProvider
);
```

### 2. PageFooterDetails

**Location**: `be.quodlibet.boxable.PageFooterDetails`

Configuration class for footer content and formatting using the builder pattern.

**Features**:
- Builder pattern for easy configuration
- Date formatting options
- Page number inclusion control
- Custom trademark text
- Font size and margin configuration

### 3. ReportException

**Location**: `be.quodlibet.boxable.ReportException`

Custom exception for report generation errors, specifically used for footer operation failures.

## Integration with Table.java

The implementation integrates with Table.java's drawRow method at two key points:

1. **Before page break** (line ~281): Adds footer to current page before creating a new page
2. **After potential last row** (line ~749): Adds footer when detecting that the next row would cause a page break

The integration is **performance-conscious** and only activates when:
- The pageProvider is an instance of SsrPageProvider
- The row being processed is not a header row
- A page break condition is detected

## Static Utility Methods

The SsrPageProvider class provides static methods for standalone footer operations:

```java
// Add page numbers only
SsrPageProvider.appendPageNumbers(document, page, font, fontSize);

// Add complete footer with custom details
SsrPageProvider.appendPageFooter(document, page, font, footerDetails);
```

## Testing

Comprehensive test coverage includes:
- 10 unit tests for SsrPageProvider functionality
- 3 integration tests demonstrating end-to-end usage
- PDF generation validation
- Error handling verification
- Static method testing

All tests pass and demonstrate that the implementation:
- Maintains backward compatibility (119/119 tests pass)
- Generates valid PDF documents with footers
- Handles error conditions gracefully
- Performs efficiently with minimal overhead

## Performance Considerations

The implementation is designed for minimal performance impact:

1. **Conditional activation**: Footer functionality only activates when SsrPageProvider is used
2. **Efficient footer rendering**: Uses PDFBox's optimized content stream operations
3. **Minimal memory overhead**: PageFooterDetails uses immutable builder pattern
4. **Error isolation**: Footer errors don't break table rendering (logged and continued)
5. **Conservative row estimation**: Uses 20f pixels as conservative estimate for next row height detection

## Usage Recommendations

1. Use SsrPageProvider when you need automatic footers on multi-page documents
2. Configure PageFooterDetails according to your branding requirements
3. Handle ReportException appropriately in your error handling strategy
4. Consider performance implications for very large documents (footer is added to every page)

## Demo

A complete demonstration is available in `SsrPageProviderDemo.java` which creates a multi-page PDF with:
- 40 rows of sample data forcing multiple page breaks
- Automatic footer generation on each page
- Page numbering and timestamp
- Custom trademark text
- Styled table headers and alternating row colors

The demo output is saved as `target/SsrPageProviderDemo.pdf` and demonstrates all features working together.