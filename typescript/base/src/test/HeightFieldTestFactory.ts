// Port of cpp/base/src/test/HeightFieldTestFactory.{h,cpp}
import { Math as JavaMath } from "java/lang/Math.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { HeightFieldSolid } from "vsdk/toolkit/environment/geometry/volume/HeightFieldSolid.js";

export class HeightFieldTestFactory {
    private constructor() {}

    // 5 x 5 grid of the plane h = 1 + x/2 + z/4 in the box [1,3]x[0,5]x[1,3].
    public static createPlane(): HeightFieldSolid {
        const solid = new HeightFieldSolid(5, 5, new Vector3Dd(1, 0, 1), new Vector3Dd(3, 5, 3));
        for (let z = 0; z < 5; ++z) {
            for (let x = 0; x < 5; ++x) solid.setHeight(x, z, 1.0 + 0.5 * x + 0.25 * z);
        }
        solid.updateBlocks();
        return solid;
    }

    // 17 x 17 grid of rolling hills in the box [1,15]x[0,6]x[1,15].
    public static createHills(): HeightFieldSolid {
        const solid = new HeightFieldSolid(17, 17, new Vector3Dd(1, 0, 1), new Vector3Dd(15, 6, 15));
        for (let z = 0; z < 17; ++z) {
            for (let x = 0; x < 17; ++x) {
                solid.setHeight(
                    x,
                    z,
                    2.5 + 1.5 * JavaMath.sin(0.7 * x) * JavaMath.cos(0.45 * z) + 0.3 * JavaMath.sin(1.9 * x + 1.3 * z),
                );
            }
        }
        solid.updateBlocks();
        return solid;
    }
}
