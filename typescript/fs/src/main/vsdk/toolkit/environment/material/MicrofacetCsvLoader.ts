import { readFileSync, existsSync } from "node:fs";
import { ColorRgb, MicrofacetConfig } from "@vitral/base";

/** Server-only loader for the filesystem constructor of Java MicroFacetedMaterial. */
export class MicrofacetCsvLoader {
    public static load(fileName: string, materialName: string): MicrofacetConfig {
        const resolved = existsSync(fileName) ? fileName : `etc/materials/${fileName}`;
        if (!existsSync(resolved)) throw new Error(`Microfacet CSV file not found: ${fileName}`);
        const lines = readFileSync(resolved, "utf8")
            .split(/\r?\n/)
            .filter((line) => line.trim().length > 0);
        const header = lines.shift();
        if (header === undefined) throw new Error(`Empty CSV header in file: ${resolved}`);
        const columns = new Map(header.split(",").map((name, i) => [name.trim().toLowerCase(), i]));
        const row = lines
            .map((line) => line.split(","))
            .find(
                (values) =>
                    this.field(values, columns, "material_name")?.toLowerCase() === materialName.trim().toLowerCase(),
            );
        if (row === undefined) throw new Error(`SimpleMaterial '${materialName}' not found in CSV: ${resolved}`);
        const number = (key: string, fallback: number): number => {
            const value = this.field(row, columns, key);
            return value === undefined || value.length === 0 ? fallback : Number(value);
        };
        const color = (r: string, g: string, b: string, fallback: ColorRgb): ColorRgb =>
            new ColorRgb(number(r, fallback.r()), number(g, fallback.g()), number(b, fallback.b()));
        const roughness = this.clamp(number("roughness", 0.35));
        const f0 = color("f0_r", "f0_g", "f0_b", new ColorRgb(0.04, 0.04, 0.04));
        return new MicrofacetConfig(
            this.field(row, columns, "material_name")!,
            new ColorRgb(0.1, 0.1, 0.1),
            color("diffuse_r", "diffuse_g", "diffuse_b", new ColorRgb(0.9, 0.5, 0.5)),
            f0,
            true,
            0,
            0,
            1,
            128,
            roughness,
            this.clamp(number("alpha", roughness * roughness)),
            f0,
            color("eta_r", "eta_g", "eta_b", new ColorRgb(1.5, 1.5, 1.5)),
            color("kappa_r", "kappa_g", "kappa_b", new ColorRgb()),
            this.clamp(number("kd", 1)),
            this.clamp(number("ks", 1)),
            Math.trunc(number("fresnel_model", 0)),
            Math.trunc(number("ndf_model", 0)),
            Math.trunc(number("geometry_model", 0)),
        );
    }
    private static field(row: string[], columns: Map<string, number>, name: string): string | undefined {
        const index = columns.get(name);
        return index === undefined ? undefined : row[index]?.trim();
    }
    private static clamp(value: number): number {
        return Math.max(0, Math.min(1, value));
    }
}
