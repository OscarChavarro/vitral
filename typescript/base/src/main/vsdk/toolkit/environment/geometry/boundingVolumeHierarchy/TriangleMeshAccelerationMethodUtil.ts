import { TriangleMeshAccelerationMethod } from "./TriangleMeshAccelerationMethod.js";

/**
C++ declares it in `TriangleMeshAccelerationMethod.h` and defines it in
`TriangleAccelerationStructure.cpp`. It is a namespace and not a class
because a static `name` member of a class would clash with `Function.name`.
*/
export namespace TriangleMeshAccelerationMethodUtil {
    export const COUNT = 3;

    export function fromCode(code: number): TriangleMeshAccelerationMethod {
        if (code === 1) {
            return TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH;
        }
        if (code === 2) {
            return TriangleMeshAccelerationMethod.VOXEL_GRID;
        }
        return TriangleMeshAccelerationMethod.BRUTE_FORCE;
    }

    /** Short name used by command lines: "bruteforce", "wald2007", "voxel" */
    export function name(value: TriangleMeshAccelerationMethod): string {
        switch (value) {
            case TriangleMeshAccelerationMethod.BRUTE_FORCE:
                return "bruteforce";
            case TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH:
                return "wald2007";
            case TriangleMeshAccelerationMethod.VOXEL_GRID:
                return "voxel";
        }
        return "unknown";
    }

    /**
    Inverse of `name`; false when the name is unknown.
    @param text name to parse
    @param outValue C++ reference output parameter: `outValue[0]` receives
    the method
    @return true when the name is known
    */
    export function fromName(text: string | null, outValue: TriangleMeshAccelerationMethod[]): boolean {
        if (text === null) {
            return false;
        }
        for (let code = 0; code < COUNT; code++) {
            const candidate: TriangleMeshAccelerationMethod = fromCode(code);
            if (text === name(candidate)) {
                outValue[0] = candidate;
                return true;
            }
        }
        return false;
    }
}
