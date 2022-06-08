package br.app.mastersofthouse.report.tools.printing;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.PrintStream;

public class Printerlookup {
    private static PrintStream configSink = System.err;
    private static PrintStream debugSink = System.err;

    public static PrintService getPrintservice(String printername) {
        return getPrintservice(printername, false, false);
    }

    public static PrintService getPrintservice(String printername, Boolean startWithMatch) {
        return getPrintservice(printername, startWithMatch, false);
    }

    public static PrintService getPrintservice(String printername, Boolean startWithMatch, Boolean escapeSpace) {
        PrintService returnval = null;
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            if (service.getName().equals(printername)) {
                returnval = service;
            }
        }
        if (returnval == null) {
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(printername)) {
                    if (returnval == null) {
                        returnval = service;
                    } else {
                    }
                }
            }
        }
        if (returnval == null && startWithMatch) {
            for (PrintService service : services) {
                if (service.getName().toLowerCase().startsWith(printername.toLowerCase())) {
                    if (returnval == null) {
                        returnval = service;
                    } else {
                        //throw new Exception("Printername \"" + printername + "\" is ambiguous!");
                    }
                }
            }
        }
        if (returnval == null && escapeSpace) {
            for (PrintService service : services) {
                String excapedPrintername = printername.toLowerCase().replace(" ", "_");
                String escapedServiceName = service.getName().toLowerCase().replace(" ", "_");
                if (escapedServiceName.equals(excapedPrintername)) {
                    if (returnval == null) {
                        returnval = service;
                    } else {
                        //throw new Exception("Printername \"" + printername + "\" is ambiguous!");
                    }
                }
            }
        }
        if (returnval == null && startWithMatch && escapeSpace) {

            for (PrintService service : services) {
                String excapedPrintername = printername.toLowerCase().replace(" ", "_");
                String escapedServiceName = service.getName().toLowerCase().replace(" ", "_");
                if (escapedServiceName.startsWith(excapedPrintername)) {
                    if (returnval == null) {
                        returnval = service;
                    } else {
                        //throw new Exception("Printername \"" + printername + "\" is ambiguous!");
                    }
                }
            }
        }
        return returnval;
    }
}
