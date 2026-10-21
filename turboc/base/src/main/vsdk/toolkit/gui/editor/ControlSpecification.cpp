#include <stddef.h>
#include <ctype.h>
#include <math.h>
#include <limits>
#include <string>
#include <vector>

#include "java/lang/Double.h"
#include "java/lang/NumberFormatException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/editor/ControlSpecification.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

const char *const ControlSpecification::INFINITE = "INFINITE";


static std::string trim(const std::string &text)
{
    size_t begin = 0;
    size_t end = text.size();
    while ( begin < end && isspace((unsigned char)text[begin]) ) {
        begin++;
    }
    while ( end > begin && isspace((unsigned char)text[end - 1]) ) {
        end--;
    }
    return text.substr(begin, end - begin);
}

/**
Splits as Java `String.split` does with a one character pattern: trailing
empty fields are dropped.
*/
static std::vector<std::string> split(const std::string &text, char separator)
{
    std::vector<std::string> parts;
    size_t begin = 0;
    for ( ;; ) {
        size_t end = text.find(separator, begin);
        if ( end == std::string::npos ) {
            parts.push_back(text.substr(begin));
            break;
        }
        parts.push_back(text.substr(begin, end - begin));
        begin = end + 1;
    }
    while ( !parts.empty() && parts.back().empty() ) {
        parts.pop_back();
    }
    return parts;
}

static double parseLimit(const std::string &limit)
{
    const std::string infinite(ControlSpecification::INFINITE);
    if ( limit == infinite || limit == "+" + infinite ) {
        return HUGE_VAL;
    }
    if ( limit == "-" + infinite ) {
        return -HUGE_VAL;
    }
    return Double::parseDouble(String(limit.c_str()));
}


ControlSpecification::ControlSpecification(const String &type,
    const String &name, const String &intervalText,
    double lowerBound, double upperBound, bool lowerInclusive,
    bool upperInclusive) :
    type(type), name(name), intervalText(intervalText),
    lowerBound(lowerBound), upperBound(upperBound),
    lowerInclusive(lowerInclusive), upperInclusive(upperInclusive)
{
}

ControlSpecification *
ControlSpecification::parse(const String &specification)
{
    std::vector<std::string> parts = split(specification.c_str(), ';');
    if ( parts.size() < 2 || parts.size() > 3 ) {
        reportMalformed(specification,
            "expected \"type;name\" or \"type;name;interval\"");
        return NULL;
    }

    std::string type = trim(parts[0]);
    std::string name = trim(parts[1]);
    if ( type.empty() || name.empty() ) {
        reportMalformed(specification, "empty type or name");
        return NULL;
    }

    if ( parts.size() == 2 || trim(parts[2]).empty() ) {
        return new ControlSpecification(type.c_str(), name.c_str(), "",
            -HUGE_VAL,
            HUGE_VAL, false, false);
    }

    std::string interval = trim(parts[2]);
    char open = interval[0];
    char close = interval[interval.size() - 1];
    if ( (open != '[' && open != '(') || (close != ']' && close != ')') ) {
        reportMalformed(specification,
            "interval must start with '[' or '(' and end with ']' or ')'");
        return NULL;
    }

    std::vector<std::string> limits =
        split(interval.substr(1, interval.size() - 2), ',');
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

    return new ControlSpecification(type.c_str(), name.c_str(),
        interval.c_str(), lower, upper, open == '[', close == ']');
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
    std::string label(name.c_str());
    if ( !label.empty() ) {
        label[0] = (char)toupper((unsigned char)label[0]);
    }
    return String(label.c_str());
}

bool
ControlSpecification::contains(double value) const
{
    if ( Cpp98Support::isNaN(value) ) {
        return false;
    }
    bool aboveLower = lowerInclusive ? value >= lowerBound :
        value > lowerBound;
    bool belowUpper = upperInclusive ? value <= upperBound :
        value < upperBound;
    return aboveLower && belowUpper;
}
