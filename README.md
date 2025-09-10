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
    <version>1.7.7</version>
</dependency>
```

### Gradle
```gradle
implementation 'com.logi.composer:boxable:1.7.7'
```

### SBT
```scala
libraryDependencies += "com.logi.composer" % "boxable" % "1.7.7"
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

## Comprehensive Examples

### 1. CSV Data Import

Convert CSV data directly into a PDF table:

```java
import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.datatable.DataTable;

public void createTableFromCSV() throws IOException {
    // CSV data (could be loaded from file)
    String csvData = "Name,Age,City,Country\n" +
                    "John Doe,30,New York,USA\n" +
                    "Jane Smith,25,London,UK\n" +
                    "Pierre Dubois,35,Paris,France\n" +
                    "Anna Müller,28,Berlin,Germany";
    
    // Create document and page
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    
    // Table setup
    float margin = 50;
    float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
    float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
    
    // Create table
    BaseTable table = new BaseTable(yStartNewPage, yStartNewPage, 70, 
                                  tableWidth, margin, document, page, true, true);
    
    // Import CSV data
    DataTable dataTable = new DataTable(table, page);
    dataTable.addCsvToTable(csvData, DataTable.HASHEADER, ',');
    
    // Draw and save
    table.draw();
    document.save("csv-example.pdf");
    document.close();
}
```

### 2. List Data Import

Convert Java Lists into tables:

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public void createTableFromList() throws IOException {
    // Prepare data as List of Lists
    List<List<String>> data = new ArrayList<>();
    data.add(Arrays.asList("Product", "Price", "Stock", "Category"));
    data.add(Arrays.asList("Laptop", "$999", "15", "Electronics"));
    data.add(Arrays.asList("Book", "$25", "100", "Education"));
    data.add(Arrays.asList("Coffee", "$5", "50", "Beverages"));
    
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    
    float margin = 50;
    float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
    float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
    
    BaseTable table = new BaseTable(yStartNewPage, yStartNewPage, 70, 
                                  tableWidth, margin, document, page, true, true);
    
    DataTable dataTable = new DataTable(table, page);
    dataTable.addListToTable(data, DataTable.HASHEADER);
    
    table.draw();
    document.save("list-example.pdf");
    document.close();
}
```

### 3. Advanced Styling and Formatting

Create a professionally styled table with various formatting options:

```java
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.VerticalAlignment;
import be.quodlibet.boxable.line.LineStyle;

public void createStyledTable() throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    
    float margin = 50;
    float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
    float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
    
    BaseTable table = new BaseTable(yStartNewPage, yStartNewPage, 70, 
                                  tableWidth, margin, document, page, true, true);
    
    // Create styled header
    Row<PDPage> headerRow = table.createRow(25f);
    Cell<PDPage> cell = headerRow.createCell(30, "Employee");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFontSize(12);
    cell.setFillColor(new Color(52, 73, 93)); // Dark blue
    cell.setTextColor(Color.WHITE);
    cell.setAlign(HorizontalAlignment.CENTER);
    cell.setValign(VerticalAlignment.MIDDLE);
    
    cell = headerRow.createCell(25, "Department");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFontSize(12);
    cell.setFillColor(new Color(52, 73, 93));
    cell.setTextColor(Color.WHITE);
    cell.setAlign(HorizontalAlignment.CENTER);
    
    cell = headerRow.createCell(20, "Salary");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFontSize(12);
    cell.setFillColor(new Color(52, 73, 93));
    cell.setTextColor(Color.WHITE);
    cell.setAlign(HorizontalAlignment.RIGHT);
    
    cell = headerRow.createCell(25, "Status");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFontSize(12);
    cell.setFillColor(new Color(52, 73, 93));
    cell.setTextColor(Color.WHITE);
    cell.setAlign(HorizontalAlignment.CENTER);
    
    table.addHeaderRow(headerRow);
    
    // Add styled data rows
    String[][] employees = {
        {"Alice Johnson", "Engineering", "$85,000", "Active"},
        {"Bob Smith", "Marketing", "$65,000", "Active"},
        {"Carol White", "HR", "$70,000", "On Leave"},
        {"David Brown", "Engineering", "$90,000", "Active"}
    };
    
    for (int i = 0; i < employees.length; i++) {
        Row<PDPage> row = table.createRow(18f);
        
        // Alternate row colors
        Color fillColor = i % 2 == 0 ? new Color(236, 240, 241) : Color.WHITE;
        
        for (int j = 0; j < employees[i].length; j++) {
            float[] widths = {30, 25, 20, 25};
            cell = row.createCell(widths[j], employees[i][j]);
            cell.setFillColor(fillColor);
            cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            cell.setFontSize(10);
            
            // Right-align salary column
            if (j == 2) {
                cell.setAlign(HorizontalAlignment.RIGHT);
            }
            
            // Color-code status
            if (j == 3) {
                if ("Active".equals(employees[i][j])) {
                    cell.setTextColor(new Color(39, 174, 96)); // Green
                } else {
                    cell.setTextColor(new Color(231, 76, 60)); // Red
                }
                cell.setAlign(HorizontalAlignment.CENTER);
            }
        }
    }
    
    table.draw();
    document.save("styled-table.pdf");
    document.close();
}
```

### 4. Images in Tables

Embed images within table cells:

```java
import be.quodlibet.boxable.image.Image;
import be.quodlibet.boxable.utils.ImageUtils;
import java.io.File;

public void createTableWithImages() throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    
    // ... table setup code ...
    
    Row<PDPage> row = table.createRow(40f); // Taller row for images
    
    // Text cell
    Cell<PDPage> textCell = row.createCell(50, "Product Description");
    textCell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
    
    // Image cell
    File imageFile = new File("product-image.jpg");
    if (imageFile.exists()) {
        Image image = ImageUtils.readImage(imageFile);
        Cell<PDPage> imageCell = row.createImageCell(50, image);
        imageCell.setAlign(HorizontalAlignment.CENTER);
        imageCell.setValign(VerticalAlignment.MIDDLE);
    }
    
    table.draw();
    document.save("table-with-images.pdf");
    document.close();
}
```

### 5. HTML Content in Cells

Use HTML tags for rich text formatting:

```java
public void createTableWithHTML() throws IOException {
    // ... document and table setup ...
    
    Row<PDPage> row = table.createRow(30f);
    
    // Cell with HTML content
    String htmlContent = "<b>Bold text</b> with <i>italic</i> and <br/>line breaks.<br/>" +
                        "<ul><li>Bullet point 1</li><li>Bullet point 2</li></ul>";
    
    Cell<PDPage> cell = row.createCell(100, htmlContent);
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
    cell.setFontSize(10);
    
    table.draw();
    document.save("html-content.pdf");
    document.close();
}
```

### 6. Multi-page Tables

Tables automatically span multiple pages when content exceeds page boundaries:

```java
public void createMultiPageTable() throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    
    float margin = 50;
    float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
    float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
    
    BaseTable table = new BaseTable(yStartNewPage, yStartNewPage, 70, 
                                  tableWidth, margin, document, page, true, true);
    
    // Create header that will repeat on each page
    Row<PDPage> headerRow = table.createRow(20f);
    Cell<PDPage> cell = headerRow.createCell(25, "ID");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFillColor(Color.LIGHT_GRAY);
    
    cell = headerRow.createCell(50, "Description");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFillColor(Color.LIGHT_GRAY);
    
    cell = headerRow.createCell(25, "Value");
    cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    cell.setFillColor(Color.LIGHT_GRAY);
    
    table.addHeaderRow(headerRow);
    
    // Add many rows to demonstrate page breaks
    for (int i = 1; i <= 100; i++) {
        Row<PDPage> dataRow = table.createRow(15f);
        dataRow.createCell(25, String.valueOf(i));
        dataRow.createCell(50, "Description for item " + i);
        dataRow.createCell(25, "$" + (i * 10));
    }
    
    table.draw();
    document.save("multi-page-table.pdf");
    document.close();
}
```

## Advanced Configuration

### Font Management

Boxable supports various font options for professional document styling:

```java
import be.quodlibet.boxable.FontSet;
import be.quodlibet.boxable.utils.FontUtils;

// Using built-in fonts
PDFont helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
PDFont helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
PDFont times = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

// Custom fonts (TrueType)
PDFont customFont = PDType0Font.load(document, new File("custom-font.ttf"));

// Font sets for consistent styling
FontSet fontSet = FontUtils.getDefaultFontSet();
table.setFontSet(fontSet);
```

### Cell Customization Options

```java
Cell<PDPage> cell = row.createCell(width, content);

// Text formatting
cell.setFont(font);
cell.setFontSize(12);
cell.setTextColor(Color.BLACK);
cell.setTextUnderline(true);

// Alignment
cell.setAlign(HorizontalAlignment.CENTER);  // LEFT, CENTER, RIGHT
cell.setValign(VerticalAlignment.MIDDLE);   // TOP, MIDDLE, BOTTOM

// Styling
cell.setFillColor(Color.LIGHT_GRAY);
cell.setBorderStyle(new LineStyle(Color.BLACK, 2));

// Padding
cell.setLeftPadding(10);
cell.setRightPadding(10);
cell.setTopPadding(5);
cell.setBottomPadding(5);

// Links
cell.setUrl(new URL("https://example.com"));
```

### Custom Page Providers

For advanced page management and custom page layouts:

```java
import be.quodlibet.boxable.page.PageProvider;

PageProvider<PDPage> customPageProvider = new PageProvider<PDPage>() {
    @Override
    public PDPage createPage() {
        PDPage page = new PDPage(PDRectangle.A4);
        // Add custom page setup (headers, footers, etc.)
        return page;
    }
    
    @Override
    public PDPage nextPage(int pageNumber, PDPage previousPage) {
        return createPage();
    }
};

BaseTable table = new BaseTable(yStart, yStartNewPage, pageTopMargin, bottomMargin, 
                               tableWidth, margin, document, page, true, true, 
                               customPageProvider);
```

### Performance Optimization

For large datasets and optimal performance:

```java
// 1. Use streaming for large CSV files
try (FileInputStream fis = new FileInputStream("large-data.csv");
     InputStreamReader isr = new InputStreamReader(fis);
     BufferedReader reader = new BufferedReader(isr)) {
    
    // Process CSV in chunks
    DataTable dataTable = new DataTable(table, page);
    // Custom processing logic here
}

// 2. Optimize memory usage
document.getDocumentInformation().setCreator("Boxable");
document.getDocumentInformation().setProducer("Your Application");

// 3. Control line drawing for performance
boolean drawLines = false; // Set to false for faster rendering during development
boolean drawContent = true;
BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, 
                               margin, document, page, drawLines, drawContent);
```

## Best Practices

### 1. Document Structure
```java
// Always use try-with-resources for proper cleanup
try (PDDocument document = new PDDocument()) {
    // Create your tables
    table.draw();
    document.save("output.pdf");
} catch (IOException e) {
    logger.error("Error creating PDF", e);
}
```

### 2. Memory Management
```java
// For large documents, save periodically and recreate document
if (pageCount > 100) {
    document.save("part-" + partNumber + ".pdf");
    document.close();
    document = new PDDocument(); // Start fresh
}
```

### 3. Error Handling
```java
try {
    table.draw();
} catch (IOException e) {
    // Handle PDF generation errors
    logger.error("Failed to generate table", e);
} catch (IllegalArgumentException e) {
    // Handle invalid table parameters
    logger.error("Invalid table configuration", e);
}
```

### 4. Responsive Design
```java
// Calculate dynamic column widths
float pageWidth = page.getMediaBox().getWidth() - (2 * margin);
float[] columnWidths = {pageWidth * 0.3f, pageWidth * 0.4f, pageWidth * 0.3f};

for (int i = 0; i < columnWidths.length; i++) {
    cell = row.createCell(columnWidths[i], data[i]);
}
```

## Troubleshooting

### Common Issues and Solutions

#### Issue: Table doesn't fit on page
```java
// Solution: Check your margins and table width
float availableWidth = page.getMediaBox().getWidth() - (2 * margin);
float tableWidth = availableWidth; // Use full available width

// Or reduce column widths proportionally
float totalWidth = 0;
for (float width : columnWidths) totalWidth += width;
if (totalWidth > availableWidth) {
    float scale = availableWidth / totalWidth;
    for (int i = 0; i < columnWidths.length; i++) {
        columnWidths[i] *= scale;
    }
}
```

#### Issue: Text is cut off in cells
```java
// Solution: Increase row height or enable text wrapping
Row<PDPage> row = table.createRow(25f); // Increase height

// Or use automatic height calculation
cell.setTextWrap(true); // If available in your version
```

#### Issue: Memory issues with large tables
```java
// Solution: Process data in chunks
int batchSize = 1000;
for (int start = 0; start < totalRows; start += batchSize) {
    int end = Math.min(start + batchSize, totalRows);
    List<String[]> batch = data.subList(start, end);
    // Process batch
    
    if (start > 0 && start % (batchSize * 10) == 0) {
        // Occasional cleanup
        System.gc();
    }
}
```

#### Issue: Fonts not displaying correctly
```java
// Solution: Use embedded fonts for special characters
PDFont font = PDType0Font.load(document, 
    getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf"));
cell.setFont(font);
```

#### Issue: Images not scaling properly
```java
// Solution: Control image scaling
Image image = ImageUtils.readImage(imageFile);
image.scale(0.5f); // Scale to 50%

// Or set specific dimensions
float maxWidth = 100;
float maxHeight = 80;
if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
    float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
    image.scale(scale);
}
```

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

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Ensure all tests pass: `mvn test`
5. Submit a pull request

### Reporting Issues
Please use the [GitHub Issues](https://github.com/LogiComposer/boxable/issues) page to report bugs or request features.

## Examples Repository

For more comprehensive examples, visit: https://github.com/dhorions/boxable/wiki

Sample outputs:
- [CSV Example Portrait](https://s3.amazonaws.com/misc.quodlibet.be/Boxable/CSVexamplePortrait.pdf)
- [List Example Landscape](https://s3.amazonaws.com/misc.quodlibet.be/Boxable/ListExampleLandscape.pdf)

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

Want to contribute? Check our [Contributing Guidelines](CONTRIBUTING.md)!

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
