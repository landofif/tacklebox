#!/bin/sh
# usage: render_all.sh <out dir>   (run after build.py)
set -e
R=${LOGO_SCRIPTS:-/home/luna/.claude/skills/logo-design/scripts}
O=$1; mkdir -p $O/png $O/web; cd $O/svg
for s in 16 24 32 48; do python3 $R/render_png.py tacklebox-icon-small.svg -o ../png/tacklebox-icon-$s.png --size $s >/dev/null; done
for s in 64 128 256 512 1024; do python3 $R/render_png.py tacklebox-icon.svg -o ../png/tacklebox-icon-$s.png --size $s >/dev/null; done
python3 $R/render_png.py tacklebox-icon-small.svg --ico ../web/favicon.ico --ico-sizes 16 32 48 >/dev/null
python3 $R/render_png.py tacklebox-icon-maskable.svg -o ../web/apple-touch-icon.png --size 180 >/dev/null
python3 $R/render_png.py tacklebox-icon-maskable.svg -o ../web/maskable-512.png --size 512 >/dev/null
python3 $R/render_png.py tacklebox-icon.svg -o ../web/icon-192.png --size 192 >/dev/null
python3 $R/render_png.py tacklebox-icon.svg -o ../web/icon-512.png --size 512 >/dev/null
python3 $R/render_png.py tacklebox-social-preview.svg -o ../png/tacklebox-social-preview-1280x640.png --width 1280 --height 640 >/dev/null
python3 $R/render_png.py tacklebox-horizontal.svg -o ../png/tacklebox-horizontal-1200.png --width 1200 >/dev/null
python3 $R/render_png.py tacklebox-horizontal-dark.svg -o ../png/tacklebox-horizontal-dark-1200.png --width 1200 >/dev/null
cp tacklebox-icon-small.svg ../web/favicon.svg
echo rendered
