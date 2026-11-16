#ifndef __STEP_TOKENIZER__
#define __STEP_TOKENIZER__

#include "java/io/InputStream.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepEntity.h"

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
the name `_StepEntity::COMPLEX_NAME` and a single param containing the
raw body text; the solid builder ignores them.

This is an internal collaborator of `StepReader`.

C++ port notes: Java's `String.strip` is `java::String::trim` (both remove
the ASCII white space a STEP file has); the strings are byte strings
(UTF-8 is kept as is, and the syntax of the file is ASCII).
*/
class _StepTokenizer {
public:
    /**
    Parses the STEP file and returns all entity instances found in
    the DATA section, keyed by entity id.
    @param in input stream for the STEP file; not closed by this method.
    @param outEntities map from entity id to parsed entity (C++ port: an
    output parameter instead of the return value)
    @throws java::Exception on I/O errors or malformed DATA section.
    */
    static void parse(java::InputStream* in, _StepEntityMap& outEntities);

    static java::ArrayList<java::String> splitTopLevelParams(
        const java::String& text);

    /**
    Parses an entity reference token like `#123` into an integer id.
    @param token the raw parameter token.
    @return the referenced entity id.
    @throws java::IllegalArgumentException when the token is not a
    reference.
    */
    static int parseRef(const java::String& token);

    /**
    Parses a numeric literal token into a double.
    @param token the raw parameter token.
    @return the numeric value.
    */
    static double parseDouble(const java::String& token);

    /**
    Extracts the items from an aggregate parameter token like
    `(#1,#2,#3)` or `(1.0E0,2.0E0,3.0E0)`.
    @param token the raw aggregate token including outer parentheses.
    @return the list of top-level element strings, stripped.
    */
    static java::ArrayList<java::String> parseAggregate(
        const java::String& token);

private:
    _StepTokenizer() {}

    static int findSectionStart(const java::String& text,
                                const java::String& marker);
    static int findSectionEnd(const java::String& text, int searchFrom);
    static void parseDataSection(const java::String& section,
                                 _StepEntityMap& entities);
    static java::ArrayList<java::String> splitEntities(
        const java::String& text);
    static _StepEntity* parseEntityString(const java::String& raw);
    static int lastTopLevelClose(const java::String& text, int openPos);
};

#endif
