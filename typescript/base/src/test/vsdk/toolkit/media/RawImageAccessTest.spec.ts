import { describe, expect, it } from "vitest";
import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import { RGBAImageUncompressed } from "vsdk/toolkit/media/RGBAImageUncompressed.js";
import { RGBImageUncompressed } from "vsdk/toolkit/media/RGBImageUncompressed.js";

/**
Translation of `java/base/src/test/vsdk/toolkit/media/RawImageAccessTest.java`.

Exercises the raw pixel access of uncompressed images, whose memory order goes
from the bottom row to the top one, while pixel coordinates start on the top row.

Java's direct `ByteBuffer` is a `Uint8Array` in this edition. A typed array has
no read position, so `position()` being zero becomes the view starting at the
first byte of its buffer.
*/
describe("RawImageAccessTest", () => {
    const TWO_ROWS_RGBA: Int8Array = Int8Array.of(
        1,
        2,
        3,
        4, // Bottom row
        5,
        6,
        7,
        8, // Top row
    );

    const TWO_ROWS_RGB: Int8Array = Int8Array.of(
        1,
        2,
        3, // Bottom row
        5,
        6,
        7, // Top row
    );

    it("given_rawRgbaPixels_when_set_then_theFirstRowIsTheBottomOne", () => {
        // Arrange
        const image = new RGBAImageUncompressed();

        // Act
        image.setRawImage(1, 2, TWO_ROWS_RGBA);

        // Assert
        expect(image.getXSize()).toBe(1);
        expect(image.getYSize()).toBe(2);
        expect(image.getPixel(0, 0).r).toBe(5);
        expect(image.getPixel(0, 1).r).toBe(1);
        expect(image.getRawImageDirectBuffer()).toBeInstanceOf(Uint8Array);
    });

    it("given_rgbaImage_when_rawImageIsAsked_then_getsAnIndependentCopy", () => {
        // Arrange
        const image = new RGBAImageUncompressed();
        image.setRawImage(1, 2, TWO_ROWS_RGBA);

        // Act
        const copy: Int8Array = image.getRawImage();
        copy[0] = 99;

        // Assert
        expect(image.getRawImage()).toEqual(TWO_ROWS_RGBA);
        expect(image.getRawImageSizeInBytes()).toBe(8);
        expect(image.getRawImageDirectBuffer().byteOffset).toBe(0);
    });

    it("given_rawRgbPixels_when_set_then_theFirstRowIsTheBottomOne", () => {
        // Arrange
        const image = new RGBImageUncompressed();

        // Act
        image.setRawImage(1, 2, TWO_ROWS_RGB);

        // Assert
        expect(image.getPixel(0, 0).r).toBe(5);
        expect(image.getPixel(0, 1).r).toBe(1);
        expect(image.getRawImage()).toEqual(TWO_ROWS_RGB);
        expect(image.getRawImageSizeInBytes()).toBe(6);
    });

    it("given_rawPixelsOfTheWrongSize_when_set_then_isRejected", () => {
        expect(() => new RGBAImageUncompressed().setRawImage(2, 2, TWO_ROWS_RGBA)).toThrow(IllegalArgumentException);
        expect(() => new RGBImageUncompressed().setRawImage(2, 2, TWO_ROWS_RGB)).toThrow(IllegalArgumentException);
    });
});
