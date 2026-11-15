import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";

/**
Exact comparisons of finite binary64 expressions using dyadic integer limbs.
Unsupported arithmetic environments or out-of-range intermediates return false.

C++ `unsigned long long` values are `bigint` reduced with `BigInt.asUintN(64)`
and the limb arrays are `BigUint64Array`, so wrap-around matches the C++ code.
*/
export class CsgExactArithmetic {
    private static decompose(value: number, significand: bigint[], exponent: number[]): boolean {
        if (!Double.isFinite(value)) return false;
        if (value === 0) {
            significand[0] = 0n;
            exponent[0] = 0;
            return true;
        }
        const magnitude = JavaMath.abs(value);
        exponent[0] = JavaMath.getExponent(magnitude);
        const normalized = JavaMath.scalb(magnitude, -exponent[0]);
        const integer = JavaMath.scalb(normalized, 52);
        if (!Double.isFinite(integer) || integer < 4503599627370496.0 || integer >= 9007199254740992.0) return false;
        significand[0] = BigInt(integer);
        exponent[0] -= 52;
        while ((significand[0] & 1n) === 0n) {
            significand[0] >>= 1n;
            ++exponent[0];
        }
        return true;
    }

    private static addWord(target: BigUint64Array, index: number, word: bigint): boolean {
        const mask = 0xffffffffn;
        while (word !== 0n) {
            if (index < 0 || index >= 140) return false;
            const sum = BigInt.asUintN(64, target[index]! + word);
            target[index] = sum & mask;
            word = sum >> 32n;
            ++index;
        }
        return true;
    }

    private static addProduct(
        positive: BigUint64Array,
        negative: BigUint64Array,
        a: number,
        b: number,
        negate: boolean,
    ): boolean {
        const x: bigint[] = [0n];
        const y: bigint[] = [0n];
        const ex: number[] = [0];
        const ey: number[] = [0];
        if (!CsgExactArithmetic.decompose(a, x, ex) || !CsgExactArithmetic.decompose(b, y, ey)) return false;
        if (x[0] === 0n || y[0] === 0n) return true;
        const mask = 0xffffffffn;
        const xl: bigint[] = [x[0]! & mask, x[0]! >> 32n];
        const yl: bigint[] = [y[0]! & mask, y[0]! >> 32n];
        const product: bigint[] = [0n, 0n, 0n, 0n];
        for (let i = 0; i < 2; ++i) {
            let carry = 0n;
            for (let j = 0; j < 2; ++j) {
                const value = BigInt.asUintN(64, xl[i]! * yl[j]! + product[i + j]! + carry);
                product[i + j] = value & mask;
                carry = value >> 32n;
            }
            product[i + 2] = carry;
        }
        const offset = ex[0]! + ey[0]! + 2148;
        if (offset < 0) return false;
        const wordOffset = Math.trunc(offset / 32);
        const shift = offset % 32;
        const target = (a < 0 !== b < 0) !== negate ? negative : positive;
        for (let i = 0; i < 4; ++i) {
            const shifted = BigInt.asUintN(64, product[i]! << BigInt(shift));
            if (
                !CsgExactArithmetic.addWord(target, wordOffset + i, shifted & mask) ||
                !CsgExactArithmetic.addWord(target, wordOffset + i + 1, shifted >> 32n)
            )
                return false;
        }
        return true;
    }

    public static equalSumProducts(
        a: readonly number[] | null,
        b: readonly number[] | null,
        c: readonly number[] | null,
        d: readonly number[] | null,
        count: number,
    ): boolean {
        if (
            a === null ||
            b === null ||
            c === null ||
            d === null ||
            count <= 0 ||
            !FloatingInterval.isSupportedEnvironment()
        )
            return false;
        const positive = new BigUint64Array(140);
        const negative = new BigUint64Array(140);
        for (let i = 0; i < count; ++i)
            if (
                !CsgExactArithmetic.addProduct(positive, negative, a[i]!, b[i]!, false) ||
                !CsgExactArithmetic.addProduct(positive, negative, c[i]!, d[i]!, true)
            )
                return false;
        for (let i = 0; i < 140; ++i) if (positive[i] !== negative[i]) return false;
        return true;
    }

    public static equalProducts(a: number, b: number, c: number, d: number): boolean {
        if (!FloatingInterval.isSupportedEnvironment()) return false;
        const positive = new BigUint64Array(140);
        const negative = new BigUint64Array(140);
        if (
            !CsgExactArithmetic.addProduct(positive, negative, a, b, false) ||
            !CsgExactArithmetic.addProduct(positive, negative, c, d, true)
        )
            return false;
        for (let i = 0; i < 140; ++i) if (positive[i] !== negative[i]) return false;
        return true;
    }

    public static equalAffine(a: number, b: number, c: number, d: number, e: number, f: number): boolean {
        if (!FloatingInterval.isSupportedEnvironment()) return false;
        const positive = new BigUint64Array(140);
        const negative = new BigUint64Array(140);
        if (
            !CsgExactArithmetic.addProduct(positive, negative, a, b, false) ||
            !CsgExactArithmetic.addProduct(positive, negative, c, 1, false) ||
            !CsgExactArithmetic.addProduct(positive, negative, d, e, true) ||
            !CsgExactArithmetic.addProduct(positive, negative, f, 1, true)
        )
            return false;
        for (let i = 0; i < 140; ++i) if (positive[i] !== negative[i]) return false;
        return true;
    }
}
