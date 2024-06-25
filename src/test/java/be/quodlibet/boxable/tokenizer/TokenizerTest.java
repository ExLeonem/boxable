package be.quodlibet.boxable.tokenizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import be.quodlibet.boxable.text.WrappingFunction;

public class TokenizerTest
{
  private Tokenizer tokenizer;

  private WrappingFunction wrappingFunction = null;

  @Before
  public void before()
  {
    tokenizer = new Tokenizer();
  }

  @Test
  public void testWrapPoints() throws Exception
  {
    // arrange
    final String text = "1 123 123456 12";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    assertEquals(Arrays.asList(
        Token.text(TokenType.TEXT, "1 "),
        new Token(TokenType.POSSIBLE_WRAP_POINT, ""),
        Token.text(TokenType.TEXT, "123 "),
        new Token(TokenType.POSSIBLE_WRAP_POINT, ""),
        Token.text(TokenType.TEXT, "123456 "),
        new Token(TokenType.POSSIBLE_WRAP_POINT, ""),
        Token.text(TokenType.TEXT, "12"),
        new Token(TokenType.POSSIBLE_WRAP_POINT, "")
    ), tokens);
  }

  @Test
  public void testEndsWithLt() throws Exception
  {
    // arrange
    final String text = "1 123 123456 12<";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    if (TokenType.CLOSE_TAG.equals(tokens.get(tokens.size() - 1).getType()))
    {
      assertEquals("Text doesn't end with '<' character", "<",
          tokens.get(tokens.size() - 1).getData());
    }
  }

  @Test
  public void testSimpleItalic_1() throws Exception
  {
    // arrange
    final String text = "1 <i>123 123456</i> 12";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    boolean italic = false;
    final StringBuilder italicText = new StringBuilder();
    collectItalicTokenData(tokens, italic, italicText);
    assertEquals("Italic text is parsed wrong", "123 123456", italicText.toString());
  }

  private static void collectItalicTokenData(List<Token> tokens, boolean italic, StringBuilder italicText)
  {
    for (final Token token : tokens)
    {
      if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i"))
      {
        italic = true;
      }
      else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i"))
      {
        italic = false;
      }
      if (TokenType.TEXT.equals(token.getType()) && italic)
      {
        italicText.append(token.getData());
      }
    }
  }

  @Test
  public void testSimpleItalic_2() throws Exception
  {
    // arrange
    final String text = "1 <i>123</i> <i> 123456</i> 12";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    boolean italic = false;
    final StringBuilder italicText = new StringBuilder();
    collectItalicTokenData(tokens, italic, italicText);
    assertEquals("Italic text is parsed wrong", "123 123456", italicText.toString());
  }

  @Test
  public void testBoldAndItalic_1() throws Exception
  {
    // arrange
    final String text = "1 <i><b>123</b> 123456</i> 12";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    final StringBuilder boldItalicText = new StringBuilder();
    boolean bold = false;
    boolean italic = false;
    for (final Token token : tokens)
    {
      if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("b"))
      {
        bold = true;
      }
      else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("b"))
      {
        bold = false;
      }
      if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i"))
      {
        italic = true;
      }
      else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i"))
      {
        italic = false;
      }

      if (TokenType.TEXT.equals(token.getType()) && bold && italic)
      {
        boldItalicText.append(token.getData());
      }
    }
    assertEquals("Bold-italic text is parsed wrong", "123", boldItalicText.toString());
  }

  @Test
  public void testBoldAndItalic_2() throws Exception
  {
    // arrange
    final String text = "1 <i>123</i> <i> <b>123456</i></b> 12";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    final StringBuilder boldItalicText = new StringBuilder();
    boolean bold = false;
    boolean italic = false;
    for (final Token token : tokens)
    {
      if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("b"))
      {
        bold = true;
      }
      else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("b"))
      {
        bold = false;
      }
      if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i"))
      {
        italic = true;
      }
      else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i"))
      {
        italic = false;
      }

      if (TokenType.TEXT.equals(token.getType()) && bold && italic)
      {
        boldItalicText.append(token.getData());
      }
    }
    assertEquals("Bold-italic text is parsed wrong", "123456", boldItalicText.toString());
  }

  @Test
  public void test_emptyString() throws Exception
  {
    // arrange
    final String text = "";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    for (final Token token : tokens)
    {
      if (TokenType.TEXT.equals(token.getType()) && token.getData().equals(""))
      {
        assertEquals("Bold-italic text is parsed wrong", "", token.getData());
      }
    }
  }

  @Test
  public void test_nullString() throws Exception
  {
    // arrange
    final String textNull = null;

    // act
    final List<Token> tokens = tokenizer.tokenize(textNull, wrappingFunction);

    // assert
    assertEquals("Bold-italic text is parsed wrong", Collections.emptyList(), tokens);
  }

  @Test
  public void test_illegalTagsAreFiltered() throws Exception
  {
    // arrange
    final String text = "1 123 123456 12 <a href=\"http://whatever.com\">SomeLink</a> <div>Hello world</div>";

    // act
    final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);

    // assert
    assertTextTokensDoNotIncludeIllegalTags(tokens);
  }

  private static void assertTextTokensDoNotIncludeIllegalTags(List<Token> tokens)
  {
    for (final Token token : tokens)
    {
      if (token.getType() != TokenType.TEXT)
      {
        continue;
      }

      String tokenData = token.getData();
      boolean containsDivTag = tokenData.contains("<div>");
      boolean containsLinkTag = tokenData.contains("<a>");
      assertFalse(containsDivTag || containsLinkTag);
    }
  }
}
