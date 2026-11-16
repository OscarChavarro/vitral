//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

/**
Axis aligned box of the build. The default box is the "negative box"
[+inf, -inf] of [WALD2007].3.4: enclosing it in another box leaves that box
unchanged, so bins and partitions grow without emptiness checks.
*/
export class _BinnedSahBox {
    public readonly lo: number[] = [0, 0, 0];
    public readonly hi: number[] = [0, 0, 0];

    public constructor() {
        for (let a = 0; a < 3; a++) {
            this.lo[a] = 1e30;
            this.hi[a] = -1e30;
        }
    }

    public isEmpty(): boolean {
        return this.lo[0]! > this.hi[0]! || this.lo[1]! > this.hi[1]! || this.lo[2]! > this.hi[2]!;
    }

    public enclose(other: _BinnedSahBox): void {
        for (let a = 0; a < 3; a++) {
            this.lo[a] = other.lo[a]! < this.lo[a]! ? other.lo[a]! : this.lo[a]!;
            this.hi[a] = other.hi[a]! > this.hi[a]! ? other.hi[a]! : this.hi[a]!;
        }
    }

    public enclosePoint(point: number[]): void {
        for (let a = 0; a < 3; a++) {
            this.lo[a] = point[a]! < this.lo[a]! ? point[a]! : this.lo[a]!;
            this.hi[a] = point[a]! > this.hi[a]! ? point[a]! : this.hi[a]!;
        }
    }

    public centroid(axis: number): number {
        return 0.5 * (this.lo[axis]! + this.hi[axis]!);
    }

    public extent(axis: number): number {
        return this.hi[axis]! - this.lo[axis]!;
    }

    /** Half the surface area: the SAH cost of [WALD2007].3.1 drops the
    constant factors and only compares ratios */
    public halfArea(): number {
        if (this.isEmpty()) {
            return 0.0;
        }
        const dx = this.hi[0]! - this.lo[0]!;
        const dy = this.hi[1]! - this.lo[1]!;
        const dz = this.hi[2]! - this.lo[2]!;
        return dx * dy + dy * dz + dz * dx;
    }
}
