import { RGBImageUncompressed } from "../media/RGBImageUncompressed.js";
import { RGBPixel } from "../media/RGBPixel.js";
import type { ZBuffer } from "../media/ZBuffer.js";
import { ImageProcessing } from "../processing/ImageProcessing.js";
import { RenderingElement } from "./RenderingElement.js";

/**
The AutoStereogramGenerator class implements an strategy for calculating
autostereographic images from random color tiles or SIRDS.

The algorithm in this class is an adapted version of "OpenStereogram" system
available at http://code.google.com/p/openstereogram.

Java's `int` arithmetic is kept: every `(int)` cast and every integer
division truncates (`Math.trunc`).
*/
export class AutoStereogramGenerator extends RenderingElement {
    private static getMinDepth(
        separationFactor: number,
        maxdepth: number,
        observationDistance: number,
        suppliedMinDepth: number,
    ): number {
        const computedMinDepth: number = Math.trunc(
            (separationFactor * maxdepth * observationDistance) /
                ((1 - separationFactor) * maxdepth + observationDistance),
        );

        return Math.min(Math.max(computedMinDepth, suppliedMinDepth), maxdepth);
    }

    private static getMaxDepth(suppliedMaxDepth: number, observationDistance: number): number {
        return Math.max(Math.min(suppliedMaxDepth, observationDistance), 0);
    }

    private static convertoToPixels(valueInches: number, ppi: number): number {
        return Math.trunc(valueInches * ppi);
    }

    /** Given a double depth value (usually from 0 to 1), this function
    scales it linearly and clamps it to [minDepth, maxDepth],
    usually [0, 255] */
    private static getZ(depth: number, maxDepth: number, minDepth: number): number {
        return Math.trunc((Math.trunc(depth * 255) * (maxDepth - minDepth)) / 255);
    }

    private static getSeparation(observationDistance: number, eyeSeparation: number, depth: number): number {
        return Math.trunc((eyeSeparation * depth) / (depth + observationDistance));
    }

    /**
    Easy to use version of `generate` method with some common defaults.
    */
    public static generate(result: RGBImageUncompressed, tilePattern: RGBImageUncompressed, depthMap: ZBuffer): void;

    /**
    Given a z-buffer depth map, this method fills in the `resulting` image,
    which must be previously initialized to be the same size of the depth map.

    The method builds a SIRD (autostereogram) over repetitions of the
    `tilePattern` image, wich must be repeatable.

    Requires result and depthMap to be of equal size
    */
    public static generate(
        result: RGBImageUncompressed,
        tilePattern: RGBImageUncompressed,
        depthMap: ZBuffer,
        observationDistanceInches: number,
        eyeSeparationInches: number,
        maxDepthInches: number,
        minDepthInches: number,
        horizontalPPI: number,
        verticalPPI: number,
        animationXOffset: number,
        animationYOffset: number,
    ): void;

    public static generate(
        result: RGBImageUncompressed,
        tilePattern: RGBImageUncompressed,
        depthMap: ZBuffer,
        observationDistanceInches?: number,
        eyeSeparationInches?: number,
        maxDepthInches?: number,
        minDepthInches?: number,
        horizontalPPI?: number,
        verticalPPI?: number,
        animationXOffset?: number,
        animationYOffset?: number,
    ): void {
        if (observationDistanceInches === undefined) {
            AutoStereogramGenerator.generateFull(result, tilePattern, depthMap, 14.0, 2.5, 12, 0, 81, 81, 0, 0);
            return;
        }
        AutoStereogramGenerator.generateFull(
            result,
            tilePattern,
            depthMap,
            observationDistanceInches,
            eyeSeparationInches!,
            maxDepthInches!,
            minDepthInches!,
            horizontalPPI!,
            verticalPPI!,
            animationXOffset!,
            animationYOffset!,
        );
    }

    private static generateFull(
        result: RGBImageUncompressed,
        tilePattern: RGBImageUncompressed,
        depthMap: ZBuffer,
        observationDistanceInches: number,
        eyeSeparationInches: number,
        maxDepthInches: number,
        minDepthInches: number,
        horizontalPPI: number,
        verticalPPI: number,
        animationXOffset: number,
        animationYOffset: number,
    ): void {
        let x: number, y: number;
        let rdx: number, rdy: number, tdy: number;

        if (animationXOffset < 0) {
            animationXOffset *= -1;
        }
        if (animationYOffset < 0) {
            animationYOffset *= -1;
        }
        animationXOffset %= 256;
        animationYOffset %= 256;

        rdx = result.getXSize();
        rdy = result.getYSize();

        let p: RGBPixel;

        p = new RGBPixel();

        const linksL: Int32Array = new Int32Array(rdx);
        const linksR: Int32Array = new Int32Array(rdx);

        const observationDistance: number = AutoStereogramGenerator.convertoToPixels(
            observationDistanceInches,
            horizontalPPI,
        );
        const eyeSeparation: number = AutoStereogramGenerator.convertoToPixels(eyeSeparationInches, horizontalPPI);
        const maxDepth: number = AutoStereogramGenerator.getMaxDepth(
            AutoStereogramGenerator.convertoToPixels(maxDepthInches, horizontalPPI),
            observationDistance,
        );
        const minDepth: number = AutoStereogramGenerator.getMinDepth(
            0.55,
            maxDepth,
            observationDistance,
            AutoStereogramGenerator.convertoToPixels(minDepthInches, horizontalPPI),
        );
        const verticalShift: number = Math.trunc(verticalPPI / 16);
        const maxSeparation: number = AutoStereogramGenerator.getSeparation(
            observationDistance,
            eyeSeparation,
            maxDepth,
        );

        const scaledTilePattern: RGBImageUncompressed = new RGBImageUncompressed();
        scaledTilePattern.initNoFill(
            maxSeparation,
            Math.trunc((maxSeparation * tilePattern.getYSize()) / tilePattern.getXSize()),
        );

        // Java also reads the scaled tile's width into an unused `tdx`
        tdy = scaledTilePattern.getYSize();

        ImageProcessing.resize(tilePattern, scaledTilePattern);

        for (y = 0; y < rdy; y++) {
            for (x = 0; x < rdx; x++) {
                linksL[x] = x;
                linksR[x] = x;
            }
            for (x = 0; x < rdx; x++) {
                const depth: number = AutoStereogramGenerator.getZ(depthMap.getZ(x, y), maxDepth, minDepth);
                const separation: number = AutoStereogramGenerator.getSeparation(
                    observationDistance,
                    eyeSeparation,
                    depth,
                );
                const left: number = x - Math.trunc(separation / 2);
                const right: number = left + separation;

                if (left >= 0 && right < rdx) {
                    let visible = true;

                    if (linksL[right] !== right) {
                        if (linksL[right]! < left) {
                            linksR[linksL[right]!] = linksL[right]!;
                            linksL[right] = right;
                        } else {
                            visible = false;
                        }
                    }
                    if (linksR[left] !== left) {
                        if (linksR[left]! > right) {
                            linksL[linksR[left]!] = linksR[left]!;
                            linksR[left] = left;
                        } else {
                            visible = false;
                        }
                    }

                    if (visible) {
                        linksL[right] = left;
                        linksR[left] = right;
                    }
                }
            }

            let lastLinked = -10;
            for (x = 0; x < rdx; x++) {
                if (linksL[x] === x) {
                    if (lastLinked === x - 1) {
                        result.getPixelRgb(x - 1, y, p);
                        result.putPixelRgb(x, y, p);
                    } else {
                        scaledTilePattern.getPixelRgb(
                            (x + animationXOffset) % maxSeparation,
                            (y + animationYOffset + Math.trunc(x / maxSeparation) * verticalShift) % tdy,
                            p,
                        );
                        result.putPixelRgb(x, y, p);
                    }
                } else {
                    result.getPixelRgb(linksL[x]!, y, p);
                    result.putPixelRgb(x, y, p);
                    lastLinked = x;
                }
            }
        }
    }
}
