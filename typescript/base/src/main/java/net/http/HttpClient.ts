import { CompletionStage } from "../../util/concurrent/CompletionStage.js";
export interface HttpRequest { readonly url: string | URL; readonly init?: RequestInit; }
export class HttpClient {
  public sendAsync(request: HttpRequest): CompletionStage<Response> { return new CompletionStage(globalThis.fetch(request.url, request.init)); }
  public static newHttpClient(): HttpClient { return new HttpClient(); }
  public static newBuilder(): { build(): HttpClient } { return { build: () => new HttpClient() }; }
}
