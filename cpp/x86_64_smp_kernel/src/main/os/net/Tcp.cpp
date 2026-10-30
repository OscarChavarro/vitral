#include "net/Tcp.h"
#include "fs/Err.h"
#include "kernel/Clock.h"
#include "kernel/KernelHeap.h"
#include "kernel/Scheduler.h"
#include "net/Checksum.h"
#include "net/TcpFlag.h"
#include "net/TcpHeader.h"
#include "lib/vitralKernel/LockGuard.h"
#include "net/IpProtocol.h"
#include "net/Ipv4Header.h"

Tcp::Socket Tcp::sockets[Tcp::MAX_SOCKETS];
SpinLock Tcp::lock;
uint32_t Tcp::isnCounter = 0x1000;

static constexpr int EAGAIN_ = 11;
static constexpr int EBADF_ = 9;
static constexpr int EPIPE_ = 32;
static constexpr int ECONNRESET_ = 104;
static constexpr int ETIMEDOUT_ = 110;
static constexpr int EADDRINUSE_ = 98;

void
Tcp::init() { Ipv4::registerProtocol(IpProtocol::Tcp, &Tcp::input); }

unsigned
Tcp::connectionCount() {
    LockGuard guard(lock);
    unsigned n = 0;
    for (const Socket &s : sockets) {
        if (s.state != State::Free && s.state != State::Listen) {
            n++;
        }
    }
    return n;
}

Tcp::Socket *
Tcp::get(int64_t handle) {
    if (handle < 1 || handle > (int64_t)MAX_SOCKETS) {
        return nullptr;
    }
    Socket &s = sockets[handle - 1];
    return s.state == State::Free || s.appClosed ? nullptr : &s;
}

int
Tcp::allocate() {
    for (unsigned i = 0; i < MAX_SOCKETS; i++) {
        if (sockets[i].state == State::Free) {
            int event = sockets[i].event;
            Memory::set(&sockets[i], 0, sizeof(Socket));
            sockets[i].event = event;
            sockets[i].listener = -1;
            return (int)i;
        }
    }
    Socket *oldest = nullptr;
    for (Socket &s : sockets) {
        if (s.state == State::TimeWait && s.appClosed && (!oldest || s.timeWaitUntil < oldest->timeWaitUntil)) {
            oldest = &s;
        }
    }
    if (oldest) {
        release(*oldest);
        return allocate();
    }
    return -1;
}

void
Tcp::release(Socket &s) {
    if (s.rx) {
        KernelHeap::release(s.rx);
    }
    if (s.tx) {
        KernelHeap::release(s.tx);
    }
    s.rx = s.tx = nullptr;
    s.state = State::Free;
    wake(s);
}

void
Tcp::wake(Socket &s) {
    __atomic_fetch_add(&s.event, 1, __ATOMIC_SEQ_CST);
    Scheduler::futexWake(&s.event, 0x7FFFFFFF);
}

int *
Tcp::eventWord(int64_t handle) {
    LockGuard guard(lock);
    if (handle < 1 || handle > (int64_t)MAX_SOCKETS) {
        return nullptr;
    }
    return &sockets[handle - 1].event;
}

Tcp::Socket *
Tcp::find(const Ipv4Address &local, uint16_t localPort, const Ipv4Address &remote, uint16_t remotePort) {
    for (Socket &s : sockets) {
        if (s.state != State::Free && s.state != State::Listen && s.localPort == localPort && s.remotePort == remotePort &&
            s.localAddress == local && s.remoteAddress == remote) {
            return &s;
        }
    }
    return nullptr;
}

Tcp::Socket *
Tcp::listenerOn(uint16_t port) {
    for (Socket &s : sockets) {
        if (s.state == State::Listen && s.localPort == port) {
            return &s;
        }
    }
    return nullptr;
}

void
Tcp::closed(Socket &s) {
    s.state = State::Closed;
    s.retransmitAt = 0;
    if (s.appClosed) {
        release(s);
    } else {
        wake(s);
    }
}

void
Tcp::abort(Socket &s, int error) {
    s.error = error;
    closed(s);
}

int64_t
Tcp::listen(uint16_t port) {
    if (port == 0) {
        return -Err::INVAL;
    }
    LockGuard guard(lock);
    if (listenerOn(port)) {
        return -EADDRINUSE_;
    }
    int slot = allocate();
    if (slot < 0) {
        return -Err::NOMEM;
    }
    sockets[slot].state = State::Listen;
    sockets[slot].localPort = port;
    return slot + 1;
}

int64_t
Tcp::accept(int64_t handle) {
    LockGuard guard(lock);
    Socket *l = get(handle);
    if (!l || l->state != State::Listen) {
        return -EBADF_;
    }
    if (l->pendingCount == 0) {
        return -EAGAIN_;
    }
    int slot = l->pending[0];
    for (unsigned i = 1; i < l->pendingCount; i++) {
        l->pending[i - 1] = l->pending[i];
    }
    l->pendingCount--;
    return slot + 1;
}

int64_t
Tcp::receive(int64_t handle, void *buffer, size_t size) {
    LockGuard guard(lock);
    Socket *s = get(handle);
    if (!s || s->state == State::Listen) {
        return -EBADF_;
    }
    if (s->rxCount > 0) {
        size_t n = size < s->rxCount ? size : s->rxCount;
        uint16_t windowBefore = advertisedWindow(*s);
        for (size_t i = 0; i < n; i++) {
            ((uint8_t *)buffer)[i] = s->rx[(s->rxHead + i) % RX_BUFFER];
        }
        s->rxHead = (s->rxHead + n) % RX_BUFFER;
        s->rxCount -= n;
        if (windowBefore < s->mss && advertisedWindow(*s) >= s->mss && s->state != State::Closed) {
            sendAck(*s);
        }
        return (int64_t)n;
    }
    if (s->error) {
        return -s->error;
    }
    if (s->finReceived || s->state == State::Closed || s->state == State::TimeWait) {
        return 0;
    }
    return -EAGAIN_;
}

int64_t
Tcp::send(int64_t handle, const void *buffer, size_t size) {
    LockGuard guard(lock);
    Socket *s = get(handle);
    if (!s || s->state == State::Listen) {
        return -EBADF_;
    }
    if (s->error) {
        return -s->error;
    }
    if (s->finQueued || (s->state != State::Established && s->state != State::CloseWait)) {
        return -EPIPE_;
    }
    size_t room = TX_BUFFER - s->txCount;
    if (room == 0) {
        return -EAGAIN_;
    }
    size_t n = size < room ? size : room;
    for (size_t i = 0; i < n; i++) {
        s->tx[(s->txHead + s->txCount + i) % TX_BUFFER] = ((const uint8_t *)buffer)[i];
    }
    s->txCount += n;
    transmit(*s);
    return (int64_t)n;
}

int64_t
Tcp::close(int64_t handle) {
    LockGuard guard(lock);
    Socket *s = get(handle);
    if (!s) {
        return -EBADF_;
    }
    if (s->state == State::Listen) {
        for (unsigned i = 0; i < s->pendingCount; i++) {
            Socket &c = sockets[s->pending[i]];
            emit(c, c.sndNxt, TcpFlag::RST | TcpFlag::ACK, nullptr, 0);
            c.appClosed = true;
            closed(c);
        }
        for (Socket &c : sockets) {
            if (c.state == State::SynReceived && c.listener == (int)(s - sockets)) {
                c.appClosed = true;
                closed(c);
            }
        }
        release(*s);
        return 0;
    }
    s->appClosed = true;
    s->rxCount = 0;
    switch (s->state) {
    case State::Closed:
    case State::SynReceived:
        if (s->state == State::SynReceived) {
            emit(*s, s->sndNxt, TcpFlag::RST | TcpFlag::ACK, nullptr, 0);
        }
        release(*s);
        break;
    case State::Established:
    case State::CloseWait:
        s->finQueued = true;
        transmit(*s);
        break;
    default:
        break;
    }
    return 0;
}

bool
Tcp::peer(int64_t handle, Ipv4Address &address, uint16_t &port) {
    LockGuard guard(lock);
    Socket *s = get(handle);
    if (!s || s->state == State::Listen) {
        return false;
    }
    address = s->remoteAddress;
    port = s->remotePort;
    return true;
}

uint16_t
Tcp::advertisedWindow(const Socket &s) {
    size_t free = RX_BUFFER - s.rxCount;
    return (uint16_t)(s.appClosed ? RX_BUFFER : free);
}

void
Tcp::emitRaw(NetDevice &device, const Ipv4Address &source, const Ipv4Address &destination, uint16_t sourcePort,
             uint16_t destinationPort, uint32_t sequence, uint32_t acknowledgment, uint8_t flags, uint16_t window,
             const uint8_t *data, size_t length, bool mssOption, uint16_t mss) {
    PacketBuffer *packet = PacketBuffer::allocate();
    if (!packet) {
        return;
    }
    size_t headerSize = sizeof(TcpHeader) + (mssOption ? 4 : 0);
    uint8_t *area = packet->put(headerSize + length);
    if (!area) {
        PacketBuffer::release(packet);
        return;
    }
    TcpHeader *tcp = (TcpHeader *)area;
    tcp->sourcePort.set(sourcePort);
    tcp->destinationPort.set(destinationPort);
    tcp->sequence.set(sequence);
    tcp->acknowledgment.set(acknowledgment);
    tcp->dataOffset = (uint8_t)(headerSize / 4 << 4);
    tcp->flags = flags;
    tcp->window.set(window);
    tcp->checksum.set(0);
    tcp->urgent.set(0);
    if (mssOption) {
        area[20] = 2;
        area[21] = 4;
        area[22] = (uint8_t)(mss >> 8);
        area[23] = (uint8_t)mss;
    }
    if (length) {
        Memory::copy(area + headerSize, data, length);
    }
    size_t total = headerSize + length;
    uint8_t pseudo[12] = {source.byte(0), source.byte(1), source.byte(2), source.byte(3), destination.byte(0),
                          destination.byte(1), destination.byte(2), destination.byte(3), 0, (uint8_t)IpProtocol::Tcp,
                          (uint8_t)(total >> 8), (uint8_t)total};
    tcp->checksum.set(Checksum::finish(Checksum::add(Checksum::add(0, pseudo, sizeof(pseudo)), area, total)));
    Ipv4::output(device, destination, IpProtocol::Tcp, packet);
}

void
Tcp::emit(Socket &s, uint32_t sequence, uint8_t flags, const uint8_t *data, size_t length) {
    bool syn = flags & TcpFlag::SYN;
    emitRaw(*s.device, s.localAddress, s.remoteAddress, s.localPort, s.remotePort, sequence, s.rcvNxt, flags,
            advertisedWindow(s), data, length, syn, s.mss);
}

void
Tcp::sendAck(Socket &s) { emit(s, s.sndNxt, TcpFlag::ACK, nullptr, 0); }

void
Tcp::sendSynAck(Socket &s) { emit(s, s.iss, TcpFlag::SYN | TcpFlag::ACK, nullptr, 0); }

void
Tcp::reset(NetDevice &device, const Ipv4Header &ip, const TcpHeader &tcp, size_t payload) {
    if (tcp.flags & TcpFlag::RST) {
        return;
    }
    if (tcp.flags & TcpFlag::ACK) {
        emitRaw(device, ip.destination, ip.source, tcp.destinationPort.get(), tcp.sourcePort.get(), tcp.acknowledgment.get(), 0,
                TcpFlag::RST, 0, nullptr, 0, false, 0);
    } else {
        uint32_t length = (uint32_t)payload + ((tcp.flags & TcpFlag::SYN) ? 1 : 0) + ((tcp.flags & TcpFlag::FIN) ? 1 : 0);
        emitRaw(device, ip.destination, ip.source, tcp.destinationPort.get(), tcp.sourcePort.get(), 0,
                tcp.sequence.get() + length, TcpFlag::RST | TcpFlag::ACK, 0, nullptr, 0, false, 0);
    }
}

void
Tcp::armTimer(Socket &s) {
    unsigned shift = s.retries < 5 ? s.retries : 5;
    s.retransmitAt = Clock::ticks() + (Clock::msToTicks(1000) << shift);
}

void
Tcp::transmit(Socket &s) {
    if (s.state != State::Established && s.state != State::CloseWait && s.state != State::FinWait1 &&
        s.state != State::LastAck && s.state != State::Closing) {
        return;
    }
    uint32_t flightLimit = s.sndWnd < MAX_FLIGHT ? s.sndWnd : MAX_FLIGHT;
    for (;;) {
        uint32_t inFlight = s.sndNxt - s.sndUna;
        uint32_t dataInFlight = inFlight - (s.finSent && !before(s.sndNxt, s.finSeq + 1) ? 1 : 0);
        if (dataInFlight >= s.txCount) {
            break;
        }
        size_t unsent = s.txCount - dataInFlight;
        uint32_t room = flightLimit > inFlight ? flightLimit - inFlight : 0;
        if (room == 0 && inFlight == 0 && s.retries > 0) {
            room = 1;
        }
        size_t n = unsent < s.mss ? unsent : s.mss;
        if (n > room) {
            n = room;
        }
        if (n == 0) {
            break;
        }
        uint8_t chunk[1500];
        if (n > sizeof(chunk)) {
            n = sizeof(chunk);
        }
        for (size_t i = 0; i < n; i++) {
            chunk[i] = s.tx[(s.txHead + dataInFlight + i) % TX_BUFFER];
        }
        emit(s, s.sndNxt, TcpFlag::ACK | (n == unsent ? TcpFlag::PSH : 0), chunk, n);
        s.sndNxt += (uint32_t)n;
        if (before(s.sndMax, s.sndNxt)) {
            s.sndMax = s.sndNxt;
        }
        if (!s.retransmitAt) {
            armTimer(s);
        }
    }
    uint32_t inFlight = s.sndNxt - s.sndUna;
    if (s.finQueued && inFlight == s.txCount && !s.finSent) {
        if (!s.finSent) {
            s.finSeq = s.sndNxt;
            s.finSent = true;
            if (s.state == State::Established) {
                s.state = State::FinWait1;
            } else if (s.state == State::CloseWait) {
                s.state = State::LastAck;
            }
        }
        emit(s, s.finSeq, TcpFlag::FIN | TcpFlag::ACK, nullptr, 0);
        s.sndNxt = s.finSeq + 1;
        if (before(s.sndMax, s.sndNxt)) {
            s.sndMax = s.sndNxt;
        }
        if (!s.retransmitAt) {
            armTimer(s);
        }
    }
}

void
Tcp::timer() {
    LockGuard guard(lock);
    uint64_t now = Clock::ticks();
    for (Socket &s : sockets) {
        if (s.state == State::Free || s.state == State::Listen) {
            continue;
        }
        if (s.state == State::TimeWait) {
            if (now >= s.timeWaitUntil) {
                closed(s);
            }
            continue;
        }
        if (!s.retransmitAt || now < s.retransmitAt) {
            continue;
        }
        if (++s.retries > MAX_RETRIES) {
            abort(s, ETIMEDOUT_);
            continue;
        }
        armTimer(s);
        if (s.state == State::SynReceived) {
            sendSynAck(s);
        } else {
            s.sndNxt = s.sndUna;
            if (s.finSent) {
                s.finSent = false;
            }
            transmit(s);
            if (s.sndNxt == s.sndUna) {
                s.retransmitAt = 0;
            }
        }
    }
}

void
Tcp::input(NetDevice &device, PacketBuffer *packet, const Ipv4Header &ip) {
    size_t total = packet->length();
    bool valid = total >= sizeof(TcpHeader) && ip.destination == device.ipv4();
    if (valid) {
        const TcpHeader *tcp = packet->as<TcpHeader>();
        size_t headerLength = tcp->headerLength();
        uint8_t pseudo[12] = {ip.source.byte(0), ip.source.byte(1), ip.source.byte(2), ip.source.byte(3),
                              ip.destination.byte(0), ip.destination.byte(1), ip.destination.byte(2), ip.destination.byte(3),
                              0, (uint8_t)IpProtocol::Tcp, (uint8_t)(total >> 8), (uint8_t)total};
        valid = headerLength >= sizeof(TcpHeader) && headerLength <= total &&
                Checksum::finish(Checksum::add(Checksum::add(0, pseudo, sizeof(pseudo)), packet->data(), total)) == 0;
    }
    if (!valid) {
        PacketBuffer::release(packet);
        return;
    }
    TcpHeader tcp = *packet->as<TcpHeader>();
    size_t headerLength = tcp.headerLength();
    const uint8_t *options = packet->data() + sizeof(TcpHeader);
    const uint8_t *data = packet->data() + headerLength;
    size_t length = total - headerLength;

    {
        LockGuard guard(lock);
        Socket *s = find(ip.destination, tcp.destinationPort.get(), ip.source, tcp.sourcePort.get());
        bool synOnly = (tcp.flags & (TcpFlag::SYN | TcpFlag::ACK | TcpFlag::RST)) == TcpFlag::SYN;
        if (s && synOnly && (s->state == State::TimeWait || s->state == State::Closed) && s->appClosed) {
            release(*s);
            s = nullptr;
        }
        if (s) {
            segment(*s, tcp, data, length);
        } else if (synOnly && listenerOn(tcp.destinationPort.get())) {
            newConnection(device, ip, tcp, options, headerLength - sizeof(TcpHeader));
        } else {
            reset(device, ip, tcp, length);
        }
    }
    PacketBuffer::release(packet);
}

void
Tcp::newConnection(NetDevice &device, const Ipv4Header &ip, const TcpHeader &tcp, const uint8_t *options,
                   size_t optionBytes) {
    Socket *l = listenerOn(tcp.destinationPort.get());
    unsigned waiting = 0;
    for (const Socket &c : sockets) {
        if (c.state == State::SynReceived && c.listener == (int)(l - sockets)) {
            waiting++;
        }
    }
    if (l->pendingCount + waiting >= BACKLOG) {
        return;
    }
    int slot = allocate();
    if (slot < 0) {
        return;
    }
    Socket &s = sockets[slot];
    s.rx = (uint8_t *)KernelHeap::tryAllocate(RX_BUFFER);
    s.tx = (uint8_t *)KernelHeap::tryAllocate(TX_BUFFER);
    if (!s.rx || !s.tx) {
        release(s);
        return;
    }
    s.state = State::SynReceived;
    s.device = &device;
    s.localAddress = ip.destination;
    s.remoteAddress = ip.source;
    s.localPort = tcp.destinationPort.get();
    s.remotePort = tcp.sourcePort.get();
    s.listener = (int)(l - sockets);
    uint16_t peerMss = 536;
    for (size_t i = 0; i + 1 < optionBytes;) {
        uint8_t kind = options[i];
        if (kind == 0) {
            break;
        }
        if (kind == 1) {
            i++;
            continue;
        }
        uint8_t size = options[i + 1];
        if (size < 2 || i + size > optionBytes) {
            break;
        }
        if (kind == 2 && size == 4) {
            peerMss = (uint16_t)(options[i + 2] << 8 | options[i + 3]);
        }
        i += size;
    }
    uint16_t ours = (uint16_t)(device.mtu() - Ipv4::HEADER_SIZE - sizeof(TcpHeader));
    s.mss = peerMss < ours ? peerMss : ours;
    isnCounter += 64000;
    s.iss = (uint32_t)(Clock::nanoseconds() / 4000) + isnCounter;
    s.irs = tcp.sequence.get();
    s.rcvNxt = s.irs + 1;
    s.sndUna = s.iss;
    s.sndNxt = s.sndMax = s.iss + 1;
    s.sndWnd = tcp.window.get();
    sendSynAck(s);
    armTimer(s);
}

void
Tcp::segment(Socket &s, const TcpHeader &tcp, const uint8_t *data, size_t length) {
    uint32_t seq = tcp.sequence.get();
    uint32_t ack = tcp.acknowledgment.get();
    uint8_t flags = tcp.flags;

    if (s.state == State::Closed) {
        if (!(flags & TcpFlag::RST)) {
            emit(s, s.sndNxt, TcpFlag::RST | TcpFlag::ACK, nullptr, 0);
        }
        return;
    }
    if (flags & TcpFlag::RST) {
        bool acceptable;
        if (s.state == State::SynReceived) {
            acceptable = seq == s.rcvNxt;
        } else {
            acceptable = !before(seq, s.rcvNxt) && before(seq, s.rcvNxt + (uint32_t)RX_BUFFER + 1);
        }
        if (acceptable) {
            abort(s, ECONNRESET_);
        }
        return;
    }
    if (flags & TcpFlag::SYN) {
        if (s.state == State::SynReceived) {
            sendSynAck(s);
        } else {
            sendAck(s);
        }
        return;
    }
    if (!(flags & TcpFlag::ACK)) {
        return;
    }

    if (s.state == State::SynReceived) {
        if (ack != s.iss + 1) {
            emitRaw(*s.device, s.localAddress, s.remoteAddress, s.localPort, s.remotePort, ack, 0, TcpFlag::RST, 0, nullptr, 0,
                    false, 0);
            return;
        }
        Socket &l = sockets[s.listener];
        if (l.state != State::Listen || l.pendingCount >= BACKLOG) {
            emit(s, s.sndNxt, TcpFlag::RST | TcpFlag::ACK, nullptr, 0);
            s.appClosed = true;
            closed(s);
            return;
        }
        s.state = State::Established;
        s.sndUna = ack;
        s.retransmitAt = 0;
        s.retries = 0;
        s.listener = -1;
        l.pending[l.pendingCount++] = (int)(&s - sockets);
        wake(l);
    }

    if (before(s.sndUna, ack) && !before(s.sndMax, ack)) {
        uint32_t advanced = ack - s.sndUna;
        bool finAcked = s.finSent && before(s.finSeq, ack);
        uint32_t dataAcked = advanced - (finAcked ? 1 : 0);
        if (dataAcked > s.txCount) {
            dataAcked = (uint32_t)s.txCount;
        }
        s.txHead = (s.txHead + dataAcked) % TX_BUFFER;
        s.txCount -= dataAcked;
        s.sndUna = ack;
        if (before(s.sndNxt, ack)) {
            s.sndNxt = ack;
        }
        s.retries = 0;
        s.retransmitAt = 0;
        if (s.sndUna != s.sndMax) {
            armTimer(s);
        }
        if (finAcked) {
            if (s.state == State::FinWait1) {
                s.state = State::FinWait2;
            } else if (s.state == State::Closing) {
                s.state = State::TimeWait;
                s.timeWaitUntil = Clock::ticks() + Clock::msToTicks(TIME_WAIT_MS);
            } else if (s.state == State::LastAck) {
                closed(s);
                return;
            }
        }
        wake(s);
    }
    if (!before(ack, s.sndUna)) {
        s.sndWnd = tcp.window.get();
    }

    bool acceptsData = s.state == State::Established || s.state == State::FinWait1 || s.state == State::FinWait2;
    bool fin = flags & TcpFlag::FIN;
    uint32_t finPosition = seq + (uint32_t)length;
    size_t originalLength = length;
    bool needAck = false;
    if (before(seq, s.rcvNxt)) {
        uint32_t skip = s.rcvNxt - seq;
        if (skip > length) {
            skip = (uint32_t)length;
        }
        data += skip;
        length -= skip;
        seq += skip;
    }
    if (length > 0) {
        if (acceptsData && seq == s.rcvNxt) {
            size_t free = s.appClosed ? length : RX_BUFFER - s.rxCount;
            size_t n = length < free ? length : free;
            if (!s.appClosed) {
                for (size_t i = 0; i < n; i++) {
                    s.rx[(s.rxHead + s.rxCount + i) % RX_BUFFER] = data[i];
                }
                s.rxCount += n;
            }
            s.rcvNxt += (uint32_t)n;
            if (n < length) {
                fin = false;
            }
            wake(s);
        } else {
            fin = false;
        }
        needAck = true;
    } else if (originalLength > 0) {
        needAck = true;
    }
    if (fin) {
        needAck = true;
        if (acceptsData && finPosition == s.rcvNxt) {
            s.rcvNxt++;
            s.finReceived = true;
            if (s.state == State::Established) {
                s.state = State::CloseWait;
            } else if (s.state == State::FinWait1) {
                s.state = State::Closing;
            } else {
                s.state = State::TimeWait;
                s.timeWaitUntil = Clock::ticks() + Clock::msToTicks(TIME_WAIT_MS);
                s.retransmitAt = 0;
            }
            wake(s);
        } else if (s.state == State::TimeWait) {
            s.timeWaitUntil = Clock::ticks() + Clock::msToTicks(TIME_WAIT_MS);
        }
    }
    transmit(s);
    if (needAck) {
        sendAck(s);
    }
}
