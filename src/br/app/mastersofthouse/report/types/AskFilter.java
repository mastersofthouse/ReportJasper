
package br.app.mastersofthouse.report.types;

public enum AskFilter {

    /**
     * all (user and system definded) prarms
     */
    a,
    /**
     * all empty params
     */
    ae,
    /**
     * user params
     */
    u,
    /**
     * empty user params
     */
    ue,
    /**
     * user params marked for prompting
     */
    p,
    /**
     * empty user params markted for prompting
     */
    pe;
}
