import type { Ray } from "../element/Ray.js";

/**
Leaf test of `BinnedSahInstanceHierarchy.traverseClosest`: tests the
geometry of one instance with the ray already taken to its object space,
and lowers `inOutBestT[0]` when it finds a closer hit (or applies its own
tie rule, for instance the lowest instance index on equal distances).
*/
export interface BinnedSahInstanceVisitor {
    /**
    @param objectRay the world ray with origin and direction multiplied by
    the world to object matrix of the instance; the direction is not
    normalized, so a parameter `t` along it is the same `t` along the world
    ray, and `inOutBestT[0]` is comparable across instances
    @param inOutBestT one-element input/output holder
    */
    visit(instance: number, objectRay: Ray, inOutBestT: number[]): void;
}
