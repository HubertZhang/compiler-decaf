package decaf.error;

import decaf.Location;

/**
 * example：operands to ?:  have different types int and bool
 * PA2
 */
public class IncompatTreOpError extends DecafError {

    private String trueTypeString;

    private String falseTypeString;

    public IncompatTreOpError(Location location, String trueTypeString, String falseTypeString) {
        super(location);
        this.trueTypeString = trueTypeString;
        this.falseTypeString = falseTypeString;
    }

    @Override
    protected String getErrMsg() {
        return "operands to ?:  have different types " + this.trueTypeString + " and " + falseTypeString;
    }

}
