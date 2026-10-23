#include <stddef.h>
#include <ctype.h>
#include <math.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/NumberFormatException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/editor/ControlSpecification.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "java/util/ArrayList.txx"

const char *const ControlSpecification::INFINITE = "INFINITE";


static String trim(const String &text)
{
    int begin = 0;
    int end = text.size();
    while ( begin < end && isspace((unsigned char)text[begin]) ) {
        begin++;
    }
    while ( end > begin && isspace((unsigned char)text[end - 1]) ) {
        end--;
    }
    return text.substring(begin, end);
}

/**
Splits as Java `String.split` does with a one character pattern: trailing
empty fields are dropped.
*/
static ArrayList<String> split(const String &text, char separator)
{
    ArrayList<String> parts;
    int begin = 0;
    for ( ;; ) {
        int end = text.indexOf(separator, begin);
        if ( end < 0 ) {
            parts.add(text.substring(begin));
            break;
        }
        parts.add(text.substring(begin, end));
        begin = end + 1;
    }
    while ( !parts.isEmpty() && parts[parts.size() - 1].isEmpty() ) {
        parts.remove(parts.size() - 1);
    }
    return parts;
}

static double parseLimit(const String &limit)
{
    const String infinite(ControlSpecification::INFINITE);
    if ( limit == infinite || limit == "+" + infinite ) {
        return HUGE_VAL;
    }
    if ( limit == "-" + infinite ) {
        return -HUGE_VAL;
    }
    return Double::parseDouble(limit);
}


ControlSpecification::ControlSpecification(const String &type,
    const String &name, const String &intervalText,
    double lowerBound, double upperBound, BOOLEAN lowerInclusive,
    BOOLEAN upperInclusive) :
    type(type), name(name), intervalText(intervalText),
    lowerBound(lowerBound), upperBound(upperBound),
    lowerInclusive(lowerInclusive), upperInclusive(upperInclusive)
{
}

ControlSpecification *
ControlSpecification::parse(const String &specification)
{
    ArrayList<String> parts = split(specification, ';');
    if ( parts.size() < 2 || parts.size() > 3 ) {
        reportMalformed(specification,
            "expected \"type;name\" or \"type;name;interval\"");
        return NULL;
    }

    String type = trim(parts[0]);
    String name = trim(parts[1]);
    if ( type.isEmpty() || name.isEmpty() ) {
        reportMalformed(specification, "empty type or name");
        return NULL;
    }

    if ( parts.size() == 2 || trim(parts[2]).isEmpty() ) {
        return new ControlSpecification(type, name, "",
            -HUGE_VAL,
            HUGE_VAL, FALSE, FALSE);
    }

    String interval = trim(parts[2]);
    char open = interval[0];
    char close = interval[interval.size() - 1];
    if ( (open != '[' && open != '(') || (close != ']' && close != ')') ) {
        reportMalformed(specification,
            "interval must start with '[' or '(' and end with ']' or ')'");
        return NULL;
    }

    ArrayList<String> limits =
        split(interval.substring(1, interval.size() - 1), ',');
    if ( limits.size() != 2 ) {
        reportMalformed(specification,
            "interval must have two limits separated by ','");
        return NULL;
    }

    double lower;
    double upper;
    try {
        lower = parseLimit(trim(limits[0]));
        upper = parseLimit(trim(limits[1]));
    }
    catch ( const NumberFormatException & ) {
        reportMalformed(specification, "invalid interval limit");
        return NULL;
    }
    if ( lower > upper ) {
        reportMalformed(specification,
            "lower limit is greater than upper limit");
        return NULL;
    }

    return new ControlSpecification(type, name,
        interval, lower, upper, open == '[', close == ']');
}

void
ControlSpecification::reportMalformed(const String &specification,
                                      const char *reason)
{
    Logger::reportMessage("ControlSpecification", Logger::WARNING,
        "ControlSpecification.parse",
        String("Ignoring malformed control specification \"") +
        specification + "\": " + reason + ". Expected format is " +
        "\"type;name;interval\", for example " +
        "\"double;radius;(0, INFINITE)\".");
}

String
ControlSpecification::getLabel() const
{
    String label(name);
    if ( !label.isEmpty() ) {
        label[0] = (char)toupper((unsigned char)label[0]);
    }
    return label;
}

BOOLEAN
ControlSpecification::contains(double value) const
{
    if ( Cpp98Support::isNaN(value) ) {
        return FALSE;
    }
    BOOLEAN aboveLower = lowerInclusive ? value >= lowerBound :
        value > lowerBound;
    BOOLEAN belowUpper = upperInclusive ? value <= upperBound :
        value < upperBound;
    return aboveLower && belowUpper;
}
