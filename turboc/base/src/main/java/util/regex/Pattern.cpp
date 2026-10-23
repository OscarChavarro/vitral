#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"



/**
Appends the POSIX bracket expression contents for a Java class escape
(`\\s`, `\\d`, `\\w`), or returns false if `escape` is not one of them.
*/
static BOOLEAN
appendClassContents(char escape, StringBuilder &out)
{
    switch ( escape ) {
      case 's':
        out.append("[:space:]");
        return TRUE;
      case 'd':
        out.append("0-9");
        return TRUE;
      case 'w':
        out.append("[:alnum:]_");
        return TRUE;
      default:
        return FALSE;
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
translate(const char *javaRegex, StringBuilder &out,
          ArrayList<int> &posixGroupOfJavaGroup)
{
    int posixGroups = 0;
    posixGroupOfJavaGroup.clear();
    posixGroupOfJavaGroup.add(0);

    for ( int i = 0; javaRegex[i] != '\0'; i++ ) {
        char c = javaRegex[i];
        if ( c == '\\' && javaRegex[i + 1] != '\0' ) {
            char next = javaRegex[++i];
            StringBuilder contents;
            if ( appendClassContents(next, contents) ) {
                out.append("[").append(contents.toString()).append("]");
            }
            else if ( next == 'S' || next == 'D' || next == 'W' ) {
                appendClassContents((char)(next - 'A' + 'a'), contents);
                out.append("[^").append(contents.toString()).append("]");
            }
            else if ( next == 'n' ) {
                out.append('\n');
            }
            else if ( next == 't' ) {
                out.append('\t');
            }
            else if ( next == 'r' ) {
                out.append('\r');
            }
            else {
                out.append('\\');
                out.append(next);
            }
        }
        else if ( c == '[' ) {
            // Bracket expression: POSIX takes backslashes literally
            out.append('[');
            i++;
            if ( javaRegex[i] == '^' ) {
                out.append('^');
                i++;
            }
            BOOLEAN first = TRUE;
            for ( ; javaRegex[i] != '\0' && (first || javaRegex[i] != ']');
                  i++ ) {
                first = FALSE;
                char member = javaRegex[i];
                if ( member == '\\' && javaRegex[i + 1] != '\0' ) {
                    char next = javaRegex[++i];
                    if ( !appendClassContents(next, out) ) {
                        if ( next == 'n' ) {
                            out.append('\n');
                        }
                        else if ( next == 't' ) {
                            out.append('\t');
                        }
                        else if ( next == 'r' ) {
                            out.append('\r');
                        }
                        else {
                            out.append(next);
                        }
                    }
                }
                else {
                    out.append(member);
                }
            }
            out.append(']');
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
                posixGroupOfJavaGroup.add(posixGroups);
            }
            out.append('(');
        }
        else {
            out.append(c);
        }
    }
}


//= CompiledExpression ====================================================

CompiledExpression::CompiledExpression(const char *javaRegex)
{
    StringBuilder posixRegex;
    translate(javaRegex, posixRegex, posixGroupOfJavaGroup);
    int status = regcomp(&expression, posixRegex.toString().c_str(),
                         REG_EXTENDED);
    if ( status != 0 ) {
        char message[256];
        regerror(status, &expression, message, sizeof(message));
        regfree(&expression);
        throw IllegalArgumentException(
            String("Invalid regular expression: ") + message);
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
    StringBuilder quoted;
    const char *source = text.c_str();

    for ( int i = 0; source[i] != '\0'; i++ ) {
        for ( const char *s = special; *s != '\0'; s++ ) {
            if ( *s == source[i] ) {
                quoted.append('\\');
                break;
            }
        }
        quoted.append(source[i]);
    }
    return quoted.toString();
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
    expression(expression), input(input), match(), searchFrom(0),
    matched(FALSE)
{
    regmatch_t empty;
    empty.rm_so = -1;
    empty.rm_eo = -1;
    for ( size_t i = 0; i < expression->expression.re_nsub + 1; i++ ) {
        match.add(empty);
    }
}

BOOLEAN
Matcher::find()
{
    if ( searchFrom > (size_t)input.size() ) {
        matched = FALSE;
        return FALSE;
    }
    int flags = searchFrom > 0 ? REG_NOTBOL : 0;
    matched = regexec(&expression->expression, input.c_str() + searchFrom,
                      (size_t)match.size(), match.data(), flags) == 0;
    if ( !matched ) {
        searchFrom = (size_t)input.size() + 1;
        return FALSE;
    }
    // Offsets are relative to the start of the search
    for ( long i = 0; i < match.size(); i++ ) {
        if ( match[i].rm_so >= 0 ) {
            match[i].rm_so += (regoff_t)searchFrom;
            match[i].rm_eo += (regoff_t)searchFrom;
        }
    }
    size_t matchStart = (size_t)match[0].rm_so;
    size_t matchEnd = (size_t)match[0].rm_eo;
    // An empty match advances one character, as Java does
    searchFrom = matchEnd > matchStart ? matchEnd : matchEnd + 1;
    return TRUE;
}

BOOLEAN
Matcher::matches()
{
    searchFrom = (size_t)input.size() + 1;
    matched = regexec(&expression->expression, input.c_str(), match.size(),
                      match.data(), 0) == 0 &&
        match[0].rm_so == 0 && match[0].rm_eo == (regoff_t)input.size();
    return matched;
}

String
Matcher::group(int group) const
{
    if ( !matched ) {
        throw IllegalStateException("No match found");
    }
    if ( group < 0 || group > groupCount() ) {
        throw IndexOutOfBoundsException(String("No group ") + String::valueOf(group));
    }
    const regmatch_t &part = match[expression->posixGroupOfJavaGroup[group]];
    if ( part.rm_so < 0 ) {
        return String("");
    }
    return input.substring((int)part.rm_so, (int)part.rm_eo);
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
        throw IllegalStateException("No match found");
    }
    return (int)match[0].rm_so;
}

int
Matcher::end() const
{
    if ( !matched ) {
        throw IllegalStateException("No match found");
    }
    return (int)match[0].rm_eo;
}

