import type { InputStream } from "../../../../java/io/InputStream.js";
import { VSDKFatalException } from "../../common/VSDKFatalException.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { HeightFieldSolid } from "../../environment/geometry/volume/HeightFieldSolid.js";
import { _CScanf } from "./_CScanf.js";

const EOF = -1;

/** Byte input with the single character push back of C `ungetc`. */
class _CharacterInput {
    private position = 0;
    private pushed = EOF - 1;
    public constructor(private readonly bytes: Uint8Array) {}
    public fgetc(): number {
        if (this.pushed !== EOF - 1) {
            const c = this.pushed;
            this.pushed = EOF - 1;
            return c;
        }
        return this.position < this.bytes.length ? this.bytes[this.position++]! : EOF;
    }
    public ungetc(c: number): void {
        this.pushed = c;
    }
}

const SPACE = 0x20;
const TAB = 0x09;
const CR = 0x0d;
const LF = 0x0a;
const SHARP = 0x23;

/**
Reads a text altimetry grid into a height field. The format is the header
"altimetry <columns> <rows>" followed by columns*rows heights, row by row
from z = 0, each row from x = 0; "#" starts a comment up to the end of the
line. Both counts must be at least 2. The solid's box spans the whole grid,
from the lowest to the highest height; when every height is equal the box
is one unit tall above them.

`@vitral/base` stays platform-neutral: this class reads an `InputStream`, and
`@vitral/fs`'s `ReaderAltimetry` adds the cpp entry point taking a file name.
*/
export class ReaderAltimetry {
    // Next whitespace separated token, skipping comments; null at end of file
    // (cpp: false). A token of `capacity` characters or more also gives null.
    private static readToken(file: _CharacterInput, capacity: number): string | null {
        let c = file.fgetc();
        while (c !== EOF) {
            if (c === SHARP) {
                while (c !== EOF && c !== LF) c = file.fgetc();
            } else if (c === SPACE || c === TAB || c === CR || c === LF) {
                c = file.fgetc();
            } else {
                break;
            }
        }
        if (c === EOF) return null;
        let token = "";
        while (c !== EOF && c !== SPACE && c !== TAB && c !== CR && c !== LF && c !== SHARP) {
            if (token.length + 1 >= capacity) return null;
            token += String.fromCharCode(c);
            c = file.fgetc();
        }
        if (c === SHARP) file.ungetc(c);
        return token;
    }

    /** `value` is a one-element output holder. */
    private static parseNumber(token: string | null, value: number[]): boolean {
        if (token === null) return false;
        const parsed = _CScanf.strtodWhole(token);
        if (parsed === null) return false;
        value[0] = parsed;
        return Number.isFinite(value[0]);
    }

    // Throws VSDKFatalException for unreadable or malformed input.
    public static importHeightField(stream: InputStream): HeightFieldSolid {
        let bytes: Uint8Array;
        try {
            bytes = stream.readAllBytes();
        } catch {
            throw new VSDKFatalException("ReaderAltimetry: could not open file");
        }
        return ReaderAltimetry.importHeightFieldFrom(new _CharacterInput(bytes));
    }

    private static importHeightFieldFrom(file: _CharacterInput): HeightFieldSolid {
        const capacity = 128;
        let token: string | null;
        const columns = [0];
        const rows = [0];
        if (
            (token = ReaderAltimetry.readToken(file, capacity)) === null ||
            token !== "altimetry" ||
            !ReaderAltimetry.parseNumber(ReaderAltimetry.readToken(file, capacity), columns) ||
            !ReaderAltimetry.parseNumber(ReaderAltimetry.readToken(file, capacity), rows) ||
            columns[0]! !== Math.trunc(columns[0]!) ||
            rows[0]! !== Math.trunc(rows[0]!) ||
            columns[0]! < 2 ||
            rows[0]! < 2 ||
            columns[0]! * rows[0]! > 1.0e8
        ) {
            throw new VSDKFatalException(
                'ReaderAltimetry: header must be "altimetry <columns> <rows>" with counts of at least 2',
            );
        }
        const columnCount = Math.trunc(columns[0]!);
        const rowCount = Math.trunc(rows[0]!);
        const solid = new HeightFieldSolid(
            columnCount,
            rowCount,
            new Vector3Dd(0, 0, 0),
            new Vector3Dd(columnCount - 1, 1, rowCount - 1),
        );
        let lowest = Number.POSITIVE_INFINITY;
        let highest = Number.NEGATIVE_INFINITY;
        for (let z = 0; z < rowCount; z++) {
            for (let x = 0; x < columnCount; x++) {
                const height = [0];
                if (!ReaderAltimetry.parseNumber(ReaderAltimetry.readToken(file, capacity), height)) {
                    throw new VSDKFatalException("ReaderAltimetry: missing or invalid height");
                }
                solid.setHeight(x, z, height[0]!);
                if (height[0]! < lowest) lowest = height[0]!;
                if (height[0]! > highest) highest = height[0]!;
            }
        }
        const extra = ReaderAltimetry.readToken(file, capacity) !== null;
        if (extra) {
            throw new VSDKFatalException("ReaderAltimetry: more heights than columns*rows");
        }
        if (!(highest > lowest)) highest = lowest + 1.0;
        solid.setBounds(new Vector3Dd(0, lowest, 0), new Vector3Dd(columnCount - 1, highest, rowCount - 1));
        solid.updateBlocks();
        return solid;
    }
}
