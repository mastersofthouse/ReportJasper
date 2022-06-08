package br.app.mastersofthouse.report.types;

public enum DsType {

    none,
    csv,
    xml,
    json,
    jsonql,
    mysql("com.mysql.jdbc.Driver", 3306),
    postgres("org.postgresql.Driver", 5432),
    oracle("oracle.jdbc.OracleDriver", 1521),
    generic;
    private final String driver;
    private final Integer port;

    DsType() {
        this.driver = null;
        this.port = null;
    }

    DsType(String driver, Integer port) {
        this.driver = driver;
        this.port = port;
    }

    /**
     * <p>Getter for the field <code>driver</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getDriver() {
        return this.driver;
    }

    /**
     * <p>Getter for the field <code>port</code>.</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getPort() {
        return this.port;
    }
}
