/** Browser gzip adapter. Streaming decompression is asynchronous by Web API design. */
export class GZIPInputStream {
  public static async decompress(compressed: ReadableStream<Uint8Array>): Promise<ReadableStream<Uint8Array>> {
    if (typeof DecompressionStream === "undefined") throw new Error("This runtime does not provide DecompressionStream");
    const transform = new DecompressionStream("gzip") as unknown as TransformStream<Uint8Array, Uint8Array>;
    return compressed.pipeThrough(transform);
  }
}
