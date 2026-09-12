export class Math {
    public static readonly E = 2.7182818284590452354;
    public static readonly PI = 3.14159265358979323846;

    public static floor(a: number): number {
        return globalThis.Math.floor(a);
    }

    public static ceil(a: number): number {
        return globalThis.Math.ceil(a);
    }

    /**
    `Math.round(double)`: the closest integer, with ties rounding towards
    positive infinity (so `round(-2.5)` is `-2`, not `-3`), `0` for NaN, and
    saturation at the `long` range. JavaScript's `Math.round` already uses the
    same tie rule and handles `0.49999999999999994` the way the JDK does.
    */
    public static round(a: number): number {
        if (globalThis.Number.isNaN(a)) {
            return 0;
        }
        if (a <= -9223372036854775808) {
            return -9223372036854775808;
        }
        if (a >= 9223372036854775807) {
            return 9223372036854775807;
        }
        return globalThis.Math.round(a) + 0;
    }

    public static log(a: number): number {
        return globalThis.Math.log(a);
    }

    public static log10(a: number): number {
        return globalThis.Math.log10(a);
    }

    public static sin(a: number): number {
        return globalThis.Math.sin(a);
    }

    public static cos(a: number): number {
        return globalThis.Math.cos(a);
    }

    public static tan(a: number): number {
        return globalThis.Math.tan(a);
    }

    public static acos(a: number): number {
        return globalThis.Math.acos(a);
    }

    public static atan(a: number): number {
        return globalThis.Math.atan(a);
    }

    public static exp(a: number): number {
        return globalThis.Math.exp(a);
    }

    public static pow(a: number, e: number): number {
        return globalThis.Math.pow(a, e);
    }

    public static abs(a: number): number {
        return globalThis.Math.abs(a);
    }

    public static min(a: number, b: number): number {
        return a < b ? a : b;
    }

    public static max(a: number, b: number): number {
        return a > b ? a : b;
    }

    /** `Math.toRadians(double)`: `angdeg / 180.0 * PI`. */
    public static toRadians(angdeg: number): number {
        return (angdeg / 180.0) * Math.PI;
    }

    /** `Math.toDegrees(double)`: `angrad * 180.0 / PI`. */
    public static toDegrees(angrad: number): number {
        return (angrad * 180.0) / Math.PI;
    }

    /** `Math.floorDiv(int, int)`. */
    public static floorDiv(x: number, y: number): number {
        return globalThis.Math.floor(x / y);
    }

    /** `Math.floorMod(int, int)`: `x - floorDiv(x, y) * y`. */
    public static floorMod(x: number, y: number): number {
        return x - Math.floorDiv(x, y) * y;
    }

    public static sqrt(a: number): number {
        return globalThis.Math.sqrt(a);
    }

    public static getExponent(a: number): number {
        if (a === 0 || !globalThis.Number.isFinite(a)) {
            return 0;
        }
        return globalThis.Math.floor(globalThis.Math.log2(globalThis.Math.abs(a)));
    }

    public static scalb(a: number, scaleFactor: number): number {
        return a * globalThis.Math.pow(2.0, scaleFactor);
    }
}
