package vsdk.toolkit.render;

import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.raytracing.RasterTileArea;
import vsdk.toolkit.render.raytracing.RasterTileGenerationStrategy;
import vsdk.toolkit.render.raytracing.RasterTileGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class TileGeneratorTest
{
    @Test
    void given_serialStrategy_when_generatingTiles_then_returnsSingleFullImageTile()
    {
        // Arrange
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(10, 6);

        RasterTileGenerator generator = new RasterTileGenerator(
            RasterTileGenerationStrategy.SERIAL, image, 10, 6, 4);

        // Action
        List<RasterTileArea> tiles = generator.getTiles();

        // Assert
        assertThat(tiles).hasSize(1);
        RasterTileArea tile = tiles.get(0);
        assertThat(tile.getImage()).isSameAs(image);
        assertThat(tile.getStartX()).isEqualTo(0);
        assertThat(tile.getStartY()).isEqualTo(0);
        assertThat(tile.getWidth()).isEqualTo(10);
        assertThat(tile.getHeight()).isEqualTo(6);
        assertThat(tile.getEndX()).isEqualTo(10);
        assertThat(tile.getEndY()).isEqualTo(6);
    }

    @Test
    void given_linearStrategy_when_generatingTiles_then_splitsInHorizontalBands()
    {
        // Arrange
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(8, 7);

        RasterTileGenerator generator = new RasterTileGenerator(
            RasterTileGenerationStrategy.LINEAR, image, 8, 7, 3);

        // Action
        List<RasterTileArea> tiles = generator.getTiles();

        // Assert
        assertThat(tiles).hasSize(3);
        assertThat(tiles.get(0).getImage()).isSameAs(image);
        assertThat(tiles.get(0).getStartX()).isEqualTo(0);
        assertThat(tiles.get(0).getStartY()).isEqualTo(0);
        assertThat(tiles.get(0).getWidth()).isEqualTo(8);
        assertThat(tiles.get(0).getHeight()).isEqualTo(3);
        assertThat(tiles.get(0).getEndX()).isEqualTo(8);
        assertThat(tiles.get(0).getEndY()).isEqualTo(3);

        assertThat(tiles.get(1).getStartY()).isEqualTo(3);
        assertThat(tiles.get(1).getHeight()).isEqualTo(2);
        assertThat(tiles.get(1).getEndY()).isEqualTo(5);
        assertThat(tiles.get(2).getStartY()).isEqualTo(5);
        assertThat(tiles.get(2).getHeight()).isEqualTo(2);
        assertThat(tiles.get(2).getEndY()).isEqualTo(7);
    }

    @Test
    void given_regionOrigin_when_generatingTiles_then_preservesTileAbsolutePosition()
    {
        // Arrange
        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(20, 20);

        RasterTileGenerator generator = new RasterTileGenerator(
            RasterTileGenerationStrategy.SERIAL, image, 4, 6, 9, 5, 1);

        // Action
        RasterTileArea tile = generator.getTiles().get(0);

        // Assert
        assertThat(tile.getImage()).isSameAs(image);
        assertThat(tile.getStartX()).isEqualTo(4);
        assertThat(tile.getStartY()).isEqualTo(6);
        assertThat(tile.getWidth()).isEqualTo(9);
        assertThat(tile.getHeight()).isEqualTo(5);
        assertThat(tile.getEndX()).isEqualTo(13);
        assertThat(tile.getEndY()).isEqualTo(11);
    }
}
