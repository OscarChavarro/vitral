import { ColorRgb } from "../../common/color/ColorRgb.js";
import { SimpleMaterial } from "./SimpleMaterial.js";
export class MicrofacetConfig {
    public constructor(
        public readonly name = "VSDK_default_material",
        public readonly ambient = new ColorRgb(0.1, 0.1, 0.1),
        public readonly diffuse = new ColorRgb(0.9, 0.5, 0.5),
        public readonly specular = new ColorRgb(1, 1, 1),
        public readonly doubleSided = true,
        public readonly reflectionCoefficient = 0,
        public readonly refractionCoefficient = 0,
        public readonly opacity = 1,
        public readonly phongExponent = 128,
        public readonly roughness = 0.35,
        public readonly alpha = 0.1225,
        public readonly fresnelF0 = new ColorRgb(0.04, 0.04, 0.04),
        public readonly eta = new ColorRgb(1.5, 1.5, 1.5),
        public readonly kappa = new ColorRgb(),
        public readonly kd = 1,
        public readonly ks = 1,
        public readonly fresnelModel = 0,
        public readonly ndfModel = 0,
        public readonly geometryModel = 0,
    ) {}
}
export class MicroFacetedMaterial extends SimpleMaterial {
    public static readonly FRESNEL_MODEL_SCHLICK = 0;
    public static readonly FRESNEL_MODEL_CONDUCTOR = 1;
    public static readonly NDF_MODEL_BECKMANN = 0;
    public static readonly NDF_MODEL_GGX = 1;
    public static readonly GEOMETRY_MODEL_SMITH = 0;
    public static readonly GEOMETRY_MODEL_IMPLICIT = 1;
    private readonly config: MicrofacetConfig;
    public constructor();
    public constructor(other: MicroFacetedMaterial);
    public constructor(config: MicrofacetConfig);
    public constructor(value: MicroFacetedMaterial | MicrofacetConfig = new MicrofacetConfig()) {
        const c = value instanceof MicroFacetedMaterial ? value.config : value;
        super(
            c.name,
            c.ambient,
            c.diffuse,
            c.specular,
            c.doubleSided,
            c.reflectionCoefficient,
            c.refractionCoefficient,
            c.opacity,
            c.phongExponent,
        );
        this.config = new MicrofacetConfig(
            c.name,
            new ColorRgb(c.ambient),
            new ColorRgb(c.diffuse),
            new ColorRgb(c.specular),
            c.doubleSided,
            c.reflectionCoefficient,
            c.refractionCoefficient,
            c.opacity,
            c.phongExponent,
            Math.max(0, Math.min(1, c.roughness)),
            Math.max(0, Math.min(1, c.alpha)),
            new ColorRgb(c.fresnelF0),
            new ColorRgb(c.eta),
            new ColorRgb(c.kappa),
            Math.max(0, Math.min(1, c.kd)),
            Math.max(0, Math.min(1, c.ks)),
            c.fresnelModel,
            c.ndfModel,
            c.geometryModel,
        );
    }
    public getRoughness() {
        return this.config.roughness;
    }
    public getAlpha() {
        return this.config.alpha;
    }
    public getFresnelF0() {
        return new ColorRgb(this.config.fresnelF0);
    }
    public getEta() {
        return new ColorRgb(this.config.eta);
    }
    public getKappa() {
        return new ColorRgb(this.config.kappa);
    }
    public getKd() {
        return this.config.kd;
    }
    public getKs() {
        return this.config.ks;
    }
    public getFresnelModel() {
        return this.config.fresnelModel;
    }
    public getNdfModel() {
        return this.config.ndfModel;
    }
    public getGeometryModel() {
        return this.config.geometryModel;
    }
}
