#ifndef __MATCHER__
#define __MATCHER__

#include <regex>
#include <string>

#include "java/lang/String.h"

namespace java {
namespace util {
namespace regex {

/**
Emulation of `java.util.regex.Matcher`: finds the successive matches of a
`Pattern` over a text.
*/
class Matcher {
  private:
    std::regex expression;
    std::string input;
    std::smatch match;
    std::size_t searchFrom;
    bool matched;

  public:
    Matcher(const std::regex &expression, const java::String &input);
    Matcher(const Matcher &other);
    Matcher &operator=(const Matcher &other);

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
    java::String
    group(int group) const;

    /**
    @return the text of the last match
    */
    java::String
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

}
}
}

#endif
