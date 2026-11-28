import { RuntimeException } from "../../../java/lang/RuntimeException.js";
import { Complex } from "../common/linealAlgebra/Complex.js";
import { ProcessingElement } from "./ProcessingElement.js";

//= References:                                                             =
//= [.wPRIN2007] Princeton University, "Introduction to programming in      =
//=     Java", course notes available at                                    =
//=     http://www.cs.princeton.edu/introcs/97data/                         =
//=     last accessed, june 8 2007.                                         =

export class SignalProcessing extends ProcessingElement {
    /**
    Compute the FFT of x[], assuming its length is a power of 2.
    Bare bones implementation that runs in O(N log N) time. Design goal
    is to optimize the clarity of the code, rather than performance.

    Not the most memory efficient algorithm (because it uses
    an object type for representing complex numbers and because
    it re-allocates memory for the subarray, instead of doing
    in-place or reusing a single temporary array)

    Current implementation of complex number based on (borrowed from, jeje)
    sample in [.wPRIN2007].9 course notes.
    */
    public static fft(x: Complex[]): Complex[] {
        const N: number = x.length;

        // Base case
        if (N === 1) return [x[0]!];

        // Radix 2 Cooley-Tukey FFT
        if (N % 2 !== 0) {
            throw new RuntimeException("N is not a power of 2");
        }

        // Fft of even terms
        const even: Complex[] = new Array<Complex>(N / 2);
        for (let k = 0; k < N / 2; k++) {
            even[k] = x[2 * k]!;
        }
        const q: Complex[] = SignalProcessing.fft(even);

        // Fft of odd terms
        const odd: Complex[] = even; // reuse the array
        for (let k = 0; k < N / 2; k++) {
            odd[k] = x[2 * k + 1]!;
        }
        const r: Complex[] = SignalProcessing.fft(odd);

        // Combine
        const y: Complex[] = new Array<Complex>(N);
        for (let k = 0; k < N / 2; k++) {
            // Java evaluates `-2 * k` in int arithmetic, so k = 0 gives +0, not -0.
            const kth: number = (Math.imul(-2, k) * Math.PI) / N;
            const wk: Complex = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k]!.plus(wk.times(r[k]!));
            y[k + N / 2] = q[k]!.minus(wk.times(r[k]!));
        }
        return y;
    }

    /**
    Compute the inverse FFT of x[], assuming its length is a power of 2.
    Bare bones implementation that runs in O(N log N) time. Design goal
    is to optimize the clarity of the code, rather than performance.

    Not the most memory efficient algorithm (because it uses
    an object type for representing complex numbers and because
    it re-allocates memory for the subarray, instead of doing
    in-place or reusing a single temporary array)

    Current implementation of complex number based on (borrowed from, jeje)
    sample in [.wPRIN2007].9 course notes.
    */
    public static ifft(x: Complex[]): Complex[] {
        const N: number = x.length;
        let y: Complex[] = new Array<Complex>(N);

        // Take conjugate
        for (let i = 0; i < N; i++) {
            y[i] = x[i]!.conjugate();
        }

        // Compute forward FFT
        y = SignalProcessing.fft(y);

        // Take conjugate again
        for (let i = 0; i < N; i++) {
            y[i] = y[i]!.conjugate();
        }

        // Divide by N
        for (let i = 0; i < N; i++) {
            y[i] = y[i]!.times(1.0 / N);
        }

        return y;
    }

    /**
    Compute the circular convolution of x and y.
    Current implementation of complex number based on (borrowed from, jeje)
    sample in [.wPRIN2007].9 course notes.
    */
    public static circularConvolve(x: Complex[], y: Complex[]): Complex[] {
        // Should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.length !== y.length) {
            throw new RuntimeException("Dimensions don't agree");
        }

        const N: number = x.length;

        // Compute FFT of each sequence
        const a: Complex[] = SignalProcessing.fft(x);
        const b: Complex[] = SignalProcessing.fft(y);

        // Point-wise multiply
        const c: Complex[] = new Array<Complex>(N);
        for (let i = 0; i < N; i++) {
            c[i] = a[i]!.times(b[i]!);
        }

        // Compute inverse FFT
        return SignalProcessing.ifft(c);
    }

    /**
    Compute the linear convolution of x and y.
    Current implementation of complex number based on (borrowed from, he he)
    sample in [.wPRIN2007].9 course notes.
    */
    public static linearConvolve(x: Complex[], y: Complex[]): Complex[] {
        const ZERO: Complex = new Complex(0, 0);
        const a: Complex[] = new Array<Complex>(2 * x.length);
        for (let i = 0; i < x.length; i++) a[i] = x[i]!;
        for (let i = x.length; i < 2 * x.length; i++) a[i] = ZERO;
        const b: Complex[] = new Array<Complex>(2 * y.length);
        for (let i = 0; i < y.length; i++) b[i] = y[i]!;
        for (let i = y.length; i < 2 * y.length; i++) b[i] = ZERO;
        return SignalProcessing.circularConvolve(a, b);
    }
}
