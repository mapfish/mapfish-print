package org.mapfish.print.output;

import org.mapfish.print.Constants;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.layout.Layout;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfStream;
import com.itextpdf.text.pdf.PdfWriter;

public abstract class AbstractOutputFormat implements OutputFormat {

    protected RenderingContext doPrint(PrintParams params) throws DocumentException {
        final String layoutName = params.jsonSpec.getString(Constants.JSON_LAYOUT_KEY);
        Layout layout = params.config.getLayout(layoutName);
        if (layout == null) {
            throw new RuntimeException("Unknown layout '" + layoutName + "'");
        }

        Document doc = new Document(layout.getFirstPageSize(null,params.jsonSpec));
        PdfWriter writer = PdfWriter.getInstance(doc, params.outputStream);
        if (!layout.isSupportLegacyReader()) {
            writer.setFullCompression();
            writer.setPdfVersion(PdfWriter.PDF_VERSION_1_5);
            writer.setCompressionLevel(PdfStream.BEST_COMPRESSION);
        }
        RenderingContext context = new RenderingContext(doc, writer, params.config, params.jsonSpec, params.configDir.getPath(), layout, params.headers);

        layout.render(params.jsonSpec, context);

        doc.close();
        writer.close();

        return context;
    }
}
