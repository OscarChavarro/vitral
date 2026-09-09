import { VSDK } from "../VSDK.js";

/** Centralized primitive and intersection counters. */
export class RenderingStatistics {
  private static readonly primitiveCount = new Int32Array(VSDK.PRIMITIVE_TYPE_COUNT);
  private static readonly intersectionCount = new Int32Array(VSDK.INTERSECTION_TYPE_COUNT);
  public static resetPrimitiveCounters(): void { this.primitiveCount.fill(0); }
  public static resetIntersectionCounters(): void { this.intersectionCount.fill(0); }
  public static accumulatePrimitiveCount(type: number, count: number): void { if (!Number.isInteger(type) || type < 0 || type >= this.primitiveCount.length) throw new RangeError(`Primitive type out of bounds: ${type}`); this.primitiveCount[type]! += count; }
}
