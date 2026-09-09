import { CooperativeFiberScheduler, type CooperativeFiber } from "@vitral/base";

export function createRandomValues(count: number): Int32Array {
  if (!Number.isInteger(count) || count < 0) throw new RangeError("count must be a non-negative integer");
  const values = new Int32Array(count);
  for (let i = 0; i < values.length; i++) values[i] = Math.floor(Math.random() * 100) + 1;
  return values;
}

function* sumFiber(values: Int32Array, start: number, end: number, yieldEvery = 65_536): CooperativeFiber<number> {
  let total = 0;
  for (let i = start; i < end; i++) {
    total += values[i] ?? 0;
    if ((i - start + 1) % yieldEvery === 0) yield;
  }
  return total;
}

/**
 * M is selected from hardwareConcurrency to keep this example comparable to
 * the workers sample. These are cooperative fibers on one event loop, not M
 * CPU threads; use the worker example when CPU parallelism is required.
 */
export async function sumWithFibers(values: Int32Array, fibers = navigator.hardwareConcurrency || 1): Promise<number> {
  const count = Math.max(1, Math.min(fibers, values.length || 1));
  const tasks = Array.from({ length: count }, (_, index) => sumFiber(values, Math.floor(values.length * index / count), Math.floor(values.length * (index + 1) / count)));
  return (await new CooperativeFiberScheduler().run(tasks)).reduce((total, partial) => total + partial, 0);
}

const count = Number(new URLSearchParams(window.location.search).get("n") ?? "10000000");
const values = createRandomValues(count);
const startedAt = performance.now();
sumWithFibers(values).then((sum) => console.log({ count, fibers: navigator.hardwareConcurrency || 1, sum, elapsedMilliseconds: performance.now() - startedAt, parallel: false }));
