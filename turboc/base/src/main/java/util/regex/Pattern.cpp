#include <stdexcept>

#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"
#include "vsdk/toolkit/common/Cpp98Support.h"



/**
Appends the POSIX bracket expression contents for a Java class escape
(`\\s`, `\\d`, `\\w`), or returns false if `escape` is not one of them.
*/
static bool
appendClassContents(char escape, std::string &out)
{
    switch ( escape ) {
      case 's':
        out += "[:space:]";
        return true;
      case 'd':
        out += "0-9";
        return true;
      case 'w':
        out += "[:alnum:]_";
        return true;
      default:
        return false;
    }
}

/**
Translates a Java expression to POSIX extended syntax.
@param javaRegex expression in Java syntax
@param out POSIX expression
@param posixGroupOfJavaGroup receives the POSIX group of each Java
capturing group (index 0 is the whole match)
*/
static void
translate(const char *javaRegex, std::string &out,
          std::vector<int> &posixGroupOfJavaGroup)
{
    int posixGroups = 0;
    posixGroupOfJavaGroup.clear();
    posixGroupOfJavaGroup.push_back(0);

    for ( int i = 0; javaRegex[i] != '\0'; i++ ) {
        char c = javaRegex[i];
        if ( c == '\\' && javaRegex[i + 1] != '\0' ) {
            char next = javaRegex[++i];
            std::string contents;
            if ( appendClassContents(next, contents) ) {
                out += "[" + contents + "]";
            }
            else if ( next == 'S' || next == 'D' || next == 'W' ) {
                appendClassContents((char)(next - 'A' + 'a'), contents);
                out += "[^" + contents + "]";
            }
            else if ( next == 'n' ) {
                out += '\n';
            }
            else if ( next == 't' ) {
                out += '\t';
            }
            else if ( next == 'r' ) {
                out += '\r';
            }
            else {
                out += '\\';
                out += next;
            }
        }
        else if ( c == '[' ) {
            // Bracket expression: POSIX takes backslashes literally
            out += '[';
            i++;
            if ( javaRegex[i] == '^' ) {
                out += '^';
                i++;
            }
            bool first = true;
            for ( ; javaRegex[i] != '\0' && (first || javaRegex[i] != ']');
                  i++ ) {
                first = false;
                char member = javaRegex[i];
                if ( member == '\\' && javaRegex[i + 1] != '\0' ) {
                    char next = javaRegex[++i];
                    if ( !appendClassContents(next, out) ) {
                        if ( next == 'n' ) {
                            out += '\n';
                        }
                        else if ( next == 't' ) {
                            out += '\t';
                        }
                        else if ( next == 'r' ) {
                            out += '\r';
                        }
                        else {
                            out += next;
                        }
                    }
                }
                else {
                    out += member;
                }
            }
            out += ']';
            if ( javaRegex[i] == '\0' ) {
                break;
            }
        }
        else if ( c == '(' ) {
            posixGroups++;
            if ( javaRegex[i + 1] == '?' && javaRegex[i + 2] == ':' ) {
                i += 2;
            }
            else {
                posixGroupOfJavaGroup.push_back(posixGroups);
            }
            out += '(';
        }
        else {
            out += c;
        }
    }
}


//= CompiledExpression ====================================================

CompiledExpression::CompiledExpression(const char *javaRegex)
{
    std::string posixRegex;
    translate(javaRegex, posixRegex, posixGroupOfJavaGroup);
    int status = regcomp(&expression, posixRegex.c_str(), REG_EXTENDED);
    if ( status != 0 ) {
        char message[256];
        regerror(status, &expression, message, sizeof(message));
        regfree(&expression);
        throw std::invalid_argument(std::string("Invalid regular expression: ") +
                                    message);
    }
}

CompiledExpression::~CompiledExpression()
{
    regfree(&expression);
}

//= Pattern ===============================================================

Pattern::Pattern(const String &regex) :
    patternText(regex),
    expression(new CompiledExpression(regex.c_str()))
{
}

Pattern
Pattern::compile(const String &regex)
{
    return Pattern(regex);
}

String
Pattern::quote(const String &text)
{
    static const char *special = "\\^$.|?*+()[]{}";
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
    return String(quoted.c_str());
}

Matcher
Pattern::matcher(const String &input) const
{
    return Matcher(expression, input);
}

const String &
Pattern::pattern() const
{
    return patternText;
}

//= Matcher ===============================================================

Matcher::Matcher(const SharedPointer<CompiledExpression> &expression,
                 const String &input) :
    expression(expression), input(input.c_str()),
    match(expression->expression.re_nsub + 1), searchFrom(0),
    matched(false)
{
}

bool
Matcher::find()
{
    if ( searchFrom > input.size() ) {
        matched = false;
        return false;
    }
    int flags = searchFrom > 0 ? REG_NOTBOL : 0;
    matched = regexec(&expression->expression, input.c_str() + searchFrom,
                      match.size(), &match[0], flags) == 0;
    if ( !matched ) {
        searchFrom = input.size() + 1;
        return false;
    }
    // Offsets are relative to the start of the search
    for ( size_t i = 0; i < match.size(); i++ ) {
        if ( match[i].rm_so >= 0 ) {
            match[i].rm_so += (regoff_t)searchFrom;
            match[i].rm_eo += (regoff_t)searchFrom;
        }
    }
    size_t matchStart = (size_t)match[0].rm_so;
    size_t matchEnd = (size_t)match[0].rm_eo;
    // An empty match advances one character, as Java does
    searchFrom = matchEnd > matchStart ? matchEnd : matchEnd + 1;
    return true;
}

bool
Matcher::matches()
{
    searchFrom = input.size() + 1;
    matched = regexec(&expression->expression, input.c_str(), match.size(),
                      &match[0], 0) == 0 &&
        match[0].rm_so == 0 && (size_t)match[0].rm_eo == input.size();
    return matched;
}

String
Matcher::group(int group) const
{
    if ( !matched ) {
        throw std::logic_error("No match found");
    }
    if ( group < 0 || group > groupCount() ) {
        throw std::out_of_range("No group " + Cpp98Support::toString(group));
    }
    const regmatch_t &part = match[expression->posixGroupOfJavaGroup[group]];
    if ( part.rm_so < 0 ) {
        return String("");
    }
    return String(input.substr((size_t)part.rm_so,
        (size_t)(part.rm_eo - part.rm_so)).c_str());
}

String
Matcher::group() const
{
    return group(0);
}

int
Matcher::groupCount() const
{
    return ((int)(expression->posixGroupOfJavaGroup.size())) - 1;
}

int
Matcher::start() const
{
    if ( !matched ) {
        throw std::logic_error("No match found");
    }
    return (int)match[0].rm_so;
}

int
Matcher::end() const
{
    if ( !matched ) {
        throw std::logic_error("No match found");
    }
    return (int)match[0].rm_eo;
}

