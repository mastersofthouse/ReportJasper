package br.app.mastersofthouse.report;

import br.app.mastersofthouse.report.types.AskFilter;
import br.app.mastersofthouse.report.types.Dest;
import br.app.mastersofthouse.report.types.DsType;
import br.app.mastersofthouse.report.types.OutputFormat;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sourceforge.argparse4j.annotation.Arg;
import org.apache.commons.lang3.LocaleUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


public class Config {

    String versionString;
    @Arg(dest = Dest.DEBUG)
    boolean verbose;
    @Arg(dest = Dest.LOCALE)
    String locale;
    @Arg(dest = Dest.COMMAND)
    String command;
    @Arg(dest = Dest.INPUT)
    String input;
    @Arg(dest = Dest.OUTPUT)
    String output;
    @Arg(dest = Dest.OUTPUT_FORMATS)
    List<OutputFormat> outputFormats;
    @Arg(dest = Dest.JDBC_DIR)
    File jdbcDir;
    @Arg(dest = Dest.RESOURCE)
    String resource;
    @Arg(dest = Dest.ASK)
    AskFilter askFilter;
    @Arg(dest = Dest.DS_TYPE)
    DsType dbType;
    @Arg(dest = Dest.PARAMS)
    List<String> params;
    @Arg(dest = Dest.DATA_FILE)
    File dataFile;
    @Arg(dest = Dest.XML_XPATH)
    String xmlXpath;
    @Arg(dest = Dest.CSV_CHARSET)
    String csvCharset;
    @Arg(dest = Dest.CSV_FIRST_ROW)
    boolean csvFirstRow;
    @Arg(dest = Dest.CSV_COLUMNS)
    String csvColumns;
    @Arg(dest = Dest.CSV_RECORD_DEL)
    String csvRecordDel;
    @Arg(dest = Dest.CSV_FIELD_DEL)
    String csvFieldDel;
    @Arg(dest = Dest.JSON_QUERY)
    String jsonQuery;
    @Arg(dest = Dest.JSONQL_QUERY)
    String jsonQLQuery;
    @Arg(dest = Dest.DB_HOST)
    String dbHost;
    @Arg(dest = Dest.DB_NAME)
    String dbName;
    @Arg(dest = Dest.DB_PASSWD)
    String dbPasswd;
    @Arg(dest = Dest.DB_PORT)
    Integer dbPort;
    @Arg(dest = Dest.DB_USER)
    String dbUser;
    @Arg(dest = Dest.DB_SID)
    String dbSid;
    @Arg(dest = Dest.DB_DRIVER)
    String dbDriver;
    @Arg(dest = Dest.DB_URL)
    String dbUrl;
    @Arg(dest = Dest.OUT_FIELD_DEL)
    String outFieldDel;
    @Arg(dest = Dest.OUT_CHARSET)
    String outCharset;
    @Arg(dest = Dest.COPIES)
    Integer copies;
    @Arg(dest = Dest.REPORT_NAME)
    String reportName;
    @Arg(dest = Dest.PRINTER_NAME)
    String printerName;
    @Arg(dest = Dest.WITH_PRINT_DIALOG)
    boolean withPrintDialog;
    @Arg(dest = Dest.PROTECT)
    String protect;
    @Arg(dest = Dest.PROTECT_DEFAULT)
    String protectDefault;

    public Config() {

        URLClassLoader cl = (URLClassLoader) Report.class.getClassLoader();
        String reportReportsVersion = "";

        try {
            URL url = cl.findResource("META-INF/MANIFEST.MF");
            Manifest manifest = new Manifest(url.openStream());
            Attributes mainAttributes = manifest.getMainAttributes();
            String manifestVersion = mainAttributes.getValue("Manifest-Version");

            StringBuffer sb = new StringBuffer("Report ")
                    .append(manifestVersion).append("\n")
                    .append(Package.getPackage("br.app.mastersofthouse.report"))
                    .append(" ")
                    .append(manifestVersion).append("\n");
            versionString = sb.toString();
        } catch (IOException E) {
            // handle
        }
    }
    public String getVersionString() {
        return versionString;
    }
    public boolean isVerbose() {
        return verbose;
    }

    public boolean hasLocale() {
        if (locale != null && !locale.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public Locale getLocale() {
        return LocaleUtils.toLocale(locale);
    }
    public String getCommand() {
        return command;
    }
    public String getInput() {
        return input;
    }

    public boolean hasOutput() {
        if (output != null && !output.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public String getOutput() {
        return output;
    }

    public List<OutputFormat> getOutputFormats() {
        return outputFormats;
    }
    public File getJdbcDir() {
        return jdbcDir;
    }

    public void setJdbcDir(File value) {
        jdbcDir = value;
    }
    public boolean hasJdbcDir() {
        if (jdbcDir != null && !jdbcDir.getName().equals("")) {
            return true;
        } else {
            return false;
        }
    }
    public boolean hasResource() {
        // the resource default if set is "" .constant("")
        if (resource != null) {
            return true;
        } else {
            return false;
        }
    }
    public String getResource() {
        return resource;
    }

    public boolean hasAskFilter() {
        if (askFilter != null) {
            return true;
        } else {
            return false;
        }
    }

    public DsType getDbType() {
        return dbType;
    }

    public boolean hasParams() {
        if (params != null && !params.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public List<String> getParams() {
        return params;
    }

    public AskFilter getAskFilter() {
        return askFilter;
    }

    public String getDataFileName() {
        return dataFile.getName();
    }

    public InputStream getDataFileInputStream() throws JRException {
        if (getDataFileName().equals("-")) {
            return System.in;
        } else {
            return JRLoader.getInputStream(dataFile);
        }
    }

    public boolean getCsvFirstRow() {
        return csvFirstRow;
    }

    public String[] getCsvColumns() {
        if (csvColumns == null) {
            // return an empty array instead of null
            return new String[0];
        } else {
            return csvColumns.split(",");
        }
    }

    public String getCsvRecordDel() {
        return csvRecordDel;
    }

    public char getCsvFieldDel() {
        return csvFieldDel.charAt(0);
    }

    public String getJsonQuery() {
        return jsonQuery;
    }

    public void setJsonQuery(String value) {
        jsonQuery = value;
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPasswd() {
        return dbPasswd;
    }

    public Integer getDbPort() {
        if(dbPort == null){
            return DsType.mysql.getPort();
        }
        return dbPort;
    }

    public String getDbSid() {
        return dbSid;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getOutFieldDel() {
        return outFieldDel;
    }

    public String getOutCharset() {
        return outCharset;
    }

    public boolean hasCopies() {
        if (copies != null) {
            return true;
        } else {
            return false;
        }
    }

    public void setCopies(Integer value) { copies = value; }

    public Integer getCopies() {
        return copies;
    }

    public boolean hasReportName() {
        if (reportName != null && !reportName.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public String getReportName() {
        return reportName;
    }

    public boolean hasPrinterName() {
        if (printerName != null && !printerName.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public String getPrinterName() {
        return printerName;
    }

    public boolean isWithPrintDialog() {
        return withPrintDialog;
    }

    public boolean hasProtect() {
        if (protect != null && !protect.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public String getProtect() {
        return protect;
    }

    public String getProtectDefault() {
        if (protectDefault != null && !protectDefault.equals("")) {
            return protectDefault;
        } else {
            return protect;
        }
    }
}
