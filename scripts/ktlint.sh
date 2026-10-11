#!/bin/sh
#
# Runs ktlint over the project's Kotlin sources. Self-bootstraps a pinned ktlint CLI release
# into a local cache, the same way the project's own ./kotlin wrapper bootstraps its toolchain,
# so CI and local dev always run the exact same lint tool with no separate install step.
#
# Usage:
#   scripts/ktlint.sh            # check only (what CI runs)
#   scripts/ktlint.sh --format   # auto-fix in place, then re-run as a check
#
# Requires a `java` executable on PATH (ktlint's release artifact is a self-executing JAR).

set -e -u

ktlint_version=1.8.0
ktlint_sha256=a3fd620207d5c40da6ca789b95e7f823c54e854b7fade7f613e91096a3706d75
ktlint_download_url="https://github.com/ktlint/ktlint/releases/download/${ktlint_version}/ktlint"

cache_dir="${KTLINT_CACHE_DIR:-${HOME}/.cache/ktlint}/${ktlint_version}"
ktlint_bin="${cache_dir}/ktlint"

script_dir=$(cd "$(dirname "$0")" && pwd)
project_dir=$(cd "${script_dir}/.." && pwd)

die() {
  echo >&2
  echo "$@" >&2
  echo >&2
  exit 1
}

sha256_of() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    die "Need either 'shasum' or 'sha256sum' on PATH to verify the ktlint download."
  fi
}

if [ ! -x "${ktlint_bin}" ]; then
  echo "Downloading ktlint ${ktlint_version}..."
  mkdir -p "${cache_dir}"
  tmp_file="${ktlint_bin}.tmp"
  curl -sSL -o "${tmp_file}" "${ktlint_download_url}"

  actual_sha256=$(sha256_of "${tmp_file}")
  if [ "${actual_sha256}" != "${ktlint_sha256}" ]; then
    rm -f "${tmp_file}"
    die "ktlint download checksum mismatch: expected ${ktlint_sha256}, got ${actual_sha256}"
  fi

  chmod +x "${tmp_file}"
  mv "${tmp_file}" "${ktlint_bin}"
fi

format_flag=""
if [ "${1:-}" = "--format" ]; then
  format_flag="-F"
  shift
fi

cd "${project_dir}"
exec "${ktlint_bin}" ${format_flag} "**/*.kt" "!**/build/**" --editorconfig="${project_dir}/.editorconfig" "$@"
