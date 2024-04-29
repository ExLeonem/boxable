package be.quodlibet.boxable.text;

import be.quodlibet.boxable.HTMLListNode;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.Paragraph;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.*;

public class TokenProcessor {
    private final static int DEFAULT_TAB = 4;
    private final static int DEFAULT_TAB_AND_BULLET = 6;
    private final static int BULLET_SPACE = 2;

    private ParagraphProcessingContext processingContext;
    private Paragraph paragraph;
    private ProcessingResult processingResult;
    private List<String> result;

    public TokenProcessor(Paragraph paragraph) {
        this.processingContext = new ParagraphProcessingContext(paragraph.getFont());
        this.paragraph = paragraph;
        this.processingResult = new ProcessingResult();
        this.result = new ArrayList<>();
    }

    public void process(Token token) {
        switch (token.getType()) {
            case OPEN_TAG:
                processOpenTag(token);
                break;
            case CLOSE_TAG:
                processClosedTag(token);
                break;
            case POSSIBLE_WRAP_POINT:
                processPossibleWrapPoint();
                break;
            case WRAP_POINT:
                processWrapPoint(token);
                break;
            case TEXT:
                processText(token);
                break;
        }
    }

    private void processOpenTag(Token token) {
        switch (token.toHtmlToken()) {
            case B:
                processOpenBoldTag();
                break;
            case I:
                processOpenItalicTag();
                break;
            case LI:
            case UL:
                processOpenListTag(token);
                break;
        }

        processingContext.sinceLastWrapPoint.push(token);
    }

    private void processOpenBoldTag() {
        processingContext.bold = true;
        processingContext.currentFont = getCorrectFont();
    }

    private void processOpenItalicTag() {
        processingContext.italic = true;
        processingContext.currentFont = getCorrectFont();
    }

    private PDFont getCorrectFont() {
        return paragraph.getFont(processingContext.bold, processingContext.italic);
    }

    private void processOpenListTag(Token token) {
        processingContext.listLevel++;
        switch (token.toHtmlToken()) {
            case OL:
                processOpenOrderedListTag();
                break;
            case UL:
                processGeneralOpenListTag();
                break;
        }
    }

    private void processOpenOrderedListTag() {
        processingContext.numberOfOrderedLists++;
        if (processingContext.listLevel > 1) {
            processingContext.stack.add(new HTMLListNode(processingContext.orderListElement - 1,
                    processingContext.stack.isEmpty() ?
                            processingContext.orderListElement - 1 + "." :
                            processingContext.stack.peek().getValue() + (processingContext.orderListElement - 1) + "."));
        }
        processingContext.orderListElement = 1;
        processGeneralOpenListTag();
    }

    private void processGeneralOpenListTag() {
        // check if you have some text before this list,
        // if you don't then you really don't need extra line break for that
        processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        if (thereIsRegularTextBeforeList()) {
            saveAccumulatedLine();
            saveCheckpointOfTrackedLines();
            resetCurrentTextInLine();
            incrementLineCounter();
        }
    }

    private boolean thereIsRegularTextBeforeList() {
        return processingContext.textInLine.trimmedWidth() > 0;
    }

    private void incrementLineCounter() {
        processingContext.lineCounter++;
    }

    private void resetCurrentTextInLine() {
        processingContext.textInLine.reset();
    }

    private void saveAccumulatedLine() {
        // this is our line
        result.add(processingContext.textInLine.trimmedText());
    }

    private void saveCheckpointOfTrackedLines() {
        processingResult.saveLineWidth(processingContext);
        processingResult.updateMaxWidth(processingContext);
        processingResult.saveTokensIntoTokenLineMap(processingContext);
    }

    private void processClosedTag(Token token) {
        switch (token.toHtmlToken()) {
            case B:
                processClosedBoldTag(token);
                break;
            case I:
                processClosedItalicTag(token);
                break;
            case UL:
            case OL:
                processClosedListTag(token);
                break;
            case LI:
                processClosedListElementTag();
                break;
            case P:
                processClosedParagraphHtmlTag();
                break;
        }
    }

    private void processClosedBoldTag(Token token) {
        processingContext.bold = false;
        processingContext.currentFont = getCorrectFont();
        processingContext.sinceLastWrapPoint.push(token);
    }

    private void processClosedItalicTag(Token token) {
        processingContext.italic = false;
        processingContext.currentFont = getCorrectFont();
        processingContext.sinceLastWrapPoint.push(token);
    }

    private void processClosedListTag(Token token) {
        processingContext.listLevel--;
        if (token.isOrderedList()) {
            processingContext.numberOfOrderedLists--;
            // reset elements
            if (processingContext.numberOfOrderedLists > 0) {
                processingContext.orderListElement = processingContext.stack.peek().getOrderingNumber() + 1;
                processingContext.stack.pop();
            }
        }

        // ensure extra space after each lists
        // no need to worry about current line text because last closing <li> tag already done that
        if (processingContext.listLevel == 0) {
            addWhitespaceToCurrentLine();
            processingResult.lineWidths.put(processingContext.lineCounter, 0.0f);
            processingResult.mapLineTokens.put(processingContext.lineCounter, new ArrayList<Token>());
            incrementLineCounter();
        }
    }

    private void addWhitespaceToCurrentLine() {
        this.result.add(" ");
    }

    private void processClosedListElementTag() {
        // wrap at last wrap point?
        if (processingContext.textInLine.width() + processingContext.sinceLastWrapPoint.trimmedWidth() > paragraph.getWidth()) {
            saveAccumulatedLine();
            saveCheckpointOfTrackedLines();
            resetCurrentTextInLine();
            incrementLineCounter();

            // wrapping at last wrap point
            if (processingContext.numberOfOrderedLists > 0) {
                String orderingNumber = processingContext.stack.isEmpty() ?
                        processingContext.orderListElement + "." :
                        processingContext.stack.pop().getValue() + ".";
                processingContext.stack.add(new HTMLListNode(processingContext.orderListElement, orderingNumber));
                try {
                    float tab = processingResult.indentLevel(DEFAULT_TAB);
                    float fontSize = paragraph.getFontSize();
                    float orderingNumberAndTab = paragraph.getFont().getStringWidth(orderingNumber) + tab;
                    processingContext.textInLine.push(processingContext.currentFont, fontSize,
                            new Token(TokenType.PADDING, String
                                    .valueOf(orderingNumberAndTab / 1000 * fontSize)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                processingContext.orderListElement++;
            } else {
                try {
                    // if it's not left aligned then ignore list and list element and deal with it as normal text where <li> mimic <br> behaviour
                    float tabBullet =
                            paragraph.getAlign().equals(HorizontalAlignment.LEFT) ? processingResult.indentLevel(DEFAULT_TAB * Math.max(
                                    processingContext.listLevel - 1, 0) + DEFAULT_TAB_AND_BULLET) : processingResult.indentLevel(DEFAULT_TAB);
                    processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                            new Token(TokenType.PADDING,
                                    String.valueOf(tabBullet / 1000 * paragraph.getFontSize())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        }

        // wrapping at this must-have wrap point
        processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        saveAccumulatedLine();
        saveCheckpointOfTrackedLines();
        resetCurrentTextInLine();
        incrementLineCounter();
        processingContext.listElement = false;
    }

    private void processClosedParagraphHtmlTag() {
        if (processingContext.textInLine.width() + processingContext.sinceLastWrapPoint.trimmedWidth() > paragraph.getWidth()) {
            saveAccumulatedLine();
            saveCheckpointOfTrackedLines();
            resetCurrentTextInLine();
            incrementLineCounter();
        }
        // wrapping at this must-have wrap point
        processingContext.textInLine.push(processingContext.sinceLastWrapPoint);

        saveAccumulatedLine();
        saveCheckpointOfTrackedLines();
        resetCurrentTextInLine();
        incrementLineCounter();

        // extra spacing because it's a paragraph
        addWhitespaceToCurrentLine();

        processingResult.lineWidths.put(processingContext.lineCounter, 0.0f);
        processingResult.mapLineTokens.put(processingContext.lineCounter, new ArrayList<Token>());
        incrementLineCounter();
    }

    private void processPossibleWrapPoint() {
        if (processingContext.textInLine.width() + processingContext.sinceLastWrapPoint.trimmedWidth() > paragraph.getWidth()) {
            if (!processingContext.textInLine.isEmpty()) {
                saveAccumulatedLine();
                saveCheckpointOfTrackedLines();
                resetCurrentTextInLine();
                incrementLineCounter();
            }
            // wrapping at last wrap point
            if (processingContext.listElement) {
                if (processingContext.numberOfOrderedLists > 0) {
                    try {
                        float tab = paragraph.getAlign().equals(HorizontalAlignment.LEFT) ? processingResult.indentLevel(DEFAULT_TAB * Math.max(
                                processingContext.listLevel - 1, 0) + DEFAULT_TAB) : processingResult.indentLevel(DEFAULT_TAB);
                        String orderingNumber = processingContext.stack.isEmpty() ?
                                processingContext.orderListElement + "." :
                                processingContext.stack.peek().getValue() + "." + (processingContext.orderListElement - 1) + ".";
                        processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                                new Token(TokenType.PADDING,
                                        String.valueOf((tab + paragraph.getFont().getStringWidth(orderingNumber)) / 1000 * paragraph.getFontSize())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // if it's not left aligned then ignore list and list element and deal with it as normal text where <li> mimic <br> behavior
                        float tabBullet =
                                paragraph.getAlign().equals(HorizontalAlignment.LEFT) ? processingResult.indentLevel(DEFAULT_TAB * Math.max(
                                        processingContext.listLevel - 1, 0) + DEFAULT_TAB_AND_BULLET) : processingResult.indentLevel(DEFAULT_TAB);
                        processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                                new Token(TokenType.PADDING,
                                        String.valueOf(tabBullet / 1000 * paragraph.getFontSize())));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        } else {
            processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        }
    }

    private void processWrapPoint(Token token) {
        // wrap at last wrap point?
        if (processingContext.textInLine.width() + processingContext.sinceLastWrapPoint.trimmedWidth() > paragraph.getWidth()) {
            saveAccumulatedLine();
            saveCheckpointOfTrackedLines();
            resetCurrentTextInLine();
            incrementLineCounter();

            // wrapping at last wrap point
            if (processingContext.listElement) {
                if (!paragraph.getAlign().equals(HorizontalAlignment.LEFT)) {
                    processingContext.listLevel = 0;
                }
                if (processingContext.numberOfOrderedLists > 0) {
                    String orderingNumber = processingContext.stack.isEmpty() ?
                            "1" + "." :
                            processingContext.stack.pop().getValue() + ". ";
                    try {
                        float tab = processingResult.indentLevel(DEFAULT_TAB);
                        float orderingNumberAndTab = paragraph.getFont().getStringWidth(orderingNumber) + tab;
                        processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                                new Token(TokenType.PADDING, String
                                        .valueOf(orderingNumberAndTab / 1000 * paragraph.getFontSize())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // if it's not left aligned then ignore list and list element and deal with it as normal text where <li> mimic <br> behaviour
                        float tabBullet =
                                paragraph.getAlign().equals(HorizontalAlignment.LEFT) ? processingResult.indentLevel(DEFAULT_TAB * Math.max(
                                        processingContext.listLevel - 1, 0) + DEFAULT_TAB_AND_BULLET) : processingResult.indentLevel(DEFAULT_TAB);
                        processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                                new Token(TokenType.PADDING,
                                        String.valueOf(tabBullet / 1000 * paragraph.getFontSize())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        }

        switch (token.toHtmlToken()) {
            case P:
                processWrapPointParagraph();
                break;
            case UL:
            case OL:
                processWrapPointListElement();
                break;
            default:
                processDefaultWrapPoint();
        }
    }

    private void processWrapPointParagraph() {
        // check if you have some text before this paragraph, if you don't then you really don't need extra line break for that
        if (processingContext.textInLine.trimmedWidth() > 0) {
            // extra spacing because it's a paragraph
            addWhitespaceToCurrentLine();
            processingResult.lineWidths.put(processingContext.lineCounter, 0.0f);
            processingResult.mapLineTokens.put(processingContext.lineCounter, new ArrayList<Token>());
            incrementLineCounter();
        }
    }

    private void processWrapPointListElement() {
        processingContext.listElement = true;
        // token padding, token bullet
        try {
            // if it's not left aligned then ignore list and list element and deal with it as normal text where <li> mimic <br> behaviour
            float tab = paragraph.getAlign().equals(HorizontalAlignment.LEFT) ? processingResult.indentLevel(DEFAULT_TAB * Math.max(
                    processingContext.listLevel - 1, 0) + DEFAULT_TAB) : processingResult.indentLevel(DEFAULT_TAB);
            processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                    new Token(TokenType.PADDING,
                            String.valueOf(tab / 1000 * paragraph.getFontSize())));
            if (processingContext.numberOfOrderedLists > 0) {
                // if it's ordering list then move depending on your: ordering number + ". "
                String orderingNumber;
                if (processingContext.listLevel > 1) {
                    orderingNumber =
                            processingContext.stack.peek().getValue() + processingContext.orderListElement
                                    + ". ";
                } else {
                    orderingNumber = processingContext.orderListElement + ". ";
                }
                processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                        Token.text(TokenType.ORDERING, orderingNumber));
                processingContext.orderListElement++;
            } else {
                // if it's unordered list then just move by bullet character (take care of alignment!)
                processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                        Token.text(TokenType.BULLET, " "));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processDefaultWrapPoint() {
        // wrapping at this must-have wrap point
        processingContext.textInLine.push(processingContext.sinceLastWrapPoint);
        saveAccumulatedLine();
        saveCheckpointOfTrackedLines();
        resetCurrentTextInLine();
        incrementLineCounter();
        if (processingContext.listLevel > 0) {
            // preserve current indent
            try {
                if (processingContext.numberOfOrderedLists > 0) {
                    float tab = paragraph.getAlign().equals(HorizontalAlignment.LEFT) ? processingResult.indentLevel(DEFAULT_TAB * Math.max(
                            processingContext.listLevel - 1, 0)) : processingResult.indentLevel(DEFAULT_TAB);
                    // if it's ordering list then move depending on your: ordering number + ". "
                    String orderingNumber;
                    if (processingContext.listLevel > 1) {
                        orderingNumber = processingContext.stack.peek().getValue() + processingContext.orderListElement + ". ";
                    } else {
                        orderingNumber = processingContext.orderListElement + ". ";
                    }
                    float tabAndOrderingNumber = tab + paragraph.getFont().getStringWidth(orderingNumber);
                    processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                            new Token(TokenType.PADDING, String.valueOf(tabAndOrderingNumber / 1000 * paragraph.getFontSize())));
                    processingContext.orderListElement++;
                } else {
                    if (paragraph.getAlign().equals(HorizontalAlignment.LEFT)) {
                        float tab = processingResult.indentLevel(
                                DEFAULT_TAB * Math.max(processingContext.listLevel - 1, 0) + DEFAULT_TAB + BULLET_SPACE);
                        processingContext.textInLine.push(processingContext.currentFont, paragraph.getFontSize(),
                                new Token(TokenType.PADDING,
                                        String.valueOf(tab / 1000 * paragraph.getFontSize())));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processText(Token token) {
        try {
            String word = token.getData();
            float wordWidth = token.getWidth(processingContext.currentFont);
            float fontSize = paragraph.getFontSize();
            float fontWidth = paragraph.getWidth();

            if (wordWidth / 1000f * fontSize > fontWidth && fontWidth > paragraph.getFont().getAverageFontWidth() / 1000f * fontSize) {
                // you need to check if you have already something in your line
                boolean alreadyTextInLine = false;

                if (thereIsRegularTextBeforeList()) {
                    alreadyTextInLine = true;
                }

                while (wordWidth / 1000f * fontSize > fontWidth) {
                    float width = 0;
                    float firstPartWordWidth = 0;
                    float restOfTheWordWidth = 0;
                    String lastTextToken = word;
                    StringBuilder firstPartOfWord = new StringBuilder();
                    StringBuilder restOfTheWord = new StringBuilder();
                    for (int i = 0; i < lastTextToken.length(); i++) {
                        char c = lastTextToken.charAt(i);
                        try {
                            width += (processingContext.currentFont.getStringWidth(String.valueOf(c)) / 1000f * fontSize);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (alreadyTextInLine) {
                            if (width < fontWidth - processingContext.textInLine.trimmedWidth()) {
                                firstPartOfWord.append(c);
                                firstPartWordWidth = Math.max(width, firstPartWordWidth);
                            } else {
                                restOfTheWord.append(c);
                                restOfTheWordWidth = Math.max(width, restOfTheWordWidth);
                            }
                        } else {
                            if (width < fontWidth) {
                                firstPartOfWord.append(c);
                                firstPartWordWidth = Math.max(width, firstPartWordWidth);
                            } else {
                                if (i == 0) {
                                    firstPartOfWord.append(c);
                                    for (int j = 1; j < lastTextToken.length(); j++) {
                                        restOfTheWord.append(lastTextToken.charAt(j));
                                    }
                                    break;
                                } else {
                                    restOfTheWord.append(c);
                                    restOfTheWordWidth = Math.max(width, restOfTheWordWidth);

                                }
                            }
                        }
                    }
                    // reset
                    alreadyTextInLine = false;
                    processingContext.sinceLastWrapPoint.push(processingContext.currentFont, fontSize,
                            Token.text(TokenType.TEXT, firstPartOfWord.toString()));
                    processingContext.textInLine.push(processingContext.sinceLastWrapPoint);

                    saveAccumulatedLine();
                    saveCheckpointOfTrackedLines();
                    resetCurrentTextInLine();
                    incrementLineCounter();

                    word = restOfTheWord.toString();
                    wordWidth = processingContext.currentFont.getStringWidth(word);
                }
                processingContext.sinceLastWrapPoint.push(processingContext.currentFont, fontSize,
                        Token.text(TokenType.TEXT, word));
            } else {
                processingContext.sinceLastWrapPoint.push(processingContext.currentFont, fontSize, token);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ProcessingResult {
        private final Map<Integer, Float> lineWidths;
        private Map<Integer, List<Token>> mapLineTokens;
        private float maxLineWidth;
        private Float spaceWidth;

        public ProcessingResult() {
            this.lineWidths = new HashMap<>();
            this.mapLineTokens = new LinkedHashMap<>();
            this.maxLineWidth = Integer.MIN_VALUE;
        }

        private float indentLevel(int numberOfSpaces) throws IOException {
            if (spaceWidth == null) {
                spaceWidth = paragraph.getFont().getSpaceWidth();
            }
            return numberOfSpaces * spaceWidth;
        }

        private void saveLineWidth(ParagraphProcessingContext processingContext) {
            lineWidths.put(processingContext.lineCounter, processingContext.textInLine.trimmedWidth());
        }

        private void saveTokensIntoTokenLineMap(ParagraphProcessingContext processingContext) {
            mapLineTokens.put(processingContext.lineCounter, processingContext.textInLine.tokens());
        }

        private void updateMaxWidth(ParagraphProcessingContext processingContext) {
            maxLineWidth = Math.max(processingResult.maxLineWidth, processingContext.textInLine.trimmedWidth());
        }
    }
}
