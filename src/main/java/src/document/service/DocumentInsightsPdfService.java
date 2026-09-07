package src.document.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import src.document.dto.DocumentDetailsResponse;
import src.document.dto.EmailDocumentInsightsRequest;
import src.entity.User;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentInsightsPdfService {

    private static final String REGULAR_FONT =
            "/fonts/NotoSans-Regular.ttf";

    private static final String BOLD_FONT =
            "/fonts/NotoSans-Bold.ttf";

    private static final float PAGE_WIDTH =
            PDRectangle.A4.getWidth();

    private static final float PAGE_HEIGHT =
            PDRectangle.A4.getHeight();

    private static final float MARGIN =
            48F;

    private static final float CONTENT_WIDTH =
            PAGE_WIDTH - (MARGIN * 2);

    private static final float BOTTOM_LIMIT =
            58F;

    private static final Color BRAND_PURPLE =
            new Color(91, 76, 255);

    private static final Color BRAND_PURPLE_LIGHT =
            new Color(244, 242, 255);

    private static final Color TEXT_PRIMARY =
            new Color(28, 32, 44);

    private static final Color TEXT_SECONDARY =
            new Color(98, 105, 122);

    private static final Color BORDER =
            new Color(226, 229, 238);

    private static final Color WHITE =
            Color.WHITE;

    public byte[] generate(
            DocumentDetailsResponse documentDetails,
            EmailDocumentInsightsRequest insights,
            User currentUser
    ) {
        boolean bulgarian =
                isBulgarian(
                        insights.language()
                );

        try (
                PDDocument pdf = new PDDocument();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream();
                InputStream regularFontStream =
                        getFont(REGULAR_FONT);
                InputStream boldFontStream =
                        getFont(BOLD_FONT)
        ) {
            PDType0Font regular =
                    PDType0Font.load(
                            pdf,
                            regularFontStream,
                            true
                    );

            PDType0Font bold =
                    PDType0Font.load(
                            pdf,
                            boldFontStream,
                            true
                    );

            PdfLayout layout =
                    new PdfLayout(
                            pdf,
                            regular,
                            bold,
                            bulgarian
                    );

            layout.startDocument();

            layout.drawHero(
                    documentDetails.title(),
                    documentDetails.workspaceName(),
                    currentUser.getUsername()
            );

            layout.drawSectionTitle(
                    bulgarian
                            ? "ОБОБЩЕНИЕ"
                            : "EXECUTIVE SUMMARY"
            );

            layout.drawParagraph(
                    insights.summary()
            );

            layout.drawSectionTitle(
                    bulgarian
                            ? "КЛЮЧОВИ ТОЧКИ"
                            : "KEY POINTS"
            );

            layout.drawNumberedItems(
                    insights.keyPoints()
            );

            layout.drawSectionTitle(
                    bulgarian
                            ? "ВАЖНИ ФАКТИ"
                            : "IMPORTANT FACTS"
            );

            layout.drawNumberedItems(
                    insights.importantFacts()
            );

            layout.finish();

            pdf.save(
                    output
            );

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not generate the document insights PDF",
                    exception
            );
        }
    }

    private InputStream getFont(
            String path
    ) {
        InputStream stream =
                getClass()
                        .getResourceAsStream(
                                path
                        );

        if (stream == null) {
            throw new IllegalStateException(
                    "PDF font resource was not found: " +
                            path
            );
        }

        return stream;
    }

    private boolean isBulgarian(
            String language
    ) {
        if (language == null) {
            return false;
        }

        return language
                .trim()
                .toLowerCase()
                .startsWith("bg");
    }

    /*
     * ---------------------------------------------------------
     * PDF LAYOUT
     * ---------------------------------------------------------
     */
    private class PdfLayout {

        private static final float BODY_FONT_SIZE =
                10.5F;

        private static final float BODY_LINE_HEIGHT =
                16F;

        private static final float SECTION_FONT_SIZE =
                12F;

        private static final float HERO_HEIGHT =
                150F;

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private final boolean bulgarian;

        private PDPage page;
        private PDPageContentStream stream;

        private float y;
        private int pageNumber;

        private PdfLayout(
                PDDocument document,
                PDFont regular,
                PDFont bold,
                boolean bulgarian
        ) {
            this.document =
                    document;

            this.regular =
                    regular;

            this.bold =
                    bold;

            this.bulgarian =
                    bulgarian;
        }

        private void startDocument()
                throws IOException {

            newPage(
                    true
            );
        }

        private void newPage(
                boolean firstPage
        ) throws IOException {

            closeCurrentPage();

            page =
                    new PDPage(
                            PDRectangle.A4
                    );

            document.addPage(
                    page
            );

            stream =
                    new PDPageContentStream(
                            document,
                            page
                    );

            pageNumber++;

            y =
                    PAGE_HEIGHT - MARGIN;

            if (!firstPage) {
                drawContinuationHeader();
            }
        }

        /*
         * -----------------------------------------------------
         * HERO
         * -----------------------------------------------------
         */
        private void drawHero(
                String documentTitle,
                String workspaceName,
                String username
        ) throws IOException {

            float heroTop =
                    PAGE_HEIGHT;

            float heroBottom =
                    PAGE_HEIGHT -
                            HERO_HEIGHT;

            stream.setNonStrokingColor(
                    BRAND_PURPLE
            );

            stream.addRect(
                    0,
                    heroBottom,
                    PAGE_WIDTH,
                    HERO_HEIGHT
            );

            stream.fill();

            drawText(
                    "OURVAULT",
                    bold,
                    12,
                    MARGIN,
                    heroTop - 37,
                    WHITE
            );

            drawText(
                    bulgarian
                            ? "AI АНАЛИЗ НА ДОКУМЕНТ"
                            : "AI DOCUMENT INSIGHTS",
                    regular,
                    9,
                    MARGIN,
                    heroTop - 54,
                    new Color(
                            226,
                            222,
                            255
                    )
            );

            List<String> titleLines =
                    wrapText(
                            documentTitle,
                            bold,
                            20,
                            CONTENT_WIDTH
                    );

            float titleY =
                    heroTop - 85;

            for (
                    int index = 0;
                    index < Math.min(
                            titleLines.size(),
                            2
                    );
                    index++
            ) {
                drawText(
                        titleLines.get(index),
                        bold,
                        20,
                        MARGIN,
                        titleY,
                        WHITE
                );

                titleY -=
                        25;
            }

            String metadata =
                    (bulgarian
                            ? "Работно пространство: "
                            : "Workspace: ")
                            +
                            safe(
                                    workspaceName
                            )
                            +
                            "   •   "
                            +
                            (bulgarian
                                    ? "Генерирано за: "
                                    : "Generated for: ")
                            +
                            safe(
                                    username
                            );

            drawText(
                    metadata,
                    regular,
                    8.5F,
                    MARGIN,
                    heroBottom + 18,
                    new Color(
                            235,
                            232,
                            255
                    )
            );

            y =
                    heroBottom - 30;
        }

        private void drawContinuationHeader()
                throws IOException {

            drawText(
                    "OURVAULT",
                    bold,
                    10,
                    MARGIN,
                    PAGE_HEIGHT - 34,
                    BRAND_PURPLE
            );

            drawText(
                    bulgarian
                            ? "AI анализ на документ"
                            : "AI Document Insights",
                    regular,
                    8,
                    MARGIN + 72,
                    PAGE_HEIGHT - 34,
                    TEXT_SECONDARY
            );

            stream.setStrokingColor(
                    BORDER
            );

            stream.moveTo(
                    MARGIN,
                    PAGE_HEIGHT - 45
            );

            stream.lineTo(
                    PAGE_WIDTH - MARGIN,
                    PAGE_HEIGHT - 45
            );

            stream.stroke();

            y =
                    PAGE_HEIGHT - 70;
        }

        /*
         * -----------------------------------------------------
         * SECTION
         * -----------------------------------------------------
         */
        private void drawSectionTitle(
                String title
        ) throws IOException {

            ensureSpace(
                    42
            );

            y -=
                    8;

            stream.setNonStrokingColor(
                    BRAND_PURPLE
            );

            stream.addRect(
                    MARGIN,
                    y - 3,
                    4,
                    17
            );

            stream.fill();

            drawText(
                    title,
                    bold,
                    SECTION_FONT_SIZE,
                    MARGIN + 14,
                    y,
                    TEXT_PRIMARY
            );

            y -=
                    30;
        }

        /*
         * -----------------------------------------------------
         * PARAGRAPH
         * -----------------------------------------------------
         */
        private void drawParagraph(
                String text
        ) throws IOException {

            if (text == null ||
                    text.isBlank()) {

                drawText(
                        "-",
                        regular,
                        BODY_FONT_SIZE,
                        MARGIN,
                        y,
                        TEXT_SECONDARY
                );

                y -=
                        BODY_LINE_HEIGHT;

                return;
            }

            List<String> lines =
                    wrapParagraphs(
                            text.trim(),
                            regular,
                            BODY_FONT_SIZE,
                            CONTENT_WIDTH
                    );

            for (String line : lines) {

                if (line.isEmpty()) {
                    y -=
                            BODY_LINE_HEIGHT / 2;

                    continue;
                }

                ensureSpace(
                        BODY_LINE_HEIGHT
                );

                drawText(
                        line,
                        regular,
                        BODY_FONT_SIZE,
                        MARGIN,
                        y,
                        TEXT_PRIMARY
                );

                y -=
                        BODY_LINE_HEIGHT;
            }

            y -=
                    8;
        }

        /*
         * -----------------------------------------------------
         * NUMBERED CARDS
         * -----------------------------------------------------
         */
        private void drawNumberedItems(
                List<String> values
        ) throws IOException {

            if (values == null ||
                    values.isEmpty()) {

                drawParagraph(
                        "-"
                );

                return;
            }

            for (
                    int index = 0;
                    index < values.size();
                    index++
            ) {
                String value =
                        values.get(index);

                if (value == null ||
                        value.isBlank()) {
                    continue;
                }

                List<String> lines =
                        wrapText(
                                value.trim(),
                                regular,
                                BODY_FONT_SIZE,
                                CONTENT_WIDTH - 60
                        );

                float textHeight =
                        Math.max(
                                1,
                                lines.size()
                        ) *
                                BODY_LINE_HEIGHT;

                float cardHeight =
                        Math.max(
                                46,
                                textHeight + 22
                        );

                ensureSpace(
                        cardHeight + 10
                );

                float cardBottom =
                        y - cardHeight + 10;

                stream.setNonStrokingColor(
                        BRAND_PURPLE_LIGHT
                );

                stream.addRect(
                        MARGIN,
                        cardBottom,
                        CONTENT_WIDTH,
                        cardHeight
                );

                stream.fill();

                /*
                 * Number badge.
                 */
                stream.setNonStrokingColor(
                        BRAND_PURPLE
                );

                stream.addRect(
                        MARGIN + 12,
                        y - 22,
                        30,
                        30
                );

                stream.fill();

                String number =
                        String.format(
                                "%02d",
                                index + 1
                        );

                drawText(
                        number,
                        bold,
                        9,
                        MARGIN + 19,
                        y - 11,
                        WHITE
                );

                float textY =
                        y - 3;

                for (String line : lines) {
                    drawText(
                            line,
                            regular,
                            BODY_FONT_SIZE,
                            MARGIN + 56,
                            textY,
                            TEXT_PRIMARY
                    );

                    textY -=
                            BODY_LINE_HEIGHT;
                }

                y =
                        cardBottom - 10;
            }

            y -=
                    3;
        }

        /*
         * -----------------------------------------------------
         * PAGE BREAKS
         * -----------------------------------------------------
         */
        private void ensureSpace(
                float requiredHeight
        ) throws IOException {

            if (y - requiredHeight <
                    BOTTOM_LIMIT) {

                newPage(
                        false
                );
            }
        }

        /*
         * -----------------------------------------------------
         * FOOTER
         * -----------------------------------------------------
         */
        private void drawFooter()
                throws IOException {

            if (stream == null) {
                return;
            }

            stream.setStrokingColor(
                    BORDER
            );

            stream.moveTo(
                    MARGIN,
                    38
            );

            stream.lineTo(
                    PAGE_WIDTH - MARGIN,
                    38
            );

            stream.stroke();

            drawText(
                    "OurVault",
                    bold,
                    7.5F,
                    MARGIN,
                    24,
                    TEXT_SECONDARY
            );

            drawText(
                    bulgarian
                            ? "AI-базирана система за управление на знания"
                            : "AI-Powered Knowledge Base + RAG System",
                    regular,
                    7.5F,
                    MARGIN + 45,
                    24,
                    TEXT_SECONDARY
            );

            drawTextRightAligned(
                    (bulgarian
                            ? "Страница "
                            : "Page ")
                            +
                            pageNumber,
                    regular,
                    7.5F,
                    PAGE_WIDTH - MARGIN,
                    24,
                    TEXT_SECONDARY
            );
        }

        private void finish()
                throws IOException {

            closeCurrentPage();
        }

        private void closeCurrentPage()
                throws IOException {

            if (stream == null) {
                return;
            }

            drawFooter();

            stream.close();

            stream =
                    null;
        }

        /*
         * -----------------------------------------------------
         * TEXT HELPERS
         * -----------------------------------------------------
         */
        private void drawText(
                String text,
                PDFont font,
                float size,
                float x,
                float y,
                Color color
        ) throws IOException {

            stream.beginText();

            stream.setFont(
                    font,
                    size
            );

            stream.setNonStrokingColor(
                    color
            );

            stream.newLineAtOffset(
                    x,
                    y
            );

            stream.showText(
                    normalizeText(
                            text
                    )
            );

            stream.endText();
        }

        private void drawTextRightAligned(
                String text,
                PDFont font,
                float size,
                float rightX,
                float y,
                Color color
        ) throws IOException {

            float width =
                    textWidth(
                            text,
                            font,
                            size
                    );

            drawText(
                    text,
                    font,
                    size,
                    rightX - width,
                    y,
                    color
            );
        }

        private List<String> wrapParagraphs(
                String text,
                PDFont font,
                float size,
                float maximumWidth
        ) throws IOException {

            List<String> result =
                    new ArrayList<>();

            String normalized =
                    text.replace(
                            "\r",
                            ""
                    );

            String[] paragraphs =
                    normalized.split(
                            "\n",
                            -1
                    );

            for (String paragraph :
                    paragraphs) {

                if (paragraph.isBlank()) {
                    result.add(
                            ""
                    );

                    continue;
                }

                result.addAll(
                        wrapText(
                                paragraph,
                                font,
                                size,
                                maximumWidth
                        )
                );
            }

            return result;
        }

        private List<String> wrapText(
                String text,
                PDFont font,
                float size,
                float maximumWidth
        ) throws IOException {

            List<String> lines =
                    new ArrayList<>();

            if (text == null ||
                    text.isBlank()) {

                lines.add(
                        ""
                );

                return lines;
            }

            String[] words =
                    normalizeText(
                            text
                    )
                            .trim()
                            .split(
                                    "\\s+"
                            );

            StringBuilder current =
                    new StringBuilder();

            for (String word :
                    words) {

                String candidate =
                        current.isEmpty()
                                ? word
                                : current +
                                " " +
                                word;

                if (textWidth(
                        candidate,
                        font,
                        size
                ) <= maximumWidth) {

                    current.setLength(
                            0
                    );

                    current.append(
                            candidate
                    );

                    continue;
                }

                if (!current.isEmpty()) {
                    lines.add(
                            current.toString()
                    );

                    current.setLength(
                            0
                    );
                }

                /*
                 * Handle unusually long tokens.
                 */
                if (textWidth(
                        word,
                        font,
                        size
                ) >
                        maximumWidth) {

                    splitLongWord(
                            word,
                            font,
                            size,
                            maximumWidth,
                            lines,
                            current
                    );

                } else {
                    current.append(
                            word
                    );
                }
            }

            if (!current.isEmpty()) {
                lines.add(
                        current.toString()
                );
            }

            return lines;
        }

        private void splitLongWord(
                String word,
                PDFont font,
                float size,
                float maximumWidth,
                List<String> lines,
                StringBuilder remainder
        ) throws IOException {

            StringBuilder part =
                    new StringBuilder();

            for (
                    int index = 0;
                    index < word.length();
                    index++
            ) {
                char character =
                        word.charAt(
                                index
                        );

                String candidate =
                        part.toString() +
                                character;

                if (!part.isEmpty() &&
                        textWidth(
                                candidate,
                                font,
                                size
                        ) >
                                maximumWidth) {

                    lines.add(
                            part.toString()
                    );

                    part.setLength(
                            0
                    );
                }

                part.append(
                        character
                );
            }

            remainder.append(
                    part
            );
        }

        private float textWidth(
                String text,
                PDFont font,
                float size
        ) throws IOException {

            return font.getStringWidth(
                    normalizeText(
                            text
                    )
            ) /
                    1000F *
                    size;
        }

        private String normalizeText(
                String text
        ) {
            if (text == null) {
                return "";
            }

            return text
                    .replace(
                            '\u00A0',
                            ' '
                    )
                    .replace(
                            '\t',
                            ' '
                    );
        }

        private String safe(
                String value
        ) {
            if (value == null ||
                    value.isBlank()) {

                return "-";
            }

            return value.trim();
        }
    }
}