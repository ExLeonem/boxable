package be.quodlibet.boxable.tokenizer;

public class TextLookAhead<T>
{
  private String text;

  public TextLookAhead(String text) {
    this.text = text;
  }

  public boolean hasCharacterAt(int textCharStartIndex, int offsetOfStartIndex, char expectedCharacter)
  {
    return hasCharacterAt(text, textCharStartIndex, offsetOfStartIndex, expectedCharacter);
  }

  public static boolean hasCharacterAt(String text, int textCharStartIndex, int offsetOfStartIndex, char expectedCharacter)
  {
    if (textCharStartIndex >= text.length() - offsetOfStartIndex)
    {
      return false;
    }

    char actualCharacter = text.charAt(textCharStartIndex + offsetOfStartIndex);
    return actualCharacter == expectedCharacter;
  }

  public boolean hasNextCharacters(int textCharStartIndex, String nextCharacters)
  {
    return hasNextCharacters(textCharStartIndex, text, nextCharacters, 0);
  }

  public boolean hasNextCharacters(int textCharStartIndex, String nextCharacters, int additionalOffsetCharacterIndexFromText)
  {
    return hasNextCharacters(textCharStartIndex, text, nextCharacters, additionalOffsetCharacterIndexFromText);
  }

  public static boolean hasNextCharacters(
      int textCharStartIndex, String text,  String nextCharacters, int additionalOffsetCharacterIndexFromText)
  {
    if (textCharStartIndex >= text.length() - nextCharacters.length())
    {
      return false;
    }

    for (int i = 0; i < nextCharacters.length(); i++)
    {
      char nextCharacterInText = text.charAt(textCharStartIndex + (i + additionalOffsetCharacterIndexFromText + 1));
      char nextCharacterInOrder = nextCharacters.charAt(i);
      if (nextCharacterInText != nextCharacterInOrder)
      {
        return false;
      }
    }

    return true;
  }
}
