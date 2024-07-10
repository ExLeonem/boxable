package be.quodlibet.boxable;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Assert;
import org.junit.Test;

public class ParagraphTest
{
  public static final int MARGIN = 50;
  PDDocument document;
  PDPage page;
  PDFont pdFont;

  public ParagraphTest()
  {
    this.document = new PDDocument();
    this.page = new PDPage(PDRectangle.A4);
    this.pdFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  }

  @Test
  public void test_htmlTagsFilteredWhen_flagIsFalse() throws IOException
  {
    // arrange
    BaseTable table = createTable();
    Row<PDPage> row = table.createRow(25);
    Cell<PDPage> cell = row.createCell(50, "");

    final String text = "1 <i><b>123</b> 123456</i> <ul><li>8984</li><li>test</li></ul> </br> lorem <i>ipsum</i>";
    List<String> expectedItems = splitByTags(text);
    Paragraph paragraph = createParagraph(text, cell);

    // act
    List<String> result = paragraph.getLines();

    // assert
    String actual = concatAllLines(result);
    assertPureTextInResult(expectedItems, actual);
  }

  private static void assertPureTextInResult(List<String> expectedItems, String actual)
  {
    for (String expectedItem : expectedItems)
    {
      Assert.assertTrue(actual.contains(expectedItem));
    }
  }

  private static String concatAllLines(List<String> result)
  {
    StringBuilder stringBuilder = new StringBuilder();
    for (String item : result)
    {
      stringBuilder.append(item);
    }
    return stringBuilder.toString();
  }

  private List<String> splitByTags(String text)
  {
    String[] tagsToReplace = {"b", "li", "ul", "strong", "i", "br"};
    for (String tagToReplace : tagsToReplace)
    {
      text = text.replaceAll(createOpenTag(tagToReplace), " ");
      text = text.replaceAll(createClosedTag(tagToReplace), " ");
    }

    return splitByWhitespaceAndFilterEmptyElements(text);
  }

  private static List<String> splitByWhitespaceAndFilterEmptyElements(String text)
  {
    String[] items = text.split(" ");
    List<String> filteredItems = new ArrayList<>();
    for (String item : items)
    {
      if (!item.isEmpty())
      {
        filteredItems.add(item);
      }
    }

    return filteredItems;
  }

  private static String createClosedTag(String tagToReplace)
  {
    return "</" + tagToReplace + ">";
  }

  private static String createOpenTag(String tagToReplace)
  {
    return "<" + tagToReplace + ">";
  }

  @Test
  public void test_htmlTagsKep_when_flagIsTrue() throws IOException
  {
    // arrange
    BaseTable table = createTable();
    Row<PDPage> row = table.createRow(25);
    Cell<PDPage> cell = row.createCell(50, "");

    final String expected = "1 <i><b>123</b> 123456</i> <ul> <li>HelloWorld</li><li>test</li></ul>";
    Paragraph paragraph = createParagraph(expected, cell);
    paragraph.setKeepHtmlTags(true);


    // act
    List<String> result = paragraph.getLines();

    // assert
    String actual = concatAllLines(result);
    Assert.assertEquals(expected, actual);
  }

  private Paragraph createParagraph(String text, Cell<PDPage> cell)
  {
    return new Paragraph(
        text,
        pdFont,
        cell.getFontSize(),
        cell.getInnerWidth(),
        HorizontalAlignment.LEFT,
        Color.BLACK,
        null,
        cell.getWrappingFunction(),
        cell.getLineSpacing()
    );
  }

  public BaseTable createTable() throws IOException
  {
    return new BaseTable(
        0,
        0,
        MARGIN,
        MARGIN,
        page.getMediaBox().getWidth(),
        MARGIN,
        document,
        page,
        true,
        true
    );
  }
}
