import { Matcher } from "./Matcher.js";
export class Pattern {
    public static readonly CASE_INSENSITIVE = 2;
    public static readonly MULTILINE = 8;
    public static readonly DOTALL = 32;
    private constructor(
        private readonly expression: string,
        private readonly flags: string,
    ) {}
    public static compile(expression: string, flags = 0): Pattern {
        let nativeFlags = "u";
        if ((flags & Pattern.CASE_INSENSITIVE) !== 0) nativeFlags += "i";
        if ((flags & Pattern.MULTILINE) !== 0) nativeFlags += "m";
        if ((flags & Pattern.DOTALL) !== 0) nativeFlags += "s";
        new RegExp(expression, nativeFlags);
        return new Pattern(expression, nativeFlags);
    }
    public matcher(input: string): Matcher {
        return new Matcher(this.expression, this.flags, input);
    }
    public pattern(): string {
        return this.expression;
    }
}
