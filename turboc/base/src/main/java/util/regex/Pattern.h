#ifndef __PATTERN__
#define __PATTERN__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/regex/Matcher.h"
#include "vsdk/toolkit/common/memoryManagement/SharedPointer.h"


/**
Emulation of `java.util.regex.Pattern` over POSIX extended regular
expressions. The Java expression is translated
to the POSIX syntax: character classes, groups, non capturing groups
`(?:...)`, alternatives, quantifiers, `\\s`, `\\S`, `\\d`, `\\D`, `\\w`,
`\\W` and escapes are supported. Java only features (lazy and possessive
quantifiers, lookaround, backreferences, `\\Q...\\E` inside patterns) are
not: `quote` escapes each special character instead. As POSIX matching
finds the longest match at the leftmost position, alternatives that are
prefixes of other alternatives may select a longer text than Java.
*/
class Pattern {
  private:
    String patternText;
    SharedPointer<CompiledExpression> expression;

    explicit Pattern(const String &regex);

  public:
    /**
    @param regex regular expression
    @throws IllegalArgumentException if the expression is not valid (Java
    throws PatternSyntaxException)
    */
    static Pattern
    compile(const String &regex);

    /**
    @return an expression matching the given text literally
    */
    static String
    quote(const String &text);

    /**
    @param input text to search; the matcher keeps its own copy
    */
    Matcher
    matcher(const String &input) const;

    const String &
    pattern() const;
};


#endif
