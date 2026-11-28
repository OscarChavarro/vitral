import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Double } from "../../../../java/lang/Double.js";
import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { IllegalStateException } from "../../../../java/lang/IllegalStateException.js";
import { Integer } from "../../../../java/lang/Integer.js";
import { Throwable } from "../../../../java/lang/Throwable.js";
import { SimpleMaterial } from "./SimpleMaterial.js";
//= References:                                                             =
//= [COOK1982] Cook, Robert L.; Torrance, Kenneth E. "A Reflectance Model   =
//= for Computer Graphics", ACM Transactions on Graphics, 1982.             =
//= [TORR1967] Torrance, K. E.; Sparrow, E. M. "Theory for Off-Specular     =
//= Reflection from Roughened Surfaces", JOSA, 1967.                        =
//= [BECK1963] Beckmann, P.; Spizzichino, A. "The Scattering of              =
//= Electromagnetic Waves from Rough Surfaces", 1963.                        =

/**
Java's private record `MicroFacetedMaterial.MicrofacetConfig`. It is exported
from this module only so that the configuration constructor below can name
it; it is not part of the package index, as the record is not part of Java's
API.
*/
export class MicrofacetConfig {
    public constructor(
        public readonly name: string,
        public readonly ambient: ColorRgb,
        public readonly diffuse: ColorRgb,
        public readonly specular: ColorRgb,
        public readonly doubleSided: boolean,
        public readonly reflectionCoefficient: number,
        public readonly refractionCoefficient: number,
        public readonly opacity: number,
        public readonly phongExponent: number,
        public readonly roughness: number,
        public readonly alpha: number,
        public readonly fresnelF0: ColorRgb,
        public readonly eta: ColorRgb,
        public readonly kappa: ColorRgb,
        public readonly kd: number,
        public readonly ks: number,
        public readonly fresnelModel: number,
        public readonly ndfModel: number,
        public readonly geometryModel: number,
    ) {}
}

/**
Cook-Torrance oriented extension of {@link SimpleMaterial}.

This class stores the per-object microfacet parameters shared by CPU and GPU
shaders. It does not implement shading by itself.

The target BRDF structure is the Cook-Torrance microfacet model
f_r ~ D * F * G / (4 * (N.L) * (N.V)), where:
- D: normal distribution function (NDF)
- F: Fresnel reflectance
- G: geometric attenuation / masking-shadowing

Java's `MicroFacetedMaterial(String csvFileName, String materialName)` does two
things: it finds the CSV file (`resolveCsvFile` tries the name as given and
then under `etc/materials`) and it parses it. Only the first is bound to a file
system, so it stays with the caller: `MicrofacetCsvLoader.load` in
`@vitral/fs` is that constructor, and a browser fetches the text. The parse is
`fromCsvText` here, literal down to the exception each failure raises.

At the end of the stream Java's `PersistenceElement.readAsciiLine` throws
`EOFException` (its `readBytes` rejects a short read), and the loop catches
it as `eofLike` and leaves the table. So the text after the last newline is
not a line, and a missing material raises the `IllegalArgumentException`
after the loop; the same happens here. Before the 2026-11-28 update of
Java's `PersistenceElement`, `readBytes` ignored the end of the stream and
that read never ended (`OutOfMemoryError`).
*/
export class MicroFacetedMaterial extends SimpleMaterial {
    public static readonly FRESNEL_MODEL_SCHLICK = 0;
    public static readonly FRESNEL_MODEL_CONDUCTOR = 1;

    public static readonly NDF_MODEL_BECKMANN = 0;
    public static readonly NDF_MODEL_GGX = 1;

    public static readonly GEOMETRY_MODEL_SMITH = 0;
    public static readonly GEOMETRY_MODEL_IMPLICIT = 1;

    // [COOK1982] RMS slope proxy ("m") controlling micro-facet spread.
    private readonly roughness: number;
    // Helper used by common NDF/G parameterization (often alpha = m^2).
    private readonly alpha: number;

    // [COOK1982] Reflectance at normal incidence (per channel), used as
    // practical Fresnel anchor when full spectral data is unavailable.
    private readonly fresnelF0: ColorRgb;
    // [COOK1982] Complex refractive index terms for conductor Fresnel:
    // n (eta) and k (kappa), per channel.
    private readonly eta: ColorRgb;
    private readonly kappa: ColorRgb;

    // Runtime diffuse/specular energy split (implementation policy).
    private readonly kd: number;
    private readonly ks: number;

    // Optional model selectors for runtime shader branching:
    // D term (Beckmann/GGX), F term (Schlick/Conductor), G term.
    private readonly fresnelModel: number;
    private readonly ndfModel: number;
    private readonly geometryModel: number;

    /**
    Java's `MicroFacetedMaterial()`, `MicroFacetedMaterial(MicroFacetedMaterial)`
    and the private `MicroFacetedMaterial(MicrofacetConfig)`.
    */
    public constructor();
    public constructor(other: MicroFacetedMaterial);
    public constructor(config: MicrofacetConfig);
    public constructor(value?: MicroFacetedMaterial | MicrofacetConfig) {
        if (value instanceof MicroFacetedMaterial) {
            const other: MicroFacetedMaterial = value;
            super(
                other.getName(),
                other.getAmbientReference(),
                other.getDiffuseReference(),
                other.getSpecularReference(),
                other.isDoubleSided(),
                other.getReflectionCoefficient(),
                other.getRefractionCoefficient(),
                other.getOpacity(),
                other.getPhongExponent(),
            );
            this.roughness = other.roughness;
            this.alpha = other.alpha;
            this.fresnelF0 = new ColorRgb(other.fresnelF0);
            this.eta = new ColorRgb(other.eta);
            this.kappa = new ColorRgb(other.kappa);
            this.kd = other.kd;
            this.ks = other.ks;
            this.fresnelModel = other.fresnelModel;
            this.ndfModel = other.ndfModel;
            this.geometryModel = other.geometryModel;
            return;
        }
        const config: MicrofacetConfig = value === undefined ? MicroFacetedMaterial.defaultConfig() : value;
        super(
            config.name,
            config.ambient,
            config.diffuse,
            config.specular,
            config.doubleSided,
            config.reflectionCoefficient,
            config.refractionCoefficient,
            config.opacity,
            config.phongExponent,
        );
        this.roughness = config.roughness;
        this.alpha = config.alpha;
        this.fresnelF0 = new ColorRgb(config.fresnelF0);
        this.eta = new ColorRgb(config.eta);
        this.kappa = new ColorRgb(config.kappa);
        this.kd = config.kd;
        this.ks = config.ks;
        this.fresnelModel = config.fresnelModel;
        this.ndfModel = config.ndfModel;
        this.geometryModel = config.geometryModel;
    }

    public getRoughness(): number {
        return this.roughness;
    }

    public getAlpha(): number {
        return this.alpha;
    }

    public getFresnelF0(): ColorRgb {
        return new ColorRgb(this.fresnelF0);
    }

    public getEta(): ColorRgb {
        return new ColorRgb(this.eta);
    }

    public getKappa(): ColorRgb {
        return new ColorRgb(this.kappa);
    }

    public getKd(): number {
        return this.kd;
    }

    public getKs(): number {
        return this.ks;
    }

    public getFresnelModel(): number {
        return this.fresnelModel;
    }

    public getNdfModel(): number {
        return this.ndfModel;
    }

    public getGeometryModel(): number {
        return this.geometryModel;
    }

    private static clampToUnitInterval(value: number): number {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    /**
    Java's `MicroFacetedMaterial(String csvFileName, String materialName)`,
    over the text of the CSV rather than its file name. See the class note
    above for the split.

    @param csvText The complete contents of the microfacet CSV file
    @param materialName The `material_name` to look for, matched ignoring case
    @param sourceName Java's `csvFile.getAbsolutePath()`, for the messages
    */
    public static fromCsvText(
        csvText: string | null,
        materialName: string | null,
        sourceName: string,
    ): MicroFacetedMaterial {
        return new MicroFacetedMaterial(MicroFacetedMaterial.loadConfigFromCsv(csvText, materialName, sourceName));
    }

    private static loadConfigFromCsv(
        csvText: string | null,
        materialName: string | null,
        sourceName: string,
    ): MicrofacetConfig {
        if (csvText === null || materialName === null) {
            return MicroFacetedMaterial.defaultConfig();
        }

        const defaultConfig: MicrofacetConfig = MicroFacetedMaterial.defaultConfig();
        const normalizedName: string = MicroFacetedMaterial.trim(materialName).toLowerCase();
        try {
            const lines: string[] = MicroFacetedMaterial.readAsciiLines(csvText);
            let lineIndex = 0;
            const headerLine: string | null = lineIndex < lines.length ? lines[lineIndex++]! : null;
            if (headerLine === null || MicroFacetedMaterial.isBlank(headerLine)) {
                throw new IllegalArgumentException("Empty CSV header in file: " + sourceName);
            }
            const headerIndex: Map<string, number> = MicroFacetedMaterial.parseHeaderIndex(headerLine);

            while (true) {
                let line: string;
                if (lineIndex >= lines.length) {
                    break;
                }
                line = lines[lineIndex++]!;
                if (MicroFacetedMaterial.isBlank(line)) {
                    continue;
                }
                const row: string[] = MicroFacetedMaterial.splitCsv(line);
                const rowName: string | null = MicroFacetedMaterial.field(row, headerIndex, "material_name");
                if (rowName === null) {
                    continue;
                }
                if (MicroFacetedMaterial.trim(rowName).toLowerCase() !== normalizedName) {
                    continue;
                }

                const roughness: number = MicroFacetedMaterial.clampToUnitInterval(
                    MicroFacetedMaterial.doubleField(row, headerIndex, "roughness", defaultConfig.roughness),
                );
                const alpha: number = MicroFacetedMaterial.clampToUnitInterval(
                    MicroFacetedMaterial.doubleField(row, headerIndex, "alpha", roughness * roughness),
                );
                const diffuseColor: ColorRgb = MicroFacetedMaterial.rgbField(
                    row,
                    headerIndex,
                    "diffuse_r",
                    "diffuse_g",
                    "diffuse_b",
                    defaultConfig.diffuse,
                );
                const f0: ColorRgb = MicroFacetedMaterial.rgbField(
                    row,
                    headerIndex,
                    "f0_r",
                    "f0_g",
                    "f0_b",
                    defaultConfig.fresnelF0,
                );
                const eta: ColorRgb = MicroFacetedMaterial.rgbField(
                    row,
                    headerIndex,
                    "eta_r",
                    "eta_g",
                    "eta_b",
                    defaultConfig.eta,
                );
                const kappa: ColorRgb = MicroFacetedMaterial.rgbField(
                    row,
                    headerIndex,
                    "kappa_r",
                    "kappa_g",
                    "kappa_b",
                    defaultConfig.kappa,
                );
                return new MicrofacetConfig(
                    MicroFacetedMaterial.trim(rowName),
                    defaultConfig.ambient,
                    diffuseColor,
                    f0,
                    defaultConfig.doubleSided,
                    defaultConfig.reflectionCoefficient,
                    defaultConfig.refractionCoefficient,
                    defaultConfig.opacity,
                    defaultConfig.phongExponent,
                    roughness,
                    alpha,
                    f0,
                    eta,
                    kappa,
                    MicroFacetedMaterial.clampToUnitInterval(
                        MicroFacetedMaterial.doubleField(row, headerIndex, "kd", defaultConfig.kd),
                    ),
                    MicroFacetedMaterial.clampToUnitInterval(
                        MicroFacetedMaterial.doubleField(row, headerIndex, "ks", defaultConfig.ks),
                    ),
                    MicroFacetedMaterial.intField(
                        row,
                        headerIndex,
                        "fresnel_model",
                        MicroFacetedMaterial.FRESNEL_MODEL_SCHLICK,
                    ),
                    MicroFacetedMaterial.intField(
                        row,
                        headerIndex,
                        "ndf_model",
                        MicroFacetedMaterial.NDF_MODEL_BECKMANN,
                    ),
                    MicroFacetedMaterial.intField(
                        row,
                        headerIndex,
                        "geometry_model",
                        MicroFacetedMaterial.GEOMETRY_MODEL_SMITH,
                    ),
                );
            }
        } catch (e) {
            throw new IllegalStateException(
                "Failed reading microfacet material '" + materialName + "' from " + sourceName,
                e instanceof Throwable ? e : undefined,
            );
        }

        throw new IllegalArgumentException("SimpleMaterial '" + materialName + "' not found in CSV: " + sourceName);
    }

    /**
    The lines Java's loop obtains from `readAsciiLine`: every `\r` is dropped,
    and each `\n` ends a line. The text after the last `\n` is not a line (see
    the class note).
    */
    private static readAsciiLines(csvText: string): string[] {
        const lines: string[] = csvText.split("\n").map((line) => line.replace(/\r/g, ""));
        lines.pop();
        return lines;
    }

    /** Java's `String.trim`: strips the code points up to U+0020 at both ends. */
    private static trim(value: string): string {
        // eslint-disable-next-line no-control-regex
        return value.replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g, "");
    }

    /** Java's `String.isBlank`, for the `Character.isWhitespace` code points. */
    private static isBlank(value: string): boolean {
        // eslint-disable-next-line no-control-regex
        return /^[\t\n\u000b\f\r\u001c-\u001f \u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]*$/.test(value);
    }

    private static parseHeaderIndex(headerLine: string): Map<string, number> {
        const header: string[] = MicroFacetedMaterial.splitCsv(headerLine);
        const headerIndex = new Map<string, number>();
        let i: number;
        for (i = 0; i < header.length; i++) {
            headerIndex.set(MicroFacetedMaterial.trim(header[i]!).toLowerCase(), i);
        }
        return headerIndex;
    }

    /** Java's `line.split(",", -1)`, which keeps trailing empty fields. */
    private static splitCsv(line: string): string[] {
        return line.split(",");
    }

    private static field(row: string[], headerIndex: Map<string, number>, key: string): string | null {
        const index: number | undefined = headerIndex.get(key);
        if (index === undefined) {
            return null;
        }
        if (index < 0 || index >= row.length) {
            return null;
        }
        return MicroFacetedMaterial.trim(row[index]!);
    }

    private static doubleField(
        row: string[],
        headerIndex: Map<string, number>,
        key: string,
        defaultValue: number,
    ): number {
        const value: string | null = MicroFacetedMaterial.field(row, headerIndex, key);
        if (value === null || MicroFacetedMaterial.isBlank(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    private static intField(
        row: string[],
        headerIndex: Map<string, number>,
        key: string,
        defaultValue: number,
    ): number {
        const value: string | null = MicroFacetedMaterial.field(row, headerIndex, key);
        if (value === null || MicroFacetedMaterial.isBlank(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static rgbField(
        row: string[],
        headerIndex: Map<string, number>,
        keyR: string,
        keyG: string,
        keyB: string,
        defaultValue: ColorRgb,
    ): ColorRgb {
        const r: number = MicroFacetedMaterial.doubleField(row, headerIndex, keyR, defaultValue.r());
        const g: number = MicroFacetedMaterial.doubleField(row, headerIndex, keyG, defaultValue.g());
        const b: number = MicroFacetedMaterial.doubleField(row, headerIndex, keyB, defaultValue.b());
        return new ColorRgb(r, g, b);
    }

    private static defaultConfig(): MicrofacetConfig {
        return new MicrofacetConfig(
            "VSDK_default_material",
            new ColorRgb(0.1, 0.1, 0.1),
            new ColorRgb(0.9, 0.5, 0.5),
            new ColorRgb(1, 1, 1),
            true,
            0.0,
            0.0,
            1.0,
            128.0,
            0.35,
            0.35 * 0.35,
            new ColorRgb(0.04, 0.04, 0.04),
            new ColorRgb(1.5, 1.5, 1.5),
            new ColorRgb(0.0, 0.0, 0.0),
            1.0,
            1.0,
            MicroFacetedMaterial.FRESNEL_MODEL_SCHLICK,
            MicroFacetedMaterial.NDF_MODEL_BECKMANN,
            MicroFacetedMaterial.GEOMETRY_MODEL_SMITH,
        );
    }
}
