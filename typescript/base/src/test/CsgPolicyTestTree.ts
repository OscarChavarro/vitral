// Port of cpp/base/src/test/CsgPolicyTestTree.{h,cpp}
import { CsgOperation } from "vsdk/toolkit/environment/scene/csg/CsgOperation.js";
import { CsgTree } from "vsdk/toolkit/environment/scene/csg/CsgTree.js";

export class CsgPolicyTestTree extends CsgTree {
    public constructor() {
        super(CsgOperation.UNION);
    }
}
