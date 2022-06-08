
package br.app.mastersofthouse.report;

import br.app.mastersofthouse.report.types.DsType;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.data.JsonQLDataSource;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    private static PrintStream configSink = System.err;
    private static PrintStream debugSink = System.err;

    public Db() {
        configSink = System.err;
        debugSink = System.err;
    }

    public JRCsvDataSource getCsvDataSource(Config config) throws JRException {
        JRCsvDataSource ds;
        try {
            ds = new JRCsvDataSource(config.getDataFileInputStream(), config.csvCharset);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalArgumentException("Unknown CSV charset: "
                    + config.csvCharset
                    + ex.getMessage(), ex);
        }

        ds.setUseFirstRowAsHeader(config.getCsvFirstRow());
        if (!config.getCsvFirstRow()) {
            ds.setColumnNames(config.getCsvColumns());
        }

        ds.setRecordDelimiter(
                StringEscapeUtils.unescapeJava(config.getCsvRecordDel()));
        ds.setFieldDelimiter(config.getCsvFieldDel());

        return ds;
    }

	public JRXmlDataSource getXmlDataSource(Config config) throws JRException {
		JRXmlDataSource ds;
		ds = new JRXmlDataSource(config.getDataFileInputStream(), config.xmlXpath);
		return ds;
	}

    public JsonDataSource getJsonDataSource(Config config) throws JRException {
		JsonDataSource ds;
		ds = new JsonDataSource(config.getDataFileInputStream(), config.jsonQuery);
		return ds;
	}

    public JsonQLDataSource getJsonQLDataSource(Config config) throws JRException {
		JsonQLDataSource ds;
		ds = new JsonQLDataSource(config.getDataFileInputStream(), config.jsonQLQuery);
		return ds;
	}

    public Connection getConnection(Config config) throws ClassNotFoundException, SQLException {
        Connection conn = null;
        DsType dbtype = config.getDbType();
        String host = config.getDbHost();
        String user = config.getDbUser();
        String passwd = config.getDbPasswd();
        String driver = null;
        String dbname = null;
        String port = null;
        String sid = null;
        String connectString = null;
        if (DsType.mysql.equals(dbtype)) {
            driver = DsType.mysql.getDriver();
            port = config.getDbPort().toString();
            dbname = config.getDbName();
            connectString = "jdbc:mysql://" + host + ":" + port + "/" + dbname + "?useSSL=false";
        } else if (DsType.postgres.equals(dbtype)) {
            driver = DsType.postgres.getDriver();
            port = config.getDbPort().toString();
            dbname = config.getDbName();
            connectString = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
        } else if (DsType.oracle.equals(dbtype)) {
            driver = DsType.oracle.getDriver();
            port = config.getDbPort().toString();
            sid = config.getDbSid();
            connectString = "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
        } else if (DsType.generic.equals(dbtype)) {
            driver = config.getDbDriver();
            connectString = config.getDbUrl();
        }

        Class.forName(driver);
        conn = DriverManager.getConnection(connectString, user, passwd);

        return conn;
    }
}
