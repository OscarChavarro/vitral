/** Context-local storage. Async worker code should pass context explicitly. */
export class ThreadLocal<T> {
  private value: T | undefined;
  public constructor(private readonly initial?: () => T) {}
  public get(): T | undefined { if (this.value === undefined && this.initial !== undefined) this.value = this.initial(); return this.value; }
  public set(value: T): void { this.value = value; }
  public remove(): void { this.value = undefined; }
}
