import { ProcessingElement } from "./ProcessingElement.js";
export class StopWatch extends ProcessingElement {private t0=0;private t1=0;private running=false;public start():void{this.t0=Date.now();this.running=true;}public stop():void{this.t1=Date.now();this.running=false;}public getElapsedRealTime():number{return ((this.running?Date.now():this.t1)-this.t0)/1000;}}
