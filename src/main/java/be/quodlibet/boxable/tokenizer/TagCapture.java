package be.quodlibet.boxable.tokenizer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagCapture
{
  public static final String OPEN_HTML_TAG_PATTERN = "<.+>";
  public static final String INSTANT_CLOSING_HTML_TAG_PATTERN = "<.+/>";
  public static final String CLOSING_HTML_TAG_PATTERN = "</.+>";
  private String text;
  private StringBuilder stringBuilder;

  private int tagStartIndex;
  private int tagEndIndex;

  boolean isOpenTag;
  boolean isClosedTag;
  boolean isInstantCloseTag;
  private boolean isCollectingTagInformation;

  private Token capturedToken;

  private final Set<String> ALLOWED_HTML_TAGS = new HashSet<>();

  public TagCapture(String text)
  {
    this.text = text;
    this.stringBuilder = new StringBuilder();
    this.isOpenTag = false;

    ALLOWED_HTML_TAGS.add("i");
    ALLOWED_HTML_TAGS.add("b");
    ALLOWED_HTML_TAGS.add("p");
    ALLOWED_HTML_TAGS.add("br");
    ALLOWED_HTML_TAGS.add("li");
    ALLOWED_HTML_TAGS.add("ul");
    ALLOWED_HTML_TAGS.add("ol");
  }

  public void collect(int index)
  {
    char charAtIndex = text.charAt(index);
    if (shouldCaptureItem(index))
    {
      stringBuilder.append(charAtIndex);
      String tag = stringBuilder.toString();
      this.isClosedTag = isClosedTag(tag);
      this.isInstantCloseTag = !isClosedTag && isInstantClosingTag(tag);
      this.isOpenTag = !isInstantCloseTag && !isClosedTag && isOpenTag(tag);
      tagEndIndex = index;
      return;
    }

    if (startCollecting(index))
    {
      tagStartIndex = index;
      isCollectingTagInformation = true;
      this.stringBuilder = new StringBuilder();
    }


    stringBuilder.append(charAtIndex);
  }

  private boolean shouldCaptureItem(int index)
  {
    return isCollectingTagInformation
        && TextLookAhead.hasCharacterAt(text, index, 0, '>');
  }

  private boolean isOpenTag(String tag)
  {
    Matcher matcher = getMatcher(tag, OPEN_HTML_TAG_PATTERN);
    return matchEqualsCollectedTag(tag, matcher);
  }

  private static boolean matchEqualsCollectedTag(String tag, Matcher matcher)
  {
    return matcher.matches()
        && matcher.start() == 0 && matcher.end() == tag.length();
  }

  private static Matcher getMatcher(String tag, String regex)
  {
    Pattern pattern = Pattern.compile(regex);
    return pattern.matcher(tag);
  }

  private boolean isInstantClosingTag(String tag)
  {
    Matcher matcher = getMatcher(tag, INSTANT_CLOSING_HTML_TAG_PATTERN);
    return matchEqualsCollectedTag(tag, matcher);
  }

  private boolean isClosedTag(String tag)
  {
    Matcher matcher = getMatcher(tag, CLOSING_HTML_TAG_PATTERN);
    return matchEqualsCollectedTag(tag, matcher);
  }

  private String getTagName(String tag)
  {
    String tagContent = getInnerTagContent(tag);
    String[] tagParts = tagContent.split(" ");
    validateHasAtLeastTagName(tag, tagParts);
    return tagParts[0];
  }

  private String getInnerTagContent(String tag)
  {
    if (isClosedTag)
    {
      return tag.substring(2, tag.length() - 2).trim();
    }
    else if (isInstantCloseTag)
    {
      return tag.substring(1, tag.length() - 3);
    }

    return tag.substring(1, tag.length() - 2);
  }

  private static void validateHasAtLeastTagName(String tag, String[] tagParts)
  {
    if (tagParts.length == 0)
    {
      throw new IllegalArgumentException("Error in TagCapture.getTagName. "
          + "Encountered a Tag without content '" + tag + "'.");
    }
  }

  public boolean hasCapturedItem()
  {
    return false;
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
