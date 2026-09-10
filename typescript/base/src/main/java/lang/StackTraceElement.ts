export class StackTraceElement {
    public constructor(
        private readonly className: string,
        private readonly methodName: string,
        private readonly fileName?: string,
        private readonly lineNumber = -1,
    ) {}
    public getClassName(): string {
        return this.className;
    }
    public getMethodName(): string {
        return this.methodName;
    }
    public getFileName(): string | undefined {
        return this.fileName;
    }
    public getLineNumber(): number {
        return this.lineNumber;
    }
    public isNativeMethod(): boolean {
        return this.lineNumber === -2;
    }
    public toString(): string {
        return `${this.className}.${this.methodName}(${this.fileName === undefined ? "Unknown Source" : this.lineNumber < 0 ? this.fileName : `${this.fileName}:${this.lineNumber}`})`;
    }
}
