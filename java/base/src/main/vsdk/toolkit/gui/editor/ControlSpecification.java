package vsdk.toolkit.gui.editor;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

/**
Parsed form of one control specification string, as stored in
`Entity.getControlSpecifications()`. The string format is
"type;name;valid interval", for example "double;radius;(0, INFINITE)".

The name implies a pair of accessors on the entity: for "radius", the methods
`getRadius()` and `setRadius(value)`. In the interval, "[" and "]" denote
inclusive limits, while "(" and ")" denote exclusive limits. The words
"INFINITE" and "-INFINITE" denote unbounded limits. The interval part may be
omitted, meaning that any value is valid.
*/
public class ControlSpecification
{
    public static final String INFINITE = "INFINITE";

    private final String type;
    private final String name;
    private final String intervalText;
    private final double lowerBound;
    private final double upperBound;
    private final boolean lowerInclusive;
    private final boolean upperInclusive;

    private ControlSpecification(String type, String name,
        String intervalText, double lowerBound, double upperBound,
        boolean lowerInclusive, boolean upperInclusive)
    {
        this.type = type;
        this.name = name;
        this.intervalText = intervalText;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
    }

    /**
    Parses a control specification string.
    @param specification string in "type;name;valid interval" format
    @return the parsed specification, or null if the string is malformed
    */
    public static ControlSpecification parse(String specification)
    {
        if ( specification == null ) {
            reportMalformed(specification, "null specification");
            return null;
        }

        String[] parts = specification.split(";");
        if ( parts.length < 2 || parts.length > 3 ) {
            reportMalformed(specification,
                "expected \"type;name\" or \"type;name;interval\"");
            return null;
        }

        String type = parts[0].trim();
        String name = parts[1].trim();
        if ( type.isEmpty() || name.isEmpty() ) {
            reportMalformed(specification, "empty type or name");
            return null;
        }

        if ( parts.length == 2 || parts[2].trim().isEmpty() ) {
            return new ControlSpecification(type, name, "",
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                false, false);
        }

        String interval = parts[2].trim();
        char open = interval.charAt(0);
        char close = interval.charAt(interval.length() - 1);
        if ( (open != '[' && open != '(') || (close != ']' && close != ')') ) {
            reportMalformed(specification,
                "interval must start with '[' or '(' and end with ']' or ')'");
            return null;
        }

        String[] limits = interval.substring(1, interval.length() - 1)
            .split(",");
        if ( limits.length != 2 ) {
            reportMalformed(specification,
                "interval must have two limits separated by ','");
            return null;
        }

        double lower;
        double upper;
        try {
            lower = parseLimit(limits[0].trim());
            upper = parseLimit(limits[1].trim());
        }
        catch ( NumberFormatException e ) {
            reportMalformed(specification, "invalid interval limit");
            return null;
        }
        if ( lower > upper ) {
            reportMalformed(specification,
                "lower limit is greater than upper limit");
            return null;
        }

        return new ControlSpecification(type, name, interval, lower, upper,
            open == '[', close == ']');
    }

    private static double parseLimit(String limit)
    {
        if ( limit.equals(INFINITE) || limit.equals("+" + INFINITE) ) {
            return Double.POSITIVE_INFINITY;
        }
        if ( limit.equals("-" + INFINITE) ) {
            return Double.NEGATIVE_INFINITY;
        }
        return Double.parseDouble(limit);
    }

    private static void reportMalformed(String specification, String reason)
    {
        Logger.reportMessage(null, VSDK.WARNING, "ControlSpecification.parse",
            "Ignoring malformed control specification \"" + specification +
            "\": " + reason + ". Expected format is \"type;name;interval\", " +
            "for example \"double;radius;(0, INFINITE)\".");
    }

    /**
    @return the type name, for example "double"
    */
    public String getType()
    {
        return type;
    }

    /**
    @return the attribute name, for example "radius"
    */
    public String getName()
    {
        return name;
    }

    /**
    @return the attribute name with its first letter in upper case, as used
    in accessor names and GUI labels, for example "Radius"
    */
    public String getLabel()
    {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
    @return the interval as written in the specification, or an empty string
    when the value is unbounded
    */
    public String getIntervalText()
    {
        return intervalText;
    }

    /**
    @param value value to test
    @return true if the value lies inside the valid interval
    */
    public boolean contains(double value)
    {
        if ( Double.isNaN(value) ) {
            return false;
        }
        boolean aboveLower = lowerInclusive ? value >= lowerBound :
            value > lowerBound;
        boolean belowUpper = upperInclusive ? value <= upperBound :
            value < upperBound;
        return aboveLower && belowUpper;
    }

}
