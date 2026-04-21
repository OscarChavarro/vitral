package vsdk.toolkit.render;

import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.media.RGBImage;

import static org.assertj.core.api.Assertions.assertThat;

class TileGeneratorTest
{
    @Test
    void given_serialStrategy_when_generatingTiles_then_returnsSingleFullImageTile()
    {
        RGBImage image = new RGBImage();
        image.init(10, 6);

        TileGenerator generator = new TileGenerator(
            TileGenerationStrategy.SERIAL, image, 10, 6, 4);

        List<Tile> tiles = generator.getTiles();

        assertThat(tiles).hasSize(1);
        Tile tile = tiles.get(0);
        assertThat(tile.getImage()).isSameAs(image);
        assertThat(tile.getX0()).isEqualTo(0);
        assertThat(tile.getY0()).isEqualTo(0);
        assertThat(tile.getDx()).isEqualTo(10);
        assertThat(tile.getDy()).isEqualTo(6);
        assertThat(tile.getX1()).isEqualTo(10);
        assertThat(tile.getY1()).isEqualTo(6);
    }

    @Test
    void given_linearStrategy_when_generatingTiles_then_splitsInHorizontalBands()
    {
        RGBImage image = new RGBImage();
        image.init(8, 7);

        TileGenerator generator = new TileGenerator(
            TileGenerationStrategy.LINEAR, image, 8, 7, 3);

        List<Tile> tiles = generator.getTiles();

        assertThat(tiles).hasSize(3);
        assertThat(tiles.get(0).getImage()).isSameAs(image);
        assertThat(tiles.get(0).getX0()).isEqualTo(0);
        assertThat(tiles.get(0).getY0()).isEqualTo(0);
        assertThat(tiles.get(0).getDx()).isEqualTo(8);
        assertThat(tiles.get(0).getDy()).isEqualTo(3);
        assertThat(tiles.get(0).getX1()).isEqualTo(8);
        assertThat(tiles.get(0).getY1()).isEqualTo(3);

        assertThat(tiles.get(1).getY0()).isEqualTo(3);
        assertThat(tiles.get(1).getDy()).isEqualTo(2);
        assertThat(tiles.get(1).getY1()).isEqualTo(5);
        assertThat(tiles.get(2).getY0()).isEqualTo(5);
        assertThat(tiles.get(2).getDy()).isEqualTo(2);
        assertThat(tiles.get(2).getY1()).isEqualTo(7);
    }

    @Test
    void given_regionOrigin_when_generatingTiles_then_preservesTileAbsolutePosition()
    {
        RGBImage image = new RGBImage();
        image.init(20, 20);

        TileGenerator generator = new TileGenerator(
            TileGenerationStrategy.SERIAL, image, 4, 6, 9, 5, 1);

        Tile tile = generator.getTiles().get(0);

        assertThat(tile.getImage()).isSameAs(image);
        assertThat(tile.getX0()).isEqualTo(4);
        assertThat(tile.getY0()).isEqualTo(6);
        assertThat(tile.getDx()).isEqualTo(9);
        assertThat(tile.getDy()).isEqualTo(5);
        assertThat(tile.getX1()).isEqualTo(13);
        assertThat(tile.getY1()).isEqualTo(11);
    }
}
