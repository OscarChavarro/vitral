# Shared by run16.sh / run32.sh / run32failon64.sh / run64.sh (sourced, not executed).
# Extra QEMU options (e.g. -smp) go in the QEMU_EXTRA array before calling run_qemu. QEMU_VGA (array, default
# -vga std) selects the display adapter and QEMU_RAM (default 64) the megabytes of RAM.
# Network: an Intel e1000 card. QEMU_NET=bridge (default) puts it on the host's physical network, as another station of
# the LAN (QEMU_BRIDGE_IF = the host interface; default en0 on macOS, the interface of the default route on Linux);
# QEMU_NET=user is QEMU's built-in NAT network (no privileges needed, but nothing outside can reach the guest) and
# QEMU_NET=none has no card. QEMU_NIC (an array) replaces the whole -nic option. On Linux a bridge interface (br0) is used
# with QEMU's bridge helper and a physical one (eno1) through a macvtap device (wired cards only; the host itself cannot
# reach the guest through it). macOS vmnet and the bridge helper need root: the QEMU command then runs under sudo (the
# build does not). The macvtap device is created with sudo and handed to the user, so QEMU itself runs unprivileged.
# run_qemu <qemu-binary> <cpu> <build-dir> [extra cmake -D options...]
#   Builds into <build-dir>, assembles boot.img and boots it, unless -n was given to the script.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Parallel build jobs: one per host core (nproc on Linux, hw.ncpu on macOS); JOBS= overrides it.
JOBS="${JOBS:-$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || getconf _NPROCESSORS_ONLN 2>/dev/null || echo 1)}"

run_qemu() {
    local qemu="$1" cpu="$2" build="$3"; shift 3
    cmake -S "$ROOT" -B "$build" -DCMAKE_TOOLCHAIN_FILE="$ROOT/cmake/toolchain.cmake" "$@" >/dev/null
    cmake --build "$build" --parallel "$JOBS"

    # Disk image: boot sector + stage 2 (boot.bin), then the 64-bit kernel image (sector
    # __kernel_lba = size of boot.bin / 512, see src/main/os/linker.ld). No padding: it is only as big as its
    # contents, rounded up to a sector, and QEMU attaches it as a hard disk.
    local kernel="${BM_KERNEL:-kernel64}"
    local img="$build/boot.img"
    local boot_bytes kernel_bytes
    boot_bytes="$(stat -f%z "$build/boot.bin" 2>/dev/null || stat -c%s "$build/boot.bin")"
    kernel_bytes="$(stat -f%z "$build/$kernel.bin" 2>/dev/null || stat -c%s "$build/$kernel.bin")"
    dd if="$build/boot.bin" of="$img" 2>/dev/null
    dd if="$build/$kernel.bin" of="$img" bs=512 seek=$((boot_bytes / 512)) conv=notrunc 2>/dev/null
    local pad=$(( (512 - $(stat -f%z "$img" 2>/dev/null || stat -c%s "$img") % 512) % 512 ))
    [[ $pad -gt 0 ]] && dd if=/dev/zero bs=1 count=$pad >> "$img" 2>/dev/null
    # QEMU derives the BIOS geometry (16 heads x 63 sectors) from the size of the image and drops the last partial
    # cylinder: the kernel carries the root file system image, so pad up to a whole cylinder or its tail is unreachable.
    local cylinder=$((16 * 63 * 512)) size
    size="$(stat -f%z "$img" 2>/dev/null || stat -c%s "$img")"
    pad=$(( (cylinder - size % cylinder) % cylinder ))
    [[ $pad -gt 0 ]] && dd if=/dev/zero bs=$pad count=1 >> "$img" 2>/dev/null
    echo "Image: $img (boot+stage 2: $boot_bytes bytes, kernel: $kernel_bytes bytes)"

    [[ "${NO_RUN:-}" == "1" ]] && exit 0

    launch_qemu "$qemu" "$cpu" -drive "file=$img,format=raw,if=ide" -boot c
}

# launch_qemu <qemu-binary> <cpu> <drive/boot options...>
# Starts QEMU with the common options (RAM, extra -smp from QEMU_EXTRA, display) and waits for it.
launch_qemu() {
    local qemu="$1" cpu="$2"; shift 2
    local vga=(-vga std)
    [[ -n "${QEMU_VGA[*]+x}" ]] && vga=("${QEMU_VGA[@]}")
    qemu_network
    local nic=(${NET_NIC[@]+"${NET_NIC[@]}"}) runner=(${NET_RUNNER[@]+"${NET_RUNNER[@]}"})
    local display=cocoa
    if "$qemu" -display help 2>&1 | grep -qx sdl; then display=sdl; fi
    local display_opts="$display"
    [[ "$display" == "cocoa" ]] && display_opts="cocoa,zoom-to-fit=on"  # content follows the window size

    ${runner[@]+"${runner[@]}"} "$qemu" -machine pc -cpu "$cpu" -m "${QEMU_RAM:-64}" ${QEMU_EXTRA[@]+"${QEMU_EXTRA[@]}"} \
        "${vga[@]}" ${nic[@]+"${nic[@]}"} -display "$display_opts" "$@" &
    local qemu_pid=$!
    trap 'kill "$qemu_pid" 2>/dev/null' INT TERM
    if [[ "$display" == "cocoa" ]]; then
        # Under sudo, $qemu_pid is sudo's: the window belongs to the QEMU process itself.
        ( [[ ${#runner[@]} -gt 0 ]] && sleep 2 && qemu_pid="$(pgrep -n -x "$(basename "$qemu")" || echo "$qemu_pid")"
          scale_window "$qemu_pid" "${ZOOM:-2}" ) &
    fi
    local status=0
    wait "$qemu_pid" || status=$?
    ${NET_CLEANUP[@]+"${NET_CLEANUP[@]}"}
    return $status
}

# qemu_network
# Sets the arrays NET_NIC (QEMU's network card options), NET_RUNNER (command prefix they need: sudo) and NET_CLEANUP
# (command to run after QEMU exits), from QEMU_NET.
qemu_network() {
    NET_NIC=() NET_RUNNER=() NET_CLEANUP=()
    if [[ -n "${QEMU_NIC[*]+x}" ]]; then NET_NIC=("${QEMU_NIC[@]}"); return 0; fi
    case "${QEMU_NET:-bridge}" in
        none) NET_NIC=(-nic none) ;;
        user) NET_NIC=(-nic user,model=e1000) ;;
        bridge)
            local sudo=()
            if [[ "$(id -u)" != "0" && "$(uname)" == "Darwin" ]]; then
                sudo=(sudo)
            elif [[ "$(id -u)" != "0" ]]; then
                # Root needs the user's X11 / Wayland session to open QEMU's window.
                local cookies
                for cookies in "/run/user/$(id -u)/gdm/Xauthority" "$HOME/.Xauthority"; do
                    [[ -z "${XAUTHORITY:-}" && -f "$cookies" ]] && export XAUTHORITY="$cookies"
                done
                sudo=(sudo --preserve-env=DISPLAY,XAUTHORITY,WAYLAND_DISPLAY,XDG_RUNTIME_DIR)
            fi
            local interface="${QEMU_BRIDGE_IF:-}"
            if [[ -z "$interface" && "$(uname)" == "Darwin" ]]; then
                interface=en0
            elif [[ -z "$interface" ]]; then
                interface="$(ip route show default 2>/dev/null | awk '{ print $5; exit }')"
            fi
            [[ -n "$interface" ]] || { echo "No default route: set QEMU_BRIDGE_IF or use QEMU_NET=user" >&2; exit 1; }
            if [[ ${#sudo[@]} -gt 0 ]]; then
                echo "Bridged network ($interface) needs root: sudo is used (QEMU_NET=user avoids it)" >&2
                sudo -v
            fi
            if [[ "$(uname)" == "Darwin" ]]; then
                NET_NIC=(-nic "vmnet-bridged,ifname=$interface,model=e1000")
                NET_RUNNER=(${sudo[@]+"${sudo[@]}"})
            elif [[ -d "/sys/class/net/$interface/bridge" ]]; then
                # The bridge helper reads /etc/qemu/bridge.conf, which must contain "allow <bridge>".
                NET_NIC=(-nic "bridge,br=$interface,model=e1000")
                NET_RUNNER=(${sudo[@]+"${sudo[@]}"})
            else
                linux_macvtap "$interface" ${sudo[@]+"${sudo[@]}"}
            fi ;;
        *) echo "QEMU_NET must be bridge, user or none" >&2; exit 1 ;;
    esac
}

# linux_macvtap <interface> [sudo]
# Creates a macvtap device on top of a physical interface for qemu_network: the guest gets the device's MAC address and
# QEMU reads and writes its /dev/tapN, which is given to the user and opened as file descriptor 3 by the shell that then
# becomes QEMU. QEMU does not run as root, so it opens its window in the user's X11 / Wayland session like any program.
linux_macvtap() {
    local interface="$1"; shift
    local sudo=("$@")
    [[ -d "/sys/class/net/$interface" ]] || { echo "Network interface $interface not found (QEMU_BRIDGE_IF)" >&2; exit 1; }
    local tap="qvga$$"
    ${sudo[@]+"${sudo[@]}"} ip link add link "$interface" name "$tap" type macvtap mode bridge || exit 1
    ${sudo[@]+"${sudo[@]}"} ip link set "$tap" up || { ${sudo[@]+"${sudo[@]}"} ip link delete "$tap"; exit 1; }
    local index mac
    index="$(cat "/sys/class/net/$tap/ifindex")"
    mac="$(cat "/sys/class/net/$tap/address")"
    local device="/dev/tap$index"
    local attempt
    for attempt in 1 2 3 4 5 6 7 8 9 10; do  # udev creates the device node
        [[ -e "$device" ]] && break
        sleep 0.2
    done
    [[ -e "$device" ]] || { echo "$device did not appear" >&2; ${sudo[@]+"${sudo[@]}"} ip link delete "$tap"; exit 1; }
    ${sudo[@]+"${sudo[@]}"} chown "$(id -u)" "$device" || { ${sudo[@]+"${sudo[@]}"} ip link delete "$tap"; exit 1; }
    NET_NIC=(-netdev "tap,id=net0,fd=3" -device "e1000,netdev=net0,mac=$mac")
    NET_RUNNER=(bash -c 'exec 3<>"$0" && exec "$@"' "$device")
    NET_CLEANUP=(${sudo[@]+"${sudo[@]}"} ip link delete "$tap")
    echo "Bridged network: macvtap $tap on $interface, guest MAC $mac" >&2
}

# macOS: QEMU's cocoa window opens at the guest's native size (720x400 in text mode), which is small.
# Resizes it to ZOOM times that size (default 2 = 200%, 1 = native; ZOOM=0 leaves it alone). Needs the terminal to
# be allowed in System Settings > Privacy & Security > Accessibility; otherwise it does nothing and
# the window can still be resized by hand (the content scales with it).
scale_window() {
    local pid="$1" zoom="$2"
    [[ "$zoom" == "0" ]] && return 0
    command -v osascript >/dev/null || return 0
    local width=$((720 * zoom)) height=$((400 * zoom + 28))  # 28: title bar
    local attempt
    for attempt in 1 2 3 4 5 6; do  # the window may resize itself while the BIOS starts: repeat
        sleep 1
        osascript -e "tell application \"System Events\"
            set p to first process whose unix id is $pid
            set position of window 1 of p to {20, 40}
            set size of window 1 of p to {$width, $height}
        end tell" >/dev/null 2>&1 || true
    done
}

# Number of online logical CPUs, including hardware threads.
host_logical_cpus() {
    local n
    n="$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || true)"
    [[ "$n" =~ ^[0-9]+$ && "$n" -gt 0 ]] || n=1
    echo "$n"
}
