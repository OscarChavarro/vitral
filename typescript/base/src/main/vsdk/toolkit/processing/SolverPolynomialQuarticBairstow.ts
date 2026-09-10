import { ProcessingElement } from "./ProcessingElement.js";

/** Literal port of Java's legacy Bairstow quartic solver. */
export class SolverPolynomialQuarticBairstow extends ProcessingElement {
    private real: number[];
    private img: number[];

    public constructor();
    public constructor(a4: number, a3: number, a2: number, a1: number, a0: number);
    public constructor(a4?: number, a3?: number, a2?: number, a1?: number, a0?: number) {
        super();
        if (a4 === undefined || a3 === undefined || a2 === undefined || a1 === undefined || a0 === undefined) {
            this.real = new Array<number>(4).fill(0);
            this.img = new Array<number>(4).fill(0);
            return;
        }
        const a = [a0, a1, a2, a3, a4];
        this.real = new Array<number>(a.length).fill(0);
        this.img = new Array<number>(a.length).fill(0);
        this.Bairstow(a, -1, -1, this.real, this.img);
    }

    public getReal(): number[] {
        return this.real;
    }
    public setReal(aRes: number[]): void {
        this.real = aRes;
    }
    public getImg(): number[] {
        return this.img;
    }
    public setImg(aImg: number[]): void {
        this.img = aImg;
    }

    /** This algorithm is failing. Use a raytracer to visually debug it. */
    public Bairstow(a: number[], r0: number, s0: number, re: number[], im: number[]): void {
        let n = a.length;
        let iter = 0;
        const b = new Array<number>(n).fill(0);
        const c = new Array<number>(n).fill(0);
        let ea1 = 1;
        let ea2 = 1;
        const T = 0.0000001;
        let r = r0;
        let s = s0;
        const maxIter = 100;

        for (iter = 0; iter < maxIter && n > 3; iter++) {
            let turns = 0;
            do {
                this.derivedDivision(a, b, c, r, s, n);
                const det = c[2]! * c[2]! - c[3]! * c[1]!;
                if (det !== 0) {
                    const dr = (-b[1]! * c[2]! + b[0]! * c[3]!) / det;
                    const ds = (-b[0]! * c[2]! + b[1]! * c[1]!) / det;
                    r += dr;
                    s += ds;
                    if (r !== 0) ea1 = Math.abs(dr / r) * 100;
                    if (s !== 0) ea2 = Math.abs(ds / s) * 100;
                } else {
                    r = 5 * r + 1;
                    s += 1;
                    iter = 0;
                }
                turns++;
            } while (ea1 > T && ea2 > T && turns < 100);

            this.roots(r, s, re, im, n);
            n -= 2;
            for (let i = 0; i < n; i++) a[i] = b[i + 2]!;
            if (n < 4) break;
        }

        if (n === 3) {
            r = -a[1]! / a[2]!;
            s = -a[0]! / a[2]!;
            this.roots(r, s, re, im, n);
        } else {
            re[n - 1] = -a[0]! / a[1]!;
            im[n - 1] = 0;
        }

        for (let i = 1; i < re.length; i++) {
            this.real[i - 1] = re[i]!;
            this.img[i - 1] = im[i]!;
        }
    }

    public derivedDivision(a: number[], b: number[], c: number[], r: number, s: number, n: number): void {
        b[n - 1] = a[n - 1]!;
        b[n - 2] = a[n - 2]! + r * b[n - 1]!;
        c[n - 1] = b[n - 1]!;
        c[n - 2] = b[n - 2]! + r * c[n - 1]!;
        for (let i = n - 3; i >= 0; i--) {
            b[i] = a[i]! + r * b[i + 1]! + s * b[i + 2]!;
            c[i] = b[i]! + r * c[i + 1]! + s * c[i + 2]!;
        }
    }

    public print(x: number[], n: number): void {
        let result = "";
        for (let i = n - 1; i >= 0; i--) result += x[i]! > 0 ? `+ ${x[i]}x${i} ` : `- ${-x[i]!}x${i} `;
        console.log(result);
    }

    public print2(x: number[], n: number): void {
        let result = "";
        for (let i = n - 1; i >= 2; i--) result += x[i]! > 0 ? `+ ${x[i]}x${i - 2} ` : `- ${-x[i]!}x${i - 2} `;
        console.log(`${result}Residuo = {${x[1]}, ${x[0]}}`);
    }

    public roots(r: number, s: number, re: number[], im: number[], n: number): void {
        const d = r * r + 4 * s;
        if (d > 0) {
            re[n - 1] = (r + Math.sqrt(d)) / 2;
            re[n - 2] = (r - Math.sqrt(d)) / 2;
            im[n - 1] = 0;
            im[n - 2] = 0;
        } else {
            re[n - 1] = r / 2;
            re[n - 2] = re[n - 1]!;
            im[n - 1] = Math.sqrt(-d) / 2;
            im[n - 2] = -im[n - 1]!;
        }
    }
}
