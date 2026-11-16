package br.edu.ifrn.promptuario.reporting.worker.exporters;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportExporter implements ReportExporter {

    private static final int ROWS_PER_TABLE_CHUNK = 500;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);
    private static final Color BRAND = new Color(0x1A, 0x6E, 0x6E);
    private static final Color STRIPE = new Color(0xF1, 0xF5, 0xF5);

    @Override
    public byte[] export(List<Map<String, Object>> data, String reportTitle, Map<String, Object> metadata) {
        List<Map<String, Object>> rows = ExporterSupport.safeData(data);
        List<String> columns = ExporterSupport.columns(rows);
        Map<String, Object> safeMetadata = ExporterSupport.safeMetadata(metadata);
        Rectangle pageSize = columns.size() > 6 ? PageSize.A4.rotate() : PageSize.A4;
        Document document = new Document(pageSize, 36, 36, 42, 48);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PageNumberEvent());
            document.open();
            addReportHeader(document, reportTitle, safeMetadata);

            if (columns.isEmpty()) {
                document.add(new Paragraph("Nenhum dado disponível", bodyFont()));
            } else if (rows.isEmpty()) {
                PdfPTable table = createTable(columns);
                addColumnHeaders(table, columns);
                document.add(table);
            } else {
                int chunks = (rows.size() + ROWS_PER_TABLE_CHUNK - 1) / ROWS_PER_TABLE_CHUNK;
                for (int chunk = 0; chunk < chunks; chunk++) {
                    int from = chunk * ROWS_PER_TABLE_CHUNK;
                    int to = Math.min(from + ROWS_PER_TABLE_CHUNK, rows.size());
                    PdfPTable table = createTable(columns);
                    addColumnHeaders(table, columns);
                    for (int i = from; i < to; i++) {
                        Map<String, Object> row = rows.get(i);
                        Color background = i % 2 == 0 ? Color.WHITE : STRIPE;
                        for (String column : columns) {
                            PdfPCell cell = new PdfPCell(new Phrase(
                                    ExporterSupport.asText(ExporterSupport.value(row, column)), bodyFont()));
                            cell.setPadding(5);
                            cell.setBorderColor(new Color(0xD5, 0xDE, 0xE0));
                            cell.setBackgroundColor(background);
                            table.addCell(cell);
                        }
                    }
                    table.setComplete(true);
                    document.add(table);
                }
            }
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Não foi possível gerar o relatório PDF", exception);
        }
    }

    private void addReportHeader(Document document, String title, Map<String, Object> metadata)
            throws DocumentException {
        Paragraph heading = new Paragraph(title == null || title.isBlank() ? "Relatório" : title, titleFont());
        heading.setAlignment(Element.ALIGN_LEFT);
        heading.setSpacingAfter(8);
        document.add(heading);

        Object generatedAt = metadata.getOrDefault("generated_at", metadata.get("generatedAt"));
        String generationTime = generatedAt == null
                ? TIMESTAMP_FORMAT.format(Instant.now())
                : formatTimestamp(generatedAt);
        Object requestedBy = metadata.getOrDefault("requested_by", metadata.get("requestedBy"));

        Paragraph details = new Paragraph("Emitido em: " + generationTime
                + (requestedBy == null ? "" : "    Solicitante: " + requestedBy), smallFont());
        details.setSpacingAfter(14);
        document.add(details);
    }

    private String formatTimestamp(Object value) {
        if (value instanceof Instant instant) {
            return TIMESTAMP_FORMAT.format(instant);
        }
        if (value instanceof OffsetDateTime dateTime) {
            return TIMESTAMP_FORMAT.format(dateTime.toInstant());
        }
        return value.toString();
    }

    private PdfPTable createTable(List<String> columns) throws DocumentException {
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);
        return table;
    }

    private void addColumnHeaders(PdfPTable table, List<String> columns) {
        Font font = headerFont();
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, font));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(6);
            cell.setBorderColor(BRAND);
            table.addCell(cell);
        }
    }

    private Font titleFont() {
        return new Font(Font.HELVETICA, 18, Font.BOLD, BRAND);
    }

    private Font headerFont() {
        return new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    }

    private Font bodyFont() {
        return new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
    }

    private Font smallFont() {
        return new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    private static final class PageNumberEvent extends PdfPageEventHelper {
        private PdfTemplate totalPages;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(30, 12);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException exception) {
                throw new IllegalStateException("Não foi possível inicializar a numeração de páginas", exception);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            var canvas = writer.getDirectContent();
            float y = document.bottomMargin() - 18;
            canvas.beginText();
            canvas.setFontAndSize(baseFont, 8);
            canvas.setTextMatrix(document.right() - 100, y);
            canvas.showText("Página " + writer.getPageNumber() + " de ");
            canvas.endText();
            canvas.addTemplate(totalPages, document.right() - 30, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPages.beginText();
            totalPages.setFontAndSize(baseFont, 8);
            totalPages.setTextMatrix(0, 0);
            totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPages.endText();
        }
    }
}