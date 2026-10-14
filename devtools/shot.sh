#!/bin/bash
# Captura del emulador reducida: devtools/shot.sh nombre
A=/c/Users/daniel.gonzalez/AppData/Local/Android/Sdk/platform-tools/adb.exe
D=/c/Users/daniel.gonzalez/Downloads/VoltBodyApp/android/app/build/emu
mkdir -p $D
$A exec-out screencap -p > $D/$1.png
python -c "
from PIL import Image
im=Image.open(r'$D/$1.png'.replace('/c/','C:/')).convert('RGB'); im.resize((im.size[0]*2//5, im.size[1]*2//5)).save(r'$D/$1_s.jpg'.replace('/c/','C:/'),quality=80)"
echo $D/$1_s.jpg
