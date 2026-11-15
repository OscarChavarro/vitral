// GoogleTest assertion semantics for ports of the cpp gtest suites. Every
// assertion stops the test when it fails, so EXPECT_* behaves like ASSERT_*.
import { expect } from "vitest";

const MAXIMUM_ULPS = 4n;

function biased(value: number): bigint {
    const view = new DataView(new ArrayBuffer(8));
    view.setFloat64(0, value);
    const bits = view.getBigUint64(0);
    const sign = 1n << 63n;
    return (bits & sign) !== 0n ? (~bits + 1n) & 0xffffffffffffffffn : bits | sign;
}

/** True when `a` and `b` are within 4 ULPs (GoogleTest `AlmostEquals`). */
export function almostEquals(a: number, b: number): boolean {
    if (Number.isNaN(a) || Number.isNaN(b)) return false;
    const ba = biased(a);
    const bb = biased(b);
    const distance = ba >= bb ? ba - bb : bb - ba;
    return distance <= MAXIMUM_ULPS;
}

/** `EXPECT_DOUBLE_EQ(expected, actual)`. */
export function expectDoubleEq(expected: number, actual: number, message?: string): void {
    expect(almostEquals(expected, actual), `${message ?? ""} expected ${expected} but was ${actual}`).toBe(true);
}

/** `EXPECT_NEAR(expected, actual, tolerance)`. */
export function expectNear(expected: number, actual: number, tolerance: number, message?: string): void {
    expect(
        Math.abs(expected - actual) <= tolerance,
        `${message ?? ""} expected ${expected} within ${tolerance} but was ${actual}`,
    ).toBe(true);
}

/** `operator==` of the cpp test: numbers by value (so 0 == -0), other values by identity. */
function gtestEquals(a: unknown, b: unknown): boolean {
    return a === b;
}

/** `EXPECT_EQ(expected, actual)`. */
export function expectEq(expected: unknown, actual: unknown, message?: string): void {
    expect(
        gtestEquals(expected, actual),
        `${message ?? ""} expected ${String(expected)} but was ${String(actual)}`,
    ).toBe(true);
}

/** `EXPECT_NE(a, b)`. */
export function expectNe(a: unknown, b: unknown, message?: string): void {
    expect(!gtestEquals(a, b), `${message ?? ""} ${String(a)} != ${String(b)}`).toBe(true);
}

export function expectTrue(value: boolean, message?: string): void {
    expect(value, message).toBe(true);
}

export function expectFalse(value: boolean, message?: string): void {
    expect(value, message).toBe(false);
}

export function expectLt(a: number, b: number, message?: string): void {
    expect(a < b, `${message ?? ""} ${a} < ${b}`).toBe(true);
}

export function expectLe(a: number, b: number, message?: string): void {
    expect(a <= b, `${message ?? ""} ${a} <= ${b}`).toBe(true);
}

export function expectGt(a: number, b: number, message?: string): void {
    expect(a > b, `${message ?? ""} ${a} > ${b}`).toBe(true);
}

export function expectGe(a: number, b: number, message?: string): void {
    expect(a >= b, `${message ?? ""} ${a} >= ${b}`).toBe(true);
}

/** `EXPECT_THROW(statement, type)`. */
export function expectThrow(
    type: abstract new (...args: never[]) => unknown,
    statement: () => unknown,
    message?: string,
): void {
    expect(statement, message).toThrow(type as unknown as new (...args: unknown[]) => Error);
}

/** `EXPECT_ANY_THROW(statement)`. */
export function expectAnyThrow(statement: () => unknown, message?: string): void {
    expect(statement, message).toThrow();
}

/** `EXPECT_NO_THROW(statement)`. */
export function expectNoThrow(statement: () => unknown, message?: string): void {
    expect(statement, message).not.toThrow();
}
