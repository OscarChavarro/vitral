// VSDK classes
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { PresentationElement } from "../PresentationElement.js";
import type { VisualVariableSet } from "./VisualVariableSet.js";

export class PercentageWheelWidget extends PresentationElement {
    private dataset: VisualVariableSet;

    /// A number in the range (0.0, outerRadius)
    private innerRadius: number;

    /// A number in the range (innerRadius, 1.0-borderWidth]
    private outerRadius: number;

    /// Width of the wheel border in the range (0.0, 1.0)
    private borderWidth: number;

    /// A number to split 360.0 degrees on used to draw wheel
    private aproximationSteps: number;

    /// Geometric configuration
    private position: Vector3Dd;
    private scale: number;

    /// Interaction control. -1 means "no selection"
    private selectedSector: number;
    private highligtedSector: number;
    private defaultTitle: string;

    /// Graphic design configuration
    private borderBackgroundColor: ColorRgb;
    private borderSelectedColor: ColorRgb;
    private borderHighlightedColor: ColorRgb;
    private sectorForegroundColor: ColorRgb;
    private sectorLineColor: ColorRgb;
    private wheelBackgroundColor: ColorRgb;
    private trackLineColor: ColorRgb;
    private selectedSectorColor: ColorRgb;
    private highlightedSectorColor: ColorRgb;

    public constructor(dataset: VisualVariableSet) {
        super();
        this.dataset = dataset;
        this.innerRadius = 0.2; // 20%
        this.borderWidth = 0.07; // 7%
        this.outerRadius = 1.0 - this.borderWidth;

        // Should be entirely divisible by dataset size!
        this.aproximationSteps = 160.0;

        this.borderBackgroundColor = new ColorRgb(0.38671, 0.38671, 0.38671);
        this.sectorLineColor = new ColorRgb(0.1289, 0.1289, 0.1289);
        this.sectorForegroundColor = new ColorRgb(1, 1, 0);
        this.wheelBackgroundColor = new ColorRgb(43.0 / 256.0, 43.0 / 256.0, 43.0 / 256.0);
        this.trackLineColor = new ColorRgb(60.0 / 256.0, 60.0 / 256.0, 60.0 / 256.0);
        this.selectedSectorColor = new ColorRgb(1, 1, 1);
        this.highlightedSectorColor = new ColorRgb(1, 1, 0.5);
        this.borderSelectedColor = new ColorRgb(1, 1, 1);
        this.borderHighlightedColor = new ColorRgb(146.0 / 256.0, 146.0 / 256.0, 146.0 / 256.0);

        this.position = new Vector3Dd(0, 0, 0);

        this.scale = 1.0;

        this.selectedSector = -1;
        this.highligtedSector = -1;

        this.defaultTitle = "Select Issue";
    }

    /**
    @return the innerRadius
    */
    public getInnerRadius(): number {
        return this.innerRadius;
    }

    /**
    @param innerRadius the innerRadius to set
    */
    public setInnerRadius(innerRadius: number): void {
        this.innerRadius = innerRadius;
    }

    /**
    @return the outerRadius
    */
    public getOuterRadius(): number {
        return this.outerRadius;
    }

    /**
    @param outerRadius the outerRadius to set
    */
    public setOuterRadius(outerRadius: number): void {
        this.outerRadius = outerRadius;
    }

    /**
    @return the aproximationSteps
    */
    public getAproximationSteps(): number {
        return this.aproximationSteps;
    }

    /**
    @param aproximationStep the aproximationSteps to set
    */
    public setAproximationSteps(aproximationStep: number): void {
        this.aproximationSteps = aproximationStep;
    }

    /**
    @return the borderWidth
    */
    public getBorderWidth(): number {
        return this.borderWidth;
    }

    /**
    @param boderWidth the borderWidth to set
    */
    public setBorderWidth(boderWidth: number): void {
        this.borderWidth = boderWidth;
    }

    /**
    @return the borderBackgroundColor
    */
    public getBorderBackgroundColor(): ColorRgb {
        return this.borderBackgroundColor;
    }

    /**
    @param borderBackgroundColor the borderBackgroundColor to set
    */
    public setBorderBackgroundColor(borderBackgroundColor: ColorRgb): void {
        this.borderBackgroundColor = borderBackgroundColor;
    }

    /**
    @return the dataset
    */
    public getDataset(): VisualVariableSet {
        return this.dataset;
    }

    /**
    @param dataset the dataset to set
    */
    public setDataset(dataset: VisualVariableSet): void {
        this.dataset = dataset;
    }

    /**
    @return the sectorLineColor
    */
    public getSectorLineColor(): ColorRgb {
        return this.sectorLineColor;
    }

    /**
    @param sectorLineColor the sectorLineColor to set
    */
    public setSectorLineColor(sectorLineColor: ColorRgb): void {
        this.sectorLineColor = sectorLineColor;
    }

    /**
    @return the sectorForegroundColor
    */
    public getSectorForegroundColor(): ColorRgb {
        return this.sectorForegroundColor;
    }

    /**
    @param sectorBackgroundColor the sectorForegroundColor to set
    */
    public setSectorForegroundColor(sectorBackgroundColor: ColorRgb): void {
        this.sectorForegroundColor = sectorBackgroundColor;
    }

    /**
    @return the wheelBackgroundColor
    */
    public getWheelBackgroundColor(): ColorRgb {
        return this.wheelBackgroundColor;
    }

    /**
    @param wheelBackgroundColor the wheelBackgroundColor to set
    */
    public setWheelBackgroundColor(wheelBackgroundColor: ColorRgb): void {
        this.wheelBackgroundColor = wheelBackgroundColor;
    }

    /**
    @return the trackLineColor
    */
    public getTrackLineColor(): ColorRgb {
        return this.trackLineColor;
    }

    /**
    @param trackLineColor the trackLineColor to set
    */
    public setTrackLineColor(trackLineColor: ColorRgb): void {
        this.trackLineColor = trackLineColor;
    }

    /**
    @return the position
    */
    public getPosition(): Vector3Dd {
        return this.position;
    }

    /**
    @param position the position to set
    */
    public setPosition(position: Vector3Dd): void {
        this.position = position;
    }

    /**
    @return the scale
    */
    public getScale(): number {
        return this.scale;
    }

    /**
    @param scale the scale to set
    */
    public setScale(scale: number): void {
        this.scale = scale;
    }

    /**
    @return the selectedSector
    */
    public getSelectedSector(): number {
        return this.selectedSector;
    }

    /**
    @param selectedSector the selectedSector to set
    */
    public setSelectedSector(selectedSector: number): void {
        this.selectedSector = selectedSector;
    }

    /**
    @return the highligtedSector
    */
    public getHighligtedSector(): number {
        return this.highligtedSector;
    }

    /**
    @param highligtedSector the highligtedSector to set
    */
    public setHighligtedSector(highligtedSector: number): void {
        this.highligtedSector = highligtedSector;
    }

    /**
    @return the selectedSectorColor
    */
    public getSelectedSectorColor(): ColorRgb {
        return this.selectedSectorColor;
    }

    /**
    @param selectedSectorColor the selectedSectorColor to set
    */
    public setSelectedSectorColor(selectedSectorColor: ColorRgb): void {
        this.selectedSectorColor = selectedSectorColor;
    }

    /**
    @return the highlightedSectorColor
    */
    public getHighlightedSectorColor(): ColorRgb {
        return this.highlightedSectorColor;
    }

    /**
    @param highlightedSectorColor the highlightedSectorColor to set
    */
    public setHighlightedSectorColor(highlightedSectorColor: ColorRgb): void {
        this.highlightedSectorColor = highlightedSectorColor;
    }

    /**
    @return the borderSelectedColor
    */
    public getBorderSelectedColor(): ColorRgb {
        return this.borderSelectedColor;
    }

    /**
    @param borderSelectedColor the borderSelectedColor to set
    */
    public setBorderSelectedColor(borderSelectedColor: ColorRgb): void {
        this.borderSelectedColor = borderSelectedColor;
    }

    /**
    @return the borderHighlightedColor
    */
    public getBorderHighlightedColor(): ColorRgb {
        return this.borderHighlightedColor;
    }

    /**
    @param borderHighlightedColor the borderHighlightedColor to set
    */
    public setBorderHighlightedColor(borderHighlightedColor: ColorRgb): void {
        this.borderHighlightedColor = borderHighlightedColor;
    }

    /**
    @return the defaultTitle
    */
    public getDefaultTitle(): string {
        return this.defaultTitle;
    }

    /**
    @param defaultTitle the defaultTitle to set
    */
    public setDefaultTitle(defaultTitle: string): void {
        this.defaultTitle = defaultTitle;
    }
}
