package decaf.error;

import decaf.Location;

/**
 * example：lvalue required as ++ operand<br>
 * PA2
 */
public class BadLValueError extends DecafError {
    String op;

    public BadLValueError(Location location, String op) {
        super(location);
        this.op = op;
    }

    @Override
    protected String getErrMsg() {
        return "lvalue required as "+op+" operand";
    }
}
