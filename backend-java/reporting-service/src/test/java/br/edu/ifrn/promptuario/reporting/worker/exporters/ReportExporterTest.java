package br.edu.ifrn.promptuario.reporting.worker.exporters;

import com.lowagie.text.pdf.PdfReader;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExporterTest {

    @Test
    void csvIncludesUtf8BomAndUsesFirstRowColumns() {
        CsvReportExporter exporter = new CsvReportExporter();
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("nome", "João, Silva");
        first.put("total", 2);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("nome", "Ana");
        second.put("total", 3);
        second.put("ignored", true);
        List<Map<String, Object>> rows = List.of(first, second);

        byte[] csv = exporter.export(rows, "Relatório", Map.of());
        String contents = new String(csv, StandardCharsets.UTF_8);

        assertTrue(contents.startsWith("\uFEFF"));
        assertTrue(contents.contains("nome,total"));
        assertTrue(contents.contains("\"João, Silva\",2"));
        assertTrue(!contents.contains("ignored"));
    }

    @Test
    void xlsxUsesStreamingWorkbookAndCreatesFullSystemTabs() throws Exception {
        ExcelReportExporter exporter = new ExcelReportExporter();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Data", LocalDate.of(2026, 11, 15));
        row.put("Consultas", 23);
        row.put("Novos Pacientes", 4);
        row.put("Cancelamentos", 2);
        row.put("Prescrições", 7);
        row.put("Consultas por Médico", 10);

        byte[] xlsx = exporter.export(List.of(row), "FULL_SYSTEM", Map.of());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertEquals(6, workbook.getNumberOfSheets());
            assertEquals("Resumo", workbook.getSheetAt(0).getSheetName());
            assertEquals("Consultas", workbook.getSheetAt(1).getSheetName());
            assertEquals(CellType.NUMERIC, workbook.getSheet("Resumo").getRow(1).getCell(1).getCellType());
            assertTrue(workbook.getSheet("Resumo").getRow(1).getCell(0).getDateCellValue() != null);
        }
    }

    @Test
    void pdfIsValidAndIncludesHeaderAndPageNumbers() throws Exception {
        PdfReportExporter exporter = new PdfReportExporter();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            rows.add(Map.of("Data", "2026-11-15", "Consultas", i));
        }

        byte[] pdf = exporter.export(rows, "Relatório de Consultas", Map.of("requested_by", "admin"));
        PdfReader reader = new PdfReader(pdf);
        try {
            assertTrue(reader.getNumberOfPages() > 1);
            assertTrue(pdf.length > 1_000);
        } finally {
            reader.close();
        }
    }
}
