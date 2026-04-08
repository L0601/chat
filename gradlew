#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

for candidate in \
  "$HOME/.gradle/wrapper/dists/gradle-8.14-bin"/*/gradle-8.14/bin/gradle \
  "$HOME/.gradle/wrapper/dists/gradle-8.13-bin"/*/gradle-8.13/bin/gradle
do
  if [ -x "$candidate" ]; then
    exec "$candidate" -p "$APP_HOME" "$@"
  fi
done

if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$APP_HOME" "$@"
fi

echo "Gradle 不可用，请先安装 Gradle 或准备本地 Gradle wrapper 缓存。" >&2
exit 1

