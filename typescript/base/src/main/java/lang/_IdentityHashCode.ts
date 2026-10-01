/**
Java's `System.identityHashCode(Object)`. There is no object address on this
runtime, so identity codes are handed out lazily, one per object, from a
`WeakMap` (which does not keep the objects alive). It lives apart from
`System` because that class reaches Node streams, and this one is also used
by browser code.
*/
const identityHashCodes: WeakMap<object, number> = new WeakMap<object, number>();
let nextIdentityHashCode: number = 1;

/**
@param value an object, or null
@return a code that is the same for the same object and distinct for
distinct objects alive at the same time; 0 for null, as in Java
*/
export function identityHashCode(value: object | null): number {
    if (value === null) {
        return 0;
    }
    let code: number | undefined = identityHashCodes.get(value);
    if (code === undefined) {
        code = nextIdentityHashCode;
        nextIdentityHashCode++;
        identityHashCodes.set(value, code);
    }
    return code;
}
