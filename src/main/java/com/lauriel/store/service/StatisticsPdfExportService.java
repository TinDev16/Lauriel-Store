package com.lauriel.store.service;

import com.lauriel.store.controller.web.WebViewSupport;
import com.lauriel.store.dto.statistics.StatisticsReport;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class StatisticsPdfExportService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsPdfExportService.class);

    private final ITemplateEngine templateEngine;
    private final WebViewSupport viewSupport;

    public StatisticsPdfExportService(
        ITemplateEngine templateEngine,
        WebViewSupport viewSupport
    ) {
        this.templateEngine = templateEngine;
        this.viewSupport = viewSupport;
    }

    public void exportReportPdf(
        StatisticsReport report,
        String section,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        int threshold,
        int top,
        OutputStream outputStream
    ) throws IOException {
        Context context = new Context(Locale.forLanguageTag("vi-VN"));
        context.setVariable("report", report);
        context.setVariable("section", section);
        context.setVariable("preset", preset);
        context.setVariable("fromDate", fromDate);
        context.setVariable("toDate", toDate);
        context.setVariable("groupBy", groupBy);
        context.setVariable("threshold", threshold);
        context.setVariable("top", top);
        context.setVariable("view", viewSupport);

        String html = templateEngine.process("admin/statistics-print", context);

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();

        configureFonts(builder);

        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();
    }

    private void configureFonts(PdfRendererBuilder builder) {
        boolean regularConfigured = false;

        // 1. Try Windows font path
        File winArial = new File("C:/Windows/Fonts/arial.ttf");
        if (winArial.exists()) {
            builder.useFont(winArial, "Arial");
            regularConfigured = true;
        }

        // 2. Try classpath font if Windows font not available
        if (!regularConfigured) {
            InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf");
            if (is != null) {
                try {
                    is.close();
                    builder.useFont(() -> getClass().getResourceAsStream("/fonts/arial.ttf"), "Arial");
                    regularConfigured = true;
                } catch (IOException ignored) {}
            }
        }

        // 3. Try Linux standard font paths
        if (!regularConfigured) {
            String[] linuxPaths = {
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/truetype/freefont/FreeSans.ttf"
            };
            for (String path : linuxPaths) {
                File f = new File(path);
                if (f.exists()) {
                    builder.useFont(f, "Arial");
                    regularConfigured = true;
                    break;
                }
            }
        }

        // Bold font configuration
        File winArialBd = new File("C:/Windows/Fonts/arialbd.ttf");
        if (winArialBd.exists()) {
            builder.useFont(winArialBd, "Arial", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
        } else {
            InputStream isBd = getClass().getResourceAsStream("/fonts/arialbd.ttf");
            if (isBd != null) {
                try {
                    isBd.close();
                    builder.useFont(() -> getClass().getResourceAsStream("/fonts/arialbd.ttf"), "Arial", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
                } catch (IOException ignored) {}
            }
        }

        if (!regularConfigured) {
            log.warn("Could not find a Vietnamese Unicode TrueType font (Arial). PDF characters may not render accurately.");
        }
    }
}

