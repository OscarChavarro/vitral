import { ArrayList } from "../../../../java/util/ArrayList.js";
import { Collections } from "../../../../java/util/Collections.js";
import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { IllegalStateException } from "../../../../java/lang/IllegalStateException.js";

import type { Image } from "../../media/Image.js";
import { RasterTileArea } from "./RasterTileArea.js";
import { RasterTileGenerationStrategy } from "./RasterTileGenerationStrategy.js";

/**
Generates image sub-regions ("tiles") according to a scheduling strategy.

Current strategies:
  - `LINEAR`: creates horizontal bands.
  - `SERIAL`: creates one tile for the whole requested region.
*/
export class RasterTileGenerator {
    private readonly strategy: RasterTileGenerationStrategy;
    private readonly image: Image;
    private readonly x0: number;
    private readonly y0: number;
    private readonly width: number;
    private readonly height: number;
    private readonly numberOfThreads: number;
    private readonly tiles: ArrayList<RasterTileArea>;

    public constructor(
        strategy: RasterTileGenerationStrategy,
        image: Image,
        width: number,
        height: number,
        numberOfThreads: number,
    );
    public constructor(
        strategy: RasterTileGenerationStrategy,
        image: Image,
        x0: number,
        y0: number,
        width: number,
        height: number,
        numberOfThreads: number,
    );
    public constructor(
        strategy: RasterTileGenerationStrategy,
        image: Image,
        a: number,
        b: number,
        c: number,
        d?: number,
        e?: number,
    ) {
        const x0: number = e === undefined ? 0 : a;
        const y0: number = e === undefined ? 0 : b;
        const width: number = e === undefined ? a : c;
        const height: number = e === undefined ? b : d!;
        const numberOfThreads: number = e === undefined ? c : e;

        if (strategy === null) {
            throw new IllegalArgumentException("strategy can not be null");
        }
        if (image === null) {
            throw new IllegalArgumentException("image can not be null");
        }
        if (x0 < 0 || y0 < 0) {
            throw new IllegalArgumentException("origin must be >= 0");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
        if (x0 + width > image.getXSize() || y0 + height > image.getYSize()) {
            throw new IllegalArgumentException("requested tile area must be inside image");
        }
        if (numberOfThreads <= 0) {
            throw new IllegalArgumentException("numberOfThreads must be > 0");
        }

        this.strategy = strategy;
        this.image = image;
        this.x0 = x0;
        this.y0 = y0;
        this.width = width;
        this.height = height;
        this.numberOfThreads = numberOfThreads;
        this.tiles = Collections.unmodifiableList(this.generateTiles());
    }

    public getTiles(): ArrayList<RasterTileArea> {
        return this.tiles;
    }

    private generateTiles(): ArrayList<RasterTileArea> {
        switch (this.strategy) {
            case RasterTileGenerationStrategy.LINEAR:
                return this.generateLinearTiles();
            case RasterTileGenerationStrategy.SERIAL:
                return this.generateSerialTile();
            default:
                throw new IllegalStateException("Unsupported tile strategy: " + String(this.strategy));
        }
    }

    private generateLinearTiles(): ArrayList<RasterTileArea> {
        const out: ArrayList<RasterTileArea> = new ArrayList<RasterTileArea>();

        let workerBands: number = this.numberOfThreads;
        if (workerBands > this.height) {
            workerBands = this.height;
        }

        const baseBandHeight: number = Math.trunc(this.height / workerBands);
        const extraRows: number = this.height % workerBands;
        let y = 0;

        for (let i = 0; i < workerBands; i++) {
            const currentBandHeight: number = baseBandHeight + (i < extraRows ? 1 : 0);
            out.add(new RasterTileArea(this.image, this.x0, this.y0 + y, this.width, currentBandHeight));
            y += currentBandHeight;
        }

        return out;
    }

    private generateSerialTile(): ArrayList<RasterTileArea> {
        const out: ArrayList<RasterTileArea> = new ArrayList<RasterTileArea>();
        out.add(new RasterTileArea(this.image, this.x0, this.y0, this.width, this.height));
        return out;
    }
}
