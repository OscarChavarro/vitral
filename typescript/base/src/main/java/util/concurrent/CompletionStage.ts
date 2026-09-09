/** Promise-backed Java CompletionStage compatibility surface. */
export class CompletionStage<T> {
  public constructor(private readonly promise: Promise<T>) {}
  public thenApply<U>(mapper: (value: T) => U | PromiseLike<U>): CompletionStage<U> { return new CompletionStage(this.promise.then(mapper)); }
  public thenAccept(consumer: (value: T) => void | PromiseLike<void>): CompletionStage<void> { return new CompletionStage(this.promise.then(consumer)); }
  public exceptionally(recover: (reason: unknown) => T | PromiseLike<T>): CompletionStage<T> { return new CompletionStage(this.promise.catch(recover)); }
  public toPromise(): Promise<T> { return this.promise; }
  public static completedFuture<T>(value: T): CompletionStage<T> { return new CompletionStage(Promise.resolve(value)); }
  public static failedFuture<T = never>(reason: unknown): CompletionStage<T> { return new CompletionStage(Promise.reject(reason)); }
}
