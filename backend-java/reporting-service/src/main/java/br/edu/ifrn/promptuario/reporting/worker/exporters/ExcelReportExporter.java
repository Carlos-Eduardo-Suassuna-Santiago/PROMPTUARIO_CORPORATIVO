package br.edu.ifrn.promptuario.reporting.worker.exporters;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class ExcelReportExporter implements ReportExporter {

    private static final int ROW_WINDOW_SIZE = 100;
    private static final String[] FULL_SYSTEM_METRICS = {
            "Consultas", "Novos Pacientes", "Cancelamentos", "Prescrições", "Consultas por Médico"
    };

    @Override
    public byte[] export(List<Map<String, Object>> data, String reportTitle, Map<String, Object> metadata) {
        List<Map<String, Object>> rows = ExporterSupport.safeData(data);
        List<String> columns = ExporterSupport.columns(rows);

        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_WINDOW_SIZE);
        try (workbook; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            Styles styles = createStyles(workbook);

            if (ExporterSupport.isFullSystem(reportTitle, metadata)) {
                writeSheet(workbook, "Resumo", rows, columns, styles);
                for (String metric : FULL_SYSTEM_METRICS) {
                    writeMetricSheet(workbook, metric, rows, columns, styles);
                }
            } else {
                writeSheet(workbook, sheetName(reportTitle), rows, columns, styles);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar a planilha XLSX", exception);
        } finally {
            // SXSSF writes row data to temporary files; always remove them after generation.
            workbook.dispose();
        }
    }

    private void writeMetricSheet(Workbook workbook, String metric, List<Map<String, Object>> rows,
                                  List<String> columns, Styles styles) {
        String metricColumn = findMetricColumn(columns, metric);
        if (metricColumn == null) {
            return;
        }
        String dateColumn = findDateColumn(columns);
        List<String> projectedColumns = dateColumn == null ? List.of(metricColumn) : List.of(dateColumn, metricColumn);
        Sheet sheet = workbook.createSheet(sheetName(metric));
        writeHeader(sheet, projectedColumns, styles.header());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> source = rows.get(i);
            Row row = sheet.createRow(i + 1);
            CellStyle stripe = i % 2 == 0 ? styles.even() : styles.odd();
            for (int c = 0; c < projectedColumns.size(); c++) {
                String key = projectedColumns.get(c);
                writeCell(row.createCell(c), source == null ? null : source.get(key), stripe, styles);
            }
        }
        autoSize(sheet, projectedColumns.size());
    }

    private void writeSheet(Workbook workbook, String name, List<Map<String, Object>> rows,
                            List<String> columns, Styles styles) {
        Sheet sheet = workbook.createSheet(sheetName(name));
        if (columns.isEmpty()) {
            Row message = sheet.createRow(0);
            message.createCell(0).setCellValue("Nenhum dado disponível");
            sheet.setColumnWidth(0, 30 * 256);
            return;
        }
        writeHeader(sheet, columns, styles.header());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> source = rows.get(i);
            Row row = sheet.createRow(i + 1);
            CellStyle stripe = i % 2 == 0 ? styles.even() : styles.odd();
            for (int c = 0; c < columns.size(); c++) {
                writeCell(row.createCell(c), source == null ? null : source.get(columns.get(c)), stripe, styles);
            }
        }
        autoSize(sheet, columns.size());
    }

    private void writeHeader(Sheet sheet, List<String> columns, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        header.setHeightInPoints(24);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns.get(i));
            cell.setCellStyle(headerStyle);
            ((SXSSFSheet) sheet).trackColumnForAutoSizing(i);
        }
    }

    private void writeCell(Cell cell, Object value, CellStyle baseStyle, Styles styles) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            cell.setCellStyle(baseStyle == styles.even() ? styles.numericEven() : styles.numericOdd());
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            cell.setCellStyle(baseStyle == styles.even() ? styles.dateEven() : styles.dateOdd());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
            cell.setCellStyle(baseStyle == styles.even() ? styles.dateEven() : styles.dateOdd());
        } else if (value instanceof Instant instant) {
            cell.setCellValue(Date.from(instant));
            cell.setCellStyle(baseStyle == styles.even() ? styles.dateEven() : styles.dateOdd());
        } else if (value instanceof OffsetDateTime dateTime) {
            cell.setCellValue(Date.from(dateTime.toInstant()));
            cell.setCellStyle(baseStyle == styles.even() ? styles.dateEven() : styles.dateOdd());
        } else if (value instanceof Date date) {
            cell.setCellValue(date);
            cell.setCellStyle(baseStyle == styles.even() ? styles.dateEven() : styles.dateOdd());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
            cell.setCellStyle(baseStyle);
        } else {
            cell.setCellValue(ExporterSupport.asText(value));
            cell.setCellStyle(baseStyle);
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            int width = Math.min(Math.max(sheet.getColumnWidth(column) + 512, 3 * 256), 60 * 256);
            sheet.setColumnWidth(column, width);
        }
    }

    private Styles createStyles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle header = workbook.createCellStyle();
        header.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                new byte[]{(byte) 0x1A, (byte) 0x6E, (byte) 0x6E}, null));
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setFont(headerFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorders(header);

        CellStyle even = dataStyle(workbook, false);
        CellStyle odd = dataStyle(workbook, true);
        CellStyle numericEven = dataStyle(workbook, false);
        CellStyle numericOdd = dataStyle(workbook, true);
        CellStyle dateEven = dataStyle(workbook, false);
        CellStyle dateOdd = dataStyle(workbook, true);
        DataFormat format = workbook.createDataFormat();
        numericEven.setDataFormat(format.getFormat("#,##0.##"));
        numericOdd.setDataFormat(format.getFormat("#,##0.##"));
        dateEven.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm"));
        dateOdd.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm"));
        return new Styles(header, even, odd, numericEven, numericOdd, dateEven, dateOdd);
    }

    private CellStyle dataStyle(Workbook workbook, boolean striped) {
        CellStyle style = workbook.createCellStyle();
        if (striped) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorders(style);
        return style;
    }

    private void setThinBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String findMetricColumn(List<String> columns, String metric) {
        String expected = metric.toLowerCase(java.util.Locale.ROOT);
        return columns.stream()
                .filter(column -> column.toLowerCase(java.util.Locale.ROOT).equals(expected))
                .findFirst()
                .orElse(null);
    }

    private String findDateColumn(List<String> columns) {
        return columns.stream()
                .filter(column -> column.equalsIgnoreCase("data") || column.equalsIgnoreCase("date"))
                .findFirst()
                .orElse(null);
    }

    private String sheetName(String requested) {
        String name = requested == null || requested.isBlank() ? "Relatório" : requested;
        name = name.replaceAll("[\\\\/?*\\[\\]:]", "_").trim();
        return name.isEmpty() ? "Relatório" : name.substring(0, Math.min(31, name.length()));
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    private record Styles(CellStyle header, CellStyle even, CellStyle odd,
                          CellStyle numericEven, CellStyle numericOdd,
                          CellStyle dateEven, CellStyle dateOdd) {
    }
}