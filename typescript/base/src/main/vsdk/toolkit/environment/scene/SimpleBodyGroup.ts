import { Double } from "../../../../java/lang/Double.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Entity } from "../../common/Entity.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Ray } from "../geometry/element/Ray.js";
import { RayHit } from "../geometry/element/RayHit.js";
import type { SimpleBody } from "./SimpleBody.js";

export class SimpleBodyGroup extends Entity {
    //=======================================================================
    //- Model (1/6): set of bodies ------------------------------------
    private bodies: ArrayList<SimpleBody>;

    //- Model (2/6): body geometric transformations -------------------
    private position: Vector3Dd;
    private scale: Vector3Dd;
    /** Warning: The translation value in this matrix must be &lt;0, 0, 0&gt; */
    private rotation: Matrix4x4d;
    /** Warning: The translation value in this matrix must be &lt;0, 0, 0&gt; */
    private rotation_i: Matrix4x4d;

    //- Model (3/6): body visual data ---------------------------------

    //- Model (4/6): body physical data -------------------------------

    //- Model (5/6): body structural relationships --------------------

    //- Model (6/6): body semantic data -------------------------------
    /**
     * This string should be used for specific application defined
     * functionality. Can be null.
     */
    private name: string | null = null;
    //=======================================================================

    public constructor() {
        super();
        this.bodies = new ArrayList<SimpleBody>();
        this.rotation = new Matrix4x4d();
        this.rotation_i = new Matrix4x4d();
        this.position = new Vector3Dd(0, 0, 0);
        this.scale = new Vector3Dd(1, 1, 1);
    }

    public getBodies(): ArrayList<SimpleBody> {
        return this.bodies;
    }

    public getName(): string | null {
        return this.name;
    }

    public setName(n: string | null): void {
        this.name = n;
    }

    public getRotation(): Matrix4x4d {
        return this.rotation;
    }

    public setRotation(rotation: Matrix4x4d): void {
        // This is an homogeneous matrix, but it should contain only rotation.
        this.rotation = rotation.withoutTranslation();
    }

    public getRotationInverse(): Matrix4x4d {
        return this.rotation_i;
    }

    public setRotationInverse(rotationi: Matrix4x4d): void {
        this.rotation_i = rotationi;
    }

    public getPosition(): Vector3Dd {
        return this.position;
    }

    public setPosition(p: Vector3Dd): void {
        this.position = p;
    }

    public getScale(): Vector3Dd {
        return this.scale;
    }

    public setScale(s: Vector3Dd): void {
        this.scale = s;
    }

    public getTransformationMatrix(): Matrix4x4d {
        let S = new Matrix4x4d();
        let T = new Matrix4x4d();
        let M: Matrix4x4d;
        S = S.scale(this.scale);
        T = T.translation(this.position);
        M = T.multiply(this.rotation.multiply(S));
        return M;
    }

    /**
    Given a Ray in world coordinates, this method calculates the intersection
    with a Geometry located at the position and with the rotation stored
    in this object. Note that this method only relies in the capability of
    a geometry to calculate an intersection with a ray IN IT'S OWN OBJECT
    SPACE COORDINATES. Note that this technique is a central part of the
    VSDK geometric modeling proposal, where geometric transformations are
    not included in the geometries representations, making the internal
    code of `doIntersectionFirstHit` methods much easier to develop and maintain.
    @param inOutRay ray to be tested for intersection
    @returns true if given line intersects with any body inside current body
    group
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null {
        let myRay: Ray;
        let i: number;

        inOutRay = inOutRay.withT(Double.MAX_VALUE);

        myRay = new Ray(
            this.rotation_i.multiply(inOutRay.getOrigin().subtract(this.position)),
            this.rotation_i.multiply(inOutRay.getDirection()),
        );
        myRay = myRay.withT(inOutRay.getT());

        let nearestHit: Ray | null = null;

        for (i = 0; i < this.bodies.size(); i++) {
            const hit = new RayHit();
            if (this.bodies.get(i).getGeometry()!.doIntersectionFirstHit(myRay, hit)) {
                if ((hit.ray() as Ray).getT() < inOutRay.getT()) {
                    inOutRay = inOutRay.withT((hit.ray() as Ray).getT());
                    nearestHit = inOutRay;
                }
            }
        }
        return nearestHit;
    }

    public getMinMax(): number[] {
        //-----------------------------------------------------------------
        let i: number;
        let bi: SimpleBody;
        let T = new Matrix4x4d();
        let R: Matrix4x4d;
        let S = new Matrix4x4d();
        let M: Matrix4x4d;
        let p2: Vector3Dd;
        const points = new ArrayList<Vector3Dd>();
        let minmaxSub: Float64Array | number[];

        for (i = 0; i < this.bodies.size(); i++) {
            bi = this.bodies.get(i);
            minmaxSub = bi.getGeometry()!.getMinMax();
            R = bi.getRotation();
            T = T.translation(bi.getPosition());
            S = S.scale(bi.getScale());
            M = T.multiply(R).multiply(S);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0] as number, minmaxSub[1] as number, minmaxSub[2] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3] as number, minmaxSub[1] as number, minmaxSub[2] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0] as number, minmaxSub[4] as number, minmaxSub[2] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3] as number, minmaxSub[4] as number, minmaxSub[2] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0] as number, minmaxSub[1] as number, minmaxSub[5] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3] as number, minmaxSub[1] as number, minmaxSub[5] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[0] as number, minmaxSub[4] as number, minmaxSub[5] as number));
            points.add(p2);

            p2 = M.multiply(new Vector3Dd(minmaxSub[3] as number, minmaxSub[4] as number, minmaxSub[5] as number));
            points.add(p2);
        }

        //-----------------------------------------------------------------
        const MinMax = new Array<number>(6).fill(0);
        let minX = Double.MAX_VALUE;
        let minY = Double.MAX_VALUE;
        let minZ = Double.MAX_VALUE;
        let maxX = -Double.MAX_VALUE;
        let maxY = -Double.MAX_VALUE;
        let maxZ = -Double.MAX_VALUE;

        for (i = 0; i < points.size(); i++) {
            const p = points.get(i);
            if (p.x() < minX) minX = p.x();
            if (p.y() < minY) minY = p.y();
            if (p.z() < minZ) minZ = p.z();
            if (p.x() > maxX) maxX = p.x();
            if (p.y() > maxY) maxY = p.y();
            if (p.z() > maxZ) maxZ = p.z();
        }
        MinMax[0] = minX;
        MinMax[1] = minY;
        MinMax[2] = minZ;
        MinMax[3] = maxX;
        MinMax[4] = maxY;
        MinMax[5] = maxZ;

        return MinMax;
    }
}
