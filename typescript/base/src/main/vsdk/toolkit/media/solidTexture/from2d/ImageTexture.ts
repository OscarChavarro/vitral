import { ColorRgba } from "../../../common/color/ColorRgba.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { IndexedColorImageHDRUncompressed } from "../../IndexedColorImageHDRUncompressed.js";
import { RGBAPixelHDR } from "../../RGBAPixelHDR.js";
import type { ControlledRGBAImageHDRUncompressed } from "./ControlledRGBAImageHDRUncompressed.js";
import { ImageToSolidTextureInterpolationTypes } from "./ImageToSolidTextureInterpolationTypes.js";
import { SolidTextureCoordinate, SolidTextureCoordinateMapper } from "./SolidTextureCoordinateMapper.js";

/**
Java's `int[] index` out parameter, which every sampling entry point below
writes the palette index into alongside the color it accumulates. A holder
object is the TypeScript spelling; see {@link SolidTextureCoordinate}.
*/
class _MaterialIndex {
    public value = 0;
}

/**
Port of `vsdk.toolkit.media.solidTexture.from2d.ImageTexture`.

Samples a 2D image as if it were a solid texture: {@link imageMap} takes the
color, {@link materialMap} takes the palette index as a material number, and
{@link bumpMap} takes three neighboring samples and builds a perturbed normal
from their luminance.

As in {@link ColorTextureFixture}, every sampler *adds* into the color it is
given rather than assigning it.
*/
export class ImageTexture {
    private static readonly MAX_PTS = 4;

    public imageMap(
        x: number,
        y: number,
        z: number,
        image: ControlledRGBAImageHDRUncompressed,
        color: ColorRgba,
        smallTolerance: number,
    ): void {
        const mapper = new SolidTextureCoordinateMapper();
        const coordinate = new SolidTextureCoordinate();
        const regNumber = new _MaterialIndex();
        if (mapper.map(x, y, z, image, coordinate, smallTolerance)) {
            color.setR(1.0);
            color.setG(1.0);
            color.setB(1.0);
            color.setA(1.0);
            return;
        }
        ImageTexture.imageColorAt(image, coordinate.x, coordinate.y, color, regNumber);
    }

    public materialMap(
        intersectionPoint: Vector3Dd,
        textureTransformationInverse: Matrix4x4d | null,
        materialImage: ControlledRGBAImageHDRUncompressed,
        numberOfMaterials: number,
        smallTolerance: number,
    ): number {
        let transformedPoint: Vector3Dd;
        if (textureTransformationInverse !== null) {
            transformedPoint = textureTransformationInverse.transpose().multiply(intersectionPoint);
        } else {
            transformedPoint = intersectionPoint;
        }

        const mapper = new SolidTextureCoordinateMapper();
        const coordinate = new SolidTextureCoordinate();
        const regNumber = new _MaterialIndex();
        let materialNumber: number;
        if (
            mapper.map(
                transformedPoint.x(),
                transformedPoint.y(),
                transformedPoint.z(),
                materialImage,
                coordinate,
                smallTolerance,
            )
        ) {
            materialNumber = 0;
        } else {
            const color = new ColorRgba();
            ImageTexture.imageColorAt(materialImage, coordinate.x, coordinate.y, color, regNumber);
            if (materialImage.getIndexedData() === null) {
                materialNumber = Math.trunc(color.getR()) * 255;
            } else {
                materialNumber = regNumber.value;
            }
        }
        if (numberOfMaterials > 0 && materialNumber >= numberOfMaterials) {
            materialNumber %= numberOfMaterials;
        }
        if (materialNumber < numberOfMaterials) {
            return materialNumber;
        }
        return -1;
    }

    public bumpMap(
        x: number,
        y: number,
        z: number,
        bumpImage: ControlledRGBAImageHDRUncompressed,
        bumpAmount: number,
        normal: Vector3Dd,
        smallTolerance: number,
    ): Vector3Dd {
        const mapper = new SolidTextureCoordinateMapper();
        const coordinate = new SolidTextureCoordinate();
        if (mapper.map(x, y, z, bumpImage, coordinate, smallTolerance)) {
            return normal;
        }

        const index = new _MaterialIndex();
        const index2 = new _MaterialIndex();
        const index3 = new _MaterialIndex();
        const color = new ColorRgba();
        const color2 = new ColorRgba();
        const color3 = new ColorRgba();
        ImageTexture.imageColorAt(bumpImage, coordinate.x, coordinate.y, color, index);

        coordinate.x--;
        coordinate.y++;
        ImageTexture.wrap(bumpImage, coordinate);
        ImageTexture.imageColorAt(bumpImage, coordinate.x, coordinate.y, color2, index2);

        coordinate.x += 2.0;
        ImageTexture.wrap(bumpImage, coordinate);
        ImageTexture.imageColorAt(bumpImage, coordinate.x, coordinate.y, color3, index3);

        let p1: Vector3Dd;
        let p2: Vector3Dd;
        let p3: Vector3Dd;
        if (bumpImage.getIndexedData() === null || bumpImage.getUseColorFlag()) {
            p1 = new Vector3Dd(0.0, bumpAmount * ImageTexture.luminance(color), 0.0);
            p2 = new Vector3Dd(0.0, bumpAmount * ImageTexture.luminance(color2), 1.0);
            p3 = new Vector3Dd(1.0, bumpAmount * ImageTexture.luminance(color3), 1.0);
        } else {
            p1 = new Vector3Dd(0.0, bumpAmount * index.value, 0.0);
            p2 = new Vector3Dd(0.0, bumpAmount * index2.value, 1.0);
            p3 = new Vector3Dd(1.0, bumpAmount * index3.value, 1.0);
        }

        let xPrime: Vector3Dd = p1.subtract(p2);
        let yPrime: Vector3Dd = p3.subtract(p2);
        const bumpNormal: Vector3Dd = yPrime.crossProduct(xPrime).normalized();

        yPrime = new Vector3Dd(normal.x(), normal.y(), normal.z());
        const temp = new Vector3Dd(0.0, 1.0, 0.0);
        xPrime = yPrime.crossProduct(temp);
        let length: number = xPrime.length();
        if (length < 1.0e-9) {
            if (Math.abs(normal.y() - 1.0) < smallTolerance) {
                yPrime = new Vector3Dd(0.0, 1.0, 0.0);
            } else {
                yPrime = new Vector3Dd(0.0, -1.0, 0.0);
            }
            xPrime = new Vector3Dd(1.0, 0.0, 0.0);
            length = 1.0;
        }
        xPrime = xPrime.multiply(1.0 / length);
        let zPrime: Vector3Dd = xPrime.crossProduct(yPrime).normalized();
        xPrime = xPrime.multiply(bumpNormal.x());
        yPrime = yPrime.multiply(bumpNormal.y());
        zPrime = zPrime.multiply(bumpNormal.z());
        return xPrime.add(yPrime).add(zPrime).normalized();
    }

    private static imageColorAt(
        image: ControlledRGBAImageHDRUncompressed,
        xCoordinate: number,
        yCoordinate: number,
        color: ColorRgba,
        index: _MaterialIndex,
    ): void {
        if (image.getInterpolationTypeEnum() === ImageToSolidTextureInterpolationTypes.NO_INTERPOLATION) {
            ImageTexture.noInterpolation(image, xCoordinate, yCoordinate, color, index);
        } else {
            ImageTexture.interp(image, xCoordinate, yCoordinate, color, index);
        }
    }

    private static noInterpolation(
        image: ControlledRGBAImageHDRUncompressed,
        xCoordinate: number,
        yCoordinate: number,
        color: ColorRgba,
        index: _MaterialIndex,
    ): void {
        const wrapped = new SolidTextureCoordinate(xCoordinate, yCoordinate);
        ImageTexture.wrap(image, wrapped);
        const x: number = Math.trunc(wrapped.x);
        const y: number = Math.trunc(wrapped.y);

        const indexedData: IndexedColorImageHDRUncompressed | null = image.getIndexedData();
        if (indexedData === null) {
            const pixel = new RGBAPixelHDR();
            image.getPixel(x, y, pixel);
            color.setR(color.getR() + pixel.r / 255.0);
            color.setG(color.getG() + pixel.g / 255.0);
            color.setB(color.getB() + pixel.b / 255.0);
            index.value = -1;
        } else {
            index.value = indexedData.getPixel(x, y);
            const mapColor: RGBAPixelHDR = indexedData.getColorTable()![index.value]!;
            color.setR(color.getR() + mapColor.r / 255.0);
            color.setG(color.getG() + mapColor.g / 255.0);
            color.setB(color.getB() + mapColor.b / 255.0);
            color.setA(color.getA() + mapColor.a / 255.0);
        }
    }

    private static interp(
        image: ControlledRGBAImageHDRUncompressed,
        xCoordinate: number,
        yCoordinate: number,
        color: ColorRgba,
        index: _MaterialIndex,
    ): void {
        const cornersIndex = new Array<number>(4).fill(0);
        const indexCrn = new Array<number>(4).fill(0);
        const cornerColor: ColorRgba[] = [new ColorRgba(), new ColorRgba(), new ColorRgba(), new ColorRgba()];
        const redCrn = new Array<number>(4).fill(0);
        const greenCrn = new Array<number>(4).fill(0);
        const blueCrn = new Array<number>(4).fill(0);
        const alphaCrn = new Array<number>(4).fill(0);
        let val1 = 0.0;
        let val2 = 0.0;
        let val3 = 0.0;
        let val4 = 0.0;
        const x: number = Math.trunc(xCoordinate);
        const y: number = Math.trunc(yCoordinate);

        if (image.getInterpolationTypeEnum() === ImageToSolidTextureInterpolationTypes.BI_LINEAR) {
            cornersIndex[0] = ImageTexture.noInterpolationAt(image, x + 1.0, y, cornerColor[0]!);
            cornersIndex[1] = ImageTexture.noInterpolationAt(image, x, y, cornerColor[1]!);
            cornersIndex[2] = ImageTexture.noInterpolationAt(image, x + 1.0, y - 1.0, cornerColor[2]!);
            cornersIndex[3] = ImageTexture.noInterpolationAt(image, x, y - 1.0, cornerColor[3]!);
            ImageTexture.fillChannels(cornerColor, redCrn, greenCrn, blueCrn, alphaCrn);
            val1 = ImageTexture.biLinear(redCrn, xCoordinate, yCoordinate);
            val2 = ImageTexture.biLinear(greenCrn, xCoordinate, yCoordinate);
            val3 = ImageTexture.biLinear(blueCrn, xCoordinate, yCoordinate);
            val4 = ImageTexture.biLinear(alphaCrn, xCoordinate, yCoordinate);
        }
        if (image.getInterpolationTypeEnum() === ImageToSolidTextureInterpolationTypes.NORMALIZED_DIST) {
            cornersIndex[0] = ImageTexture.noInterpolationAt(image, x, y - 1.0, cornerColor[0]!);
            cornersIndex[1] = ImageTexture.noInterpolationAt(image, x + 1.0, y - 1.0, cornerColor[1]!);
            cornersIndex[2] = ImageTexture.noInterpolationAt(image, x, y, cornerColor[2]!);
            cornersIndex[3] = ImageTexture.noInterpolationAt(image, x + 1.0, y, cornerColor[3]!);
            ImageTexture.fillChannels(cornerColor, redCrn, greenCrn, blueCrn, alphaCrn);
            val1 = ImageTexture.normDist(redCrn, xCoordinate, yCoordinate);
            val2 = ImageTexture.normDist(greenCrn, xCoordinate, yCoordinate);
            val3 = ImageTexture.normDist(blueCrn, xCoordinate, yCoordinate);
            val4 = ImageTexture.normDist(alphaCrn, xCoordinate, yCoordinate);
        }

        color.setR(color.getR() + val1);
        color.setG(color.getG() + val2);
        color.setB(color.getB() + val3);
        color.setA(color.getA() + val4);
        for (let i = 0; i < 4; i++) {
            indexCrn[i] = cornersIndex[i]!;
        }
        if (image.getInterpolationTypeEnum() === ImageToSolidTextureInterpolationTypes.BI_LINEAR) {
            index.value = Math.trunc(ImageTexture.biLinear(indexCrn, xCoordinate, yCoordinate) + 0.5);
        }
        if (image.getInterpolationTypeEnum() === ImageToSolidTextureInterpolationTypes.NORMALIZED_DIST) {
            index.value = Math.trunc(ImageTexture.normDist(indexCrn, xCoordinate, yCoordinate) + 0.5);
        }
    }

    private static noInterpolationAt(
        image: ControlledRGBAImageHDRUncompressed,
        x: number,
        y: number,
        color: ColorRgba,
    ): number {
        const index = new _MaterialIndex();
        ImageTexture.noInterpolation(image, x, y, color, index);
        return index.value;
    }

    private static biLinear(corners: readonly number[], x: number, y: number): number {
        const p: number = x - Math.trunc(x);
        const q: number = y - Math.trunc(y);
        if (p === 0.0 && q === 0.0) {
            return corners[0]!;
        }
        return (
            p * q * corners[0]! +
            q * (1.0 - p) * corners[1]! +
            p * (1.0 - q) * corners[2]! +
            (1.0 - p) * (1.0 - q) * corners[3]!
        );
    }

    private static normDist(corners: readonly number[], x: number, y: number): number {
        const p: number = x - Math.trunc(x);
        const q: number = y - Math.trunc(y);
        if (p === 0.0 && q === 0.0) {
            return corners[0]!;
        }
        const wts = new Array<number>(ImageTexture.MAX_PTS).fill(0);
        wts[0] = ImageTexture.pythagoreanSq(p, q);
        wts[1] = ImageTexture.pythagoreanSq(1.0 - p, q);
        wts[2] = ImageTexture.pythagoreanSq(p, 1.0 - q);
        wts[3] = ImageTexture.pythagoreanSq(1.0 - p, 1.0 - q);
        let sumInvWts = 0.0;
        let sumI = 0.0;
        for (let i = 0; i < ImageTexture.MAX_PTS; i++) {
            sumInvWts += 1.0 / wts[i]!;
            sumI += corners[i]! / wts[i]!;
        }
        return sumI / sumInvWts;
    }

    private static pythagoreanSq(a: number, b: number): number {
        return a * a + b * b;
    }

    private static fillChannels(
        cornerColor: readonly ColorRgba[],
        redCrn: number[],
        greenCrn: number[],
        blueCrn: number[],
        alphaCrn: number[],
    ): void {
        for (let i = 0; i < 4; i++) {
            redCrn[i] = cornerColor[i]!.getR();
            greenCrn[i] = cornerColor[i]!.getG();
            blueCrn[i] = cornerColor[i]!.getB();
            alphaCrn[i] = cornerColor[i]!.getA();
        }
    }

    private static luminance(color: ColorRgba): number {
        return 0.229 * color.getR() + 0.587 * color.getG() + 0.114 * color.getB();
    }

    private static wrap(image: ControlledRGBAImageHDRUncompressed, coordinate: SolidTextureCoordinate): void {
        if (coordinate.x < 0.0) coordinate.x += image.getXSize();
        else if (coordinate.x >= image.getXSize()) coordinate.x -= image.getXSize();
        if (coordinate.y < 0.0) coordinate.y += image.getYSize();
        else if (coordinate.y >= image.getYSize()) coordinate.y -= image.getYSize();
    }
}
