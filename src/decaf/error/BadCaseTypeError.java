package decaf.error;

import decaf.Location;

/**
 * example：incompatible case: int constant is expected<br>
 * PA2
 */
public class BadCaseTypeError extends DecafError {

    public BadCaseTypeError(Location location) {
        super(location);
    }

    @Override
    protected String getErrMsg() {
        return "incompatible case: int constant is expected";
    }

}
