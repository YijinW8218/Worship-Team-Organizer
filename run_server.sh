#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

javac -d "$ROOT_DIR/out/server" \
  "$ROOT_DIR"/WT_Server/src/model/*.java \
  "$ROOT_DIR"/WT_Server/src/storage/*.java \
  "$ROOT_DIR"/WT_Server/src/Server.java

cd "$ROOT_DIR/WT_Server"
exec java -cp "$ROOT_DIR/out/server" Server
