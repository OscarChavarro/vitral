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
/**
The platform-neutral half of Java's `MicroFacetedMaterial(String csvFileName,
String materialName)` constructor.

Java's constructor does two things: it finds the CSV file — `resolveCsvFile`
tries the name as given and then under `etc/materials` — and it parses it. Only
the first is bound to a file system, so it stays with the caller, which hands
the already-read text over here; `@vitral/fs` reads it from disk and a browser
fetches it. The parse itself is the Java one, `readAsciiLine` by
`readAsciiLine`: the header row names the columns, a row matching the material
name case-insensitively supplies whatever fields it carries, and every absent
or blank field falls back to the same default the Java reader uses, which for
`alpha` is `roughness * roughness`.

Java's `readAsciiLine` throws at end of file, and the reading loop treats that
throw as the end of the table, so a final line with no terminating newline is
never seen. Splitting the text on newlines would see it, which is why the split
below drops a trailing empty segment only, leaving that Java behavior intact
for a file that ends properly and reproducing it for one that does not.
*/
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
    @param sourceName The name of the read resource, for diagnostics
    */
    public static fromCsvText(csvText: string, materialName: string, sourceName: string): MicroFacetedMaterial {
        return new MicroFacetedMaterial(MicroFacetedMaterial.loadConfigFromCsvText(csvText, materialName, sourceName));
    }

    private static loadConfigFromCsvText(csvText: string, materialName: string, sourceName: string): MicrofacetConfig {
        const defaultConfig: MicrofacetConfig = new MicrofacetConfig();
        const normalizedName: string = materialName.trim().toLowerCase();
        const lines: string[] = MicroFacetedMaterial.readAsciiLines(csvText);

        const headerLine: string | null = lines.length > 0 ? lines[0]! : null;
        if (headerLine === null || headerLine.trim().length < 1) {
            throw new Error("Empty CSV header in file: " + sourceName);
        }
        const headerIndex: Map<string, number> = MicroFacetedMaterial.parseHeaderIndex(headerLine);

        let lineNumber: number;
        for (lineNumber = 1; lineNumber < lines.length; lineNumber++) {
            const line: string = lines[lineNumber]!;
            if (line.trim().length < 1) {
                continue;
            }
            const row: string[] = MicroFacetedMaterial.splitCsv(line);
            const rowName: string | null = MicroFacetedMaterial.field(row, headerIndex, "material_name");
            if (rowName === null) {
                continue;
            }
            if (rowName.trim().toLowerCase() !== normalizedName) {
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
                rowName.trim(),
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
                MicroFacetedMaterial.intField(row, headerIndex, "ndf_model", MicroFacetedMaterial.NDF_MODEL_BECKMANN),
                MicroFacetedMaterial.intField(
                    row,
                    headerIndex,
                    "geometry_model",
                    MicroFacetedMaterial.GEOMETRY_MODEL_SMITH,
                ),
            );
        }

        throw new Error("SimpleMaterial '" + materialName + "' not found in CSV: " + sourceName);
    }

    /**
    The lines Java's loop would obtain from `readAsciiLine`: the terminator is
    dropped and a `\r` before it with it, and the segment after a final
    newline — which Java never reaches, because that read throws — is not a
    line.
    */
    private static readAsciiLines(csvText: string): string[] {
        const lines: string[] = csvText.split("\n").map((line) => line.replace(/\r/g, ""));
        if (lines.length > 0 && lines[lines.length - 1] === "") {
            lines.pop();
        }
        return lines;
    }

    private static parseHeaderIndex(headerLine: string): Map<string, number> {
        const header: string[] = MicroFacetedMaterial.splitCsv(headerLine);
        const headerIndex = new Map<string, number>();
        let i: number;
        for (i = 0; i < header.length; i++) {
            headerIndex.set(header[i]!.trim().toLowerCase(), i);
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
        return row[index]!.trim();
    }

    private static doubleField(
        row: string[],
        headerIndex: Map<string, number>,
        key: string,
        defaultValue: number,
    ): number {
        const value: string | null = MicroFacetedMaterial.field(row, headerIndex, key);
        if (value === null || value.trim().length < 1) {
            return defaultValue;
        }
        return Number.parseFloat(value);
    }

    private static intField(
        row: string[],
        headerIndex: Map<string, number>,
        key: string,
        defaultValue: number,
    ): number {
        const value: string | null = MicroFacetedMaterial.field(row, headerIndex, key);
        if (value === null || value.trim().length < 1) {
            return defaultValue;
        }
        return Number.parseInt(value, 10);
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
}
