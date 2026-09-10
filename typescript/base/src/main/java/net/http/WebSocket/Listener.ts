export interface Listener {
    onOpen?(): void;
    onText?(data: string): void;
    onClose?(code: number, reason: string): void;
    onError?(event: Event): void;
}
