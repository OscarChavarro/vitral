import { IllegalArgumentException } from "../lang/IllegalArgumentException.js";
import { FieldPosition } from "./FieldPosition.js";

/** Deliberate, locale-neutral subset of java.text.DecimalFormat patterns. */
export class DecimalFormat {
  private readonly minimumFractionDigits: number;
  private readonly maximumFractionDigits: number;
  private readonly grouping: boolean;

  public constructor(pattern = "#0.###") {
    const decimal = pattern.indexOf(".");
    const whole = decimal < 0 ? pattern : pattern.slice(0, decimal);
    const fraction = decimal < 0 ? "" : pattern.slice(decimal + 1);
    if (!/^[#,0]+$/.test(whole) || !/^[#0]*$/.test(fraction) || pattern.indexOf(".", decimal + 1) >= 0) {
      throw new IllegalArgumentException(`Unsupported DecimalFormat pattern: ${pattern}`);
    }
    this.minimumFractionDigits = [...fraction].filter((c) => c === "0").length;
    this.maximumFractionDigits = fraction.length;
    this.grouping = whole.includes(",");
  }

  public format(value: number | bigint, destination?: { append(text: string): unknown }, position?: FieldPosition): string | { append(text: string): unknown } {
    const text = new Intl.NumberFormat("en-US", {
      useGrouping: this.grouping,
      minimumFractionDigits: this.minimumFractionDigits,
      maximumFractionDigits: this.maximumFractionDigits
    }).format(value);
    if (position !== undefined) { position.setBeginIndex(0); position.setEndIndex(text.length); }
    if (destination === undefined) return text;
    destination.append(text);
    return destination;
  }
}
