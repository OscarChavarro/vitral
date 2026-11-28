import { existsSync, readFileSync } from "node:fs";
import * as path from "node:path";
import { IllegalArgumentException, MicroFacetedMaterial } from "@vitral/base";

/**
The file-system half of Java's `MicroFacetedMaterial(String csvFileName,
String materialName)`: `loadConfigFromCsv` up to the opening of the file,
`resolveCsvFile`, and the read of its bytes. The parse is
`MicroFacetedMaterial.fromCsvText` in `@vitral/base`.
*/
export class MicrofacetCsvLoader {
    public static load(csvFileName: string | null, materialName: string | null): MicroFacetedMaterial {
        if (csvFileName === null || materialName === null) {
            return new MicroFacetedMaterial();
        }
        const csvFile: string | null = MicrofacetCsvLoader.resolveCsvFile(csvFileName);
        if (csvFile === null || !existsSync(csvFile)) {
            throw new IllegalArgumentException("Microfacet CSV file not found: " + csvFileName);
        }
        // Java reads the file byte by byte, one char per byte.
        const csvText: string = readFileSync(csvFile).toString("latin1");
        return MicroFacetedMaterial.fromCsvText(csvText, materialName, path.resolve(csvFile));
    }

    private static resolveCsvFile(csvFileName: string): string {
        const direct: string = csvFileName;
        if (existsSync(direct)) {
            return direct;
        }
        const fromEtc: string = path.join("etc/materials", csvFileName);
        if (existsSync(fromEtc)) {
            return fromEtc;
        }
        return direct;
    }
}
