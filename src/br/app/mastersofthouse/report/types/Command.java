package br.app.mastersofthouse.report.types;

public enum Command {

    COMPILE,
    CP,
    PROCESS,
    PR,
    LIST_PRINTERS,
    PRINTERS,
    LPR,
    LIST_PARAMETERS,
    PARAMS,
    LPA;

    public static Command getCommand(String name) {
        return valueOf(name.toUpperCase());
    }
}
