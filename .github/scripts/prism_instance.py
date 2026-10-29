"""Builds a Prism Launcher instance zip: the freshly built mod plus a few performance mods from Modrinth."""
import glob
import json
import os
import shutil
import struct
import urllib.parse
import urllib.request
import zipfile

MC = "1.21.11"
LOADER = "0.19.5"
NAME = "tacklebox Test"
# The mod's own dependencies, then performance mods. Required dependencies are added automatically.
MODS = ["fabric-api", "modmenu", "cloth-config",
        "sodium", "lithium", "ferrite-core", "entityculling", "immediatelyfast", "modernfix", "dynamic-fps"]

API = "https://api.modrinth.com/v2"
HEADERS = {"User-Agent": "landofif/tacklebox ci (prism test instance)"}


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS)) as r:
        return r.read()


def newest(project):
    q = urllib.parse.urlencode({"game_versions": json.dumps([MC]), "loaders": json.dumps(["fabric"])})
    versions = json.loads(get(f"{API}/project/{project}/version?{q}"))
    return versions[0] if versions else None


root = "build/prism"
shutil.rmtree(root, ignore_errors=True)
mods_dir = f"{root}/{NAME}/minecraft/mods"
os.makedirs(mods_dir)

seen, queue, added, missing = set(), list(MODS), [], []
while queue:
    p = queue.pop(0)
    if p in seen:
        continue
    seen.add(p)
    v = newest(p)
    if v is None:
        missing.append(p)
        continue
    seen.add(v["project_id"])
    f = next((f for f in v["files"] if f["primary"]), v["files"][0])
    with open(f"{mods_dir}/{f['filename']}", "wb") as out:
        out.write(get(f["url"]))
    added.append(f"{p} {v['version_number']}")
    queue += [d["project_id"] for d in v["dependencies"] if d["dependency_type"] == "required" and d.get("project_id")]

# foecollab (required) is not on modrinth: take the jar from its latest github release
try:
    req = {"User-Agent": HEADERS["User-Agent"], "Accept": "application/vnd.github+json"}
    if os.environ.get("GITHUB_TOKEN"):
        req["Authorization"] = "Bearer " + os.environ["GITHUB_TOKEN"]
    with urllib.request.urlopen(urllib.request.Request(
            "https://api.github.com/repos/Kimberlyow/FOECollab/releases/latest", headers=req)) as r:
        release = json.loads(r.read())
    asset = next(a for a in release["assets"] if a["name"].endswith(".jar") and "sources" not in a["name"])
    with open(f"{mods_dir}/{asset['name']}", "wb") as out:
        out.write(get(asset["browser_download_url"]))
    added.append(f"foecollab {release['tag_name']}")
except Exception as e:  # the instance still builds; add foecollab by hand then
    missing.append(f"foecollab ({e})")

for jar in glob.glob("build/libs/*.jar"):
    if not jar.endswith(("-sources.jar", "-dev.jar")):
        shutil.copy(jar, mods_dir)
        added.append("tacklebox " + os.path.basename(jar))

with open(f"{root}/{NAME}/instance.cfg", "w") as f:
    f.write(f"[General]\nConfigVersion=1.2\nInstanceType=OneSix\nname={NAME}\niconKey=default\n")

with open(f"{root}/{NAME}/mmc-pack.json", "w") as f:
    json.dump({"formatVersion": 1, "components": [
        {"uid": "net.minecraft", "version": MC, "important": True},
        {"uid": "net.fabricmc.intermediary", "version": MC, "dependencyOnly": True},
        {"uid": "net.fabricmc.fabric-loader", "version": LOADER},
    ]}, f, indent=1)


# servers.dat with fishonmc already in the list (uncompressed nbt)
def s(t):
    b = t.encode()
    return struct.pack(">H", len(b)) + b


server = b"\x08" + s("name") + s("FishOnMC") + b"\x08" + s("ip") + s("play.fishonmc.net") + b"\x00"
nbt = b"\x0a" + s("") + b"\x09" + s("servers") + b"\x0a" + struct.pack(">i", 1) + server + b"\x00"
with open(f"{root}/{NAME}/minecraft/servers.dat", "wb") as f:
    f.write(nbt)

with zipfile.ZipFile("build/tacklebox-prism.zip", "w", zipfile.ZIP_DEFLATED) as z:
    for dirpath, _, files in os.walk(root):
        for name in files:
            full = os.path.join(dirpath, name)
            z.write(full, os.path.relpath(full, root))

print("added:\n  " + "\n  ".join(added))
if missing:
    print("no 1.21.11 fabric build for: " + ", ".join(missing))
