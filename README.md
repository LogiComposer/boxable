# Boxable - Java PDF Table Library

Boxable is a powerful and flexible Java library for creating professional tables in PDF documents. Built on top of [Apache PDFBox](https://pdfbox.apache.org/), it provides an intuitive API for developers to generate complex tables with rich formatting, styling, and content options.

## Key Benefits

- 🚀 **Easy to Use**: Simple, intuitive API that gets you started quickly
- 🎨 **Rich Styling**: Support for colors, fonts, borders, alignment, and more
- 📊 **Flexible Data Sources**: Import from CSV, Lists, or build tables programmatically
- 🖼️ **Multimedia Support**: Embed images and HTML content in cells
- 📱 **Multi-page**: Automatic page breaks and multi-page table support
- ⚡ **High Performance**: Optimized for large datasets and complex layouts
- 🔧 **Highly Configurable**: Extensive customization options for professional output

## Features

### Core Table Features
- ✅ Create tables from scratch with full programmatic control
- ✅ Import CSV data directly into PDF tables
- ✅ Convert Java Lists into formatted tables
- ✅ Multi-page table support with automatic page breaks
- ✅ Header rows that repeat on each page

### Rich Content Support
- ✅ **HTML Tags**: Support for `<p>`, `<i>`, `<b>`, `<br>`, `<ul>`, `<ol>`, `<li>` tags
- ✅ **Images**: Embed images inside cells with scaling support
- ✅ **Hyperlinks**: Clickable URLs in cell content
- ✅ **Unicode**: Full Unicode text support including special characters

### Advanced Styling
- ✅ **Text Alignment**: Horizontal (left, center, right) and vertical alignment
- ✅ **Fonts**: Support for built-in and custom fonts
- ✅ **Colors**: Background fills and text colors
- ✅ **Borders**: Customizable border styles and colors
- ✅ **Text Effects**: Underline, bold, italic, and text rotation (90 degrees)
- ✅ **Cell Padding**: Configurable cell padding and spacing

## Installation

### Maven
```xml
<dependency>
    <groupId>com.logi.composer</groupId>
    <artifactId>boxable</artifactId>
    <version>1.8.0</version>
</dependency>
```

### Gradle
```gradle
implementation 'com.logi.composer:boxable:1.8.0'
```

### SBT
```scala
libraryDependencies += "com.logi.composer" % "boxable" % "1.8.0"
```

### Required Dependencies

Boxable requires the following dependencies which will be automatically included:
- **Apache PDFBox 3.0.2+**: Core PDF manipulation library
- **Apache Commons CSV 1.9.0+**: For CSV data parsing
- **SLF4J API 1.7.36+**: For logging

## Quick Start Guide

Here's a complete example that creates a PDF with a simple table:

```java
import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.awt.Color;
import java.io.IOException;

public class BoxableQuickStart {
    public static void main(String[] args) throws IOException {
        // Create a new PDF document
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        // Set up table parameters
        float margin = 50;
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        float bottomMargin = 70;
        
        // Create the table
        BaseTable table = new BaseTable(yStartNewPage, yStartNewPage, bottomMargin, 
                                      tableWidth, margin, document, page, true, true);
        
        // Create header row
        Row<PDPage> headerRow = table.createRow(20f);
        Cell<PDPage> cell = headerRow.createCell(25, "Name");
        cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
        cell.setFillColor(Color.LIGHT_GRAY);
        
        cell = headerRow.createCell(25, "Age");
        cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
        cell.setFillColor(Color.LIGHT_GRAY);
        
        cell = headerRow.createCell(50, "Email");
        cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
        cell.setFillColor(Color.LIGHT_GRAY);
        
        table.addHeaderRow(headerRow);
        
        // Add data rows
        String[][] data = {
            {"John Doe", "30", "john.doe@example.com"},
            {"Jane Smith", "25", "jane.smith@example.com"},
            {"Bob Johnson", "35", "bob.johnson@example.com"}
        };
        
        for (String[] rowData : data) {
            Row<PDPage> dataRow = table.createRow(15f);
            dataRow.createCell(25, rowData[0]);
            dataRow.createCell(25, rowData[1]);
            dataRow.createCell(50, rowData[2]);
        }
        
        // Draw the table
        table.draw();
        
        // Save the document
        document.save("quick-start-example.pdf");
        document.close();
        
        System.out.println("PDF created successfully!");
    }
}
```


## Core Concepts

Understanding the key classes and their relationships will help you use Boxable effectively:

### Key Classes

- **`BaseTable`**: The main table class that handles layout, page management, and rendering
- **`Row<T>`**: Represents a table row containing multiple cells
- **`Cell<T>`**: Individual table cells with content, styling, and formatting options
- **`DataTable`**: Utility class for importing CSV data and Lists into tables
- **`Paragraph`**: Text formatting and layout within cells, supports HTML tags

### Table Hierarchy
```
PDDocument
├── PDPage(s)
    └── BaseTable
        ├── HeaderRows (optional, repeat on each page)
        └── DataRows
            └── Cell(s)
                └── Content (text, images, paragraphs)
```

### Constructor Parameters Explained

The `BaseTable` constructor requires several important parameters:

```java
BaseTable(float yStart, float yStartNewPage, float bottomMargin, 
          float tableWidth, float margin, PDDocument document, 
          PDPage currentPage, boolean drawLines, boolean drawContent)
```

- **`yStart`**: Y-coordinate where the table starts on the first page
- **`yStartNewPage`**: Y-coordinate where the table starts on subsequent pages
- **`bottomMargin`**: Minimum margin from the bottom of the page
- **`tableWidth`**: Total width of the table in points
- **`margin`**: Left margin from the page edge
- **`document`**: The PDDocument instance
- **`currentPage`**: The PDPage where the table begins
- **`drawLines`**: Whether to draw cell borders (true/false)
- **`drawContent`**: Whether to draw cell content (true/false, useful for testing layouts)

### Debug Mode

Enable debug information for troubleshooting:

```java
// Enable border drawing to see cell boundaries
BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, 
                               margin, document, page, true, true);

// Use different colors for debugging
cell.setFillColor(Color.YELLOW); // Temporary debug color
cell.setBorderStyle(new LineStyle(Color.RED, 2)); // Visible borders
```

## Building from Source

To build Boxable from source:

```bash
# Clone the repository
git clone https://github.com/LogiComposer/boxable.git
cd boxable

# Build with Maven
mvn clean compile

# Run tests
mvn test

# Create JAR
mvn package

# Install to local repository
mvn install
```

### Requirements
- Java 8 or higher
- Maven 3.6 or higher
- Apache PDFBox 3.0.2+


### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Ensure all tests pass: `mvn test`
5. Submit a pull request


## API Reference

### Core Classes

| Class | Description |
|-------|-------------|
| `BaseTable` | Main table implementation with page management |
| `Row<T>` | Table row containing cells |
| `Cell<T>` | Individual table cell with content and styling |
| `DataTable` | Utility for CSV and List data import |
| `Paragraph` | Text formatting with HTML support |

### Enums

| Enum | Values | Description |
|------|--------|-------------|
| `HorizontalAlignment` | LEFT, CENTER, RIGHT | Text horizontal alignment |
| `VerticalAlignment` | TOP, MIDDLE, BOTTOM | Text vertical alignment |
| `TextType` | Various text formatting options | Text styling options |

### Supported HTML Tags

| Tag | Description | Example |
|-----|-------------|---------|
| `<b>` | Bold text | `<b>Bold</b>` |
| `<i>` | Italic text | `<i>Italic</i>` |
| `<br>` | Line break | `Line 1<br>Line 2` |
| `<p>` | Paragraph | `<p>Paragraph text</p>` |
| `<ul>` | Unordered list | `<ul><li>Item</li></ul>` |
| `<ol>` | Ordered list | `<ol><li>Item</li></ol>` |
| `<li>` | List item | `<li>List item</li>` |

## Contributors

Special thanks to these awesome contributors who have helped make Boxable better:

- [@johnmanko](https://github.com/johnmanko) - Core development and features
- [@Vobarian](https://github.com/vobarian) - Bug fixes and improvements
- [@Giboow](https://github.com/giboow) - Feature enhancements
- [@Ogmios-Voice](https://github.com/ogmios-voice) - Documentation and testing
- [@zaqpiotr](https://github.com/zaqpiotr) - Performance optimizations
- [@Frulenzo](https://github.com/Frulenzo) - HTML support improvements
- [@dgautier](https://github.com/dgautier) - Core architecture
- [@ZeerDonker](https://github.com/ZeerDonker) - Font management
- [@dobluth](https://github.com/dobluth) - Image handling
- [@schmitzhermes](https://github.com/schmitzhermes) - Multi-page support


---

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
