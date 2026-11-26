import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";

/**
Port of the Java helper `vsdk.toolkit.io.geometry._CScanf`: the subset of C
`sscanf`, `strtod` and `strtol` that the MIT scene, altimetry and mesh readers
ported from cpp use, with the same return values (assigned conversions, or -1
when the input ends before the first assigned conversion, as glibc does), so
the ported parsing code keeps its control flow. Supported directives: white
space, `%d`, `%lf`, `%f`, `%s` and `%Ns`, each optionally suppressed with `*`.
Numbers follow the C locale grammar: decimal and hexadecimal floating point,
`inf`, `infinity` and `nan`. Outputs are one-element holders: `number[]` for
`%d` and `%lf`, `string[]` for `%s`.
*/
export class _CScanf {
    private constructor() {}

    private static isSpace(c: string): boolean {
        return c === " " || c === "\t" || c === "\n" || c === "\u000b" || c === "\f" || c === "\r";
    }

    private static isDigit(c: string): boolean {
        return c >= "0" && c <= "9";
    }

    private static isHexDigit(c: string): boolean {
        return _CScanf.isDigit(c) || (c >= "a" && c <= "f") || (c >= "A" && c <= "F");
    }

    private static isLetterOrDigit(c: string): boolean {
        return _CScanf.isDigit(c) || (c >= "a" && c <= "z") || (c >= "A" && c <= "Z");
    }

    private static matchesIgnoreCase(s: string, at: number, word: string): boolean {
        return s.substring(at, at + word.length).toLowerCase() === word;
    }

    // Java `Double.parseDouble` of "0x<mantissa>.<fraction>p<exponent>".
    private static parseHexFloat(mantissa: string, fraction: string, exponent: string): number {
        const digits = mantissa + fraction;
        let value = 0n;
        for (let i = 0; i < digits.length; i++) value = value * 16n + BigInt(parseInt(digits.charAt(i), 16));
        if (value === 0n) return 0;
        let binaryExponent = Number.parseInt(exponent, 10) - 4 * fraction.length;
        // Keep 64 significant bits plus a sticky bit, then round once.
        let bits = value.toString(2).length;
        if (bits > 64) {
            const drop = BigInt(bits - 64);
            const sticky = (value & ((1n << drop) - 1n)) !== 0n ? 1n : 0n;
            value = (value >> drop) | sticky;
            binaryExponent += bits - 64;
            bits = 64;
        }
        const exponentOfTop = binaryExponent + bits - 1;
        // Bits of precision available at this magnitude (subnormals have fewer).
        const precision = exponentOfTop >= -1022 ? 53 : Math.max(0, 53 - (-1022 - exponentOfTop));
        if (bits > precision) {
            const drop = BigInt(bits - precision);
            const half = 1n << (drop - 1n);
            const rest = value & ((1n << drop) - 1n);
            let kept = value >> drop;
            if (rest > half || (rest === half && (kept & 1n) === 1n)) kept += 1n;
            value = kept;
            binaryExponent += bits - precision;
        }
        if (binaryExponent > 1023) return Number.POSITIVE_INFINITY;
        // Exact: the significand fits in 53 bits and the power of two is
        // applied in two exact steps.
        const halfExponent = Math.trunc(binaryExponent / 2);
        return Number(value) * 2 ** halfExponent * 2 ** (binaryExponent - halfExponent);
    }

    /**
    Length of the longest floating point prefix of `s` starting at `start`,
    or 0 when there is none. `value[0]` receives the parsed number.
    With `scanfExponent`, an exponent marker without digits is consumed as
    glibc `scanf` does ("1e" reads as 1), while `strtod` leaves it unread.
    */
    public static floatPrefix(s: string, start: number, value: number[], scanfExponent = false): number {
        let i = start;
        const n = s.length;
        let negative = false;
        if (i < n && (s.charAt(i) === "+" || s.charAt(i) === "-")) {
            negative = s.charAt(i) === "-";
            i++;
        }
        if (_CScanf.matchesIgnoreCase(s, i, "infinity")) {
            value[0] = negative ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
            return i + 8 - start;
        }
        if (_CScanf.matchesIgnoreCase(s, i, "inf")) {
            value[0] = negative ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY;
            return i + 3 - start;
        }
        if (_CScanf.matchesIgnoreCase(s, i, "nan")) {
            let end = i + 3;
            if (end < n && s.charAt(end) === "(") {
                let k = end + 1;
                while (k < n && (_CScanf.isLetterOrDigit(s.charAt(k)) || s.charAt(k) === "_")) k++;
                if (k < n && s.charAt(k) === ")") end = k + 1;
            }
            value[0] = Number.NaN;
            return end - start;
        }
        if (i + 1 < n && s.charAt(i) === "0" && (s.charAt(i + 1) === "x" || s.charAt(i + 1) === "X")) {
            let k = i + 2;
            let digits = 0;
            let mantissa = "";
            while (k < n && _CScanf.isHexDigit(s.charAt(k))) {
                mantissa += s.charAt(k);
                k++;
                digits++;
            }
            let fraction = "";
            if (k < n && s.charAt(k) === ".") {
                let f = k + 1;
                while (f < n && _CScanf.isHexDigit(s.charAt(f))) {
                    fraction += s.charAt(f);
                    f++;
                    digits++;
                }
                if (digits > 0) k = f;
            }
            if (digits > 0) {
                let exponent = "0";
                if (k < n && (s.charAt(k) === "p" || s.charAt(k) === "P")) {
                    let e = k + 1;
                    const signAt = e;
                    if (e < n && (s.charAt(e) === "+" || s.charAt(e) === "-")) e++;
                    const digitsStart = e;
                    while (e < n && _CScanf.isDigit(s.charAt(e))) e++;
                    if (e > digitsStart) {
                        exponent = s.substring(signAt, e);
                        k = e;
                    }
                }
                const v = _CScanf.parseHexFloat(mantissa.length > 0 ? mantissa : "0", fraction, exponent);
                value[0] = negative ? -v : v;
                return k - start;
            }
            // "0x" without hexadecimal digits: the "0" alone is the number.
        }
        let k = i;
        let digits = 0;
        while (k < n && _CScanf.isDigit(s.charAt(k))) {
            k++;
            digits++;
        }
        if (k < n && s.charAt(k) === ".") {
            let f = k + 1;
            let fractionDigits = 0;
            while (f < n && _CScanf.isDigit(s.charAt(f))) {
                f++;
                fractionDigits++;
            }
            if (digits + fractionDigits > 0) {
                k = f;
                digits += fractionDigits;
            }
        }
        if (digits === 0) return 0;
        if (k < n && (s.charAt(k) === "e" || s.charAt(k) === "E")) {
            let e = k + 1;
            if (e < n && (s.charAt(e) === "+" || s.charAt(e) === "-")) e++;
            const digitsStart = e;
            while (e < n && _CScanf.isDigit(s.charAt(e))) e++;
            if (e > digitsStart || scanfExponent) k = e;
        }
        let text = s.substring(i, k);
        if (scanfExponent) {
            // Drop an exponent marker (and sign) that has no digits.
            let cut = text.length;
            while (cut > 0 && !_CScanf.isDigit(text.charAt(cut - 1)) && text.charAt(cut - 1) !== ".") cut--;
            text = text.substring(0, cut);
        }
        if (text.endsWith(".")) text = text + "0";
        if (text.startsWith(".")) text = "0" + text;
        const v = Number(text);
        value[0] = negative ? -v : v;
        return k - start;
    }

    /**
    Length of the longest decimal integer prefix (optional sign, digits) of
    `s` starting at `start`, or 0 when there is none.
    */
    public static integerPrefix(s: string, start: number, value: number[]): number {
        let i = start;
        const n = s.length;
        let negative = false;
        if (i < n && (s.charAt(i) === "+" || s.charAt(i) === "-")) {
            negative = s.charAt(i) === "-";
            i++;
        }
        let k = i;
        let v = 0;
        // Java saturates below Long.MAX_VALUE / 10; every value the readers
        // accept is far below 2^53, where number arithmetic is exact.
        while (k < n && _CScanf.isDigit(s.charAt(k))) {
            if (v < Number.MAX_SAFE_INTEGER / 10) v = v * 10 + (s.charCodeAt(k) - 48);
            k++;
        }
        if (k === i) return 0;
        value[0] = negative ? -v : v;
        return k - start;
    }

    /**
    C `strtod(s + start, &end)`: leading white space is skipped and the
    longest floating point prefix is converted into `value[0]` (0 when there
    is none).
    @return the index of `end`: just after the number, or `start` when no
    conversion is done
    */
    public static strtod(s: string, start: number, value: number[]): number {
        let i = start;
        while (i < s.length && _CScanf.isSpace(s.charAt(i))) i++;
        const length = _CScanf.floatPrefix(s, i, value);
        if (length === 0) {
            value[0] = 0;
            return start;
        }
        return i + length;
    }

    /**
    C `strtod` over a whole token: the number when the token is entirely a
    floating point number, otherwise null.
    */
    public static strtodWhole(token: string): number | null {
        let i = 0;
        while (i < token.length && _CScanf.isSpace(token.charAt(i))) i++;
        const value = [0];
        const length = _CScanf.floatPrefix(token, i, value);
        if (length === 0 || i + length !== token.length) return null;
        return value[0]!;
    }

    // Java `(int)` of a long: the low 32 bits as a signed integer.
    private static toInt(value: number): number {
        return Number(BigInt.asIntN(32, BigInt(value)));
    }

    /** C `sscanf`. */
    public static sscanf(input: string, format: string, ...outputs: (number[] | string[])[]): number {
        let inp = 0;
        let out = 0;
        let assigned = 0;
        const n = input.length;
        for (let f = 0; f < format.length; f++) {
            const fc = format.charAt(f);
            if (_CScanf.isSpace(fc)) {
                while (inp < n && _CScanf.isSpace(input.charAt(inp))) inp++;
                continue;
            }
            if (fc !== "%") {
                if (inp >= n) return assigned > 0 ? assigned : -1;
                if (input.charAt(inp) !== fc) return assigned;
                inp++;
                continue;
            }
            f++;
            let suppress = false;
            if (format.charAt(f) === "*") {
                suppress = true;
                f++;
            }
            let width = 0;
            while (_CScanf.isDigit(format.charAt(f))) {
                width = width * 10 + (format.charCodeAt(f) - 48);
                f++;
            }
            let longModifier = false;
            if (format.charAt(f) === "l") {
                longModifier = true;
                f++;
            }
            const conversion = format.charAt(f);
            while (inp < n && _CScanf.isSpace(input.charAt(inp))) inp++;
            if (inp >= n) return assigned > 0 ? assigned : -1;
            const scope = width > 0 && inp + width < n ? input.substring(0, inp + width) : input;
            if (conversion === "s") {
                let k = inp;
                while (k < scope.length && !_CScanf.isSpace(scope.charAt(k))) k++;
                if (!suppress) {
                    (outputs[out++] as string[])[0] = input.substring(inp, k);
                    assigned++;
                }
                inp = k;
            } else if (conversion === "d") {
                const value = [0];
                const length = _CScanf.integerPrefix(scope, inp, value);
                if (length === 0) return assigned;
                if (!suppress) {
                    (outputs[out++] as number[])[0] = _CScanf.toInt(value[0]!);
                    assigned++;
                }
                inp += length;
            } else if (conversion === "f") {
                const value = [0];
                const length = _CScanf.floatPrefix(scope, inp, value, true);
                if (length === 0) return assigned;
                if (!suppress) {
                    if (!longModifier) {
                        throw new IllegalArgumentException("only %lf conversions are supported");
                    }
                    (outputs[out++] as number[])[0] = value[0]!;
                    assigned++;
                }
                inp += length;
            } else {
                throw new IllegalArgumentException("unsupported conversion %" + conversion);
            }
        }
        return assigned;
    }
}
