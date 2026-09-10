import { Entity } from "../Entity.js";

/** Visitor lifecycle contract used by N-ary tree clients. */
export abstract class NAryTreeTraverser extends Entity {
    public abstract start(): void;
    public abstract end(): void;
    public abstract visit(element: unknown, level: number): void;
    protected formatHeader(level: number): string {
        if (level === 2) return "  - ";
        if (level === 3) return "      . ";
        return level >= 4 ? `        ${"  ".repeat(level - 4)}` : "";
    }
}
