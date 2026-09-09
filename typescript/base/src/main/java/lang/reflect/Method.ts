export class Method<TTarget, TResult> {
  public constructor(private readonly nameValue: string, private readonly invokeValue: (target: TTarget, ...args: unknown[]) => TResult) {}
  public getName(): string { return this.nameValue; }
  public invoke(target: TTarget, ...args: unknown[]): TResult { return this.invokeValue(target, ...args); }
}
