import { ProcessingElement } from "./ProcessingElement.js";
import { Complex } from "../common/linealAlgebra/Complex.js";
export class SignalProcessing extends ProcessingElement {
    public static fft(x: Complex[]): Complex[] {
        const n = x.length;
        if (n === 1) return [x[0]!];
        if (n % 2 !== 0) throw new Error("N is not a power of 2");
        const q = this.fft(x.filter((_, i) => i % 2 === 0)),
            r = this.fft(x.filter((_, i) => i % 2 === 1)),
            out: Complex[] = Array(n);
        for (let k = 0; k < n / 2; k++) {
            const w = new Complex(Math.cos((-2 * k * Math.PI) / n), Math.sin((-2 * k * Math.PI) / n));
            out[k] = q[k]!.plus(w.times(r[k]!));
            out[k + n / 2] = q[k]!.minus(w.times(r[k]!));
        }
        return out;
    }
    public static ifft(x: Complex[]): Complex[] {
        const y = this.fft(x.map((v) => v.conjugate())).map((v) => v.conjugate());
        return y.map((v) => v.times(1 / x.length));
    }
    public static circularConvolve(x: Complex[], y: Complex[]): Complex[] {
        if (x.length !== y.length) throw new Error("Dimensions don't agree");
        const a = this.fft(x),
            b = this.fft(y);
        return this.ifft(a.map((v, i) => v.times(b[i]!)));
    }
    public static linearConvolve(x: Complex[], y: Complex[]): Complex[] {
        const z = new Complex(0, 0),
            a = [...x, ...Array.from({ length: x.length }, () => z)],
            b = [...y, ...Array.from({ length: y.length }, () => z)];
        return this.circularConvolve(a, b);
    }
}
