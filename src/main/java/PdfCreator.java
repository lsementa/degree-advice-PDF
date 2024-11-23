import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.ContentHandler;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class PdfCreator {

    public static void main(String[] args) {
        Logger fopLogger = Logger.getLogger("org.apache.fop");
        fopLogger.setLevel(Level.SEVERE);

        // Paths
        String xmlDirectory = "C:\\Users\\lsementa\\Desktop\\indiv9346-xml\\";
        String pdfDirectory = "C:\\Users\\lsementa\\Desktop\\indiv9346\\";

        // XSL
        String xsltFile = "C:\\Users\\lsementa\\Documents\\Degree Advice\\degree-advice-pdf\\xsl\\fopaudits.xsl";
        String xsltDashboardFile = "C:\\Users\\lsementa\\Documents\\Degree Advice\\degree-advice-pdf\\Dashboard\\DGW_Report.xsl";

        String fails = "\nFails:\n";
        File dir = new File(xmlDirectory);
        File[] xmlFiles = dir.listFiles((dir1, name) -> name.endsWith(".xml"));

        if (xmlFiles != null && xmlFiles.length > 0) {
            int totalFiles = xmlFiles.length;

            ProgressBarBuilder builder = new ProgressBarBuilder();
            builder.setTaskName("Creating PDFs...");
            builder.setInitialMax(totalFiles);
            builder.setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK);

            ProgressBar pb = builder.build();

            for (File xmlFile : xmlFiles) {
                String outputPdf = pdfDirectory + xmlFile.getName().replace(".xml", ".pdf");
                String outputHtml = pdfDirectory + xmlFile.getName().replace(".xml", ".html");

                try {
                    generatePDF(xmlFile.getAbsolutePath(), xsltFile, outputPdf);
                } catch (Exception e) {
                    fails += "Primary template failed for: " + xmlFile.getName() + ". Attempting fallback with HTML...\n";
                    try {
                        generateHTML(xmlFile.getAbsolutePath(), xsltDashboardFile, outputHtml);
                        try {
                            generatePDFWithHtml(outputHtml, outputPdf);
                            fails += "PDF created successfully with HTML.\n";
                            // Delete the HTML file
                            File htmlFile = new File(outputHtml);
                            if (htmlFile.exists() && htmlFile.delete()) {
                                fails += "Temporary HTML file deleted successfully.\n";
                            } else {
                                fails += "Failed to delete temporary HTML file.\n";
                            }
                        }catch (Exception e2) {
                            fails += "Failed to create PDF with HTML.\n";
                        }
                    } catch (Exception htmlException) {
                        fails += "Fallback HTML-to-PDF conversion failed.\n";
                        htmlException.printStackTrace();
                    }
                }

                pb.step();
            }

            // Close progress bar
            pb.close();
            // Display fails
            System.out.println(fails);

        } else {
            System.out.println("No XML files found in the specified directory.");
        }
    }

    public static void generatePDF(String xmlFile, String xsltFile, String outputPdf) throws Exception {
        FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
        OutputStream out = Files.newOutputStream(Paths.get(outputPdf));
        out = new java.io.BufferedOutputStream(out);

        try {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(new File(xsltFile)));
            Source src = new StreamSource(new File(xmlFile));
            ContentHandler handler = fop.getDefaultHandler();
            transformer.transform(src, new SAXResult(handler));
        } finally {
            out.close();
        }
    }

    public static void generateHTML(String xmlFile, String xsltFile, String outputHtml) throws Exception {
        //System.out.println("Generating HTML for " + xmlFile + "...");
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(new File(xsltFile)));

        // Source XML
        Source src = new StreamSource(new File(xmlFile));

        // Output HTML
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputHtml))) {
            Result result = new StreamResult(out);
            transformer.transform(src, result);
        }
    }

    public static void generatePDFWithHtml(String htmlFile, String outputPdf) throws Exception {
        // Path to wkhtmltopdf executable
        String wkhtmltopdfPath = "C:\\Program Files\\wkhtmltopdf\\bin\\wkhtmltopdf.exe";

        // Build the process
        ProcessBuilder processBuilder = new ProcessBuilder(
                wkhtmltopdfPath,
                "--enable-local-file-access",
                htmlFile,
                outputPdf
        );

        // Redirect error and output streams
        processBuilder.redirectErrorStream(true);

        // Start the process
        Process process = processBuilder.start();

        // Wait for the process to complete
        int exitCode = process.waitFor();
    }

}
