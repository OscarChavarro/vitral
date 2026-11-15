// Port of cpp/base/src/test/GeometryWorkspaceTestWorker.{h,cpp}
import type { Runnable } from "java/lang/Runnable.js";
import type { ArrayList } from "java/util/ArrayList.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { GeometryQueryWorkspace } from "vsdk/toolkit/environment/geometry/element/GeometryQueryWorkspace.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import type { RaySolidBoundary } from "vsdk/toolkit/environment/geometry/element/RaySolidBoundary.js";
import { RaySolidQueryStatus } from "vsdk/toolkit/environment/geometry/element/RaySolidQueryStatus.js";
import type { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";

export class GeometryWorkspaceTestWorker implements Runnable {
    private readonly sphere: Sphere;
    private readonly workspace: GeometryQueryWorkspace;
    private success: boolean;

    public constructor(value: Sphere) {
        this.sphere = value;
        this.workspace = new GeometryQueryWorkspace();
        this.success = true;
    }

    public run(): void {
        const ray = Ray.fromAffine(new Vector3Dd(-3, 0, 0), new Vector3Dd(2, 0, 0));
        for (let i = 0; i < 500; i++) {
            this.workspace.clear();
            this.workspace.hitResult().resetForDistanceOnly();
            if (
                this.sphere.queryFirstHit(ray, new RayCrossingQuery(0, 4), this.workspace.hitResult()) !==
                    RayFirstHitStatus.Hit ||
                this.workspace.hitResult().getHitDistance() !== 1 ||
                this.sphere.classifyRay(ray, new RayCrossingQuery(0, 4), this.workspace.solidResult()) !==
                    RaySolidQueryStatus.Supported ||
                this.workspace.solidResult().boundaries.size() !== 2 ||
                this.workspace.solidResult().boundaries.get(1)!.t !== 2
            ) {
                this.success = false;
                return;
            }
        }
    }

    public passed(): boolean {
        return this.success;
    }

    // The C++ port returns the boundary array storage pointer; the list
    // object identifies that storage here.
    public boundaryStorage(): ArrayList<RaySolidBoundary> {
        return this.workspace.solidResult().boundaries;
    }
}
