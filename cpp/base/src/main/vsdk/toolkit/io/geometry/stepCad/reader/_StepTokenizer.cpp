#include <cstring>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepTokenizer.h"

#include "java/lang/Double.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Integer.h"
#include "java/lang/NumberFormatException.h"

const char* const _StepEntity::COMPLEX_NAME = "**COMPLEX**";

void _StepTokenizer::parse(java::InputStream* in, _StepEntityMap& outEntities)
{
    // Java: `byte[] bytes = in.readAllBytes()`
    java::ArrayList<char> bytes;
    unsigned char buffer[65536];
    int n;
    while ( (n = in->read(buffer, 0, (int)sizeof(buffer))) > 0 ) {
        int i;
        for ( i = 0; i < n; i++ ) {
            bytes.add((char)buffer[i]);
        }
    }
    bytes.add('\0');
    java::String text(bytes.data());

    int dataStart = findSectionStart(text, "DATA;");
    int dataEnd = findSectionEnd(text, dataStart);

    java::String dataSection = text.substring(dataStart, dataEnd);
    parseDataSection(dataSection, outEntities);
}

//=================================================================

namespace {

/**
C++ port note: `java::String::length` and `charAt` measure the text with
`strlen` on each call, so the scanners below read the characters through
`c_str()` and cut the ranges with this helper (Java: `substring`), keeping
the 1.8 MB DATA sections of real STEP files linear.
*/
java::String substringOf(const char* chars, int beginIndex, int endIndex)
{
    int n = endIndex - beginIndex;
    char* buffer = new char[n + 1];
    std::memcpy(buffer, chars + beginIndex, n);
    buffer[n] = '\0';
    java::String out(buffer);
    delete[] buffer;
    return out;
}

}

int _StepTokenizer::findSectionStart(const java::String& text,
                                     const java::String& marker)
{
    int idx = text.indexOf(marker);
    if ( idx < 0 ) {
        throw java::IllegalArgumentException(
            java::String("STEP file is missing '") + marker + "' marker.");
    }
    return idx + marker.length();
}

int _StepTokenizer::findSectionEnd(const java::String& text, int searchFrom)
{
    int idx = text.indexOf(java::String("ENDSEC;"), searchFrom);
    if ( idx < 0 ) {
        throw java::IllegalArgumentException(
            "STEP file DATA section is missing closing 'ENDSEC;'.");
    }
    return idx;
}

void _StepTokenizer::parseDataSection(const java::String& section,
                                      _StepEntityMap& entities)
{
    java::ArrayList<java::String> rawEntities = splitEntities(section);
    long int i;
    for ( i = 0; i < rawEntities.size(); i++ ) {
        java::String trimmed = rawEntities.get(i).trim();
        if ( trimmed.isEmpty() ) {
            continue;
        }
        _StepEntity* entity = parseEntityString(trimmed);
        if ( entity != nullptr ) {
            entities.put(entity->id, entity);
        }
    }
}

/**
Splits the DATA section text into individual entity strings by
scanning for the `;` terminator at depth 0 (not inside string
literals or aggregate parentheses).
*/
java::ArrayList<java::String> _StepTokenizer::splitEntities(
    const java::String& text)
{
    java::ArrayList<java::String> entities;
    const char* chars = text.c_str();
    int len = text.length();
    int depth = 0;
    bool inString = false;
    int entityStart = 0;

    int i;
    for ( i = 0; i < len; i++ ) {
        char c = chars[i];
        if ( inString ) {
            if ( c == '\'' ) {
                if ( i + 1 < len && chars[i + 1] == '\'' ) {
                    i++;
                }
                else {
                    inString = false;
                }
            }
        }
        else if ( c == '\'' ) {
            inString = true;
        }
        else if ( c == '(' ) {
            depth++;
        }
        else if ( c == ')' ) {
            depth--;
        }
        else if ( c == ';' && depth == 0 ) {
            entities.add(substringOf(chars, entityStart, i));
            entityStart = i + 1;
        }
    }
    return entities;
}

/**
Parses a single entity string of the form `#id=NAME(params)` or
`#id=( NAME(...) NAME(...) )` into a `_StepEntity`.
Returns null for lines that do not start with `#`.
*/
_StepEntity* _StepTokenizer::parseEntityString(const java::String& raw)
{
    if ( raw.isEmpty() || raw.charAt(0) != '#' ) {
        return nullptr;
    }

    int eqPos = raw.indexOf('=');
    if ( eqPos < 0 ) {
        return nullptr;
    }

    int id;
    try {
        id = java::Integer::parseInt(raw.substring(1, eqPos).trim());
    }
    catch ( const java::NumberFormatException& ) {
        return nullptr;
    }

    java::String body = raw.substring(eqPos + 1).trim();

    if ( body.startsWith("(") ) {
        java::ArrayList<java::String> single;
        single.add(body);
        return new _StepEntity(id, _StepEntity::COMPLEX_NAME, single);
    }

    int parenOpen = body.indexOf('(');
    if ( parenOpen < 0 ) {
        return nullptr;
    }

    java::String name = body.substring(0, parenOpen).trim().toUpperCase();
    int parenClose = lastTopLevelClose(body, parenOpen);
    if ( parenClose < 0 ) {
        return nullptr;
    }
    java::String paramsText = body.substring(parenOpen + 1, parenClose);
    java::ArrayList<java::String> params = splitTopLevelParams(paramsText);

    return new _StepEntity(id, name, params);
}

/**
Finds the matching top-level closing parenthesis for the opening
parenthesis at `openPos` in `text`.
*/
int _StepTokenizer::lastTopLevelClose(const java::String& text, int openPos)
{
    int depth = 0;
    bool inString = false;
    const char* chars = text.c_str();
    int len = text.length();
    int i;
    for ( i = openPos; i < len; i++ ) {
        char c = chars[i];
        if ( inString ) {
            if ( c == '\'' ) {
                if ( i + 1 < len && chars[i + 1] == '\'' ) {
                    i++;
                }
                else {
                    inString = false;
                }
            }
        }
        else if ( c == '\'' ) {
            inString = true;
        }
        else if ( c == '(' ) {
            depth++;
        }
        else if ( c == ')' ) {
            depth--;
            if ( depth == 0 ) {
                return i;
            }
        }
    }
    return -1;
}

/**
Splits the parameter text (the content between the outermost
parentheses of an entity) into individual parameter strings at
commas that are at depth 0 and outside string literals.
Leading and trailing whitespace is stripped from each token.
*/
java::ArrayList<java::String> _StepTokenizer::splitTopLevelParams(
    const java::String& text)
{
    java::ArrayList<java::String> params;
    int depth = 0;
    bool inString = false;
    int tokenStart = 0;
    const char* chars = text.c_str();
    int len = text.length();
    int i;

    for ( i = 0; i < len; i++ ) {
        char c = chars[i];
        if ( inString ) {
            if ( c == '\'' ) {
                if ( i + 1 < len && chars[i + 1] == '\'' ) {
                    i++;
                }
                else {
                    inString = false;
                }
            }
        }
        else if ( c == '\'' ) {
            inString = true;
        }
        else if ( c == '(' ) {
            depth++;
        }
        else if ( c == ')' ) {
            depth--;
        }
        else if ( c == ',' && depth == 0 ) {
            params.add(substringOf(chars, tokenStart, i).trim());
            tokenStart = i + 1;
        }
    }
    java::String last = text.substring(tokenStart).trim();
    if ( !last.isEmpty() ) {
        params.add(last);
    }
    return params;
}

int _StepTokenizer::parseRef(const java::String& token)
{
    java::String t = token.trim();
    if ( t.isEmpty() || t.charAt(0) != '#' ) {
        throw java::IllegalArgumentException(
            java::String("Expected entity reference, got: '") + token + "'");
    }
    return java::Integer::parseInt(t.substring(1));
}

double _StepTokenizer::parseDouble(const java::String& token)
{
    return java::Double::parseDouble(token.trim());
}

java::ArrayList<java::String> _StepTokenizer::parseAggregate(
    const java::String& token)
{
    java::String t = token.trim();
    if ( t.isEmpty() || t.charAt(0) != '(' ) {
        throw java::IllegalArgumentException(
            java::String("Expected aggregate token starting with '(', got: '")
            + token + "'");
    }
    int close = lastTopLevelClose(t, 0);
    if ( close < 0 ) {
        throw java::IllegalArgumentException(
            java::String("Unterminated aggregate: '") + token + "'");
    }
    java::String inner = t.substring(1, close);
    return splitTopLevelParams(inner);
}
