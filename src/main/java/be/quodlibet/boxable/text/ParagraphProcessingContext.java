package be.quodlibet.boxable.text;

import java.util.Stack;

import org.apache.pdfbox.pdmodel.font.PDFont;

import be.quodlibet.boxable.HTMLListNode;

public class ParagraphProcessingContext
{
  public final PipelineLayer textInLine;
  public final PipelineLayer sinceLastWrapPoint;
  public int lineCounter;
  public boolean italic;
  public boolean bold;
  public boolean listElement;
  public PDFont currentFont;
  public int orderListElement;
  public int numberOfOrderedLists;
  public int listLevel;
  public Stack<HTMLListNode> stack;

  public ParagraphProcessingContext(PDFont currentFont)
  {
    this.textInLine = new PipelineLayer();
    this.sinceLastWrapPoint = new PipelineLayer();
    this.lineCounter = 0;
    this.italic = false;
    this.bold = false;
    this.listElement = false;
    this.currentFont = currentFont;
    this.orderListElement = 1;
    this.numberOfOrderedLists = 0;
    this.listLevel = 0;
    this.stack = new Stack<>();
  }


}
