package be.quodlibet.boxable.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

import be.quodlibet.boxable.text.WrappingFunction;

public class ExtendedTokenizer
{
  private final List<Token> tokens;
  private char characterAtCurrentIndex;
  private int indexOfCurrentCharacterInText;
  private TextLookAhead textLookAhead;
  private TagCapture capture;

  private static final Token OPEN_TAG_I = new Token(TokenType.OPEN_TAG, "i");
  private static final Token OPEN_TAG_B = new Token(TokenType.OPEN_TAG, "b");
  private static final Token OPEN_TAG_OL = new Token(TokenType.OPEN_TAG, "ol");
  private static final Token OPEN_TAG_UL = new Token(TokenType.OPEN_TAG, "ul");
  private static final Token CLOSE_TAG_I = new Token(TokenType.CLOSE_TAG, "i");
  private static final Token CLOSE_TAG_B = new Token(TokenType.CLOSE_TAG, "b");
  private static final Token CLOSE_TAG_OL = new Token(TokenType.CLOSE_TAG, "ol");
  private static final Token CLOSE_TAG_UL = new Token(TokenType.CLOSE_TAG, "ul");
  private static final Token CLOSE_TAG_P = new Token(TokenType.CLOSE_TAG, "p");
  private static final Token CLOSE_TAG_LI = new Token(TokenType.CLOSE_TAG, "li");
  private static final Token POSSIBLE_WRAP_POINT = new Token(TokenType.POSSIBLE_WRAP_POINT, "");
  private static final Token WRAP_POINT_P = new Token(TokenType.WRAP_POINT, "p");
  private static final Token WRAP_POINT_LI = new Token(TokenType.WRAP_POINT, "li");
  private static final Token WRAP_POINT_BR = new Token(TokenType.WRAP_POINT, "br");

  public ExtendedTokenizer()
  {
    tokens = new ArrayList<>();
    indexOfCurrentCharacterInText = 0;
  }

  public List<Token> tokenize(final String text, final WrappingFunction wrappingFunction)
  {
    if (text == null)
    {
      return Collections.emptyList();
    }

    textLookAhead = new TextLookAhead(text);
    capture = new TagCapture(text);

    final Stack<Integer> possibleWrapPoints = wrappingFunction == null
        ? findWrapPoints(text)
        : findWrapPointsWithFunction(text, wrappingFunction);

    final StringBuilder sb = new StringBuilder();

    // taking first wrap point
    Integer currentWrapPoint = possibleWrapPoints.pop();
    while (indexOfCurrentCharacterInText < text.length())
    {
      if (indexOfCurrentCharacterInText == currentWrapPoint)
      {
        createTextTokenAndCleanStringBuilder(sb);
        tokens.add(POSSIBLE_WRAP_POINT);
        currentWrapPoint = possibleWrapPoints.pop();
      }

      characterAtCurrentIndex = text.charAt(indexOfCurrentCharacterInText);
      indexOfCurrentCharacterInText = processCharacter(sb);
      indexOfCurrentCharacterInText++;
    }

    createTextTokenAndCleanStringBuilder(sb);
    tokens.add(POSSIBLE_WRAP_POINT);
    return tokens;
  }

  private static Stack<Integer> findWrapPoints(String text)
  {
    Stack<Integer> result = new Stack<>();
    result.push(text.length());
    for (int i = text.length() - 2; i >= 0; i--)
    {
      if (isWrapPointChar(text.charAt(i)))
      {
        result.push(i + 1);
      }
    }
    return result;
  }

  private static boolean isWrapPointChar(char ch)
  {
    return ch == ' '
        || ch == ','
        || ch == '.'
        || ch == '-'
        || ch == '@'
        || ch == ':'
        || ch == ';'
        || ch == '\n'
        || ch == '\t'
        || ch == '\r'
        || ch == '\f'
        || ch == '\u000B';
  }

  private static Stack<Integer> findWrapPointsWithFunction(String text, WrappingFunction wrappingFunction)
  {
    final String[] split = wrappingFunction.getLines(text);
    int textIndex = text.length();
    final Stack<Integer> possibleWrapPoints = new Stack<>();
    possibleWrapPoints.push(textIndex);
    for (int i = split.length - 1; i > 0; i--)
    {
      final int splitLength = split[i].length();
      possibleWrapPoints.push(textIndex - splitLength);
      textIndex -= splitLength;
    }
    return possibleWrapPoints;
  }

  private int processCharacter(StringBuilder sb)
  {
    if (characterAtCurrentIndex != '<')
    {
      sb.append(characterAtCurrentIndex);
      return indexOfCurrentCharacterInText;
    }

    // Potentiall closing tag
    if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "/"))
    {

    }

    boolean consumed = false;
    if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "/"))
    {
      consumed = processPotentialClosingTag(sb, consumed);
    }
    else
    {
      consumed = processPotentialOpenTag(sb, consumed);
    }

    if (!consumed)
    {
      sb.append('<');
    }

    return indexOfCurrentCharacterInText;
  }

  private boolean processPotentialClosingTag(StringBuilder sb, boolean consumed)
  {
    // Look ahead needs to be offset by 1 because
    // the first character after the current character ('<') is `/` for closing tags
    int characterIndexOffset = 1;
    int numOfCharactersInTag = -1;
    Token tokenToAdd = null;

    if (isOneCharacterClosingTag())
    {
      // Minus 1 because the '<' symbol is at the current index. Example: </i>, </b>, </p>
      numOfCharactersInTag = 3;
      tokenToAdd = getOneCharacterClosingTagToAdd(characterIndexOffset);
    }

    if (isTwoCharacterClosingTag())
    {
      // Minus 1 because the '<' symbol is at the current index. Example: </ol>, </ul>, </li>
      numOfCharactersInTag = 4;
      tokenToAdd = getTwoCharacterClosingTagToAdd(characterIndexOffset);
    }

    if (tokenToAdd != null)
    {
      processToken(sb, numOfCharactersInTag, tokenToAdd);
      consumed = true;
    }

    return consumed;
  }

  private Token getOneCharacterClosingTagToAdd(int characterIndexOffset)
  {
    if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "i", characterIndexOffset))
    {
      return CLOSE_TAG_I;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "b", characterIndexOffset))
    {
      return CLOSE_TAG_B;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "p", characterIndexOffset))
    {
      return CLOSE_TAG_P;
    }
    return null;
  }

  private Token getTwoCharacterClosingTagToAdd(int characterIndexOffset)
  {
    if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "ol", characterIndexOffset))
    {
      return CLOSE_TAG_OL;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "ul", characterIndexOffset))
    {
      return CLOSE_TAG_UL;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "li", characterIndexOffset))
    {
      return CLOSE_TAG_LI;
    }

    return null;
  }

  private boolean isOneCharacterClosingTag()
  {
    return textLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, 3, '>');
  }

  private boolean isTwoCharacterClosingTag()
  {
    return textLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, 4, '>');
  }

  private boolean processPotentialOpenTag(StringBuilder sb, boolean consumed)
  {
    int numOfTargetCharacters = -1;
    Token tokenToAdd = null;
    if (isOneCharacterOpenTag())
    {
      tokenToAdd = getOneCharacterOpenTag();
      numOfTargetCharacters = 2;
    }

    if (isTwoCharacterOpenTag())
    {
      tokenToAdd = getTwoCharacterOpenTag();
      numOfTargetCharacters = 3;
    }

    // Instant closing tags, example <br/>
    if (textLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, 4, '>'))
    {
      if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "br/"))
      {
        // normal notation <br/>
        tokenToAdd = WRAP_POINT_BR;
        numOfTargetCharacters = 4;
      }
    }

    // Instant closing tags with space, example <br/>
    if (textLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, 5, '>'))
    {
      if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "br/ "))
      {
        // in case it is notation <br />
        tokenToAdd = WRAP_POINT_BR;
        numOfTargetCharacters = 5;
      }
    }

    if (tokenToAdd != null)
    {
      processToken(sb, numOfTargetCharacters, tokenToAdd);
      consumed = true;
    }

    return consumed;
  }

  private Token getOneCharacterOpenTag()
  {
    if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "i"))
    {
      return OPEN_TAG_I;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "b"))
    {
      return OPEN_TAG_B;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "p"))
    {
      return WRAP_POINT_P;
    }
    return null;
  }

  private Token getTwoCharacterOpenTag()
  {
    if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "br"))
    {
      return WRAP_POINT_BR;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "ol"))
    {
      return OPEN_TAG_OL;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "ul"))
    {
      return OPEN_TAG_UL;
    }
    else if (textLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, "li"))
    {
      return WRAP_POINT_LI;
    }

    return null;
  }

  private boolean isOneCharacterOpenTag()
  {
    return textLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, 2, '>');
  }

  private boolean isTwoCharacterOpenTag()
  {
    return textLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, 3, '>');
  }

  private void processToken(StringBuilder stringBuilder, int numOfTagCharacters, Token tokenToAdd)
  {
    createTextTokenAndCleanStringBuilder(stringBuilder);
    tokens.add(tokenToAdd);
    indexOfCurrentCharacterInText += numOfTagCharacters;
  }

  private void createTextTokenAndCleanStringBuilder(StringBuilder stringBuilder)
  {
    if (stringBuilder.length() > 0)
    {
      tokens.add(Token.text(TokenType.TEXT, stringBuilder.toString()));
      stringBuilder.delete(0, stringBuilder.length()); // clean string builder
    }
  }
}
