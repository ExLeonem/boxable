package be.quodlibet.boxable;

public interface PageBreakIdentifier
{
  boolean shouldPerformPageBreak(TableElement elementType, float yStart, float pageBottomMargin, float spaceToUse);
}
