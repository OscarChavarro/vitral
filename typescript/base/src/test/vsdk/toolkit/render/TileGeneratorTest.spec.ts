import { describe, expect, it } from "vitest";
import type { ArrayList } from "java/util/ArrayList.js";
import { RGBImageUncompressed } from "vsdk/toolkit/media/RGBImageUncompressed.js";
import type { RasterTileArea } from "vsdk/toolkit/render/raytracing/RasterTileArea.js";
import { RasterTileGenerationStrategy } from "vsdk/toolkit/render/raytracing/RasterTileGenerationStrategy.js";
import { RasterTileGenerator } from "vsdk/toolkit/render/raytracing/RasterTileGenerator.js";

/** Translation of `java/base/src/test/vsdk/toolkit/render/TileGeneratorTest.java`. */
describe("TileGeneratorTest", () => {
    it("given_serialStrategy_when_generatingTiles_then_returnsSingleFullImageTile", () => {
        // Arrange
        const image = new RGBImageUncompressed();
        image.init(10, 6);

        const generator = new RasterTileGenerator(RasterTileGenerationStrategy.SERIAL, image, 10, 6, 4);

        // Action
        const tiles: ArrayList<RasterTileArea> = generator.getTiles();

        // Assert
        expect(tiles.size()).toBe(1);
        const tile: RasterTileArea = tiles.get(0);
        expect(tile.getImage()).toBe(image);
        expect(tile.getStartX()).toBe(0);
        expect(tile.getStartY()).toBe(0);
        expect(tile.getWidth()).toBe(10);
        expect(tile.getHeight()).toBe(6);
        expect(tile.getEndX()).toBe(10);
        expect(tile.getEndY()).toBe(6);
    });

    it("given_linearStrategy_when_generatingTiles_then_splitsInHorizontalBands", () => {
        // Arrange
        const image = new RGBImageUncompressed();
        image.init(8, 7);

        const generator = new RasterTileGenerator(RasterTileGenerationStrategy.LINEAR, image, 8, 7, 3);

        // Action
        const tiles: ArrayList<RasterTileArea> = generator.getTiles();

        // Assert
        expect(tiles.size()).toBe(3);
        expect(tiles.get(0).getImage()).toBe(image);
        expect(tiles.get(0).getStartX()).toBe(0);
        expect(tiles.get(0).getStartY()).toBe(0);
        expect(tiles.get(0).getWidth()).toBe(8);
        expect(tiles.get(0).getHeight()).toBe(3);
        expect(tiles.get(0).getEndX()).toBe(8);
        expect(tiles.get(0).getEndY()).toBe(3);

        expect(tiles.get(1).getStartY()).toBe(3);
        expect(tiles.get(1).getHeight()).toBe(2);
        expect(tiles.get(1).getEndY()).toBe(5);
        expect(tiles.get(2).getStartY()).toBe(5);
        expect(tiles.get(2).getHeight()).toBe(2);
        expect(tiles.get(2).getEndY()).toBe(7);
    });

    it("given_regionOrigin_when_generatingTiles_then_preservesTileAbsolutePosition", () => {
        // Arrange
        const image = new RGBImageUncompressed();
        image.init(20, 20);

        const generator = new RasterTileGenerator(RasterTileGenerationStrategy.SERIAL, image, 4, 6, 9, 5, 1);

        // Action
        const tile: RasterTileArea = generator.getTiles().get(0);

        // Assert
        expect(tile.getImage()).toBe(image);
        expect(tile.getStartX()).toBe(4);
        expect(tile.getStartY()).toBe(6);
        expect(tile.getWidth()).toBe(9);
        expect(tile.getHeight()).toBe(5);
        expect(tile.getEndX()).toBe(13);
        expect(tile.getEndY()).toBe(11);
    });
});
