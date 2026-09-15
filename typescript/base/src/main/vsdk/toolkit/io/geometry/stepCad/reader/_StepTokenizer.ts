import type { InputStream } from "../../../../../../java/io/InputStream.js";
import { Double } from "../../../../../../java/lang/Double.js";
import { IllegalArgumentException } from "../../../../../../java/lang/IllegalArgumentException.js";
import { Integer } from "../../../../../../java/lang/Integer.js";
import { IntegerKeyHashMap } from "../../../../../../java/util/IntegerKeyHashMap.js";
import { _StepEntity } from "./_StepEntity.js";

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

Port of `vsdk.toolkit.io.geometry.stepCad.reader._StepTokenizer`. The entity
map is a `java.util.HashMap<Integer, _StepEntity>` in Java, and the builder
iterates it, so it is an `IntegerKeyHashMap`, which iterates in the order a
JVM does. Java's `NumberFormatException` is an `IllegalArgumentException`,
which is what the ported `Integer.parseInt` throws, so the one `catch` that
names it catches that. `String.strip()` is `trim()`.
*/
export class _StepTokenizer {
    private constructor() {}

    /**
    Parses the STEP file and returns all entity instances found in
    the DATA section, keyed by entity id.
    @param inStream input stream for the STEP file; not closed by this method.
    @return map from entity id to parsed entity.
    @throws Exception on I/O errors or malformed DATA section.
    */
    public static parse(inStream: InputStream): IntegerKeyHashMap<_StepEntity> {
        const bytes: Uint8Array = inStream.readAllBytes();
        const text: string = new TextDecoder("utf-8").decode(bytes);

        const dataStart: number = _StepTokenizer.findSectionStart(text, "DATA;");
        const dataEnd: number = _StepTokenizer.findSectionEnd(text, dataStart);

        const dataSection: string = text.substring(dataStart, dataEnd);
        return _StepTokenizer.parseDataSection(dataSection);
    }

    //=================================================================

    private static findSectionStart(text: string, marker: string): number {
        const idx: number = text.indexOf(marker);
        if (idx < 0) {
            throw new IllegalArgumentException("STEP file is missing '" + marker + "' marker.");
        }
        return idx + marker.length;
    }

    private static findSectionEnd(text: string, searchFrom: number): number {
        const idx: number = text.indexOf("ENDSEC;", searchFrom);
        if (idx < 0) {
            throw new IllegalArgumentException("STEP file DATA section is missing closing 'ENDSEC;'.");
        }
        return idx;
    }

    private static parseDataSection(section: string): IntegerKeyHashMap<_StepEntity> {
        const entities = new IntegerKeyHashMap<_StepEntity>();
        const rawEntities: string[] = _StepTokenizer.splitEntities(section);
        for (const raw of rawEntities) {
            const trimmed: string = raw.trim();
            if (trimmed.length === 0) {
                continue;
            }
            const entity: _StepEntity | null = _StepTokenizer.parseEntityString(trimmed);
            if (entity !== null) {
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
    private static splitEntities(text: string): string[] {
        const entities: string[] = [];
        const len: number = text.length;
        let depth = 0;
        let inString = false;
        let entityStart = 0;

        let i: number;
        for (i = 0; i < len; i++) {
            const c: string = text.charAt(i);
            if (inString) {
                if (c === "'") {
                    if (i + 1 < len && text.charAt(i + 1) === "'") {
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else if (c === "'") {
                inString = true;
            } else if (c === "(") {
                depth++;
            } else if (c === ")") {
                depth--;
            } else if (c === ";" && depth === 0) {
                entities.push(text.substring(entityStart, i));
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
    private static parseEntityString(raw: string): _StepEntity | null {
        if (raw.length === 0 || raw.charAt(0) !== "#") {
            return null;
        }

        const eqPos: number = raw.indexOf("=");
        if (eqPos < 0) {
            return null;
        }

        let id: number;
        try {
            id = Integer.parseInt(raw.substring(1, eqPos).trim());
        } catch (ignored) {
            if (ignored instanceof IllegalArgumentException) {
                return null;
            }
            throw ignored;
        }

        const body: string = raw.substring(eqPos + 1).trim();

        if (body.startsWith("(")) {
            const single: string[] = [];
            single.push(body);
            return new _StepEntity(id, _StepEntity.COMPLEX_NAME, single);
        }

        const parenOpen: number = body.indexOf("(");
        if (parenOpen < 0) {
            return null;
        }

        const name: string = body.substring(0, parenOpen).trim().toUpperCase();
        const parenClose: number = _StepTokenizer.lastTopLevelClose(body, parenOpen);
        if (parenClose < 0) {
            return null;
        }
        const paramsText: string = body.substring(parenOpen + 1, parenClose);
        const params: string[] = _StepTokenizer.splitTopLevelParams(paramsText);

        return new _StepEntity(id, name, params);
    }

    /**
    Finds the matching top-level closing parenthesis for the opening
    parenthesis at `openPos` in `text`.
    */
    private static lastTopLevelClose(text: string, openPos: number): number {
        let depth = 0;
        let inString = false;
        let i: number;
        for (i = openPos; i < text.length; i++) {
            const c: string = text.charAt(i);
            if (inString) {
                if (c === "'") {
                    if (i + 1 < text.length && text.charAt(i + 1) === "'") {
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else if (c === "'") {
                inString = true;
            } else if (c === "(") {
                depth++;
            } else if (c === ")") {
                depth--;
                if (depth === 0) {
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
    public static splitTopLevelParams(text: string): string[] {
        const params: string[] = [];
        let depth = 0;
        let inString = false;
        let tokenStart = 0;
        const len: number = text.length;
        let i: number;

        for (i = 0; i < len; i++) {
            const c: string = text.charAt(i);
            if (inString) {
                if (c === "'") {
                    if (i + 1 < len && text.charAt(i + 1) === "'") {
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else if (c === "'") {
                inString = true;
            } else if (c === "(") {
                depth++;
            } else if (c === ")") {
                depth--;
            } else if (c === "," && depth === 0) {
                params.push(text.substring(tokenStart, i).trim());
                tokenStart = i + 1;
            }
        }
        const last: string = text.substring(tokenStart).trim();
        if (last.length !== 0) {
            params.push(last);
        }
        return params;
    }

    /**
    Parses an entity reference token like `#123` into an integer id.
    @param token the raw parameter token.
    @return the referenced entity id.
    @throws IllegalArgumentException when the token is not a reference.
    */
    public static parseRef(token: string): number {
        const t: string = token.trim();
        if (t.length === 0 || t.charAt(0) !== "#") {
            throw new IllegalArgumentException("Expected entity reference, got: '" + token + "'");
        }
        return Integer.parseInt(t.substring(1));
    }

    /**
    Parses a numeric literal token into a double.
    @param token the raw parameter token.
    @return the numeric value.
    */
    public static parseDouble(token: string): number {
        return Double.parseDouble(token.trim());
    }

    /**
    Extracts the items from an aggregate parameter token like
    `(#1,#2,#3)` or `(1.0E0,2.0E0,3.0E0)`.
    @param token the raw aggregate token including outer parentheses.
    @return the list of top-level element strings, stripped.
    */
    public static parseAggregate(token: string): string[] {
        const t: string = token.trim();
        if (t.length === 0 || t.charAt(0) !== "(") {
            throw new IllegalArgumentException("Expected aggregate token starting with '(', got: '" + token + "'");
        }
        const close: number = _StepTokenizer.lastTopLevelClose(t, 0);
        if (close < 0) {
            throw new IllegalArgumentException("Unterminated aggregate: '" + token + "'");
        }
        const inner: string = t.substring(1, close);
        return _StepTokenizer.splitTopLevelParams(inner);
    }
}
