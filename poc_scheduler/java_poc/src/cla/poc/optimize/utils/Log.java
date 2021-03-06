package cla.poc.optimize.utils;

public class Log {
    protected Long startTs = null;
    public static void log(String format, Object... parms) {
        String  msg = String.format(format, parms);
        println(msg);
    }

    private static void println(String msg) {
        System.out.println(msg);
    }
}
