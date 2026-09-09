import { IndexOutOfBoundsException } from "../lang/IndexOutOfBoundsException.js";
import { IllegalArgumentException } from "../lang/IllegalArgumentException.js";
import { IllegalStateException } from "../lang/IllegalStateException.js";

export type ByteOrder = "BIG_ENDIAN" | "LITTLE_ENDIAN";

/**
 * Browser-safe implementation of the Java ByteBuffer state machine. Bytes are
 * kept in a Uint8Array, while single-byte reads expose Java's signed-byte
 * values (-128..127).
 */
export class ByteBuffer {
  private positionValue: number;
  private limitValue: number;
  private markValue = -1;
  private byteOrderValue: ByteOrder = "BIG_ENDIAN";

  private constructor(private readonly bytes: Uint8Array, position: number, limit: number) {
    this.positionValue = position;
    this.limitValue = limit;
  }

  public static allocate(capacity: number): ByteBuffer { return ByteBuffer.allocateChecked(capacity); }
  public static allocateDirect(capacity: number): ByteBuffer { return ByteBuffer.allocateChecked(capacity); }
  public static wrap(bytes: Uint8Array, offset = 0, length = bytes.length - offset): ByteBuffer {
    ByteBuffer.requireRange(bytes.length, offset, length);
    return new ByteBuffer(bytes, offset, offset + length);
  }

  public capacity(): number { return this.bytes.length; }
  public position(): number;
  public position(position: number): ByteBuffer;
  public position(position?: number): number | ByteBuffer {
    if (position === undefined) return this.positionValue;
    if (!Number.isInteger(position) || position < 0 || position > this.limitValue) throw new IllegalArgumentException("Invalid position");
    this.positionValue = position;
    if (this.markValue > position) this.markValue = -1;
    return this;
  }

  public limit(): number;
  public limit(limit: number): ByteBuffer;
  public limit(limit?: number): number | ByteBuffer {
    if (limit === undefined) return this.limitValue;
    if (!Number.isInteger(limit) || limit < 0 || limit > this.capacity()) throw new IllegalArgumentException("Invalid limit");
    this.limitValue = limit;
    if (this.positionValue > limit) this.positionValue = limit;
    if (this.markValue > limit) this.markValue = -1;
    return this;
  }

  public remaining(): number { return this.limitValue - this.positionValue; }
  public hasRemaining(): boolean { return this.remaining() > 0; }
  public clear(): ByteBuffer { this.positionValue = 0; this.limitValue = this.capacity(); this.markValue = -1; return this; }
  public flip(): ByteBuffer { this.limitValue = this.positionValue; this.positionValue = 0; this.markValue = -1; return this; }
  public rewind(): ByteBuffer { this.positionValue = 0; this.markValue = -1; return this; }
  public mark(): ByteBuffer { this.markValue = this.positionValue; return this; }
  public reset(): ByteBuffer {
    if (this.markValue < 0) throw new IllegalStateException("Mark has not been set");
    this.positionValue = this.markValue;
    return this;
  }

  public order(): ByteOrder;
  public order(byteOrder: ByteOrder): ByteBuffer;
  public order(byteOrder?: ByteOrder): ByteOrder | ByteBuffer {
    if (byteOrder === undefined) return this.byteOrderValue;
    this.byteOrderValue = byteOrder;
    return this;
  }

  public get(): number;
  public get(index: number): number;
  public get(destination: Uint8Array, offset?: number, length?: number): ByteBuffer;
  public get(argument?: number | Uint8Array, offset = 0, length?: number): number | ByteBuffer {
    if (argument === undefined) return this.signed(this.bytes[this.take(1)] ?? 0);
    if (typeof argument === "number") return this.signed(this.bytes[this.at(argument, 1)] ?? 0);
    const count = length ?? argument.length - offset;
    ByteBuffer.requireRange(argument.length, offset, count);
    if (count > this.remaining()) throw new IndexOutOfBoundsException("Buffer underflow");
    argument.set(this.bytes.subarray(this.positionValue, this.positionValue + count), offset);
    this.positionValue += count;
    return this;
  }

  public put(value: number): ByteBuffer;
  public put(index: number, value: number): ByteBuffer;
  public put(source: Uint8Array, offset?: number, length?: number): ByteBuffer;
  public put(first: number | Uint8Array, second?: number, third?: number): ByteBuffer {
    if (first instanceof Uint8Array) {
      const offset = second ?? 0;
      const length = third ?? first.length - offset;
      ByteBuffer.requireRange(first.length, offset, length);
      if (length > this.remaining()) throw new IndexOutOfBoundsException("Buffer overflow");
      this.bytes.set(first.subarray(offset, offset + length), this.positionValue);
      this.positionValue += length;
      return this;
    }
    if (second === undefined) this.bytes[this.take(1)] = first & 0xff;
    else this.bytes[this.at(first, 1)] = second & 0xff;
    return this;
  }

  public getShort(index?: number): number { return this.view().getInt16(index === undefined ? this.take(2) : this.at(index, 2), this.littleEndian()); }
  public putShort(value: number): ByteBuffer;
  public putShort(index: number, value: number): ByteBuffer;
  public putShort(first: number, second?: number): ByteBuffer { this.view().setInt16(second === undefined ? this.take(2) : this.at(first, 2), second ?? first, this.littleEndian()); return this; }
  public getInt(index?: number): number { return this.view().getInt32(index === undefined ? this.take(4) : this.at(index, 4), this.littleEndian()); }
  public putInt(value: number): ByteBuffer;
  public putInt(index: number, value: number): ByteBuffer;
  public putInt(first: number, second?: number): ByteBuffer { this.view().setInt32(second === undefined ? this.take(4) : this.at(first, 4), second ?? first, this.littleEndian()); return this; }
  public getLong(index?: number): bigint { return this.view().getBigInt64(index === undefined ? this.take(8) : this.at(index, 8), this.littleEndian()); }
  public putLong(value: bigint): ByteBuffer;
  public putLong(index: number, value: bigint): ByteBuffer;
  public putLong(first: number | bigint, second?: bigint): ByteBuffer {
    const index = second === undefined ? this.take(8) : this.at(first as number, 8);
    this.view().setBigInt64(index, second ?? first as bigint, this.littleEndian());
    return this;
  }
  public getFloat(index?: number): number { return this.view().getFloat32(index === undefined ? this.take(4) : this.at(index, 4), this.littleEndian()); }
  public putFloat(value: number): ByteBuffer;
  public putFloat(index: number, value: number): ByteBuffer;
  public putFloat(first: number, second?: number): ByteBuffer { this.view().setFloat32(second === undefined ? this.take(4) : this.at(first, 4), second ?? first, this.littleEndian()); return this; }
  public getDouble(index?: number): number { return this.view().getFloat64(index === undefined ? this.take(8) : this.at(index, 8), this.littleEndian()); }
  public putDouble(value: number): ByteBuffer;
  public putDouble(index: number, value: number): ByteBuffer;
  public putDouble(first: number, second?: number): ByteBuffer { this.view().setFloat64(second === undefined ? this.take(8) : this.at(first, 8), second ?? first, this.littleEndian()); return this; }

  public duplicate(): ByteBuffer { const copy = new ByteBuffer(this.bytes, this.positionValue, this.limitValue); copy.markValue = this.markValue; copy.byteOrderValue = this.byteOrderValue; return copy; }
  public slice(): ByteBuffer { const copy = new ByteBuffer(this.bytes.subarray(this.positionValue, this.limitValue), 0, this.remaining()); copy.byteOrderValue = this.byteOrderValue; return copy; }
  public compact(): ByteBuffer { this.bytes.copyWithin(0, this.positionValue, this.limitValue); const remaining = this.remaining(); this.positionValue = remaining; this.limitValue = this.capacity(); this.markValue = -1; return this; }
  public hasArray(): boolean { return true; }
  public array(): Uint8Array { return this.bytes; }
  public arrayOffset(): number { return this.bytes.byteOffset; }

  private static allocateChecked(capacity: number): ByteBuffer {
    if (!Number.isInteger(capacity) || capacity < 0) throw new IllegalArgumentException("Capacity must not be negative");
    return new ByteBuffer(new Uint8Array(capacity), 0, capacity);
  }
  private static requireRange(capacity: number, offset: number, length: number): void {
    if (!Number.isInteger(offset) || !Number.isInteger(length) || offset < 0 || length < 0 || offset > capacity - length) throw new IndexOutOfBoundsException("Invalid byte range");
  }
  private signed(byte: number): number { return byte >= 128 ? byte - 256 : byte; }
  private take(width: number): number { if (width > this.remaining()) throw new IndexOutOfBoundsException("Buffer underflow or overflow"); const index = this.positionValue; this.positionValue += width; return index; }
  private at(index: number, width: number): number { if (!Number.isInteger(index) || index < 0 || index > this.limitValue - width) throw new IndexOutOfBoundsException("Index outside buffer limit"); return index; }
  private view(): DataView { return new DataView(this.bytes.buffer, this.bytes.byteOffset, this.bytes.byteLength); }
  private littleEndian(): boolean { return this.byteOrderValue === "LITTLE_ENDIAN"; }
}
