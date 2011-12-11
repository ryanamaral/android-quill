#!/bin/sh


cat toolbox.xml | \
    sed 's/Left/XXXXX/g' | \
    sed 's/Right/Left/g' | \
    sed 's/XXXXX/Right/g' > \
    toolbox_right.xml

