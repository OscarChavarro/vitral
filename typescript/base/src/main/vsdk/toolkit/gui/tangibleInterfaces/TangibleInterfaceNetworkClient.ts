import { WebSocket, type Listener as WebSocketListener } from "../../../../java/net/http/WebSocket.js";
import { Double } from "../../../../java/lang/Double.js";
import { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { TangibleInterfaceEvent } from "./TangibleInterfaceEvent.js";
import type { TangibleInterfaceListener } from "./TangibleInterfaceListener.js";

/** WebSocket frame listener; exposed as the TypeScript counterpart of Java's nested class. */
export class FrameListener implements Partial<WebSocketListener> {
    private incomingMessage = "";

    public constructor(
        private readonly onMessage: (message: string) => void,
        private readonly onFailure: (event: Event) => void,
        private readonly onConnected?: () => void,
        private readonly onDisconnected?: (reason: string) => void,
    ) {}
    onOpen(_socket: WebSocket): void {
        this.onConnected?.();
    }
    public onText(_socket: WebSocket, data: string, last = true): void {
        this.incomingMessage += data;
        if (last) {
            const message = this.incomingMessage;
            this.incomingMessage = "";
            this.onMessage(message);
        }
    }
    public onError(_socket: WebSocket, event: Event): void {
        this.onFailure(event);
    }
    public onClose(_socket: WebSocket, _code: number, reason: string): void {
        this.onDisconnected?.(reason);
    }
}

/** Browser WebSocket client for marker-tracking pose streams. */
export class TangibleInterfaceNetworkClient {
    private static readonly GROUP_PATTERN = /\{[^{}]*\}/g;
    private static readonly LABEL_PATTERN = /"label"[ \t\n\v\f\r]*:[ \t\n\v\f\r]*"([^"]*)"/;
    private static readonly POSITION_PATTERN = /"position"[ \t\n\v\f\r]*:[ \t\n\v\f\r]*\[([^\]]*)\]/;
    private static readonly QUATERNION_PATTERN = /"quaternion"[ \t\n\v\f\r]*:[ \t\n\v\f\r]*\[([^\]]*)\]/;
    public static readonly FrameListener = FrameListener;
    private readonly listeners: TangibleInterfaceListener[] = [];
    private webSocket: WebSocket | null = null;
    public constructor(private readonly serviceUrl: string) {}

    public addListener(listener: TangibleInterfaceListener | null): void {
        if (listener !== null) this.listeners.push(listener);
    }
    public removeListener(listener: TangibleInterfaceListener): void {
        const index = this.listeners.indexOf(listener);
        if (index >= 0) this.listeners.splice(index, 1);
    }
    /**
     * Starts WebSocket connection setup. The optional callback mirrors Java's
     * handshake notification and runs on the browser event loop.
     */
    public run(connectionListener?: (connected: boolean) => void): void {
        this.disconnect();
        this.connectAndListen(connectionListener);
    }
    public disconnect(): void {
        const socket = this.webSocket;
        this.webSocket = null;
        socket?.sendClose(1000, "Client disconnect");
    }

    private connectAndListen(connectionListener?: (connected: boolean) => void): void {
        let handshakePending = true;
        const failHandshake = (message: string): void => {
            if (!handshakePending) return;
            handshakePending = false;
            console.log(`Tangible interface server not found at ${this.serviceUrl}: ${message}`);
            connectionListener?.(false);
        };
        const reportConnectionError = (message: string): void => {
            console.log(`Tangible interface connection error on ${this.serviceUrl}: ${message}`);
        };
        try {
            const socket = new WebSocket(
                this.serviceUrl,
                new FrameListener(
                    (message) => {
                        if (this.webSocket !== socket) return;
                        try {
                            this.processMessage(message);
                        } catch (error) {
                            reportConnectionError(error instanceof Error ? error.message : String(error));
                            if (this.webSocket === socket) {
                                this.webSocket = null;
                                socket.sendClose();
                            }
                        }
                    },
                    (event) => {
                        if (this.webSocket !== socket) return;
                        const message =
                            typeof ErrorEvent !== "undefined" && event instanceof ErrorEvent && event.message
                                ? event.message
                                : event.type;
                        if (handshakePending) failHandshake(message);
                        else reportConnectionError(message);
                    },
                    () => {
                        if (this.webSocket !== socket || !handshakePending) return;
                        handshakePending = false;
                        connectionListener?.(true);
                    },
                    (reason) => {
                        if (this.webSocket !== socket) return;
                        this.webSocket = null;
                        failHandshake(reason || "Connection closed");
                    },
                ),
            );
            this.webSocket = socket;
        } catch (error) {
            failHandshake(error instanceof Error ? error.message : String(error));
        }
    }

    private processMessage(message: string): void {
        for (const group of message.matchAll(TangibleInterfaceNetworkClient.GROUP_PATTERN)) {
            const event = this.parseEvent(group[0]);
            if (event !== null) this.notifyListeners(event);
        }
    }
    private parseEvent(group: string): TangibleInterfaceEvent | null {
        const label = this.extractString(TangibleInterfaceNetworkClient.LABEL_PATTERN, group);
        const position = this.extractNumbers(TangibleInterfaceNetworkClient.POSITION_PATTERN, group, 3);
        const rotation = this.extractNumbers(TangibleInterfaceNetworkClient.QUATERNION_PATTERN, group, 4);
        if (label === null || position === null || rotation === null) return null;
        return new TangibleInterfaceEvent(
            label,
            new Vector3Dd(position[0]!, position[1]!, position[2]!),
            new Quaterniond(new Vector3Dd(rotation[1]!, rotation[2]!, rotation[3]!), rotation[0]!),
        );
    }

    private extractString(pattern: RegExp, group: string): string | null {
        return pattern.exec(group)?.[1] ?? null;
    }

    private extractNumbers(pattern: RegExp, group: string, count: number): number[] | null {
        const matched = this.extractString(pattern, group);
        if (matched === null) return null;
        const fields = matched.split(",");
        // Java String.split drops trailing empty fields.
        while (fields.length > 1 && fields.at(-1) === "") fields.pop();
        if (fields.length !== count) return null;
        return fields.map((value) => Double.parseDouble(value));
    }
    private notifyListeners(event: TangibleInterfaceEvent): void {
        for (const listener of [...this.listeners]) listener.tangibleInterfaceEventReceived(event);
    }
}
