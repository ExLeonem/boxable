package be.quodlibet.boxable.tokenizer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import be.quodlibet.boxable.text.WrappingFunction;

public class ExtendedTokenizerTest
{
    private ExtendedTokenizer tokenizer;

    private WrappingFunction wrappingFunction = null;

    @Before
    public void beforeEach() {
        tokenizer = new ExtendedTokenizer();
    }

    @Test
    public void testWrapPoints() throws Exception {
        final String text = "1 123 123456 12";

        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
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
    public void testEndsWithLt() throws Exception {
        final String text = "1 123 123456 12<";
        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
        if (TokenType.CLOSE_TAG.equals(tokens.get(tokens.size() - 1).getType())) {
            assertEquals(
                    "Text doesn't end with '<' character", "<", tokens.get(tokens.size() - 1).getData());
        }
    }

    @Test
    public void testSimpleItalic_singleItalicTag() throws Exception {
        final String text = "1 <i>123 123456</i> 12";
        final StringBuilder italicText = new StringBuilder();
        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
        boolean italic = false;
        for (final Token token : tokens) {
            if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = true;
            } else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = false;
            }
            if (TokenType.TEXT.equals(token.getType()) && italic) {
                italicText.append(token.getData());
            }
        }
        assertEquals("Italic text is parsed wrong", "123 123456", italicText.toString());
    }

    @Test
    public void testSimpleItalic_multipleItalicTags() throws Exception {
        final String text = "1 <i>123</i> <i> 123456</i> 12";
        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
        final StringBuilder italicText = new StringBuilder();
        boolean italic = false;
        for (final Token token : tokens) {
            if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = true;
            } else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = false;
            }
            if (TokenType.TEXT.equals(token.getType()) && italic) {
                italicText.append(token.getData());
            }
        }
        assertEquals("Italic text is parsed wrong", "123 123456", italicText.toString());
    }

    @Test
    public void testBoldAndItalic_singleItalicAndBoldTag() throws Exception {
        final String text = "1 <i><b>123</b> 123456</i> 12";
        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
        final StringBuilder boldItalicText = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        for (final Token token : tokens) {
            if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("b")) {
                bold = true;
            } else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("b")) {
                bold = false;
            }
            if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = true;
            } else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = false;
            }

            if (TokenType.TEXT.equals(token.getType()) && bold && italic) {
                boldItalicText.append(token.getData());
            }
        }
        assertEquals("Bold-italic text is parsed wrong","123", boldItalicText.toString());
    }

    @Test
    public void testBoldAndItalic_multipleItalicAndOneBoldTag() throws Exception {
        final String text = "1 <i>123</i> <i> <b>123456</i></b> 12";
        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
        final StringBuilder boldItalicText = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        for (final Token token : tokens) {
            if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("b")) {
                bold = true;
            } else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("b")) {
                bold = false;
            }
            if (TokenType.OPEN_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = true;
            } else if (TokenType.CLOSE_TAG.equals(token.getType()) && token.getData().equals("i")) {
                italic = false;
            }

            if (TokenType.TEXT.equals(token.getType()) && bold && italic) {
                boldItalicText.append(token.getData());
            }
        }
        assertEquals("Bold-italic text is parsed wrong", "123456", boldItalicText.toString());
    }

    @Test
    public void test_emptyString() throws Exception {
        final String text = "";
        final List<Token> tokens = tokenizer.tokenize(text, wrappingFunction);
        for (final Token token : tokens) {
            if (TokenType.TEXT.equals(token.getType()) && token.getData().equals("")) {
                assertEquals("Bold-italic text is parsed wrong", "", token.getData());
            }
        }
    }

    @Test
    public void test_nullString() throws Exception {
        final String textNull = null;
        final List<Token> tokens = tokenizer.tokenize(textNull, wrappingFunction);
        assertEquals("Bold-italic text is parsed wrong", Collections.emptyList(), tokens);
    }
}
