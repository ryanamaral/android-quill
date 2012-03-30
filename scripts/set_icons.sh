#!/bin/sh

if [ ! -e icons ]; then
  echo "You must run this script in the scripts directory."
  exit 1
fi

name=$1

echo "Using iconset $name.png"

if [ ! -e "icons/${name}_150.png" ]; then
  echo "Icon set does not exist."
  echo "usage: set_icons.sh quill"
  exit 1
fi

cp "icons/${name}_150.png" ../res/drawable/icon_150.png
cp "icons/${name}-ldpi.png" ../res/drawable-ldpi/icon.png
cp "icons/${name}-mdpi.png" ../res/drawable-mdpi/icon.png
cp "icons/${name}_pressed-mdpi.png" ../res/drawable-mdpi/icon_pressed.png
cp "icons/${name}-hdpi.png" ../res/drawable-hdpi/icon.png

