import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
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
        // Suppress Apache FOP logging for only SEVERE messages
        // This allows the progress bar to be displayed properly
        Logger fopLogger = Logger.getLogger("org.apache.fop");
        fopLogger.setLevel(Level.SEVERE);

        // Directory containing the XML files
        // Located in /usr/local/app/sghe/dw/dwprod/admin/xmltrees
        String xmlDirectory = "C:\\Users\\lsementa\\Desktop\\indiv9348-xml\\";
        // Directory where the PDFs will be saved
        String pdfDirectory = "C:\\Users\\lsementa\\Desktop\\indiv9348\\";
        // Location of the main XSL template
        // Degree Advice uses 19 XSL templates and references an 'images' folder to create the PDF
        // Located in /usr/local/app/sghe/dw/dwprod/local
        String xsltFile = "C:\\Users\\lsementa\\Documents\\Degree Advice\\degree-advice-pdf\\xsl\\fopaudits.xsl";
        // String that will collect all the fails and display them at the end
        String fails = "Fails:\n";

        // Get list of all XML files in the directory
        File dir = new File(xmlDirectory);
        File[] xmlFiles = dir.listFiles((dir1, name) -> name.endsWith(".xml"));

        if (xmlFiles != null && xmlFiles.length > 0) {
            int totalFiles = xmlFiles.length;

            // Initialize ProgressBar
            ProgressBarBuilder builder = new ProgressBarBuilder();
            builder.setTaskName("Creating PDFs...");
            builder.setInitialMax(totalFiles);
            builder.setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK);

            ProgressBar pb = builder.build();

            for (File xmlFile : xmlFiles) {
                // Create output PDF file with the same name as the XML file, in the PDF directory
                String outputPdf = pdfDirectory + xmlFile.getName().replace(".xml", ".pdf");

                try {
                    // Generate PDF for the current XML file
                    generatePDF(xmlFile.getAbsolutePath(), xsltFile, outputPdf);

                    // Update progress bar
                    pb.step();
                } catch (Exception e) {
                    e.printStackTrace();
                    //System.out.println("Failed to generate PDF for: " + xmlFile.getName());
                    fails += "Failed to generate PDF for: " + xmlFile.getName() + "\n";
                }
            }

            // Close the progress bar
            pb.close();
            // Display fails
            System.out.println(fails);
        } else {
            System.out.println("No XML files found in the specified directory.");
        }
    }

    public static void generatePDF(String xmlFile, String xsltFile, String outputPdf) throws Exception {
        // Initialize FOP factory and set up configuration
        FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());

        // Set up output stream
        OutputStream out = Files.newOutputStream(Paths.get(outputPdf));
        out = new java.io.BufferedOutputStream(out);

        try {
            // Create FOP instance to generate PDF
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

            // Set up the XSLT transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(new File(xsltFile)));

            // Set the XML source file
            Source src = new StreamSource(new File(xmlFile));

            // Create a SAXResult that will send the transformation to the FOP handler
            ContentHandler handler = fop.getDefaultHandler();
            transformer.transform(src, new SAXResult(handler));  // Use SAXResult for FOP processing
        } finally {
            // Close the output stream
            out.close();
        }
    }
}