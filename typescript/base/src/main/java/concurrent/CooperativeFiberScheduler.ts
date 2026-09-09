/** A cooperative fiber is a generator that yields at safe scheduling points. */
export type CooperativeFiber<TResult> = Generator<void, TResult, void>;

/**
 * Runs CPU work cooperatively on one JavaScript event loop. This is deliberately
 * not a worker replacement: it keeps the UI responsive but cannot execute on
 * more than one CPU core. Use BrowserWorkerExecutor for parallel execution.
 */
export class CooperativeFiberScheduler {
  public constructor(private readonly yieldToHost: () => Promise<void> = CooperativeFiberScheduler.defaultYield) {}

  public async run<TResult>(fibers: readonly CooperativeFiber<TResult>[]): Promise<TResult[]> {
    const pending = fibers.map((fiber, index) => ({ fiber, index }));
    const results = new Array<TResult>(fibers.length);
    while (pending.length > 0) {
      for (let i = pending.length - 1; i >= 0; i--) {
        const current = pending[i];
        if (current === undefined) continue;
        const step = current.fiber.next();
        if (!step.done) continue;
        results[current.index] = step.value;
        pending.splice(i, 1);
      }
      if (pending.length > 0) await this.yieldToHost();
    }
    return results;
  }

  private static defaultYield(): Promise<void> {
    return new Promise((resolve) => globalThis.setTimeout(resolve, 0));
  }
}
