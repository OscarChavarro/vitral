import { Double } from "../../../../java/lang/Double.js";
import { VSDK } from "../../common/VSDK.js";
import { Logger } from "../../common/logging/Logger.js";

/**
Parsed form of one control specification string, as stored in
`Entity.getControlSpecifications()`. The string format is
"type;name;valid interval", for example "double;radius;(0, INFINITE)".

The name implies a pair of accessors on the entity: for "radius", the methods
`getRadius()` and `setRadius(value)`. In the interval, "[" and "]" denote
inclusive limits, while "(" and ")" denote exclusive limits. The words
"INFINITE" and "-INFINITE" denote unbounded limits. The interval part may be
omitted, meaning that any value is valid.
*/
export class ControlSpecification {
    public static readonly INFINITE = "INFINITE";

    private constructor(
        private readonly type: string,
        private readonly name: string,
        private readonly intervalText: string,
        private readonly lowerBound: number,
        private readonly upperBound: number,
        private readonly lowerInclusive: boolean,
        private readonly upperInclusive: boolean,
    ) {}

    /**
    Parses a control specification string.
    @param specification string in "type;name;valid interval" format
    @return the parsed specification, or null if the string is malformed
    */
    public static parse(specification: string | null): ControlSpecification | null {
        if (specification === null) {
            ControlSpecification.reportMalformed(specification, "null specification");
            return null;
        }

        // Java's `String.split` drops the trailing empty strings
        const parts: string[] = ControlSpecification.javaSplit(specification, ";");
        if (parts.length < 2 || parts.length > 3) {
            ControlSpecification.reportMalformed(specification, 'expected "type;name" or "type;name;interval"');
            return null;
        }

        const type: string = parts[0]!.trim();
        const name: string = parts[1]!.trim();
        if (type.length === 0 || name.length === 0) {
            ControlSpecification.reportMalformed(specification, "empty type or name");
            return null;
        }

        if (parts.length === 2 || parts[2]!.trim().length === 0) {
            return new ControlSpecification(type, name, "", Number.NEGATIVE_INFINITY, Number.POSITIVE_INFINITY, false, false);
        }

        const interval: string = parts[2]!.trim();
        const open: string = interval.charAt(0);
        const close: string = interval.charAt(interval.length - 1);
        if ((open !== "[" && open !== "(") || (close !== "]" && close !== ")")) {
            ControlSpecification.reportMalformed(specification, "interval must start with '[' or '(' and end with ']' or ')'");
            return null;
        }

        const limits: string[] = ControlSpecification.javaSplit(interval.substring(1, interval.length - 1), ",");
        if (limits.length !== 2) {
            ControlSpecification.reportMalformed(specification, "interval must have two limits separated by ','");
            return null;
        }

        let lower: number;
        let upper: number;
        try {
            lower = ControlSpecification.parseLimit(limits[0]!.trim());
            upper = ControlSpecification.parseLimit(limits[1]!.trim());
        } catch {
            ControlSpecification.reportMalformed(specification, "invalid interval limit");
            return null;
        }
        if (lower > upper) {
            ControlSpecification.reportMalformed(specification, "lower limit is greater than upper limit");
            return null;
        }

        return new ControlSpecification(type, name, interval, lower, upper, open === "[", close === "]");
    }

    /** Java's `String.split(regex)` for a literal one character separator. */
    private static javaSplit(text: string, separator: string): string[] {
        const parts: string[] = text.split(separator);

        while (parts.length > 1 && parts[parts.length - 1] === "") {
            parts.pop();
        }
        return parts;
    }

    private static parseLimit(limit: string): number {
        if (limit === ControlSpecification.INFINITE || limit === "+" + ControlSpecification.INFINITE) {
            return Number.POSITIVE_INFINITY;
        }
        if (limit === "-" + ControlSpecification.INFINITE) {
            return Number.NEGATIVE_INFINITY;
        }
        return Double.parseDouble(limit);
    }

    private static reportMalformed(specification: string | null, reason: string): void {
        Logger.reportMessage(
            null,
            VSDK.WARNING,
            "ControlSpecification.parse",
            'Ignoring malformed control specification "' + specification + '": ' + reason +
                '. Expected format is "type;name;interval", for example "double;radius;(0, INFINITE)".',
        );
    }

    /**
    @return the type name, for example "double"
    */
    public getType(): string {
        return this.type;
    }

    /**
    @return the attribute name, for example "radius"
    */
    public getName(): string {
        return this.name;
    }

    /**
    @return the attribute name with its first letter in upper case, as used
    in accessor names and GUI labels, for example "Radius"
    */
    public getLabel(): string {
        return this.name.substring(0, 1).toUpperCase() + this.name.substring(1);
    }

    /**
    @return the interval as written in the specification, or an empty string
    when the value is unbounded
    */
    public getIntervalText(): string {
        return this.intervalText;
    }

    /**
    @param value value to test
    @return true if the value lies inside the valid interval
    */
    public contains(value: number): boolean {
        if (Number.isNaN(value)) {
            return false;
        }
        const aboveLower: boolean = this.lowerInclusive ? value >= this.lowerBound : value > this.lowerBound;
        const belowUpper: boolean = this.upperInclusive ? value <= this.upperBound : value < this.upperBound;
        return aboveLower && belowUpper;
    }
}
