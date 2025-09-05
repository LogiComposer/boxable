package be.quodlibet.boxable;

import be.quodlibet.boxable.utils.FontUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class LargeDataTableTest {

    static {
        Map<String, PDFont> fonts = Map.of(
                "font", new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                "fontBold", new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                "fontBoldItalic", new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
                "fontItalic", new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE)
        );
        FontUtils.getDefaultfonts().putAll(fonts);
    }
    @Test
    public void Sample1() throws IOException {

        // Set margins
        float margin = 10;

        List<String[]> facts = getLargeDataSet(100000);

        // Initialize Document
        PDDocument doc = new PDDocument();
        PDPage page = addNewPage(doc);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);

        // Initialize table
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        boolean drawContent = true;
        float yStart = yStartNewPage;
        float bottomMargin = 70;
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true,
                drawContent);
        float width = (100/ 15);
        Row<PDPage> row = table.createRow(10f);
        for (int i = 0; i < 15 && i < facts.get(0).length; i++) {


            Cell<PDPage> cell = row.createCell(width, facts.get(0)[i]);
            cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            cell.setFontSize(11);
            cell.setTextUnderline(true);
            cell.setTextColor(Color.GREEN);
            cell.setFillColor(Color.yellow);
        }

        // Add multiple rows with random facts about Belgium
        for (String[] fact : facts.subList(1, facts.size())) {
            Row<PDPage> tableRow = table.createRow(10f);
            for (int i = 0; i < 15 && i < fact.length; i++) {

                Cell<PDPage> cell = tableRow.createCell(width, convertNonAsciiChars(fact[i]));
                cell.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA));
                cell.setFontSize(11);
                cell.setTextUnderline(true);
                cell.setTextColor(Color.GREEN);
                cell.setFillColor(Color.yellow);
            }
        }

        table.draw();

        // Close Stream and save pdf
        File file = new File("target/BoxableLargeData.pdf");
        System.out.println("Sample file saved at : " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        doc.save(file);
        doc.close();

    }

    private static PDPage addNewPage(PDDocument doc) {
        PDPage page = new PDPage();
        doc.addPage(page);
        return page;
    }

    private String convertNonAsciiChars(String input) {
        if (input != null && input.chars().anyMatch(c -> c > 127)) {
            byte[] bytes = input.getBytes(StandardCharsets.ISO_8859_1);
            return new String(bytes, StandardCharsets.UTF_8);
        } else {
            return input;
        }
    }

    private List<String[]> getLargeDataSet(int size) {
        List<String[]> dataList = new java.util.ArrayList<>();
        int records =0 ;
        try (BufferedReader br = new BufferedReader(new FileReader("src/test/resources/large_data/sales_data_100k.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                dataList.add(values);
                if(records++ > size || records > 100000) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataList;
    }
}
