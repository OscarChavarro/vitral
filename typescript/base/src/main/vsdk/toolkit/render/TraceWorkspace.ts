import { ColorRgb } from "../common/color/ColorRgb.js";
import { RayHit } from "../environment/geometry/element/RayHit.js";

export class TraceWorkspace {
    public static readonly DEFAULT_MAX_RECURSION_LEVEL = 8;

    public readonly traversalCandidateHit: RayHit;
    public readonly nearestHit: RayHit;
    public readonly shadowCandidateHitField: RayHit;
    public readonly reflectionHits: RayHit[];
    public readonly shadingHits: RayHit[];
    public readonly reflectionColors: ColorRgb[];

    public constructor(maxRecursionLevel: number = TraceWorkspace.DEFAULT_MAX_RECURSION_LEVEL) {
        this.traversalCandidateHit = new RayHit(RayHit.DETAIL_NONE, false);
        this.nearestHit = new RayHit(RayHit.DETAIL_NONE, false);
        this.shadowCandidateHitField = new RayHit(RayHit.DETAIL_NONE, false);

        const levels: number = maxRecursionLevel + 1;
        this.reflectionHits = new Array<RayHit>(levels);
        this.shadingHits = new Array<RayHit>(levels);
        this.reflectionColors = new Array<ColorRgb>(levels);
        for (let i = 0; i < levels; i++) {
            this.reflectionHits[i] = new RayHit(RayHit.DETAIL_NONE, false);
            this.shadingHits[i] = new RayHit();
            this.reflectionColors[i] = new ColorRgb();
        }
    }

    /**
    Java exposes both a public final field `shadowCandidateHit` and a method
    of the same name. TypeScript has a single member namespace per class, so
    the field is named `shadowCandidateHitField` here and the Java method name
    is kept for the callers (the shaders) that use it.
    */
    public shadowCandidateHit(): RayHit {
        return this.shadowCandidateHitField;
    }
}
