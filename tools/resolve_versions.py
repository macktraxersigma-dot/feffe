#!/usr/bin/env python3
"""Подбирает актуальные версии Yarn / Loader / Fabric API / Loom для Minecraft 1.21.11 и пишет gradle.properties."""
import json, re, urllib.parse, urllib.request
from pathlib import Path

MC = "1.21.11"
UA = {"User-Agent": "pulse-visual-build"}

def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=40) as r:
        return r.read().decode()

props = Path(__file__).resolve().parent.parent / "gradle.properties"
cur = dict(l.split("=", 1) for l in props.read_text().splitlines() if "=" in l and not l.startswith("#"))

def step(name, fn):
    try:
        cur[name] = fn(); print(f"{name} = {cur[name]}")
    except Exception as e:
        print(f"! {name}: не удалось получить ({e}), оставляю {cur.get(name)}")

step("yarn_mappings", lambda: json.loads(get(f"https://meta.fabricmc.net/v2/versions/yarn/{MC}"))[0]["version"])
step("loader_version", lambda: next(x["version"] for x in json.loads(get("https://meta.fabricmc.net/v2/versions/loader")) if x.get("stable")))
step("fabric_version", lambda: json.loads(get("https://api.modrinth.com/v2/project/fabric-api/version?" + urllib.parse.urlencode(
    {"game_versions": json.dumps([MC]), "loaders": json.dumps(["fabric"])})))[0]["version_number"])
step("loom_version", lambda: re.search(r"<release>([^<]+)</release>", get("https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml")).group(1))

props.write_text("# Версии подставил tools/resolve_versions.py\norg.gradle.jvmargs=-Xmx2G\n" + "\n".join(f"{k}={v}" for k, v in cur.items() if k != "org.gradle.jvmargs") + "\n")
