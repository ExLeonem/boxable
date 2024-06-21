package be.quodlibet.boxable.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import be.quodlibet.boxable.text.WrappingFunction;

public class Tokenizer
{
  private final List<Token> tokens;
  private char characterAtCurrentIndex;
  private int indexOfCurrentCharacterInText;

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

  public Tokenizer()
  {
    tokens = new ArrayList<>();
    indexOfCurrentCharacterInText = 0;
  }

  private static boolean isWrapPointChar(char ch)
  {
    return
        ch == ' ' ||
            ch == ',' ||
            ch == '.' ||
            ch == '-' ||
            ch == '@' ||
            ch == ':' ||
            ch == ';' ||
            ch == '\n' ||
            ch == '\t' ||
            ch == '\r' ||
            ch == '\f' ||
            ch == '\u000B';
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

  public List<Token> tokenize(final String text, final WrappingFunction wrappingFunction)
  {
    if (text == null)
    {
      return Collections.emptyList();
    }

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
        if (sb.length() > 0)
        {
          tokens.add(Token.text(TokenType.TEXT, sb.toString()));
          sb.delete(0, sb.length());
        }
        tokens.add(POSSIBLE_WRAP_POINT);
        currentWrapPoint = possibleWrapPoints.pop();
      }

      characterAtCurrentIndex = text.charAt(indexOfCurrentCharacterInText);
      indexOfCurrentCharacterInText = processCharacter(text, sb);
      indexOfCurrentCharacterInText++;
    }

    if (sb.length() > 0)
    {
      tokens.add(Token.text(TokenType.TEXT, sb.toString()));
      sb.delete(0, sb.length());
    }
    tokens.add(POSSIBLE_WRAP_POINT);

    return tokens;
  }

  private int processCharacter(String text, StringBuilder sb)
  {
    if (characterAtCurrentIndex != '<')
    {
      sb.append(characterAtCurrentIndex);
      return indexOfCurrentCharacterInText;
    }

    boolean consumed = false;
    if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "/"))
    {
      consumed = processPotentialClosingTag(text, sb, consumed);
    }
    else
    {
      consumed = processPotentialOpenTag(text, sb, consumed);
    }

    if (!consumed)
    {
      sb.append('<');
    }

    return indexOfCurrentCharacterInText;
  }

  private boolean processPotentialClosingTag(String text, StringBuilder sb, boolean consumed)
  {
    // Look ahead needs to be offset by 1 because
    // the first character after the current character ('<') is `/` for closing tags
    int characterIndexOffset = 1;
    int numOfCharactersInTag = -1;
    Token tokenToAdd = null;

    if (isOneCharacterClosingTag(text))
    {
      // Minus 1 because the '<' symbol is at the current index. Example: </i>, </b>, </p>
      numOfCharactersInTag = 3;
      tokenToAdd = getOneCharacterClosingTagToAdd(text, characterIndexOffset);
    }

    if (isTwoCharacterClosingTag(text))
    {
      // Minus 1 because the '<' symbol is at the current index. Example: </ol>, </ul>, </li>
      numOfCharactersInTag = 4;
      tokenToAdd = getTwoCharacterClosingTagToAdd(text, characterIndexOffset);
    }

    if (tokenToAdd != null)
    {
      processToken(sb, numOfCharactersInTag, tokenToAdd);
      consumed = true;
    }

    return consumed;
  }

  private Token getOneCharacterClosingTagToAdd(String text, int characterIndexOffset)
  {
    if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "i", characterIndexOffset))
    {
      return CLOSE_TAG_I;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "b", characterIndexOffset))
    {
      return CLOSE_TAG_B;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "p", characterIndexOffset))
    {
      return CLOSE_TAG_P;
    }
    return null;
  }

  private Token getTwoCharacterClosingTagToAdd(String text, int characterIndexOffset)
  {
    if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "ol", characterIndexOffset))
    {
      return CLOSE_TAG_OL;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "ul", characterIndexOffset))
    {
      return CLOSE_TAG_UL;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "li", characterIndexOffset))
    {
      return CLOSE_TAG_LI;
    }

    return null;
  }

  private boolean isOneCharacterClosingTag(String text)
  {
    return TextLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, text, 3, '>');
  }

  private boolean isTwoCharacterClosingTag(String text)
  {
    return TextLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, text, 4, '>');
  }

  private boolean processPotentialOpenTag(String text, StringBuilder sb, boolean consumed)
  {
    int numOfTargetCharacters = -1;
    Token tokenToAdd = null;
    if (isOneCharacterOpenTag(text))
    {
      tokenToAdd = getOneCharacterOpenTag(text);
      numOfTargetCharacters = 2;
    }

    if (isTwoCharacterOpenTag(text))
    {
      tokenToAdd =  getTwoCharacterOpenTag(text);
      numOfTargetCharacters = 3;
    }

    // Instant closing tags, example <br/>
    if (TextLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, text, 4, '>'))
    {
      if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "br/"))
      {
        // normal notation <br/>
        tokenToAdd = WRAP_POINT_BR;
        numOfTargetCharacters = 4;
      }
    }

    // Instant closing tags with space, example <br/>
    if (TextLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, text, 5, '>'))
    {
      if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "br/ "))
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

  private Token getOneCharacterOpenTag(String text)
  {
    if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "i"))
    {
      return OPEN_TAG_I;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "b"))
    {
      return OPEN_TAG_B;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "p"))
    {
      return WRAP_POINT_P;
    }
    return null;
  }

  private Token getTwoCharacterOpenTag(String text)
  {
    if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "br"))
    {
      return WRAP_POINT_BR;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "ol"))
    {
      return OPEN_TAG_OL;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "ul"))
    {
      return OPEN_TAG_UL;
    }
    else if (TextLookAhead.hasNextCharacters(indexOfCurrentCharacterInText, text, "li"))
    {
      return WRAP_POINT_LI;
    }

    return null;
  }

  private boolean isOneCharacterOpenTag(String text)
  {
    return TextLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, text, 2, '>');
  }

  private boolean isTwoCharacterOpenTag(String text)
  {
    return TextLookAhead.hasCharacterAt(indexOfCurrentCharacterInText, text, 3, '>');
  }

  private void processToken(StringBuilder stringBuilder, int numOfTagCharacters, Token tokenToAdd)
  {
    if (stringBuilder.length() > 0)
    {
      tokens.add(Token.text(TokenType.TEXT, stringBuilder.toString()));
      stringBuilder.delete(0, stringBuilder.length()); // clean string builder
    }

    tokens.add(tokenToAdd);
    indexOfCurrentCharacterInText += numOfTagCharacters;
  }
}
