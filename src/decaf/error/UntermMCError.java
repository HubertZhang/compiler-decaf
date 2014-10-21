package decaf.error;

import decaf.Location;

/**
 * example：unterminated multi comment
 * PA1
 */
public class UntermMCError extends DecafError {


    public UntermMCError(Location location) {
        super(location);
    }

    @Override
    protected String getErrMsg() {
        return "unterminated multi comment";
    }

}
