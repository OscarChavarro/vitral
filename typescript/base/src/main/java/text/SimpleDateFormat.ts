import { Date } from "../util/Date.js";
import { IllegalArgumentException } from "../lang/IllegalArgumentException.js";

/** Locale-neutral formatter for the Java date tokens used by Vitral. */
export class SimpleDateFormat {
    public constructor(private readonly pattern: string) {}
    public format(date: Date | globalThis.Date): string {
        const value = date instanceof Date ? new globalThis.Date(date.getTime()) : date;
        const part = (n: number, width: number): string => globalThis.String(n).padStart(width, "0");
        const replacements: Record<string, string> = {
            yyyy: part(value.getFullYear(), 4),
            yy: part(value.getFullYear() % 100, 2),
            MM: part(value.getMonth() + 1, 2),
            dd: part(value.getDate(), 2),
            HH: part(value.getHours(), 2),
            mm: part(value.getMinutes(), 2),
            ss: part(value.getSeconds(), 2),
            SSS: part(value.getMilliseconds(), 3),
        };
        return this.pattern.replace(/yyyy|SSS|yy|MM|dd|HH|mm|ss/g, (token) => replacements[token] ?? token);
    }
    public parse(text: string): Date {
        const time = globalThis.Date.parse(text);
        if (Number.isNaN(time)) throw new IllegalArgumentException(`Unparseable date: \"${text}\"`);
        return new Date(time);
    }
}
