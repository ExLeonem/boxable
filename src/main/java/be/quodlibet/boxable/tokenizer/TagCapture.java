package be.quodlibet.boxable.tokenizer;

public class TagCapture
{
  private String text;
  private StringBuilder stringBuilder;

  private boolean isCollectingTagInformation;

  private int tagStartIndex;
  boolean isOpenTag;

  private Token capturedToken;

  public TagCapture(String text)
  {
    this.text = text;
    this.stringBuilder = new StringBuilder();
    this.isOpenTag = false;
  }

  public void collect(int index)
  {
    if (shouldCaptureItem(index))
    {
      String tag = stringBuilder.toString();
      
      return;
    }

    if (startCollecting(index))
    {
      tagStartIndex = index;
      isCollectingTagInformation = true;
      this.stringBuilder = new StringBuilder();
    }

    char charAtIndex = text.charAt(index);
    stringBuilder.append(charAtIndex);
  }

  public boolean hasCapturedItem()
  {
    return false;
  }

  public boolean shouldCaptureItem(int index)
  {
    return isCollectingTagInformation
        && TextLookAhead.hasCharacterAt(text, index, 0, '>');
  }

  public boolean startCollecting(int index)
  {
    return TextLookAhead.hasCharacterAt(text, index, 0, '<');
  }

  public boolean isCollecting()
  {
    return this.isCollectingTagInformation;
  }
}
