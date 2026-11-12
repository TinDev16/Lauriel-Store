package com.lauriel.store.controller.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.service.StatisticsExcelExportService;
import com.lauriel.store.service.StatisticsPdfExportService;
import com.lauriel.store.service.StatisticsService;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebStatisticsControllerTest {

    private MockMvc mockMvc;
    private StatisticsService statisticsService;
    private StatisticsExcelExportService excelExportService;
    private StatisticsPdfExportService pdfExportService;
    private WebViewSupport viewSupport;
    private StatisticsReport sampleReport;

    @BeforeEach
    void setUp() {
        statisticsService = mock(StatisticsService.class);
        excelExportService = mock(StatisticsExcelExportService.class);
        pdfExportService = mock(StatisticsPdfExportService.class);
        viewSupport = new WebViewSupport();

        WebStatisticsController controller = new WebStatisticsController(
            statisticsService,
            excelExportService,
            pdfExportService,
            viewSupport
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        sampleReport = new StatisticsReport(
            new StatisticsReport.Range("THIS_MONTH", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "DAY", 5, 5),
            new StatisticsReport.Kpis(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO),
            new StatisticsReport.Sales(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            new StatisticsReport.Inventory(0, 0, 0, 0, BigDecimal.ZERO),
            List.of(),
            List.of(),
            new StatisticsReport.Purchase(0, BigDecimal.ZERO, 0),
            List.of(),
            List.of(),
            new StatisticsReport.Discounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            0,
            0,
            0,
            List.of(),
            List.of(),
            new StatisticsReport.Engagement(0, 0, 0, 0),
            List.of(),
            List.of(),
            List.of()
        );

        when(statisticsService.report(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(sampleReport);
    }

    @Test
    @DisplayName("GET /admin/statistics/print returns 200, view admin/statistics-print and model attributes")
    void printReport_ShouldReturnPrintViewWithModel() throws Exception {
        mockMvc.perform(get("/admin/statistics/print")
                .param("section", "sales")
                .param("preset", "THIS_MONTH")
                .param("fromDate", "2026-11-01")
                .param("toDate", "2026-11-30")
                .param("groupBy", "DAY")
                .param("threshold", "5")
                .param("top", "5"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/statistics-print"))
            .andExpect(model().attributeExists("report"))
            .andExpect(model().attribute("section", "sales"))
            .andExpect(model().attribute("preset", "THIS_MONTH"))
            .andExpect(model().attribute("groupBy", "DAY"))
            .andExpect(model().attribute("threshold", 5))
            .andExpect(model().attribute("top", 5))
            .andExpect(model().attributeExists("view"));

        verify(statisticsService).report(eq("THIS_MONTH"), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30)), eq("DAY"), eq(5), eq(5));
    }

    @Test
    @DisplayName("GET /admin/statistics/export.pdf returns 200, application/pdf and delegates to service")
    void exportPdf_ShouldReturnPdfStream() throws Exception {
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(8);
            os.write("%PDF-1.4 test".getBytes());
            return null;
        }).when(pdfExportService).exportReportPdf(
            eq(sampleReport),
            eq("overview"),
            eq("THIS_MONTH"),
            any(),
            any(),
            any(),
            eq(5),
            eq(5),
            any(OutputStream.class)
        );

        var result = mockMvc.perform(get("/admin/statistics/export.pdf")
                .param("section", "overview")
                .param("preset", "THIS_MONTH")
                .param("threshold", "5")
                .param("top", "5"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
            .andReturn();

        String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).isNotNull();
        assertThat(disposition).startsWith("attachment; filename=\"statistics_report_overview_");
        assertThat(disposition).endsWith(".pdf\"");
        assertThat(result.getResponse().getContentAsString()).isEqualTo("%PDF-1.4 test");
    }

    @Test
    @DisplayName("GET /admin/statistics/export.xlsx returns 200 and spreadsheet content type")
    void exportExcel_ShouldReturnExcelStream() throws Exception {
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(2);
            os.write("test-xlsx-content".getBytes());
            return null;
        }).when(excelExportService).exportReport(eq(sampleReport), eq("sales"), any(OutputStream.class));

        var result = mockMvc.perform(get("/admin/statistics/export.xlsx")
                .param("section", "sales"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andReturn();

        String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).isNotNull();
        assertThat(disposition).startsWith("attachment; filename=\"statistics_report_");
        assertThat(disposition).endsWith(".xlsx\"");
        assertThat(result.getResponse().getContentAsString()).isEqualTo("test-xlsx-content");
    }
}

