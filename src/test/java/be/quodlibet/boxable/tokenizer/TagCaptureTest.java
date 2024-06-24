package be.quodlibet.boxable.tokenizer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TagCaptureTest
{

  @Test
  public void test_capturedOpenTag()
  {
    // arrange
    String text = "abd <a> dfasdf";
    TagCapture capture = new TagCapture(text);

    // act
    iterateOverText(text, capture);

    // assert
    assertTrue(capture.isOpenTag);
    assertFalse(capture.isClosedTag);
    assertFalse(capture.isInstantCloseTag);
    assertTrue(capture.hasCapturedItem());
  }

  private static void iterateOverText(String text, TagCapture capture)
  {
    iterateOverText(text, capture, 0);
  }

  private static void iterateOverText(String text, TagCapture capture, int startAtIndex)
  {
    for (int i = startAtIndex; i < text.length(); i++)
    {
      capture.collect(i);
    }
  }

  @Test
  public void test_capturedClosedTag()
  {
    // arrange
    String text = "abd <a>some content</a> dfasdf";
    TagCapture capture = new TagCapture(text);

    // act
    iterateOverText(text, capture, 6);

    // assert
    assertTrue(capture.hasCapturedItem());
    assertFalse(capture.isOpenTag);
    assertTrue(capture.isClosedTag);
    assertTrue(capture.isInstantCloseTag);
  }

  @Test
  public void test_instantlyClosedTag()
  {
    // arrange
    String text = "<a/> instantly closed tag.";
    TagCapture capture = new TagCapture(text);

    // act
    iterateOverText(text, capture);

    // assert
    assertTrue(capture.hasCapturedItem());
    assertFalse(capture.isOpenTag);
    assertFalse(capture.isClosedTag);
    assertTrue(capture.isInstantCloseTag);
  }

  @Test
  public void test_noTagCaptured()
  {
    // arrange
    String text = "<a/> instantly closed tag.";
    TagCapture capture = new TagCapture(text);

    // act
    iterateOverText(text, capture);

    // assert
    assertTrue(capture.hasCapturedItem());
    assertFalse(capture.isOpenTag);
    assertFalse(capture.isClosedTag);
    assertTrue(capture.isInstantCloseTag);
  }
}
