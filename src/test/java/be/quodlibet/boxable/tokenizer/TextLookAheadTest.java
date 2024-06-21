package be.quodlibet.boxable.tokenizer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextLookAheadTest
{
  private static TextLookAhead sut;
  private static final int currentCharacterIndex = 0;
  private static final String text = "abcdefg";

  @BeforeEach
  public void beforeEach()
  {
    sut = new TextLookAhead(text);
  }

  @Test
  public void test_hasCharacterAt_outOfBounds_notEnoughCharactersToCheck()
  {
    // arrange
    char expectedCharacter = 'c';

    // act
    boolean actual = sut.hasCharacterAt(currentCharacterIndex, text.length() + 5, expectedCharacter);

    // act && assert
    assertFalse(actual);
  }

  @Test
  public void test_hasCharacterAt_success()
  {
    // arrange
    char expectedCharacter = 'd';

    // act
    boolean actual = sut.hasCharacterAt(currentCharacterIndex, 3, expectedCharacter);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_hasCharacterAt_start_atDifferentIndex_success()
  {
    // arrange
    char expectedCharacter = 'e';

    // act
    boolean actual = sut.hasCharacterAt(1, 3, expectedCharacter);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_hasCharacterAt_failed()
  {
    // arrange
    char expectedCharacter = 'c';

    // act
    boolean actual = sut.hasCharacterAt(currentCharacterIndex, 3, expectedCharacter);

    // assert
    assertFalse(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_success()
  {
    // arrange
    String nextCharacters = "bcd";

    // act
    boolean actual = sut.hasNextCharacters(currentCharacterIndex, nextCharacters);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_startAtDifferentIndex_success()
  {
    // arrange
    String nextCharacters = "cde";

    // act
    boolean actual = sut.hasNextCharacters(1, nextCharacters);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_withOneAdditionalCharacterOffset_success()
  {
    // arrange
    String nextCharacters = "cde";

    // act
    boolean actual = sut.hasNextCharacters(currentCharacterIndex, nextCharacters, 1);

    // assert
    assertTrue(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_failed()
  {
    // arrange
    String nextCharacters = "bcj";

    // act
    boolean actual = sut.hasNextCharacters(currentCharacterIndex, nextCharacters);

    // assert
    assertFalse(actual);
  }

  @Test
  public void test_characterAtSpecifiedIndex_toManyNextCharacters_failed()
  {
    // arrange
    String nextCharacters = "bcdefaldkfja";

    // act
    boolean actual = sut.hasNextCharacters(currentCharacterIndex, nextCharacters);

    // assert
    assertFalse(actual);
  }
}
