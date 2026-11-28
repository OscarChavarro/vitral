/**
File suffix policy shared by dialog hosts, independent of file APIs.

Java's `toLowerCase(Locale.ROOT)` is the locale-independent
`String.prototype.toLowerCase()`.
*/
export class DialogFileFilter {
    private readonly suffix: string;
    private readonly descriptionText: string;
    public constructor(suffix: string, description: string) {
        this.suffix = suffix.toLowerCase();
        this.descriptionText = description;
    }
    public accepts(path: string, directory: boolean): boolean {
        if (directory) return true;
        const dot: number = path.lastIndexOf(".");
        return dot > 0 && dot < path.length - 1 && this.suffix === path.substring(dot + 1).toLowerCase();
    }
    public description(): string {
        return this.descriptionText + " (*." + this.suffix + ")";
    }
}
