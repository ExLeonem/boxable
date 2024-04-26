package be.quodlibet.boxable.text;

public enum HtmlToken
{
  OL(),
  UL(),
  P(),
  LI(),
  I(),
  B();

  private String openTag;
  private String closedTag;
  private String tagName;

  HtmlToken()
  {
    this.tagName = this.name().toLowerCase();
    this.openTag = "<" + tagName + ">";
    this.closedTag = "</" + tagName + ">";
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
}
