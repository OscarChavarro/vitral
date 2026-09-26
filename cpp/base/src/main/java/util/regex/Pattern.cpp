#include <stdexcept>

#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"

namespace java {
namespace util {
namespace regex {

//= Pattern ===============================================================

Pattern::Pattern(const java::String &regex) :
    patternText(regex), expression(regex.c_str(), std::regex::ECMAScript)
{
}

Pattern
Pattern::compile(const java::String &regex)
{
    return Pattern(regex);
}

java::String
Pattern::quote(const java::String &text)
{
    static const char *special = "\\^$.|?*+()[]{}/-";
    std::string quoted;
    const char *source = text.c_str();

    for ( int i = 0; source[i] != '\0'; i++ ) {
        for ( const char *s = special; *s != '\0'; s++ ) {
            if ( *s == source[i] ) {
                quoted += '\\';
                break;
            }
        }
        quoted += source[i];
    }
    return java::String(quoted.c_str());
}

Matcher
Pattern::matcher(const java::String &input) const
{
    return Matcher(expression, input);
}

const java::String &
Pattern::pattern() const
{
    return patternText;
}

//= Matcher ===============================================================

Matcher::Matcher(const std::regex &expression, const java::String &input) :
    expression(expression), input(input.c_str()), searchFrom(0),
    matched(false)
{
}

// The results of the match refer to the text of the matcher: copies search
// their own text again
Matcher::Matcher(const Matcher &other) :
    expression(other.expression), input(other.input), searchFrom(0),
    matched(false)
{
}

Matcher &
Matcher::operator=(const Matcher &other)
{
    if ( this != &other ) {
        expression = other.expression;
        input = other.input;
        searchFrom = 0;
        matched = false;
        match = std::smatch();
    }
    return *this;
}

bool
Matcher::find()
{
    if ( searchFrom > input.size() ) {
        matched = false;
        return false;
    }
    std::string::const_iterator begin = input.begin() + searchFrom;
    std::regex_constants::match_flag_type flags =
        searchFrom > 0 ? std::regex_constants::match_prev_avail :
        std::regex_constants::match_default;

    matched = std::regex_search(begin, input.cend(), match, expression, flags);
    if ( !matched ) {
        searchFrom = input.size() + 1;
        return false;
    }
    std::size_t matchStart = searchFrom + match.position(0);
    std::size_t matchEnd = matchStart + match.length(0);
    // An empty match advances one character, as Java does
    searchFrom = matchEnd > matchStart ? matchEnd : matchEnd + 1;
    return true;
}

bool
Matcher::matches()
{
    searchFrom = input.size() + 1;
    matched = std::regex_match(input.cbegin(), input.cend(), match,
                               expression);
    return matched;
}

java::String
Matcher::group(int group) const
{
    if ( !matched ) {
        throw std::logic_error("No match found");
    }
    if ( group < 0 || group >= static_cast<int>(match.size()) ) {
        throw std::out_of_range("No group " + std::to_string(group));
    }
    return java::String(match.str(group).c_str());
}

java::String
Matcher::group() const
{
    return group(0);
}

int
Matcher::groupCount() const
{
    return static_cast<int>(expression.mark_count());
}

int
Matcher::start() const
{
    if ( !matched ) {
        throw std::logic_error("No match found");
    }
    return static_cast<int>(match[0].first - input.cbegin());
}

int
Matcher::end() const
{
    return start() + static_cast<int>(match.length(0));
}

}
}
}
