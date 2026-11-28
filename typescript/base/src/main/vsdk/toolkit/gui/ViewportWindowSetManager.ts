//=   previous "ViewportOrganizer" class at SceneEditorApplication example).=

// Basic Java classes
import { ArrayList } from "../../../java/util/ArrayList.js";

// VitralSDK classes
import { PresentationElement } from "./PresentationElement.js";
import type { ViewportWindow } from "./ViewportWindow.js";

export class ViewportWindowSetManager extends PresentationElement {
    private selectedViewIndex: number;
    private globalViewportXSize: number;
    private globalViewportYSize: number;
    private viewOrderStyle: number;
    private fullViewport: boolean;
    private readonly views: ArrayList<ViewportWindow>;

    public constructor() {
        super();
        this.globalViewportXSize = 0;
        this.globalViewportYSize = 0;
        this.fullViewport = false;
        this.viewOrderStyle = 0;
        this.selectedViewIndex = 0;
        this.views = new ArrayList<ViewportWindow>();
    }

    public getViews(): ArrayList<ViewportWindow> {
        return this.views;
    }

    public getViewOrderStyle(): number {
        return this.viewOrderStyle;
    }

    public setViewOrderStyle(o: number): void {
        this.viewOrderStyle = o;
    }

    public isFullViewportScreen(): boolean {
        return this.fullViewport;
    }

    public setFullViewportScreen(val: boolean): void {
        this.fullViewport = val;
    }

    public toogleFullViewportScreen(): void {
        if (this.fullViewport) {
            this.fullViewport = false;
        } else {
            this.fullViewport = true;
        }
    }

    public getSelectedViewIndex(): number {
        return this.selectedViewIndex;
    }

    public setSelectedViewIndex(i: number): void {
        this.selectedViewIndex = i;
    }

    public setGlobalViewportXSize(x: number): void {
        this.globalViewportXSize = x;
    }

    public setGlobalViewportYSize(y: number): void {
        this.globalViewportYSize = y;
    }

    public getGlobalViewportXSize(): number {
        return this.globalViewportXSize;
    }

    public getGlobalViewportYSize(): number {
        return this.globalViewportYSize;
    }

    public getSelectedViewFromPointerPosition(x: number, y: number, changeSelection: boolean): ViewportWindow | null {
        let view: ViewportWindow;
        let theView: ViewportWindow | null = null;

        //-----------------------------------------------------------------
        let i: number;
        let xpercent: number;
        let ypercent: number;

        xpercent = x / this.getGlobalViewportXSize();
        ypercent = 1 - y / this.getGlobalViewportYSize();

        for (i = 0; i < this.views.size(); i++) {
            view = this.views.get(i);
            if (view.isActive() && view.inside(xpercent, ypercent)) {
                if (changeSelection) {
                    view.setSelected(true);
                    this.setSelectedViewIndex(i);
                }
                theView = view;
            } else {
                if (changeSelection) {
                    view.setSelected(false);
                }
            }
        }
        return theView;
    }

    private static doLayout1(view: ViewportWindow): void {
        view.setActive(true);
        view.setViewportStartXPercent(0.0);
        view.setViewportStartYPercent(0.0);
        view.setViewportSizeXPercent(1.0);
        view.setViewportSizeYPercent(1.0);
    }

    private static doLayout2(views: ArrayList<ViewportWindow>, style: number): void {
        views.get(0).setActive(true);
        views.get(1).setActive(true);
        if (style === 0) {
            views.get(0).setViewportStartXPercent(0.0);
            views.get(0).setViewportStartYPercent(0.0);
            views.get(0).setViewportSizeXPercent(0.5);
            views.get(0).setViewportSizeYPercent(1.0);

            views.get(1).setViewportStartXPercent(0.5);
            views.get(1).setViewportStartYPercent(0.0);
            views.get(1).setViewportSizeXPercent(0.5);
            views.get(1).setViewportSizeYPercent(1.0);
        } else {
            views.get(0).setViewportStartXPercent(0.0);
            views.get(0).setViewportStartYPercent(0.5);
            views.get(0).setViewportSizeXPercent(1.0);
            views.get(0).setViewportSizeYPercent(0.5);

            views.get(1).setViewportStartXPercent(0.0);
            views.get(1).setViewportStartYPercent(0.0);
            views.get(1).setViewportSizeXPercent(1.0);
            views.get(1).setViewportSizeYPercent(0.5);
        }
    }

    /** Java's `new double[n][2]`: rows of zeros. */
    private static zeroMatrix(n: number): number[][] {
        const m: number[][] = [];
        for (let i = 0; i < n; i++) {
            m.push([0, 0]);
        }
        return m;
    }

    private static doLayout3(views: ArrayList<ViewportWindow>, style: number): void {
        //-----------------------------------------------------------------
        const Pstart: number[][] = ViewportWindowSetManager.zeroMatrix(3);
        const Psize: number[][] = ViewportWindowSetManager.zeroMatrix(3);
        const p00 = 0;
        const p50 = 0.5;
        const p33 = 1.0 / 3.0;
        const p66 = 2.0 / 3.0;
        const p100 = 1;
        let i: number;

        //-----------------------------------------------------------------
        switch (style % 6) {
            case 0:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p00;
                Pstart[1]![1] = p50;
                Pstart[2]![0] = p50;
                Pstart[2]![1] = p00;
                Psize[0]![0] = p50;
                Psize[0]![1] = p50;
                Psize[1]![0] = p50;
                Psize[1]![1] = p50;
                Psize[2]![0] = p50;
                Psize[2]![1] = p100;
                break;
            case 1:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p50;
                Pstart[1]![1] = p00;
                Pstart[2]![0] = p50;
                Pstart[2]![1] = p50;
                Psize[0]![0] = p50;
                Psize[0]![1] = p100;
                Psize[1]![0] = p50;
                Psize[1]![1] = p50;
                Psize[2]![0] = p50;
                Psize[2]![1] = p50;
                break;
            case 2:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p50;
                Pstart[1]![1] = p00;
                Pstart[2]![0] = p00;
                Pstart[2]![1] = p50;
                Psize[0]![0] = p50;
                Psize[0]![1] = p50;
                Psize[1]![0] = p50;
                Psize[1]![1] = p50;
                Psize[2]![0] = p100;
                Psize[2]![1] = p50;
                break;
            case 3:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p00;
                Pstart[1]![1] = p50;
                Pstart[2]![0] = p50;
                Pstart[2]![1] = p50;
                Psize[0]![0] = p100;
                Psize[0]![1] = p50;
                Psize[1]![0] = p50;
                Psize[1]![1] = p50;
                Psize[2]![0] = p50;
                Psize[2]![1] = p50;
                break;
            case 4:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p33;
                Pstart[1]![1] = p00;
                Pstart[2]![0] = p66;
                Pstart[2]![1] = p00;
                Psize[0]![0] = p33;
                Psize[0]![1] = p100;
                Psize[1]![0] = p33;
                Psize[1]![1] = p100;
                Psize[2]![0] = p33;
                Psize[2]![1] = p100;
                break;
            case 5:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p00;
                Pstart[1]![1] = p33;
                Pstart[2]![0] = p00;
                Pstart[2]![1] = p66;
                Psize[0]![0] = p100;
                Psize[0]![1] = p33;
                Psize[1]![0] = p100;
                Psize[1]![1] = p33;
                Psize[2]![0] = p100;
                Psize[2]![1] = p33;
                break;
        }
        //-----------------------------------------------------------------
        for (i = 0; i < 3; i++) {
            views.get(i).setActive(true);
            views.get(i).setViewportStartXPercent(Pstart[i]![0]!);
            views.get(i).setViewportStartYPercent(Pstart[i]![1]!);
            views.get(i).setViewportSizeXPercent(Psize[i]![0]!);
            views.get(i).setViewportSizeYPercent(Psize[i]![1]!);
        }
    }

    private static doLayout4(views: ArrayList<ViewportWindow>, style: number): void {
        //-----------------------------------------------------------------
        const Pstart: number[][] = ViewportWindowSetManager.zeroMatrix(4);
        const Psize: number[][] = ViewportWindowSetManager.zeroMatrix(4);
        const p00 = 0;
        // Java also declares `p25 = 0.25` and `p75 = 0.25`, which no layout uses.
        const p33 = 1.0 / 3.0;
        const p50 = 0.5;
        const p66 = 2.0 / 3.0;
        const p100 = 1;
        let i: number;

        //-----------------------------------------------------------------
        // The caller passes `style % 5`, so cases 5 and 6 of Java's `% 7`
        // never arise; an unmatched style leaves the views at zero, as in Java.
        switch (style % 7) {
            case 0:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[3]![0] = p00;
                Pstart[3]![1] = p33;
                Pstart[2]![0] = p00;
                Pstart[2]![1] = p66;
                Pstart[1]![0] = p33;
                Pstart[1]![1] = p00;
                Psize[0]![0] = p33;
                Psize[0]![1] = p33;
                Psize[3]![0] = p33;
                Psize[3]![1] = p33;
                Psize[2]![0] = p33;
                Psize[2]![1] = p33;
                Psize[1]![0] = p66;
                Psize[1]![1] = p100;
                break;
            case 1:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p50;
                Pstart[1]![1] = p00;
                Pstart[2]![0] = p00;
                Pstart[2]![1] = p50;
                Pstart[3]![0] = p50;
                Pstart[3]![1] = p50;
                Psize[0]![0] = p50;
                Psize[0]![1] = p50;
                Psize[1]![0] = p50;
                Psize[1]![1] = p50;
                Psize[2]![0] = p50;
                Psize[2]![1] = p50;
                Psize[3]![0] = p50;
                Psize[3]![1] = p50;
                break;
            case 2:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p50;
                Pstart[1]![1] = p00;
                Pstart[2]![0] = p50;
                Pstart[2]![1] = p33;
                Pstart[3]![0] = p50;
                Pstart[3]![1] = p66;
                Psize[0]![0] = p50;
                Psize[0]![1] = p100;
                Psize[1]![0] = p50;
                Psize[1]![1] = p33;
                Psize[2]![0] = p50;
                Psize[2]![1] = p33;
                Psize[3]![0] = p50;
                Psize[3]![1] = p33;
                break;
            case 3:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p33;
                Pstart[1]![1] = p00;
                Pstart[2]![0] = p66;
                Pstart[2]![1] = p00;
                Pstart[3]![0] = p00;
                Pstart[3]![1] = p50;
                Psize[0]![0] = p33;
                Psize[0]![1] = p50;
                Psize[1]![0] = p33;
                Psize[1]![1] = p50;
                Psize[2]![0] = p33;
                Psize[2]![1] = p50;
                Psize[3]![0] = p100;
                Psize[3]![1] = p50;
                break;
            case 4:
                Pstart[0]![0] = p00;
                Pstart[0]![1] = p00;
                Pstart[1]![0] = p00;
                Pstart[1]![1] = p50;
                Pstart[2]![0] = p33;
                Pstart[2]![1] = p50;
                Pstart[3]![0] = p66;
                Pstart[3]![1] = p50;
                Psize[0]![0] = p100;
                Psize[0]![1] = p50;
                Psize[1]![0] = p33;
                Psize[1]![1] = p50;
                Psize[2]![0] = p33;
                Psize[2]![1] = p50;
                Psize[3]![0] = p33;
                Psize[3]![1] = p50;
                break;
        }
        //-----------------------------------------------------------------
        for (i = 0; i < 4; i++) {
            views.get(i).setActive(true);
            views.get(i).setViewportStartXPercent(Pstart[i]![0]!);
            views.get(i).setViewportStartYPercent(Pstart[i]![1]!);
            views.get(i).setViewportSizeXPercent(Psize[i]![0]!);
            views.get(i).setViewportSizeYPercent(Psize[i]![1]!);
        }
    }

    private static doLayout(views: ArrayList<ViewportWindow>, selectedForFullScreen: number, style: number): number {
        let i: number,
            selected = 0;
        let view: ViewportWindow;

        if (selectedForFullScreen >= 0 && selectedForFullScreen < views.size()) {
            for (i = 0; i < views.size(); i++) {
                view = views.get(i);
                if (i === selectedForFullScreen) {
                    view.setActive(true);
                    view.setSelected(true);
                    ViewportWindowSetManager.doLayout1(view);
                    selected = i;
                } else {
                    view.setActive(false);
                    view.setSelected(false);
                }
            }
        } else {
            //-----------------------------------------------------------------
            let isSelected = false;
            for (i = 0; i < views.size(); i++) {
                view = views.get(i);
                if (!isSelected && view.getSelected()) {
                    isSelected = true;
                    selected = i;
                    continue;
                } else if (isSelected) {
                    view.setSelected(false);
                }
            }
            if (!isSelected && views.size() > 0) {
                views.get(0).setSelected(true);
                selected = 0;
            }

            //-----------------------------------------------------------------
            switch (views.size()) {
                case 0:
                    console.log("Warning: NO VIEWS TO ORDER!");
                    break;
                case 1:
                    ViewportWindowSetManager.doLayout1(views.get(0));
                    break;
                case 2:
                    ViewportWindowSetManager.doLayout2(views, style % 2);
                    break;
                case 3:
                    ViewportWindowSetManager.doLayout3(views, style % 6);
                    break;
                case 4:
                    ViewportWindowSetManager.doLayout4(views, style % 5);
                    break;
                default:
                    console.log("Warning: Not supported layout, selecting first view full screen!");
                    ViewportWindowSetManager.doLayout1(views.get(0));
                    break;
            }
        }
        return selected;
    }

    public updateLayout(): void {
        this.setSelectedViewIndex(
            ViewportWindowSetManager.doLayout(
                this.views,
                this.fullViewport ? this.selectedViewIndex : -1,
                this.viewOrderStyle,
            ),
        );
    }

    public countActiveViews(): number {
        let i: number;
        let view: ViewportWindow;
        let n = 0;

        for (i = 0; i < this.getViews().size(); i++) {
            view = this.getViews().get(i);

            if (view.isActive()) {
                n++;
            }
        }
        return n;
    }
}
