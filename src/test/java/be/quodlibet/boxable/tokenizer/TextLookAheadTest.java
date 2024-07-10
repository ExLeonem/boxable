package be.quodlibet.boxable.tokenizer;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextLookAheadTest
{
  private static final int currentCharacterIndex = 0;

  @Test
  public void test_hasCharacterAt_outOfBounds_notEnoughCharactersToCheck()
  {
    // arrange
    String text = "abcdefg";
    char expectedCharacter = 'c';

    // act && assert
    assertFalse(TextLookAhead.hasCharacterAt(currentCharacterIndex, text, text.length() + 5, expectedCharacter));
  }

  @Test
  public void test_hasCharacterAt_success()
  {
    // arrange
    String text = "abcdefg";
    char expectedCharacter = 'd';

    // act
    boolean actual = TextLookAhead.hasCharacterAt(currentCharacterIndex, text, 3, expectedCharacter);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_hasCharacterAt_start_atDifferentIndex_success()
  {
    // arrange
    String text = "abcdefg";
    char expectedCharacter = 'e';

    // act
    boolean actual = TextLookAhead.hasCharacterAt(1, text, 3, expectedCharacter);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_hasCharacterAt_failed()
  {
    // arrange
    String text = "abcdefg";
    char expectedCharacter = 'c';

    // act
    boolean actual = TextLookAhead.hasCharacterAt(currentCharacterIndex, text, 3, expectedCharacter);

    // assert
    assertFalse(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_success()
  {
    // arrange
    String text = "abcdefg";
    String nextCharacters = "bcd";

    // act
    boolean actual = TextLookAhead.hasNextCharacters(currentCharacterIndex, text, nextCharacters);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_startAtDifferentIndex_success()
  {
    // arrange
    String text = "abcdefg";
    String nextCharacters = "cde";

    // act
    boolean actual = TextLookAhead.hasNextCharacters(1, text, nextCharacters);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_withOneAdditionalCharacterOffset_success()
  {
    // arrange
    String text = "abcdefg";
    String nextCharacters = "cde";

    // act
    boolean actual = TextLookAhead.hasNextCharacters(currentCharacterIndex, text, nextCharacters, 1);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_failed()
  {
    // arrange
    String text = "abcdefg";
    String nextCharacters = "bcj";

    // act
    boolean actual = TextLookAhead.hasNextCharacters(currentCharacterIndex, text, nextCharacters);

    // assert
    assertFalse(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_toManyNextCharacters_failed()
  {
    // arrange
    String text = "abc";
    String nextCharacters = "bcde";

    // act
    boolean actual = TextLookAhead.hasNextCharacters(currentCharacterIndex, text, nextCharacters);

    // assert
    assertFalse(actual);
  }
}
