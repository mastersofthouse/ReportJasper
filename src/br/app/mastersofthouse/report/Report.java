package br.app.mastersofthouse.report;

import br.app.mastersofthouse.report.gui.ParameterPrompt;
import br.app.mastersofthouse.report.tools.printing.Printerlookup;
import br.app.mastersofthouse.report.types.DsType;
import br.app.mastersofthouse.report.types.InputType;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.data.JsonQLDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.*;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.lang3.LocaleUtils;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Report {

    private Config config;
    private File inputFile;
    private InputType initialInputType;
    private JasperDesign jasperDesign;
    private JasperReport jasperReport;
    private JasperPrint jasperPrint;
    private File output;
    private Locale defaultLocale;
    private static final String STDOUT = "-";
    private static PrintStream configSink = System.err;
    private static PrintStream debugSink = System.err;


    public Report(Config config, File inputFile) throws IllegalArgumentException {
        configSink = System.err;
        debugSink = System.err;
        this.config = config;
        defaultLocale = Locale.getDefault();

        this.inputFile = inputFile;

        Object inputObject = null;
        try {

            inputObject = JRLoader.loadObject(inputFile);
            Boolean casterror = true;
            try {
                jasperReport = (JasperReport) inputObject;
                casterror = false;
                initialInputType = InputType.JASPER_REPORT;
            } catch (ClassCastException ex) {
                // nothing to do here
            }
            try {
                jasperPrint = (JasperPrint) inputObject;
                casterror = false;
                initialInputType = InputType.JASPER_PRINT;
            } catch (ClassCastException ex) {
                // nothing to do here
            }
            if (casterror) {
                throw new IllegalArgumentException("input file: \"" + inputFile + "\" is not of a valid type");
            }
        } catch (JRException ex) {
            try {
                jasperDesign = JRXmlLoader.load(inputFile.getAbsolutePath());
                initialInputType = InputType.JASPER_DESIGN;
                compile();
            } catch (JRException ex1) {
                throw new IllegalArgumentException("input file: \"" + inputFile + "\" is not a valid jrxml file: " + ex1.getMessage(), ex1);
            }
        }

        String inputBasename = inputFile.getName().split("\\.(?=[^\\.]+$)")[0];
        if (!config.hasOutput()) {
            File parent = inputFile.getParentFile();
            if (parent != null) {
                this.output = parent;
            } else {
                this.output = new File(inputBasename);
            }
        } else {
            this.output = new File(config.getOutput());
        }
        if (this.output.isDirectory()) {
            this.output = new File(this.output, inputBasename);
        }
    }

    private void compile() throws JRException {
        jasperReport = JasperCompileManager.compileReport(jasperDesign);
    }


    public void compileToFile() {
        if (initialInputType == InputType.JASPER_DESIGN) {
            try {
                JRSaver.saveObject(jasperReport, this.output.getAbsolutePath() + ".jasper");
            } catch (JRException ex) {
                throw new IllegalArgumentException("outputFile" + this.output.getAbsolutePath() + ".jasper" + "could not be written");
            }
        } else {
            throw new IllegalArgumentException("input file: \"" + inputFile + "\" is not a valid jrxml file");
        }
    }


    public void fill() throws InterruptedException {

        PrintStream originalStdout = System.out;
        try {
            System.setOut(System.err);
            fillInternal();
        }
        finally {
            System.out.flush();
            System.setOut(originalStdout);
        }
    }

    private void fillInternal() throws InterruptedException {
        if (initialInputType != InputType.JASPER_PRINT) {

            Map<String, Object> parameters = getCmdLineReportParams();

            if (config.hasAskFilter()) {
                JRParameter[] reportParams = jasperReport.getParameters();
                parameters = promptForParams(reportParams, parameters, jasperReport.getName());
            }
            try {
                if (parameters.containsKey("REPORT_LOCALE")) {
                    if (parameters.get("REPORT_LOCALE") != null) {
                        Locale.setDefault((Locale) parameters.get("REPORT_LOCALE"));
                    }
                }
                if (DsType.none.equals(config.getDbType())) {
                    jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
                } else if (DsType.csv.equals(config.getDbType())) {
                    Db db = new Db();
                    JRCsvDataSource ds = db.getCsvDataSource(config);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, ds);
                } else if (DsType.xml.equals(config.getDbType())) {
                    if (config.xmlXpath == null) {

                        config.xmlXpath = getMainDatasetQuery();
                    }
                    Db db = new Db();
                    JRXmlDataSource ds = db.getXmlDataSource(config);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, ds);
                } else if (DsType.json.equals(config.getDbType())) {
                    if (config.jsonQuery == null) {
                        config.jsonQuery = getMainDatasetQuery();
                    }
                    Db db = new Db();
                    JsonDataSource ds = db.getJsonDataSource(config);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, ds);
                } else if (DsType.jsonql.equals(config.getDbType())) {
                    if (config.jsonQLQuery == null) {
                        config.jsonQLQuery = getMainDatasetQuery();
                    }
                    Db db = new Db();
                    JsonQLDataSource ds = db.getJsonQLDataSource(config);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, ds);
                } else {
                    Db db = new Db();
                    Connection con = db.getConnection(config);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, con);
                    con.close();
                }
                Locale.setDefault(defaultLocale);
            } catch (SQLException ex) {
                throw new IllegalArgumentException("Unable to connect to database: " + ex.getMessage(), ex);
            } catch (JRException e) {
                throw new IllegalArgumentException("Error filling report" + e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unable to load driver: " + e.getMessage(), e);
            } finally {
                Locale.setDefault(defaultLocale);
            }
        }
    }


    public void print() throws JRException {
        PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();

        if (config.hasCopies()){
            printRequestAttributeSet.add(new Copies(config.getCopies().intValue()));
        }
        PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
        JRPrintServiceExporter exporter = new JRPrintServiceExporter();
        if (config.hasReportName()) {
            jasperPrint.setName(config.getReportName());
        }
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        SimplePrintServiceExporterConfiguration expConfig = new SimplePrintServiceExporterConfiguration();
        if (config.hasPrinterName()) {
            String printerName = config.getPrinterName();
            PrintService service = Printerlookup.getPrintservice(printerName, Boolean.TRUE, Boolean.TRUE);
            expConfig.setPrintService(service);
        }
        expConfig.setPrintRequestAttributeSet(printRequestAttributeSet);
        expConfig.setPrintServiceAttributeSet(printServiceAttributeSet);
        expConfig.setDisplayPageDialog(Boolean.FALSE);
        if (config.isWithPrintDialog()) {
            setLookAndFeel();
            expConfig.setDisplayPrintDialog(Boolean.TRUE);
        } else {
            expConfig.setDisplayPrintDialog(Boolean.FALSE);
        }
        exporter.setConfiguration(expConfig);
        exporter.exportReport();
    }


    public void view() throws JRException {
        setLookAndFeel();
        JasperViewer.viewReport(jasperPrint, false);
    }


    private OutputStream getOutputStream(String suffix) throws JRException {
        OutputStream outputStream;
        if (this.output.getName().equals(STDOUT)) {
            outputStream = System.out;
        } else {
            String outputPath = this.output.getAbsolutePath() + suffix;
            try {
                outputStream = new FileOutputStream(outputPath);
            } catch (IOException ex) {
                throw new JRException("Unable to create outputStream to " + outputPath, ex);
            }
        }
        return outputStream;
    }


    public void exportJrprint() throws JRException {
        JRSaver.saveObject(jasperPrint, getOutputStream(".jrprint"));
    }


    public void exportPdf() throws JRException {
        if(config.hasProtect()){
            jasperPrint.setProperty("net.sf.jasperreports.export.pdf.encrypted", "True");
            jasperPrint.setProperty("net.sf.jasperreports.export.pdf.128.bit.key", "True");
            jasperPrint.setProperty("net.sf.jasperreports.export.pdf.permissions.allowed", "PRINTING");
            jasperPrint.setProperty("net.sf.jasperreports.export.pdf.user.password", config.getProtect());
            jasperPrint.setProperty("net.sf.jasperreports.export.pdf.owner.password", config.getProtectDefault());
        }
        JasperExportManager.exportReportToPdfStream(jasperPrint, getOutputStream(".pdf"));
    }


    public void exportRtf() throws JRException {
        JRRtfExporter exporter = new JRRtfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(getOutputStream(".rtf")));
        exporter.exportReport();
    }


    public void exportDocx() throws JRException {
        JRDocxExporter exporter = new JRDocxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".docx")));
        exporter.exportReport();
    }


    public void exportOdt() throws JRException {
        JROdtExporter exporter = new JROdtExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".odt")));
        exporter.exportReport();
    }


    public void exportHtml() throws JRException {
        HtmlExporter exporter = new HtmlExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(getOutputStream(".html")));
        exporter.exportReport();

    }


    public void exportXml() throws JRException {
        JRXmlExporter exporter = new JRXmlExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        SimpleXmlExporterOutput outputStream = new SimpleXmlExporterOutput(getOutputStream(".xml"));
        outputStream.setEmbeddingImages(false);
        exporter.setExporterOutput(outputStream);
        exporter.exportReport();
    }


    public void exportXls() throws JRException {
        Map<String, String> dateFormats = new HashMap<String, String>();
        dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");

        JRXlsExporter exporter = new JRXlsExporter();
        SimpleXlsReportConfiguration repConfig = new SimpleXlsReportConfiguration();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".xls")));
        repConfig.setDetectCellType(Boolean.TRUE);
        repConfig.setFormatPatternsMap(dateFormats);
        exporter.setConfiguration(repConfig);
        exporter.exportReport();
    }

    public void exportXlsMeta() throws JRException {
        Map<String, String> dateFormats = new HashMap<String, String>();
        dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");

        JRXlsMetadataExporter exporter = new JRXlsMetadataExporter();
        SimpleXlsMetadataReportConfiguration repConfig = new SimpleXlsMetadataReportConfiguration();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".xls")));
        repConfig.setDetectCellType(Boolean.TRUE);
        repConfig.setFormatPatternsMap(dateFormats);
        exporter.setConfiguration(repConfig);
        exporter.exportReport();
    }


    public void exportXlsx() throws JRException {
        Map<String, String> dateFormats = new HashMap<String, String>();
        dateFormats.put("EEE, MMM d, yyyy", "ddd, mmm d, yyyy");

        JRXlsxExporter exporter = new JRXlsxExporter();
        SimpleXlsxReportConfiguration repConfig = new SimpleXlsxReportConfiguration();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".xlsx")));
        repConfig.setDetectCellType(Boolean.TRUE);
        repConfig.setFormatPatternsMap(dateFormats);
        exporter.setConfiguration(repConfig);
        exporter.exportReport();
    }


    public void exportCsv() throws JRException {
        JRCsvExporter exporter = new JRCsvExporter();
        SimpleCsvExporterConfiguration configuration = new SimpleCsvExporterConfiguration();
        configuration.setFieldDelimiter(config.getOutFieldDel());
        exporter.setConfiguration(configuration);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(getOutputStream(".csv"), config.getOutCharset()));
        exporter.exportReport();
    }

    public void exportCsvMeta() throws JRException {
        JRCsvMetadataExporter exporter = new JRCsvMetadataExporter();
        SimpleCsvMetadataExporterConfiguration configuration = new SimpleCsvMetadataExporterConfiguration();
        configuration.setFieldDelimiter(config.getOutFieldDel());
        exporter.setConfiguration(configuration);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(getOutputStream(".csv"), config.getOutCharset()));
        exporter.exportReport();
    }


    public void exportOds() throws JRException {
        JROdsExporter exporter = new JROdsExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".ods")));
        exporter.exportReport();
    }


    public void exportPptx() throws JRException {
        JRPptxExporter exporter = new JRPptxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(getOutputStream(".pptx")));
        exporter.exportReport();
    }


    public void exportXhtml() throws JRException {
        HtmlExporter exporter = new HtmlExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(getOutputStream(".x.html")));
        exporter.exportReport();
    }

    private Map<String, Object> getCmdLineReportParams() {
        JRParameter[] jrParameterArray = jasperReport.getParameters();
        Map<String, JRParameter> jrParameters = new HashMap<String, JRParameter>();
        Map<String, Object> parameters = new HashMap<String, Object>();
        List<String> params;
        if (config.hasParams()) {
            params = config.getParams();
            for (JRParameter rp : jrParameterArray) {
                jrParameters.put(rp.getName(), rp);
            }
            String paramName = null;
            String paramValue = null;

            for (String p : params) {
                try {
                    paramName = p.split("=")[0];
                    paramValue = p.split("=", 2)[1];
                } catch (Exception e) {
                    throw new IllegalArgumentException("Wrong report param format! " + p, e);
                }
                if (!jrParameters.containsKey(paramName)) {
                    //ignore params not exist.
                    continue;
                }

                JRParameter reportParam = jrParameters.get(paramName);

                try {
                    if (Date.class
                            .equals(reportParam.getValueClass())) {
                        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
                        parameters.put(paramName, (Date) dateFormat.parse(paramValue));
                    } else if (Image.class
                            .equals(reportParam.getValueClass())) {
                        Image image =
                                Toolkit.getDefaultToolkit().createImage(
                                        JRLoader.loadBytes(new File(paramValue)));
                        MediaTracker traker = new MediaTracker(new Panel());
                        traker.addImage(image, 0);
                        try {
                            traker.waitForID(0);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                    "Image tracker error: " + e.getMessage(), e);
                        }
                        parameters.put(paramName, image);
                    } else if (Locale.class
                            .equals(reportParam.getValueClass())) {
                        parameters.put(paramName, LocaleUtils.toLocale(paramValue));
                    }else if(Collection.class.equals(reportParam.getValueClass())){

                        String[] values = paramValue.split(",");
                        ArrayList<String> result = new ArrayList<String>();

                        for (int i = 0; i < values.length; i++) {
                            result.add(values[i].toString().trim());
                        }
                        parameters.put(paramName, result);

                    } else {
                        try {
                            parameters.put(paramName,
                                    reportParam.getValueClass()
                                            .getConstructor(String.class)
                                            .newInstance(paramValue));
                        } catch (InstantiationException ex) {
                            throw new IllegalArgumentException("Parameter '"
                                    + paramName + "' of type '"
                                    + reportParam.getValueClass().getName()
                                    + " caused " + ex.getClass().getName()
                                    + " " + ex.getMessage(), ex);
                        } catch (IllegalAccessException ex) {
                            throw new IllegalArgumentException("Parameter '"
                                    + paramName + "' of type '"
                                    + reportParam.getValueClass().getName()
                                    + " caused " + ex.getClass().getName()
                                    + " " + ex.getMessage(), ex);
                        } catch (InvocationTargetException ex) {
                            Throwable cause = ex.getCause();
                            throw new IllegalArgumentException("Parameter '"
                                    + paramName + "' of type '"
                                    + reportParam.getValueClass().getName()
                                    + " caused " + cause.getClass().getName()
                                    + " " + cause.getMessage(), cause);
                        } catch (NoSuchMethodException ex) {
                            throw new IllegalArgumentException("Parameter '"
                                    + paramName + "' of type '"
                                    + reportParam.getValueClass().getName()
                                    + " with value '" + paramValue
                                    + "' is not supported by JasperStarter!", ex);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("NumberFormatException: " + e.getMessage() + "\" in \"" + p + "\"", e);
                } catch (java.text.ParseException e) {
                    throw new IllegalArgumentException(e.getMessage() + "\" in \"" + p + "\"", e);
                } catch (JRException e) {
                    throw new IllegalArgumentException("Unable to load image from: " + paramValue, e);
                }
            }
        }
        return parameters;
    }


    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException e) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, e);
        } catch (InstantiationException e) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, e);
        } catch (IllegalAccessException e) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Map<String, Object> promptForParams(JRParameter[] reportParams, Map<String, Object> params, String reportName) throws InterruptedException {
        boolean isForPromptingOnly = false;
        boolean isUserDefinedOnly = false;
        boolean emptyOnly = false;
        switch (config.getAskFilter()) {
            case ae:
                emptyOnly = true;
            case a:
                isForPromptingOnly = false;
                isUserDefinedOnly = false;
                break;
            case ue:
                emptyOnly = true;
            case u:
                isUserDefinedOnly = true;
                break;
            case pe:
                emptyOnly = true;
            case p:
                isUserDefinedOnly = true;
                isForPromptingOnly = true;
                break;
        }
        Report.setLookAndFeel();
        ParameterPrompt prompt = new ParameterPrompt(null, reportParams, params,
                reportName, isForPromptingOnly, isUserDefinedOnly, emptyOnly);
        if (JOptionPane.OK_OPTION != prompt.show()) {
            throw new InterruptedException("User aborted at parameter promt!");
        }
        return params;
    }


    public JRParameter[] getReportParameters() throws IllegalArgumentException {
        JRParameter[] returnval = null;
        if (jasperReport != null) {
            returnval = jasperReport.getParameters();
        } else {
            throw new IllegalArgumentException(
                    "Parameters could not be read from "
                            + inputFile.getAbsolutePath());
        }
        return returnval;
    }

    public String getMainDatasetQuery() throws IllegalArgumentException {
        if  (initialInputType == InputType.JASPER_DESIGN) {
            return jasperDesign.getMainDesignDataset().getQuery().getText();
        } else if (initialInputType == InputType.JASPER_REPORT) {
            return jasperReport.getMainDataset().getQuery().getText();
        } else {
            throw new IllegalArgumentException("No query for input type: " + initialInputType);
        }
    }
}
