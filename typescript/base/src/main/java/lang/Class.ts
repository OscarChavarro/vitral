export class Class<T> {
  public constructor(private readonly constructorValue: abstract new (...args: never[]) => T, private readonly nameValue = constructorValue.name) {}
  public getName(): string { return this.nameValue; }
  public getSimpleName(): string { return this.nameValue.split(".").at(-1) ?? this.nameValue; }
  public isInstance(value: unknown): value is T { return value instanceof this.constructorValue; }
  public cast(value: unknown): T { if (!this.isInstance(value)) throw new TypeError(`Cannot cast value to ${this.nameValue}`); return value; }
}
