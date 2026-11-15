import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";

const INT_MAX = 2147483647;
const INT_MIN = -2147483648;

/**
Signed exact dyadic number. Fixed-capacity binary limbs avoid heap allocation;
every operation reports failure and leaves its output unchanged on overflow.

C++ value semantics: `assign` copies, and results are written into caller
objects. 32-bit limbs live in a `Uint32Array`; sums and carries fit exactly
in binary64 and the 32x32 limb products use `bigint`.
*/
export class CsgDyadic {
    private static readonly CAPACITY = 1100;
    private readonly limbs: Uint32Array;
    private length: number;
    private exponent: number;
    private sign: number;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: CsgDyadic);
    public constructor(other?: CsgDyadic) {
        this.limbs = new Uint32Array(CsgDyadic.CAPACITY);
        this.length = 0;
        this.exponent = 0;
        this.sign = 0;
        if (other !== undefined) this.assign(other);
    }

    /** C++ copy assignment. */
    public assign(other: CsgDyadic): CsgDyadic {
        if (this !== other) {
            this.limbs.set(other.limbs);
            this.length = other.length;
            this.exponent = other.exponent;
            this.sign = other.sign;
        }
        return this;
    }

    private normalize(): boolean {
        while (this.length > 0 && this.limbs[this.length - 1] === 0) --this.length;
        if (!this.length) {
            this.exponent = 0;
            this.sign = 0;
            return true;
        }
        let words = 0;
        while (words < this.length && this.limbs[words] === 0) ++words;
        if (words) {
            for (let i = words; i < this.length; ++i) this.limbs[i - words] = this.limbs[i]!;
            for (let i = this.length - words; i < this.length; ++i) this.limbs[i] = 0;
            this.length -= words;
            const shiftedExponent = this.exponent + 32 * words;
            if (shiftedExponent > INT_MAX) return false;
            this.exponent = shiftedExponent;
        }
        while ((this.limbs[0]! & 1) === 0) {
            let carry = 0;
            for (let i = this.length - 1; i >= 0; --i) {
                const next = this.limbs[i]! & 1;
                this.limbs[i] = (this.limbs[i]! >>> 1) | (carry << 31);
                carry = next;
            }
            if (this.exponent === INT_MAX) return false;
            ++this.exponent;
            while (this.length > 0 && this.limbs[this.length - 1] === 0) --this.length;
        }
        return true;
    }

    public static fromDouble(value: number, result: CsgDyadic): boolean {
        if (!FloatingInterval.isSupportedEnvironment() || !Double.isFinite(value)) return false;
        const candidate = new CsgDyadic();
        if (value !== 0) {
            const absolute = JavaMath.abs(value);
            const exp = JavaMath.getExponent(absolute);
            const significand = JavaMath.scalb(JavaMath.scalb(absolute, -exp), 52);
            if (!Double.isFinite(significand) || significand < 4503599627370496.0 || significand >= 9007199254740992.0)
                return false;
            const bits = BigInt(significand);
            candidate.limbs[0] = Number(bits & 0xffffffffn);
            candidate.limbs[1] = Number(bits >> 32n);
            candidate.length = 2;
            candidate.exponent = exp - 52;
            candidate.sign = value < 0 ? -1 : 1;
            if (!candidate.normalize()) return false;
        }
        result.assign(candidate);
        return true;
    }

    private static shifted(value: CsgDyadic, targetExponent: number, result: Uint32Array): boolean {
        const difference = value.exponent - targetExponent;
        if (difference < 0) return false;
        const words = Math.trunc(difference / 32);
        const bits = difference % 32;
        if (value.length && (words >= CsgDyadic.CAPACITY || words + value.length + (bits ? 1 : 0) > CsgDyadic.CAPACITY))
            return false;
        for (let i = 0; i < CsgDyadic.CAPACITY; ++i) result[i] = 0;
        for (let i = 0; i < value.length; ++i) {
            const limb = value.limbs[i]!;
            // part = limb << bits as a 64-bit value: low and high words.
            result[words + i] = (result[words + i]! | ((limb << bits) >>> 0)) >>> 0;
            if (bits) result[words + i + 1] = (result[words + i + 1]! | (limb >>> (32 - bits))) >>> 0;
        }
        return true;
    }

    public static add(a: CsgDyadic, b: CsgDyadic, result: CsgDyadic): boolean {
        if (!a.sign) {
            result.assign(b);
            return true;
        }
        if (!b.sign) {
            result.assign(a);
            return true;
        }
        const common = a.exponent < b.exponent ? a.exponent : b.exponent;
        const x = new Uint32Array(CsgDyadic.CAPACITY);
        const y = new Uint32Array(CsgDyadic.CAPACITY);
        if (!CsgDyadic.shifted(a, common, x) || !CsgDyadic.shifted(b, common, y)) return false;
        const candidate = new CsgDyadic();
        candidate.exponent = common;
        if (a.sign === b.sign) {
            let carry = 0;
            for (let i = 0; i < CsgDyadic.CAPACITY; ++i) {
                const sum = x[i]! + y[i]! + carry;
                candidate.limbs[i] = sum % 4294967296;
                carry = Math.floor(sum / 4294967296);
            }
            if (carry) return false;
            candidate.sign = a.sign;
        } else {
            let comparison = 0;
            for (let i = CsgDyadic.CAPACITY - 1; i >= 0; --i)
                if (x[i] !== y[i]) {
                    comparison = x[i]! > y[i]! ? 1 : -1;
                    break;
                }
            if (!comparison) {
                result.assign(candidate);
                return true;
            }
            const large = comparison > 0 ? x : y;
            const small = comparison > 0 ? y : x;
            let borrow = 0;
            for (let i = 0; i < CsgDyadic.CAPACITY; ++i) {
                const lhs = large[i]!;
                const rhs = small[i]! + borrow;
                candidate.limbs[i] = (lhs - rhs + 4294967296) % 4294967296;
                borrow = lhs < rhs ? 1 : 0;
            }
            candidate.sign = comparison > 0 ? a.sign : b.sign;
        }
        candidate.length = CsgDyadic.CAPACITY;
        if (!candidate.normalize()) return false;
        result.assign(candidate);
        return true;
    }

    public static subtract(a: CsgDyadic, b: CsgDyadic, result: CsgDyadic): boolean {
        const opposite = new CsgDyadic(b);
        opposite.negate();
        return CsgDyadic.add(a, opposite, result);
    }

    public static multiply(a: CsgDyadic, b: CsgDyadic, result: CsgDyadic): boolean {
        const candidate = new CsgDyadic();
        if (!a.sign || !b.sign) {
            result.assign(candidate);
            return true;
        }
        if (a.length + b.length - 1 > CsgDyadic.CAPACITY) return false;
        for (let i = 0; i < a.length; ++i) {
            let carry = 0n;
            for (let j = 0; j < b.length; ++j) {
                const k = i + j;
                if (k >= CsgDyadic.CAPACITY) return false;
                const value = BigInt(a.limbs[i]!) * BigInt(b.limbs[j]!) + BigInt(candidate.limbs[k]!) + carry;
                candidate.limbs[k] = Number(value & 0xffffffffn);
                carry = value >> 32n;
            }
            let k = i + b.length;
            while (carry) {
                if (k >= CsgDyadic.CAPACITY) return false;
                const value = BigInt(candidate.limbs[k]!) + carry;
                candidate.limbs[k] = Number(value & 0xffffffffn);
                carry = value >> 32n;
                ++k;
            }
        }
        const exp = a.exponent + b.exponent;
        if (exp < INT_MIN || exp > INT_MAX) return false;
        candidate.exponent = exp;
        candidate.sign = a.sign * b.sign;
        candidate.length = a.length + b.length;
        if (candidate.length > CsgDyadic.CAPACITY) candidate.length = CsgDyadic.CAPACITY;
        if (!candidate.normalize()) return false;
        result.assign(candidate);
        return true;
    }

    public getSign(): number {
        return this.sign;
    }

    public equals(other: CsgDyadic): boolean {
        if (this.sign !== other.sign || this.exponent !== other.exponent || this.length !== other.length) return false;
        for (let i = 0; i < this.length; ++i) if (this.limbs[i] !== other.limbs[i]) return false;
        return true;
    }

    public negate(): void {
        this.sign = -this.sign;
    }
}
