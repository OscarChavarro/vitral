import { FundamentalEntity } from "../FundamentalEntity.js";
import { VSDK } from "../VSDK.js";

/** Complex number with the same value-oriented operations as the Java source. */
export class Complex extends FundamentalEntity {
  public constructor(public r: number, public i: number) { super(); }
  public abs(): number { return Math.hypot(this.r, this.i); }
  public phase(): number { return Math.atan2(this.i, this.r); }
  public plus(b: Complex): Complex { return new Complex(this.r + b.r, this.i + b.i); }
  public minus(b: Complex): Complex { return new Complex(this.r - b.r, this.i - b.i); }
  public times(b: Complex): Complex;
  public times(alpha: number): Complex;
  public times(value: Complex | number): Complex {
    return typeof value === "number" ? new Complex(value * this.r, value * this.i) : new Complex(this.r * value.r - this.i * value.i, this.r * value.i + this.i * value.r);
  }
  public conjugate(): Complex { return new Complex(this.r, -this.i); }
  public reciprocal(): Complex { const scale = this.r * this.r + this.i * this.i; return new Complex(this.r / scale, -this.i / scale); }
  public divides(b: Complex): Complex { return this.times(b.reciprocal()); }
  public exp(): Complex { return new Complex(Math.exp(this.r) * Math.cos(this.i), Math.exp(this.r) * Math.sin(this.i)); }
  public sin(): Complex { return new Complex(Math.sin(this.r) * Math.cosh(this.i), Math.cos(this.r) * Math.sinh(this.i)); }
  public cos(): Complex { return new Complex(Math.cos(this.r) * Math.cosh(this.i), -Math.sin(this.r) * Math.sinh(this.i)); }
  public tan(): Complex { return this.sin().divides(this.cos()); }
  public static plus(a: Complex, b: Complex): Complex { return new Complex(a.r + b.r, a.i + b.i); }
  public override toString(): string { return this.i < 0 ? `${VSDK.formatDouble(this.r)} - ${VSDK.formatDouble(-this.i)}i` : `${VSDK.formatDouble(this.r)} + ${VSDK.formatDouble(this.i)}i`; }
}
