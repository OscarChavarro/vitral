import { IllegalArgumentException } from "./IllegalArgumentException.js";

/** Java double helpers, including lossless IEEE-754 bit conversions. */
export class Double {
    public static readonly MIN_VALUE = 4.9406564584124654e-324;
    public static readonly MIN_NORMAL = 2.2250738585072014e-308;
    public static readonly MAX_VALUE = 1.7976931348623157e308;
    public static readonly POSITIVE_INFINITY = Infinity;
    public static readonly NEGATIVE_INFINITY = -Infinity;
    public static readonly NaN = NaN;
    public static readonly SIZE = 64;
    public static readonly BYTES = 8;

    public static valueOf(value: number | string): number {
        return typeof value === "number" ? value : Double.parseDouble(value);
    }
    public static parseDouble(text: string): number {
        if (text.length === 0 || text.trim() !== text)
            throw new IllegalArgumentException(`For input string: "${text}"`);
        if (text === "NaN") return NaN;
        if (text === "Infinity" || text === "+Infinity") return Infinity;
        if (text === "-Infinity") return -Infinity;
        const value = Number(
            text.endsWith("d") || text.endsWith("D") || text.endsWith("f") || text.endsWith("F")
                ? text.slice(0, -1)
                : text,
        );
        if (Number.isNaN(value)) throw new IllegalArgumentException(`For input string: "${text}"`);
        return value;
    }
    public static isNaN(value: number): boolean {
        return Number.isNaN(value);
    }
    public static isInfinite(value: number): boolean {
        return value === Infinity || value === -Infinity;
    }
    public static isFinite(value: number): boolean {
        return Number.isFinite(value);
    }
    public static compare(left: number, right: number): number {
        if (left < right) return -1;
        if (left > right) return 1;
        const a = Double.doubleToLongBits(left);
        const b = Double.doubleToLongBits(right);
        return a === b ? 0 : a < b ? -1 : 1;
    }
    public static doubleToRawLongBits(value: number): bigint {
        const view = new DataView(new ArrayBuffer(8));
        view.setFloat64(0, value, false);
        return view.getBigInt64(0, false);
    }
    public static doubleToLongBits(value: number): bigint {
        return Number.isNaN(value) ? 0x7ff8_0000_0000_0000n : Double.doubleToRawLongBits(value);
    }
    public static longBitsToDouble(bits: bigint): number {
        const view = new DataView(new ArrayBuffer(8));
        view.setBigInt64(0, BigInt.asIntN(64, bits), false);
        return view.getFloat64(0, false);
    }
    public static hashCode(value: number): number {
        const bits = BigInt.asUintN(64, Double.doubleToLongBits(value));
        return Number((bits ^ (bits >> 32n)) & 0xffff_ffffn) | 0;
    }
    public static toString(value: number): string {
        return Number.isNaN(value)
            ? "NaN"
            : value === Infinity
              ? "Infinity"
              : value === -Infinity
                ? "-Infinity"
                : String(value);
    }
}
