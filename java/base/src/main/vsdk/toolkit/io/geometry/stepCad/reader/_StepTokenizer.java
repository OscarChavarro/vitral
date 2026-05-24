package vsdk.toolkit.io.geometry.stepCad.reader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
Parses an ISO 10303-21 ASCII STEP file into a flat map of entity
instances keyed by their integer id.

The tokenizer:
  1. Reads the full stream into a UTF-8 string.
  2. Locates the DATA / ENDSEC delimiters.
  3. Splits the DATA section into individual entity strings by scanning
     for the `;` terminator while tracking string-literal and
     parenthesis depth, so embedded semicolons inside strings or
     aggregates never cause false splits.
  4. For each entity string, extracts the `#id`, the entity type name,
     and a list of top-level parameter tokens (split at commas at
     depth 0, outside string literals).

Compound (ANDOR) entities encoded as `#id=( ... )` are stored with
the name `_StepEntity.COMPLEX_NAME` and a single param containing the
raw body text; the solid builder ignores them.

This is an internal collaborator of `StepReader`.
*/
public class _StepTokenizer {

    private _StepTokenizer()
    {
    }

    /**
    Parses the STEP file and returns all entity instances found in
    the DATA section, keyed by entity id.
    @param in input stream for the STEP file; not closed by this method.
    @return map from entity id to parsed entity.
    @throws Exception on I/O errors or malformed DATA section.
    */
    public static Map<Integer, _StepEntity> parse(InputStream in)
        throws Exception
    {
        byte[] bytes = in.readAllBytes();
        String text = new String(bytes, StandardCharsets.UTF_8);

        int dataStart = findSectionStart(text, "DATA;");
        int dataEnd = findSectionEnd(text, dataStart);

        String dataSection = text.substring(dataStart, dataEnd);
        return parseDataSection(dataSection);
    }

    //=================================================================

    private static int findSectionStart(String text, String marker)
    {
        int idx = text.indexOf(marker);
        if ( idx < 0 ) {
            throw new IllegalArgumentException(
                "STEP file is missing '" + marker + "' marker.");
        }
        return idx + marker.length();
    }

    private static int findSectionEnd(String text, int searchFrom)
    {
        int idx = text.indexOf("ENDSEC;", searchFrom);
        if ( idx < 0 ) {
            throw new IllegalArgumentException(
                "STEP file DATA section is missing closing 'ENDSEC;'.");
        }
        return idx;
    }

    private static Map<Integer, _StepEntity> parseDataSection(String section)
    {
        Map<Integer, _StepEntity> entities = new HashMap<>();
        List<String> rawEntities = splitEntities(section);
        for ( String raw : rawEntities ) {
            String trimmed = raw.strip();
            if ( trimmed.isEmpty() ) {
                continue;
            }
            _StepEntity entity = parseEntityString(trimmed);
            if ( entity != null ) {
                entities.put(entity.id, entity);
            }
        }
        return entities;
    }

    /**
    Splits the DATA section text into individual entity strings by
    scanning for the `;` terminator at depth 0 (not inside string
    literals or aggregate parentheses).
    */
    private static List<String> splitEntities(String text)
    {
        List<String> entities = new ArrayList<>();
        int len = text.length();
        int depth = 0;
        boolean inString = false;
        int entityStart = 0;

        int i;
        for ( i = 0; i < len; i++ ) {
            char c = text.charAt(i);
            if ( inString ) {
                if ( c == '\'' ) {
                    if ( i + 1 < len && text.charAt(i + 1) == '\'' ) {
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
                entities.add(text.substring(entityStart, i));
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
    private static _StepEntity parseEntityString(String raw)
    {
        if ( raw.isEmpty() || raw.charAt(0) != '#' ) {
            return null;
        }

        int eqPos = raw.indexOf('=');
        if ( eqPos < 0 ) {
            return null;
        }

        int id;
        try {
            id = Integer.parseInt(raw.substring(1, eqPos).strip());
        }
        catch ( NumberFormatException ignored ) {
            return null;
        }

        String body = raw.substring(eqPos + 1).strip();

        if ( body.startsWith("(") ) {
            List<String> single = new ArrayList<>();
            single.add(body);
            return new _StepEntity(id, _StepEntity.COMPLEX_NAME, single);
        }

        int parenOpen = body.indexOf('(');
        if ( parenOpen < 0 ) {
            return null;
        }

        String name = body.substring(0, parenOpen).strip().toUpperCase();
        int parenClose = lastTopLevelClose(body, parenOpen);
        if ( parenClose < 0 ) {
            return null;
        }
        String paramsText = body.substring(parenOpen + 1, parenClose);
        List<String> params = splitTopLevelParams(paramsText);

        return new _StepEntity(id, name, params);
    }

    /**
    Finds the matching top-level closing parenthesis for the opening
    parenthesis at `openPos` in `text`.
    */
    private static int lastTopLevelClose(String text, int openPos)
    {
        int depth = 0;
        boolean inString = false;
        int i;
        for ( i = openPos; i < text.length(); i++ ) {
            char c = text.charAt(i);
            if ( inString ) {
                if ( c == '\'' ) {
                    if ( i + 1 < text.length() && text.charAt(i + 1) == '\'' ) {
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
    static List<String> splitTopLevelParams(String text)
    {
        List<String> params = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int tokenStart = 0;
        int len = text.length();
        int i;

        for ( i = 0; i < len; i++ ) {
            char c = text.charAt(i);
            if ( inString ) {
                if ( c == '\'' ) {
                    if ( i + 1 < len && text.charAt(i + 1) == '\'' ) {
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
                params.add(text.substring(tokenStart, i).strip());
                tokenStart = i + 1;
            }
        }
        String last = text.substring(tokenStart).strip();
        if ( !last.isEmpty() ) {
            params.add(last);
        }
        return params;
    }

    /**
    Parses an entity reference token like `#123` into an integer id.
    @param token the raw parameter token.
    @return the referenced entity id.
    @throws IllegalArgumentException when the token is not a reference.
    */
    public static int parseRef(String token)
    {
        String t = token.strip();
        if ( t.isEmpty() || t.charAt(0) != '#' ) {
            throw new IllegalArgumentException(
                "Expected entity reference, got: '" + token + "'");
        }
        return Integer.parseInt(t.substring(1));
    }

    /**
    Parses a numeric literal token into a double.
    @param token the raw parameter token.
    @return the numeric value.
    */
    public static double parseDouble(String token)
    {
        return Double.parseDouble(token.strip());
    }

    /**
    Extracts the items from an aggregate parameter token like
    `(#1,#2,#3)` or `(1.0E0,2.0E0,3.0E0)`.
    @param token the raw aggregate token including outer parentheses.
    @return the list of top-level element strings, stripped.
    */
    public static List<String> parseAggregate(String token)
    {
        String t = token.strip();
        if ( t.isEmpty() || t.charAt(0) != '(' ) {
            throw new IllegalArgumentException(
                "Expected aggregate token starting with '(', got: '" + token + "'");
        }
        int close = lastTopLevelClose(t, 0);
        if ( close < 0 ) {
            throw new IllegalArgumentException(
                "Unterminated aggregate: '" + token + "'");
        }
        String inner = t.substring(1, close);
        return splitTopLevelParams(inner);
    }
}
