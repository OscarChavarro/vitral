import { WebSocket, type Listener as WebSocketListener } from "../../../../java/net/http/WebSocket.js";
import { Quaterniond } from "../../common/linealAlgebra/Quaterniond.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { TangibleInterfaceEvent } from "./TangibleInterfaceEvent.js";
import type { TangibleInterfaceListener } from "./TangibleInterfaceListener.js";

type Pose = { readonly label: string; readonly position: readonly number[]; readonly quaternion: readonly number[] };

/** WebSocket frame listener; exposed as the TypeScript counterpart of Java's nested class. */
export class FrameListener implements Partial<WebSocketListener> {
    public constructor(
        private readonly onMessage: (message: string) => void,
        private readonly onFailure: (event: Event) => void,
    ) {}
    public onText(_socket: WebSocket, data: string): void {
        this.onMessage(data);
    }
    public onError(_socket: WebSocket, event: Event): void {
        this.onFailure(event);
    }
}

/** Browser WebSocket client for marker-tracking pose streams. */
export class TangibleInterfaceNetworkClient {
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
    /** Starts asynchronous WebSocket connection setup; browser WebSocket replaces Java's connection thread. */
    public run(): void {
        try {
            this.webSocket = new WebSocket(
                this.serviceUrl,
                new FrameListener(
                    (message) => this.processMessage(message),
                    (event) => this.reportError(event),
                ),
            );
        } catch (error) {
            console.log(`Tangible interface server not found at ${this.serviceUrl}: ${this.errorMessage(error)}`);
        }
    }
    public disconnect(): void {
        this.webSocket?.sendClose(1000, "Client disconnect");
        this.webSocket = null;
    }

    private processMessage(message: string): void {
        let value: unknown;
        try {
            value = JSON.parse(message);
        } catch {
            return;
        }
        const groups = Array.isArray(value) ? value : [value];
        for (const group of groups) {
            const event = this.parseEvent(group);
            if (event !== null) this.notifyListeners(event);
        }
    }
    private parseEvent(value: unknown): TangibleInterfaceEvent | null {
        if (!this.isPose(value)) return null;
        return new TangibleInterfaceEvent(
            value.label,
            new Vector3Dd(value.position[0]!, value.position[1]!, value.position[2]!),
            new Quaterniond(
                new Vector3Dd(value.quaternion[1]!, value.quaternion[2]!, value.quaternion[3]!),
                value.quaternion[0]!,
            ),
        );
    }
    private isPose(value: unknown): value is Pose {
        if (value === null || typeof value !== "object") return false;
        const candidate = value as { label?: unknown; position?: unknown; quaternion?: unknown };
        return (
            typeof candidate.label === "string" &&
            this.isNumbers(candidate.position, 3) &&
            this.isNumbers(candidate.quaternion, 4)
        );
    }
    private isNumbers(value: unknown, count: number): value is readonly number[] {
        return (
            Array.isArray(value) &&
            value.length === count &&
            value.every((item) => typeof item === "number" && Number.isFinite(item))
        );
    }
    private notifyListeners(event: TangibleInterfaceEvent): void {
        for (const listener of [...this.listeners]) listener.tangibleInterfaceEventReceived(event);
    }
    private reportError(event: Event): void {
        console.log(`Tangible interface connection error on ${this.serviceUrl}: ${event.type}`);
    }
    private errorMessage(error: unknown): string {
        return error instanceof Error ? error.message : String(error);
    }
}
