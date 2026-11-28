import { assembleReaderPlyMeshPart } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderPlyMeshWorker";
import { installNodeWorkerRuntime } from "../../../../java/concurrent/NodeWorkerRuntime.js";

installNodeWorkerRuntime(assembleReaderPlyMeshPart);
