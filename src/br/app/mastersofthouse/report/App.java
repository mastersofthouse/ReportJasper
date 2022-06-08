package br.app.mastersofthouse.report;


import br.app.mastersofthouse.report.tools.classpath.ApplicationClasspath;
import br.app.mastersofthouse.report.types.Command;
import br.app.mastersofthouse.report.types.OutputFormat;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private Map<String, Argument> allArguments = null;

    public static void main(String[] args) throws JRException, InterruptedException {

        Config config = new Config();

        App app = new App();

        app.allArguments = new HashMap<String, Argument>();

        ArgumentParser parser = app.createArgumentParser(config);

        app.isArguments(args, parser);

        app.configArguments(args, app, parser, config);

        app.loadJdbc();

        app.executeCommand(app, config);

    }

    private ArgumentParser createArgumentParser(Config config) {
        CommandArguments arguments = new CommandArguments();
        return arguments.commands(config, this.allArguments);
    }

    private void isArguments(String[] args, ArgumentParser parser) {
        if (args.length == 0) {
            System.out.println(parser.formatUsage());
            System.out.println("type: report -h to get help");
            System.exit(0);
        }
    }

    private void configArguments(String[] args, App app, ArgumentParser parser, Config config) {
        try {
            app.parseArgumentParser(args, parser, config);
        } catch (ArgumentParserException ex) {
            parser.handleError(ex);
            System.exit(1);
        }
    }

    private void parseArgumentParser(String[] args, ArgumentParser parser, Config config) throws ArgumentParserException {
        parser.parseArgs(args, config);
    }

    private void loadJdbc() {
        try {
            ApplicationClasspath.addJars("../jdbc");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeCommand(App app, Config config) throws JRException, InterruptedException {

        try {

            switch (Command.getCommand(config.getCommand())) {
                case COMPILE:
                case CP:
                    app.compile(config);
                    break;
                case PROCESS:
                case PR:
                    app.process(app, config);
                    break;
                case LIST_PRINTERS:
                case PRINTERS:
                case LPR:
                    app.listPrinters();
                    break;
                case LIST_PARAMETERS:
                case PARAMS:
                case LPA:
                    App.listReportParams(config, new File(config.getInput()).getAbsoluteFile());
                    break;
            }

        } catch (IllegalArgumentException ex) {
            throw ex;
        }
    }

    private void compile(Config config) {

        IllegalArgumentException error = null;
        File input = new File(config.getInput());

        if (input.isFile()) {
            try {
                Report report = new Report(config, input);
                report.compileToFile();
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Error: not a file: " + input.getName(), exception);
            }
        } else {
            throw new IllegalArgumentException("Error: not a file: " + input.getName());
        }
    }

    private void process(App app, Config config) throws IllegalArgumentException, InterruptedException, JRException {

        app.processJdbc(config);

        app.processResource(config);

        File inputFile = new File(config.getInput()).getAbsoluteFile();

        inputFile = locateInputFile(inputFile);

        Report report = new Report(config, inputFile);

        report.fill();

        app.output(config, report);
    }

    private void processJdbc(Config config) {
        if (config.hasJdbcDir()) {
            File jdbcDir = config.getJdbcDir();
            try {
                ApplicationClasspath.addJars(jdbcDir.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void processResource(Config config) {

        if (config.hasResource()) {
            try {
                if ("".equals(config.getResource())) {
                    File res = new File(config.getInput()).getAbsoluteFile().getParentFile();
                    if (res.isDirectory()) {
                        ApplicationClasspath.add(res);
                    } else {
                        throw new IllegalArgumentException(
                                "Resource path \"" + res + "\" is not a directory");
                    }
                } else {
                    File res = new File(config.getResource());
                    ApplicationClasspath.add(res);
                }
            } catch (IOException ex) {
                throw new IllegalArgumentException("Error adding resource \""
                        + config.getResource() + "\" to classpath", ex);
            }
        }
    }

    private File locateInputFile(File inputFile) {

        if (!inputFile.exists()) {
            File newInputfile;
            newInputfile = new File(inputFile.getAbsolutePath() + ".jasper");
            if (newInputfile.isFile()) {
                inputFile = newInputfile;
            }

            if (!inputFile.exists()) {
                newInputfile = new File(inputFile.getAbsolutePath() + ".jrxml");
                if (newInputfile.isFile()) {
                    inputFile = newInputfile;
                }
            }
        }

        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Error: file not found: " + inputFile.getAbsolutePath());
        } else if (inputFile.isDirectory()) {
            throw new IllegalArgumentException("Error: " + inputFile.getAbsolutePath() + " is a directory, file needed");
        }

        return inputFile;
    }

    private void listPrinters() {
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        System.out.println("Default printer:");
        System.out.println("-----------------");
        System.out.println((defaultService == null) ? "--- not set ---" : defaultService.getName());
        System.out.println("");
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        System.out.println("Available printers:");
        System.out.println("--------------------");
        for (PrintService service : services) {
            System.out.println(service.getName());
        }
    }

    private void output(Config config, Report report) throws JRException {

        List<OutputFormat> formats = config.getOutputFormats();

        Boolean viewIt = false;
        Boolean printIt = false;

        if (formats.size() > 1 && config.getOutput().equals("-")) {
            throw new IllegalArgumentException(
                    "output file \"-\" cannot be used with multiple output formats: " + formats);
        }

        for (OutputFormat f : formats) {
            if (OutputFormat.print.equals(f)) {
                printIt = true;
            } else if (OutputFormat.view.equals(f)) {
                viewIt = true;
            } else if (OutputFormat.pdf.equals(f)) {
                report.exportPdf();
            } else if (OutputFormat.docx.equals(f)) {
                report.exportDocx();
            } else if (OutputFormat.odt.equals(f)) {
                report.exportOdt();
            } else if (OutputFormat.rtf.equals(f)) {
                report.exportRtf();
            } else if (OutputFormat.html.equals(f)) {
                report.exportHtml();
            } else if (OutputFormat.xml.equals(f)) {
                report.exportXml();
            } else if (OutputFormat.xls.equals(f)) {
                report.exportXls();
            } else if (OutputFormat.xlsMeta.equals(f)) {
                report.exportXlsMeta();
            } else if (OutputFormat.xlsx.equals(f)) {
                report.exportXlsx();
            } else if (OutputFormat.csv.equals(f)) {
                report.exportCsv();
            } else if (OutputFormat.csvMeta.equals(f)) {
                report.exportCsvMeta();
            } else if (OutputFormat.ods.equals(f)) {
                report.exportOds();
            } else if (OutputFormat.pptx.equals(f)) {
                report.exportPptx();
            } else if (OutputFormat.xhtml.equals(f)) {
                report.exportXhtml();
            } else if (OutputFormat.jrprint.equals(f)) {
                report.exportJrprint();
            } else {
                throw new IllegalArgumentException("Error output format \"" + f +  "\" not implemented!");
            }
        }
        if (viewIt) {
            report.view();
        } else if (printIt) {
            report.print();
        }
    }

    public static void listReportParams(Config config, File input) throws IllegalArgumentException {
        boolean all;
        Report report = new Report(config, input);
        JRParameter[] params = report.getReportParameters();
        int maxName = 1;
        int maxClassName = 1;
        int maxDesc = 1;
        all = false;
        for (JRParameter param : params) {
            if (!param.isSystemDefined() || all) {
                if (param.getName() != null) {
                    maxName = Math.max(maxName, param.getName().length());
                }
                if (param.getValueClassName() != null) {
                    maxClassName = Math.max(maxClassName, param.getValueClassName().length());
                }
                if (param.getDescription() != null) {
                    maxDesc = Math.max(maxDesc, param.getDescription().length());
                }
            }
        }
        for (JRParameter param : params) {
            if (!param.isSystemDefined() || all) {
                System.out.printf("%s %-" + maxName + "s %-" + maxClassName + "s %-" + maxDesc + "s %n",
                        (param.isForPrompting() ? "P" : "N"),
                        param.getName(),
                        param.getValueClassName(),
                        (param.getDescription() != null ? param.getDescription() : ""));
            }
        }
    }
}
