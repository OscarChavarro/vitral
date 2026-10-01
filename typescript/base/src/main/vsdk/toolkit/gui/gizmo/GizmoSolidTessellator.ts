import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";

/**
Tessellates, in world space, the simple solids the gizmos are built with (the
shafts and heads of their axes, the small cubes at their tips and their plane
handles), so the technology-dependent renderers only have to send the resulting
points to the rasterizer.

Each element of a gizmo is a {@link SimpleBody} whose geometry is expressed in
its own local frame, and grows along its local +Z (see
{@link localTransform}); every method here receives that local
frame as a matrix and returns points already transformed by it.

The primitives returned follow these conventions:

- A *strip* is a sequence of points for a triangle strip.
- A *fan* is a sequence of points for a triangle fan, with its center or apex
  as the first point.
*/
export class GizmoSolidTessellator {
    /// Number of subdivisions used around the axis of a revolution solid
    public static readonly SLICES = 16;

    private static readonly SLICE_COS: number[] = [];
    private static readonly SLICE_SIN: number[] = [];

    /// The 8 corners of a box, indexed by (sx,sy,sz) sign combination as
    /// ((sx+1)/2)*4 + ((sy+1)/2)*2 + (sz+1)/2; each row is a face, given as a
    /// valid (non self-intersecting) strip of 4 corner indexes
    private static readonly BOX_FACES: readonly (readonly number[])[] = [
        [0, 1, 2, 3], [4, 5, 6, 7], [0, 1, 4, 5], [2, 3, 6, 7], [0, 2, 4, 6], [1, 3, 5, 7],
    ];

    static {
        for (let i = 0; i <= GizmoSolidTessellator.SLICES; i++) {
            const angle: number = (2 * Math.PI * i) / GizmoSolidTessellator.SLICES;

            GizmoSolidTessellator.SLICE_COS[i] = Math.cos(angle);
            GizmoSolidTessellator.SLICE_SIN[i] = Math.sin(angle);
        }
    }

    /**
    @param element element of a gizmo
    @return the matrix that takes the local frame of the element to world
    space
    */
    public static localTransform(element: SimpleBody): Matrix4x4d {
        const position: Vector3Dd = element.getPosition();

        return new Matrix4x4d().translation(position).multiply(element.getRotation());
    }

    /**
    Tessellates the lateral surface of a shaft: a truncated cone from the
    local origin, growing along the local +Z axis.

    @param local local frame of the element
    @param baseRadius radius at the local origin
    @param topRadius radius at the local height
    @param height length of the shaft, along the local +Z axis
    @return strip with the lateral surface
    */
    public static buildShaftStrip(local: Matrix4x4d, baseRadius: number, topRadius: number, height: number): Vector3Dd[] {
        const strip: Vector3Dd[] = new Array<Vector3Dd>((GizmoSolidTessellator.SLICES + 1) * 2);
        const cos: number[] = GizmoSolidTessellator.SLICE_COS;
        const sin: number[] = GizmoSolidTessellator.SLICE_SIN;

        for (let i = 0; i <= GizmoSolidTessellator.SLICES; i++) {
            strip[2 * i] = local.multiply(new Vector3Dd(baseRadius * cos[i]!, baseRadius * sin[i]!, 0));
            strip[2 * i + 1] = local.multiply(new Vector3Dd(topRadius * cos[i]!, topRadius * sin[i]!, height));
        }
        return strip;
    }

    /**
    Tessellates the lateral surface of a cone whose base is at the local
    origin and whose apex is at the local height, over the +Z axis.

    @param local local frame of the element
    @param radius radius of the base of the cone
    @param height distance from the base to the apex
    @return fan with the apex as its first point
    */
    public static buildConeSideFan(local: Matrix4x4d, radius: number, height: number): Vector3Dd[] {
        const fan: Vector3Dd[] = new Array<Vector3Dd>(GizmoSolidTessellator.SLICES + 2);
        const cos: number[] = GizmoSolidTessellator.SLICE_COS;
        const sin: number[] = GizmoSolidTessellator.SLICE_SIN;

        fan[0] = local.multiply(new Vector3Dd(0, 0, height));
        for (let i = 0; i <= GizmoSolidTessellator.SLICES; i++) {
            fan[i + 1] = local.multiply(new Vector3Dd(radius * cos[i]!, radius * sin[i]!, 0));
        }
        return fan;
    }

    /**
    Tessellates the base of a cone built by {@link buildConeSideFan}, with
    the opposite orientation, as it is seen from the other side.

    @param local local frame of the element
    @param radius radius of the base of the cone
    @return fan with the center of the base as its first point
    */
    public static buildConeBaseFan(local: Matrix4x4d, radius: number): Vector3Dd[] {
        const fan: Vector3Dd[] = new Array<Vector3Dd>(GizmoSolidTessellator.SLICES + 2);
        const cos: number[] = GizmoSolidTessellator.SLICE_COS;
        const sin: number[] = GizmoSolidTessellator.SLICE_SIN;

        fan[0] = local.multiply(new Vector3Dd(0, 0, 0));
        for (let i = 0; i <= GizmoSolidTessellator.SLICES; i++) {
            const k: number = GizmoSolidTessellator.SLICES - i;

            fan[i + 1] = local.multiply(new Vector3Dd(radius * cos[k]!, radius * sin[k]!, 0));
        }
        return fan;
    }

    /**
    Tessellates a box centered at the local origin, as its 6 faces.

    @param local local frame of the element
    @param size lengths of the sides of the box, over each local axis
    @return one strip of 4 corners per face of the box
    */
    public static buildBoxFaceStrips(local: Matrix4x4d, size: Vector3Dd): Vector3Dd[][] {
        const hx: number = size.x() / 2;
        const hy: number = size.y() / 2;
        const hz: number = size.z() / 2;
        const corners: Vector3Dd[] = [];
        const faces: Vector3Dd[][] = [];

        for (let sx = -1; sx <= 1; sx += 2) {
            for (let sy = -1; sy <= 1; sy += 2) {
                for (let sz = -1; sz <= 1; sz += 2) {
                    corners.push(local.multiply(new Vector3Dd(sx * hx, sy * hy, sz * hz)));
                }
            }
        }

        for (let face = 0; face < GizmoSolidTessellator.BOX_FACES.length; face++) {
            const strip: Vector3Dd[] = [];

            for (let corner = 0; corner < 4; corner++) {
                strip.push(corners[GizmoSolidTessellator.BOX_FACES[face]![corner]!]!);
            }
            faces.push(strip);
        }
        return faces;
    }

    /**
    Tessellates a rectangle centered at the local origin, over the local XY
    plane.

    @param local local frame of the element
    @param sizeX length of the rectangle over the local X axis
    @param sizeY length of the rectangle over the local Y axis
    @return strip with the 4 corners of the rectangle
    */
    public static buildPlaneQuad(local: Matrix4x4d, sizeX: number, sizeY: number): Vector3Dd[] {
        const hx: number = sizeX / 2;
        const hy: number = sizeY / 2;

        return [
            local.multiply(new Vector3Dd(-hx, -hy, 0)),
            local.multiply(new Vector3Dd(hx, -hy, 0)),
            local.multiply(new Vector3Dd(-hx, hy, 0)),
            local.multiply(new Vector3Dd(hx, hy, 0)),
        ];
    }
}
