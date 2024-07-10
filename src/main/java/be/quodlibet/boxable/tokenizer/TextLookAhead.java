package be.quodlibet.boxable.tokenizer;

public class TextLookAhead<T>
{
  public static boolean hasCharacterAt(int textCharStartIndex, String text, int offsetOfStartIndex, char expectedCharacter)
  {
    if (textCharStartIndex >= text.length() - offsetOfStartIndex)
    {
      return false;
    }

    char actualCharacter = text.charAt(textCharStartIndex + offsetOfStartIndex);
    return actualCharacter == expectedCharacter;
  }

  public static boolean hasNextCharacters(int textCharStartIndex, String text, String nextCharacters)
  {
    return hasNextCharacters(textCharStartIndex, text, nextCharacters, 0);
  }

  public static boolean hasNextCharacters(
      int textCharStartIndex, String text, String nextCharacters, int additionalOffsetCharacterIndexFromText)
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
