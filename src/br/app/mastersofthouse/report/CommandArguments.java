package br.app.mastersofthouse.report;

import br.app.mastersofthouse.report.types.AskFilter;
import br.app.mastersofthouse.report.types.Dest;
import br.app.mastersofthouse.report.types.DsType;
import br.app.mastersofthouse.report.types.OutputFormat;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;

import java.io.File;
import java.util.Map;

public class CommandArguments {

    public ArgumentParser commands(Config config, Map<String, Argument> arguments) {

        ArgumentParser parser = ArgumentParsers.newArgumentParser("report", false, "-", "@")
                .version(config.getVersionString());

        parser.addArgument("-h", "--help")
                .action(Arguments.help())
                .help("show this help message and exit");

        parser.addArgument("--locale")
                .dest(Dest.LOCALE)
                .metavar("<lang>")
                .help("set locale with two-letter ISO-639 code"
                        + " or a combination of ISO-639 and ISO-3166 like de_DE");

        parser.addArgument("-v", "--verbose")
                .dest(Dest.DEBUG)
                .action(Arguments.storeTrue())
                .help("display additional messages");

        parser.addArgument("-V", "--version")
                .action(Arguments.version())
                .help("display version information and exit");

        Subparsers subparsers = parser.addSubparsers()
                .title("commands").
                help("type <cmd> -h to get help on command")
                .metavar("<cmd>")
                .dest(Dest.COMMAND);

        Subparser parserCompile = subparsers.addParser("compile", true)
                .aliases("cp")
                .help("compile reports");

        createCompileArguments(parserCompile);

        Subparser parserProcess =
                subparsers.addParser("process", true).aliases("pr")
                        .help("view, print or export an existing report");

        createProcessArguments(parserProcess, arguments);

        return parser;
    }

    private void createCompileArguments(Subparser parser) {

        ArgumentGroup groupOptions = parser.addArgumentGroup("options");

        groupOptions.addArgument("input")
                .metavar("<input>")
                .dest(Dest.INPUT)
                .required(true)
                .help("input file (.jrxml)");

        groupOptions.addArgument("-o")
                .metavar("<output>")
                .dest(Dest.OUTPUT)
                .help("directory or basename of output file(s)");
    }

    private void createProcessArguments(Subparser parser, Map<String, Argument> arguments) {

        ArgumentGroup groupOptions = parser.addArgumentGroup("options");

        groupOptions.addArgument("-f")
                .metavar("<fmt>").dest(Dest.OUTPUT_FORMATS)
                .required(true)
                .nargs("+")
                .type(Arguments.enumType(OutputFormat.class))
                .help("view, print, pdf, rtf, xls, xlsMeta, xlsx, docx, odt, ods, pptx," +
                        " csv, csvMeta, html, xhtml, xml, jrprint");

        groupOptions.addArgument("input")
                .metavar("<input>")
                .dest(Dest.INPUT)
                .required(true)
                .help("input file (.jrxml|.jasper|.jrprint)");

        groupOptions.addArgument("-o")
                .metavar("<output>")
                .dest(Dest.OUTPUT)
                .help("directory or basename of output file(s), use '-' for stdout");

        groupOptions.addArgument("-protect")
                .metavar("<protect>")
                .dest(Dest.PROTECT)
                .help("Set password in pdf files.");

        groupOptions.addArgument("-protect-default")
                .metavar("<protect-default>")
                .dest(Dest.PROTECT_DEFAULT)
                .help("Set password default in pdf files.");

        ArgumentGroup groupCompileOptions = parser.addArgumentGroup("compile options");

        ArgumentGroup groupFillOptions = parser.addArgumentGroup("fill options");

        groupFillOptions.addArgument("-a").metavar("<filter>").dest(Dest.ASK)
                .type(Arguments.enumType(AskFilter.class)).nargs("?")
                .setConst(AskFilter.p)
                .help("ask for report parameters. Filter: a, ae, u, ue, p, pe"
                        + " (see usage)");

        groupFillOptions.addArgument("-P")
                .metavar("<param>")
                .dest(Dest.PARAMS)
                .nargs("+")
                .help("report parameter: name=value [...]");

        groupFillOptions.addArgument("-r")
                .metavar("<resource>")
                .dest(Dest.RESOURCE)
                .nargs("?")
                .setConst("").help("path to report resource dir or jar file. If <resource> is not"
                        + " given the input directory is used.");

        ArgumentGroup groupDatasourceOptions = parser.addArgumentGroup("datasource options");

        groupDatasourceOptions.addArgument("-t")
                .metavar("<dstype>")
                .dest(Dest.DS_TYPE)
                .required(false)
                .type(Arguments.enumType(DsType.class))
                .setDefault(DsType.none)
                .help("datasource type: none, csv, xml, json, jsonql, mysql, postgres, oracle, generic (jdbc)");

        Argument argDbHost = groupDatasourceOptions
                .addArgument("-H")
                .metavar("<dbhost>")
                .dest(Dest.DB_HOST)
                .help("database host");

        Argument argDbUser = groupDatasourceOptions
                .addArgument("-u")
                .metavar("<dbuser>")
                .dest(Dest.DB_USER)
                .help("database user");

        Argument argDbPasswd = groupDatasourceOptions
                .addArgument("-p")
                .metavar("<dbpasswd>")
                .dest(Dest.DB_PASSWD)
                .setDefault("")
                .help("database password");

        Argument argDbName = groupDatasourceOptions
                .addArgument("-n")
                .metavar("<dbname>")
                .dest(Dest.DB_NAME)
                .help("database name");

        Argument argDbSid = groupDatasourceOptions
                .addArgument("--db-sid")
                .metavar("<sid>")
                .dest(Dest.DB_SID)
                .help("oracle sid");

        Argument argDbPort = groupDatasourceOptions
                .addArgument("--db-port")
                .metavar("<port>")
                .dest(Dest.DB_PORT)
                .type(Integer.class)
                .help("database port");

        Argument argDbDriver = groupDatasourceOptions
                .addArgument("--db-driver")
                .metavar("<name>")
                .dest(Dest.DB_DRIVER)
                .help("jdbc driver class name for use with type: generic");

        Argument argDbUrl = groupDatasourceOptions
                .addArgument("--db-url")
                .metavar("<jdbcUrl>")
                .dest(Dest.DB_URL)
                .help("jdbc url without user, passwd with type:generic");

        groupDatasourceOptions
                .addArgument("--jdbc-dir")
                .metavar("<dir>")
                .dest(Dest.JDBC_DIR)
                .type(File.class)
                .help("directory where jdbc driver jars are located. Defaults to ./jdbc");

        Argument argDataFile = groupDatasourceOptions
                .addArgument("--data-file")
                .metavar("<file>")
                .dest(Dest.DATA_FILE)
                .type(Arguments.fileType().acceptSystemIn().verifyCanRead())
                .help("input file for file based datasource, use '-' for stdin");

        ArgumentGroup groupOutputOptions = parser.addArgumentGroup("output options");

        groupOutputOptions
                .addArgument("-N")
                .metavar("<printername>")
                .dest(Dest.PRINTER_NAME)
                .help("name of printer");

        groupOutputOptions
                .addArgument("-d")
                .dest(Dest.WITH_PRINT_DIALOG)
                .action(Arguments.storeTrue())
                .help("show print dialog when printing");

        groupOutputOptions
                .addArgument("-s")
                .metavar("<reportname>")
                .dest(Dest.REPORT_NAME)
                .help("set internal report/document name when printing");

        groupOutputOptions
                .addArgument("-c")
                .metavar("<copies>")
                .dest(Dest.COPIES)
                .type(Integer.class).choices(Arguments.range(1, Integer.MAX_VALUE))
                .help("number of copies. Defaults to 1");

        arguments.put(argDbHost.getDest(), argDbHost);
        arguments.put(argDbUser.getDest(), argDbUser);
        arguments.put(argDbPasswd.getDest(), argDbPasswd);
        arguments.put(argDbName.getDest(), argDbName);
        arguments.put(argDbSid.getDest(), argDbSid);
        arguments.put(argDbPort.getDest(), argDbPort);
        arguments.put(argDbDriver.getDest(), argDbDriver);
        arguments.put(argDbUrl.getDest(), argDbUrl);
        arguments.put(argDataFile.getDest(), argDataFile);
    }
}
