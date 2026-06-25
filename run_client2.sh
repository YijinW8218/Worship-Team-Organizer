#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

javac -d "$ROOT_DIR/out/client" "$ROOT_DIR"/WT_Client/src/*.java

cd "$ROOT_DIR"
exec java -cp "$ROOT_DIR/out/client" Client
