#ifndef __MATCHER__
#define __MATCHER__

#include <stddef.h>
#include <regex.h>
#include <string>
#include <vector>

#include "java/lang/String.h"
#include "vsdk/toolkit/common/memoryManagement/SharedPointer.h"


/**
Compiled POSIX expression shared by a `Pattern` and its matchers, with the
POSIX group that corresponds to each Java capturing group.
*/
class CompiledExpression {
  private:
    CompiledExpression(const CompiledExpression &other);
    CompiledExpression &operator=(const CompiledExpression &other);

  public:
    regex_t expression;
    /// Index 0 is the whole match, then one entry per Java group
    std::vector<int> posixGroupOfJavaGroup;

    /**
    @param javaRegex expression in Java syntax
    @throws std::invalid_argument if it is not valid
    */
    explicit CompiledExpression(const char *javaRegex);
    ~CompiledExpression();
};

/**
Emulation of `java.util.regex.Matcher`: finds the successive matches of a
`Pattern` over a text.
*/
class Matcher {
  private:
    SharedPointer<CompiledExpression> expression;
    std::string input;
    std::vector<regmatch_t> match;
    size_t searchFrom;
    bool matched;

  public:
    Matcher(const SharedPointer<CompiledExpression> &expression,
            const String &input);

    /**
    Finds the next match, after the previous one.
    @return true if one was found
    */
    bool
    find();

    /**
    @return true if the whole text matches
    */
    bool
    matches();

    /**
    @param group group number, 0 for the whole match
    @return the text of the group in the last match, empty if it did not
    take part in it (Java returns null)
    @throws std::logic_error if there is no match (Java throws
    IllegalStateException)
    */
    String
    group(int group) const;

    /**
    @return the text of the last match
    */
    String
    group() const;

    /**
    @return the number of capturing groups of the pattern
    */
    int
    groupCount() const;

    /**
    @return the index of the start of the last match
    */
    int
    start() const;

    /**
    @return the index after the end of the last match
    */
    int
    end() const;
};


#endif
