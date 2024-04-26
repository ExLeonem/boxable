package be.quodlibet.boxable.text;

import java.util.List;

import be.quodlibet.boxable.HTMLListNode;
import be.quodlibet.boxable.Paragraph;

public class TokenProcessor
{

  private ParagraphProcessingContext processingContext;

  private Paragraph paragraph;

  public void process(Token token, List<String> result)
  {
    switch (token.getType())
    {
    case OPEN_TAG:
//      processOpenTag(token, processingContext, result);
      break;
    case CLOSE_TAG:
//      processClosedTag(token, processingContext, result);
      break;
    case POSSIBLE_WRAP_POINT:
//      processPossibleWrapPoint(processingContext, result);
      break;
    case WRAP_POINT:
//      processWrapPoint(token, processingContext, result);
      break;
    case TEXT:
//      processText(token, processingContext, result);
      break;
    }
  }
}
