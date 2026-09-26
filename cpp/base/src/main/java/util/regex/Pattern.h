#ifndef __PATTERN__
#define __PATTERN__

#include <regex>

#include "java/lang/String.h"
#include "java/util/regex/Matcher.h"

namespace java {
namespace util {
namespace regex {

/**
Emulation of `java.util.regex.Pattern` over `std::regex` (ECMAScript
grammar). The common syntax of both works the same: character classes,
groups, non capturing groups `(?:...)`, quantifiers, `\\s`, `\\d` and
escapes. Java only features (possessive quantifiers, `\\Q...\\E` inside
patterns, lookbehind) are not supported: `quote` escapes each special
character instead.
*/
class Pattern {
  private:
    java::String patternText;
    std::regex expression;

    explicit Pattern(const java::String &regex);

  public:
    /**
    @param regex regular expression
    @throws std::regex_error if the expression is not valid (Java throws
    PatternSyntaxException)
    */
    static Pattern
    compile(const java::String &regex);

    /**
    @return an expression matching the given text literally
    */
    static java::String
    quote(const java::String &text);

    /**
    @param input text to search; the matcher keeps its own copy
    */
    Matcher
    matcher(const java::String &input) const;

    const java::String &
    pattern() const;
};

}
}
}

#endif
