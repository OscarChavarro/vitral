"use strict";
// Front end of the Vitral kernel's httpd. The kernel publishes binary messages over WebSockets (see feeds.h):
//   /ws/system  0x01 system, 0x02 process info (new processes), 0x03 process state (every update)
//   /ws/cpu     0x20 load of each core in percent (255 = offline), every 500 ms
//   /ws/log     0x10 log text, only what is new
const STATES = ["READY", "RUN", "SLEEP", "ZOMBIE", "WAIT"];   // Process::State (Ready, Running, Sleeping, Zombie, Blocked)
const processes = new Map();   // pid -> {pid, ppid, name, console, state, cpu, percent}
const system = {cpus: null, idle: "", uptimeSeconds: 0};
let snapshot = null;           // pids seen in the snapshot being received

function readVar(view, at) {
  let value = 0, shift = 0, byte;
  do {
    byte = view.getUint8(at.i++);
    value += (byte & 0x7f) * 2 ** shift;
    shift += 7;
  } while (byte & 0x80);
  return value;
}

const text = new TextDecoder();

function onSystemMessage(buffer) {
  const view = new DataView(buffer);
  const at = {i: 1};
  switch (view.getUint8(0)) {
    case 0x01: {
      system.cpus = {online: view.getUint8(1), logical: view.getUint8(2), cores: view.getUint8(3), threads: view.getUint8(4)};
      system.idle = text.decode(new Uint8Array(buffer, 6, view.getUint8(5)));
      break;
    }
    case 0x02:
      while (at.i < buffer.byteLength) {
        const pid = readVar(view, at), ppid = readVar(view, at), console = view.getUint8(at.i++);
        readVar(view, at);   // affinity mask
        const length = view.getUint8(at.i++);
        const name = text.decode(new Uint8Array(buffer, at.i, length));
        at.i += length;
        processes.set(pid, {pid, ppid, name, console, state: 3, cpu: 255, percent: 0});
      }
      break;
    case 0x03: {
      const flags = view.getUint8(1);
      system.uptimeSeconds = view.getUint32(2, true);
      at.i = 6;
      if (flags & 1) snapshot = new Set();
      while (at.i < buffer.byteLength) {
        const pid = readVar(view, at);
        const p = processes.get(pid);
        const state = view.getUint8(at.i++), cpu = view.getUint8(at.i++), percent = view.getUint8(at.i++);
        if (p) Object.assign(p, {state, cpu, percent});
        if (snapshot) snapshot.add(pid);
      }
      if ((flags & 2) && snapshot) {
        for (const pid of [...processes.keys()]) if (!snapshot.has(pid)) processes.delete(pid);
        snapshot = null;
        renderSystem();
      }
      break;
    }
  }
}

const log = {element: document.getElementById("log"), expected: null};
let logText = "";

function onLogMessage(buffer) {
  const view = new DataView(buffer);
  if (view.getUint8(0) !== 0x10) return;
  logText += text.decode(new Uint8Array(buffer, 5), {stream: true});
  if (logText.length > 65536) logText = logText.slice(-32768);
  const atEnd = log.element.scrollTop + log.element.clientHeight >= log.element.scrollHeight - 8;
  log.element.textContent = logText;
  if (atEnd) log.element.scrollTop = log.element.scrollHeight;
}

function renderSystem() {
  const s = system.uptimeSeconds, pad = n => String(n).padStart(2, "0");
  const c = system.cpus;
  const el = document.getElementById("summary");
  el.textContent = `Up ${pad(Math.floor(s / 3600))}:${pad(Math.floor(s / 60) % 60)}:${pad(s % 60)}` +
    (c ? ` · CPUs: ${c.online}/${c.logical} online (${c.cores} cores x ${c.threads} threads) · idle: ${system.idle}` : "") +
    ` · ${processes.size} processes`;
  const body = document.querySelector("#processes tbody");
  const rows = [...processes.values()].sort((a, b) => b.percent - a.percent || a.pid - b.pid);
  body.replaceChildren(...rows.map(p => {
    const tr = document.createElement("tr");
    tr.className = p.state <= 1 ? "run" : p.state === 3 ? "zombie" : "";
    const cells = [p.pid, p.ppid, p.name, STATES[p.state] || "?", p.cpu === 255 ? "-" : p.cpu, p.console, p.percent + "%"];
    cells.forEach((value, i) => {
      const td = document.createElement("td");
      td.textContent = value;
      if (typeof value === "number" || i === 4 || i === 6) td.className = "n";
      tr.append(td);
    });
    return tr;
  }));
}

// ---- Cores: load of every core against uptime ----
const HISTORY_SECONDS = 120;
const load = {samples: [], cores: 0, canvas: document.getElementById("load"), legend: document.getElementById("legend"), anchor: null};

function coreColor(i, n) { return `hsl(${Math.round(360 * i / Math.max(n, 1))} 70% 50%)`; }

function onCpuMessage(buffer) {
  const view = new DataView(buffer);
  if (view.getUint8(0) !== 0x20) return;
  const uptime = view.getUint32(1, true), cores = view.getUint8(5);
  // The uptime is whole seconds (RTC): the browser's clock places the samples inside the second.
  if (!load.anchor) load.anchor = {uptime, local: performance.now()};
  const t = load.anchor.uptime + (performance.now() - load.anchor.local) / 1000;
  if (load.samples.length && t < load.samples[load.samples.length - 1].t - 1) load.samples = [];   // the kernel restarted
  const values = [];
  for (let i = 0; i < cores; i++) values.push(view.getUint8(6 + i));
  load.samples.push({t, values});
  while (load.samples.length > 1 && t - load.samples[0].t > HISTORY_SECONDS) load.samples.shift();
  if (load.cores !== cores) {
    load.cores = cores;
    load.legend.replaceChildren(...values.map((_, i) => {
      const li = document.createElement("li");
      li.innerHTML = `<i style="background:${coreColor(i, cores)}"></i>core ${i} <span></span>`;
      return li;
    }));
  }
  [...load.legend.children].forEach((li, i) => { li.lastChild.textContent = values[i] === 255 ? "off" : values[i] + "%"; });
  drawLoad();
}

function drawLoad() {
  const c = load.canvas, g = c.getContext("2d"), w = c.width, h = c.height, left = 36, bottom = 22, top = 8;
  const style = getComputedStyle(document.body);
  g.clearRect(0, 0, w, h);
  g.font = "11px system-ui, sans-serif";
  g.fillStyle = g.strokeStyle = style.color;
  g.lineWidth = 1;
  g.globalAlpha = .25;
  for (let p = 0; p <= 100; p += 25) {
    const y = top + (h - top - bottom) * (1 - p / 100);
    g.beginPath(); g.moveTo(left, y); g.lineTo(w, y); g.stroke();
  }
  g.globalAlpha = .7;
  for (let p = 0; p <= 100; p += 25) g.fillText(p + "%", 2, top + (h - top - bottom) * (1 - p / 100) + 4);
  const last = load.samples[load.samples.length - 1];
  if (!last) return;
  const span = Math.max(Math.min(HISTORY_SECONDS, last.t - load.samples[0].t), 10), t0 = last.t - span;
  const x = t => left + (w - left) * (t - t0) / span, y = v => top + (h - top - bottom) * (1 - v / 100);
  const fmt = s => `${Math.floor(s / 60)}:${String(Math.floor(s) % 60).padStart(2, "0")}`;
  for (let k = 0; k <= 4; k++) {   // time axis: uptime
    const t = t0 + span * k / 4;
    g.fillText(fmt(Math.max(t, 0)), Math.min(x(t), w - 28), h - 6);
  }
  g.globalAlpha = 1;
  g.lineWidth = 1.5;
  for (let core = 0; core < load.cores; core++) {
    g.strokeStyle = coreColor(core, load.cores);
    g.beginPath();
    let pen = false;
    for (const s of load.samples) {
      const v = s.values[core];
      if (v === undefined || v === 255) { pen = false; continue; }
      pen ? g.lineTo(x(s.t), y(v)) : g.moveTo(x(s.t), y(v));
      pen = true;
    }
    g.stroke();
  }
}

// ---- Connection: three WebSockets, and what to show when the kernel goes quiet ----
// If nothing arrives on any of them for OFFLINE_AFTER_MS, the data sections are hidden and only a message is shown; every
// RETRY_MS the sockets are opened again without any noise, and the first message that arrives brings the page back.
const OFFLINE_AFTER_MS = 5000, RETRY_MS = 5000, FEEDS = [["system", onSystemMessage], ["cpu", onCpuMessage], ["log", onLogMessage]];
let sockets = [], offline = false, lastMessage = performance.now(), generation = 0, retryTimer = null;

function openFeed(path, handler, gen) {
  let socket;
  try {
    socket = new WebSocket(`ws://${location.host}/ws/${path}`);
  } catch (error) {
    return;
  }
  socket.binaryType = "arraybuffer";
  sockets.push(socket);
  socket.onmessage = event => {
    if (gen !== generation) return;
    lastMessage = performance.now();
    if (offline) goOnline();
    handler(event.data);
  };
  socket.onclose = () => {
    if (gen !== generation || offline) return;   // offline: the retry timer takes care
    if (path === "log") logText = "";            // the server sends the whole log again
    if (path === "system") processes.clear();
    setTimeout(() => { if (gen === generation && !offline) openFeed(path, handler, gen); }, 2000);
  };
}

function connectAll() {
  generation++;
  for (const socket of sockets) {
    socket.onmessage = socket.onclose = null;
    try { socket.close(); } catch (error) { /* already closed */ }
  }
  sockets = [];
  for (const [path, handler] of FEEDS) openFeed(path, handler, generation);
}

function goOffline() {
  offline = true;
  document.body.classList.add("offline");
  processes.clear();
  snapshot = null;
  logText = "";
  log.element.textContent = "";
  load.anchor = null;   // the samples stay: the chart continues if the kernel was only unreachable
  retryTimer = setInterval(connectAll, RETRY_MS);
}

function goOnline() {
  offline = false;
  clearInterval(retryTimer);
  document.body.classList.remove("offline");
  lastMessage = performance.now();
}

setInterval(() => {
  if (!offline && performance.now() - lastMessage > OFFLINE_AFTER_MS) goOffline();
}, 1000);
connectAll();
