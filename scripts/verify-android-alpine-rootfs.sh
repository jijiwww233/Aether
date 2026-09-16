#!/usr/bin/env bash
set -euo pipefail

apk_path=${1:?Usage: verify-android-alpine-rootfs.sh <app-debug.apk>}
rootfs_asset=$(unzip -Z1 "$apk_path" | grep -E '^assets/runtimes/alpine/arm64-v8a/rootfs\.tar(\.gz)?$' | head -n 1 || true)

if [[ -z "$rootfs_asset" ]]; then
    echo "APK does not contain an Alpine rootfs asset." >&2
    exit 1
fi

rootfs_entries() {
    if [[ "$rootfs_asset" == *.gz ]]; then
        unzip -p "$apk_path" "$rootfs_asset" | gzip -dc | tar -tf -
    else
        unzip -p "$apk_path" "$rootfs_asset" | tar -tf -
    fi
}

rootfs_details() {
    if [[ "$rootfs_asset" == *.gz ]]; then
        unzip -p "$apk_path" "$rootfs_asset" | gzip -dc | tar -tvf -
    else
        unzip -p "$apk_path" "$rootfs_asset" | tar -tvf -
    fi
}

rootfs_entries | grep -Fx './bin/sh'
rootfs_entries | grep -Fx './bin/busybox'
rootfs_entries | grep -Fx './etc/alpine-release'
rootfs_details | grep -E ' ./bin/sh -> /bin/busybox$'

echo "Verified Alpine rootfs asset: $rootfs_asset"
