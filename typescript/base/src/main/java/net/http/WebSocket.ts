export interface Listener {
    onOpen(socket: WebSocket): void;
    onText(socket: WebSocket, data: string): void;
    onClose(socket: WebSocket, code: number, reason: string): void;
    onError(socket: WebSocket, event: Event): void;
}

export class WebSocket {
    private readonly socket: globalThis.WebSocket;

    public constructor(url: string, listener?: Partial<Listener>, protocols?: string | string[]) {
        this.socket =
            protocols === undefined ? new globalThis.WebSocket(url) : new globalThis.WebSocket(url, protocols);
        this.socket.addEventListener("open", () => listener?.onOpen?.(this));
        this.socket.addEventListener("message", (event) => listener?.onText?.(this, globalThis.String(event.data)));
        this.socket.addEventListener("close", (event) => listener?.onClose?.(this, event.code, event.reason));
        this.socket.addEventListener("error", (event) => listener?.onError?.(this, event));
    }

    public sendText(text: string): void {
        this.socket.send(text);
    }

    public sendBinary(data: ArrayBuffer | ArrayBufferView): void {
        if (data instanceof ArrayBuffer) {
            this.socket.send(data);
            return;
        }
        const bytes = new Uint8Array(data.byteLength);
        bytes.set(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));
        this.socket.send(bytes);
    }

    public sendClose(code?: number, reason?: string): void {
        this.socket.close(code, reason);
    }

    public isOutputClosed(): boolean {
        return this.socket.readyState >= globalThis.WebSocket.CLOSING;
    }
}
