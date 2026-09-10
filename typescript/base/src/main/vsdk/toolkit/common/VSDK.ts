import { setWithFatalExceptions, setWithSystemExit } from "./VSDKRuntimeSettings.js";

/** Common numeric constants and utility operations in the current Java VSDK. */
export class VSDK {
    public static readonly EPSILON = 1e-6;
    public static readonly WARNING = 1;
    public static readonly ERROR = 2;
    public static readonly FATAL_ERROR = 3;
    public static readonly DEBUG = 4;
    public static readonly VERBOSE = 5;
    public static readonly POINT = 0;
    public static readonly LINE = 1;
    public static readonly TRIANGLE = 2;
    public static readonly TRIANGLE_STRIP = 3;
    public static readonly QUAD = 4;
    public static readonly QUAD_STRIP = 5;
    public static readonly PRIMITIVE_TYPE_COUNT = 6;
    public static readonly PLANE = 0;
    public static readonly SPHERE = 1;
    public static readonly CONE = 2;
    public static readonly INTERSECTION_TYPE_COUNT = 3;

    public static equals(a: number, b: number): boolean {
        return Math.abs(a - b) < VSDK.EPSILON;
    }
    public static square(a: number): number {
        return a * a;
    }
    public static formatNumberWithinZeroes(a: number | bigint, n: number): string {
        return a.toString().padStart(n, "0");
    }
    public static formatDouble(a: number): string;
    public static formatDouble(a: number, digits: number): string;
    public static formatDouble(a: number, digits = 2): string {
        return a.toFixed(digits);
    }
    public static formatByteAsHex(a: number): string {
        const i = VSDK.signedByte2unsignedInteger(a);
        return i.toString(16).padStart(2, "0").toUpperCase();
    }
    public static formatIntAsHex(a: number): string {
        return (a >>> 0).toString(16).padStart(8, "0").toUpperCase();
    }
    public static signedByte2unsignedInteger(input: number): number {
        return input < 0 ? input + 256 : input;
    }
    public static unsigned8BitInteger2signedByte(input: number): number {
        const value = Math.min(255, Math.max(0, input));
        return value > 127 ? value - 256 : value;
    }
    public static setWithSystemExit(flag: boolean): void {
        setWithSystemExit(flag);
    }
    public static setWithFatalExceptions(flag: boolean): void {
        setWithFatalExceptions(flag);
    }
}
