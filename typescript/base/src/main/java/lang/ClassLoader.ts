import { Class } from "./Class.js";
export class ClassLoader {
  public constructor(private readonly registry = new Map<string, Class<unknown>>()) {}
  public register(name: string, type: Class<unknown>): void { this.registry.set(name, type); }
  public loadClass(name: string): Class<unknown> { const type = this.registry.get(name); if (type === undefined) throw new Error(`Class not found: ${name}`); return type; }
  public static getSystemClassLoader(): ClassLoader { return new ClassLoader(); }
}
