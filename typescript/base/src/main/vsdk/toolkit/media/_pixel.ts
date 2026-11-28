import { VSDK } from "../common/VSDK.js";
import { ArrayIndexOutOfBoundsException } from "../../../java/lang/ArrayIndexOutOfBoundsException.js";
import { IndexOutOfBoundsException } from "../../../java/lang/IndexOutOfBoundsException.js";
export const byte = (value: number): number => VSDK.unsigned8BitInteger2signedByte(Math.trunc(value));
export const unsigned = (value: number): number => VSDK.signedByte2unsignedInteger(value);

/**
Java's direct `ByteBuffer.get(int)` / `put(int, byte)` check: an index outside
the buffer throws an `IndexOutOfBoundsException` with no message (JDK 17),
where a typed array would silently drop the write or read `undefined`.
*/
export const checkBufferIndex = (index: number, length: number): void => {
    if (index < 0 || index >= length) {
        throw new IndexOutOfBoundsException();
    }
};

/**
Java's array access check: an index outside a `byte[]` / `float[]` throws an
`ArrayIndexOutOfBoundsException` with the JDK's message.
*/
export const checkArrayIndex = (index: number, length: number): void => {
    if (index < 0 || index >= length) {
        throw new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
    }
};
