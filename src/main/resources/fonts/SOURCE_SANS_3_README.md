# Source Sans 3 Fonts

This directory contains Source Sans 3 font files from Adobe:

- SourceSans3-Regular.ttf
- SourceSans3-Bold.ttf  
- SourceSans3-It.ttf (Italic)
- SourceSans3-BoldIt.ttf (Bold Italic)

## Source and License

These fonts are downloaded from Adobe's official Source Sans repository:
https://github.com/adobe-fonts/source-sans

Source Sans 3 is licensed under the SIL Open Font License 1.1, which allows for use, modification, and distribution. The license is compatible with open source projects.

## Usage

To use Source Sans 3 fonts as the default fonts in your Boxable documents:

```java
PDDocument document = new PDDocument();
FontUtils.setSourceSans3FontsAsDefault(document);
```

This will set all four variants (regular, bold, italic, bold-italic) as the default fonts for use in tables and other Boxable components.

## Version

Source Sans 3, Release 3.052R (April 2023)