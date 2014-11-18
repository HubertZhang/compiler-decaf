package decaf.error;

import decaf.Location;

/**
 * example：incompatible switch: string given, int expected<br>
 * PA2
 */
public class BadSwitchTypeError extends DecafError {

    String switchType;

    public BadSwitchTypeError(Location location, String switchType) {
        super(location);
        this.switchType = switchType;
    }

    @Override
    protected String getErrMsg() {
        return "incompatible switch: "+switchType+" given, int expected";
    }

}
