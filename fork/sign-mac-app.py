#!/usr/bin/env python3
# Signs an unpacked ReStudio.app with codesign. fork/sign-mac.sh calls this script on the macOS runner of the release
# workflow, because the Linux build has no codesign. The local build signs with ReStudioMacSigning (build/src) instead.
#
# The order is from the inside to the outside:
#   1. The Mach-O libraries inside the JAR files. Apple rejects the notarization for an unsigned library there.
#   2. The Mach-O files of the app, the deepest first.
#   3. The nested bundles (app, framework, bundle, plugin, and jbr), the deepest first.
#   4. The app.
#
# A JAR of the IDE has an `__index__` entry, and the end record of the JAR points to it. This script writes a changed
# JAR again without the index. Then the class loader reads the JAR with HashMapZipFile, as it does for a plugin JAR.
#
# Usage: fork/sign-mac-app.py <app> <identity> <entitlements>
import os
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile

MACH_O_MAGICS = {
  b"\xfe\xed\xfa\xce", b"\xfe\xed\xfa\xcf", b"\xce\xfa\xed\xfe", b"\xcf\xfa\xed\xfe", b"\xca\xfe\xba\xbe", b"\xbe\xba\xfe\xca",
}
LIBRARY_SUFFIXES = (".dylib", ".jnilib", ".so")
BUNDLE_SUFFIXES = (".app", ".framework", ".bundle", ".plugin")
INDEX_ENTRY = "__index__"


def is_mach_o(data: bytes) -> bool:
  return data[:4] in MACH_O_MAGICS


def codesign(files, identity, entitlements):
  """Signs the files in chunks of 100. The Apple timestamp server sometimes gives a bad time, so a chunk has three tries."""
  command = ["codesign", "--sign", identity, "--options", "runtime", "--timestamp", "--force", "--entitlements", entitlements]
  for start in range(0, len(files), 100):
    for attempt in range(3):
      result = subprocess.run(command + files[start:start + 100], stdout=subprocess.DEVNULL)
      if result.returncode == 0:
        break
      if attempt == 2:
        raise subprocess.CalledProcessError(result.returncode, command)
      print(f"codesign failed with exit code {result.returncode}. The script tries again in 10 seconds.")
      time.sleep(10)


def sign_libraries_in_jar(jar, identity, entitlements):
  with zipfile.ZipFile(jar) as source:
    names = [info.filename for info in source.infolist()
             if not info.is_dir() and info.filename.endswith(LIBRARY_SUFFIXES) and is_mach_o(source.read(info)[:4])]
    if not names:
      return 0
    work = tempfile.mkdtemp(prefix="restudio-jar-")
    try:
      paths = {}
      for name in names:
        path = os.path.join(work, name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as target:
          target.write(source.read(name))
        paths[name] = path
      codesign(list(paths.values()), identity, entitlements)
      rewritten = jar + ".signed"
      with zipfile.ZipFile(rewritten, "w", compression=zipfile.ZIP_STORED) as target:
        for info in source.infolist():
          if info.filename == INDEX_ENTRY:
            continue
          entry = zipfile.ZipInfo(info.filename, date_time=info.date_time)
          entry.external_attr = info.external_attr
          entry.compress_type = zipfile.ZIP_STORED
          if info.filename in paths:
            with open(paths[info.filename], "rb") as signed:
              target.writestr(entry, signed.read())
          else:
            target.writestr(entry, source.read(info))
    finally:
      shutil.rmtree(work)
  os.replace(rewritten, jar)
  return len(names)


def is_bundle(path):
  if path.endswith(BUNDLE_SUFFIXES):
    return True
  return os.path.basename(path) == "jbr" and os.path.isfile(os.path.join(path, "Contents", "Info.plist"))


def depth(path):
  return path.count(os.sep)


def main():
  app, identity, entitlements = sys.argv[1:4]
  app = os.path.abspath(app)
  jars, binaries, bundles = [], [], []
  for root, dirs, files in os.walk(app):
    for name in dirs:
      path = os.path.join(root, name)
      if not os.path.islink(path) and is_bundle(path):
        bundles.append(path)
    for name in files:
      path = os.path.join(root, name)
      if os.path.islink(path):
        continue
      if name.endswith(".jar"):
        jars.append(path)
      else:
        with open(path, "rb") as file:
          if is_mach_o(file.read(4)):
            binaries.append(path)

  libraries = sum(sign_libraries_in_jar(jar, identity, entitlements) for jar in jars)
  print(f"Signed {libraries} libraries in the JAR files")
  codesign(sorted(binaries, key=depth, reverse=True), identity, entitlements)
  print(f"Signed {len(binaries)} Mach-O files")
  for bundle in sorted(bundles, key=depth, reverse=True):
    codesign([bundle], identity, entitlements)
  print(f"Signed {len(bundles)} nested bundles")
  codesign([app], identity, entitlements)
  subprocess.run(["codesign", "--verify", "--deep", "--strict", app], check=True)
  print(f"Signed {app}")


if __name__ == "__main__":
  main()
