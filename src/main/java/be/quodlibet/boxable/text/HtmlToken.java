package be.quodlibet.boxable.text;

public enum HtmlToken
{
  OL(),
  UL(),
  P(),
  LI(),
  I(),
  B(),
  NO_HTML_TOKEN();

  private String tagName;

  HtmlToken()
  {
    this.tagName = this.name().toLowerCase();
  }

  public boolean equals(String token)
  {
    return this.tagName.equals(token);
  }

  @Override
  public String toString()
  {
    return this.tagName;
  }

  public static HtmlToken fromString(String token)
  {
    try
    {
      return valueOf(token);
    } catch (IllegalArgumentException e)
    {
      return NO_HTML_TOKEN;
    }
  }
}
