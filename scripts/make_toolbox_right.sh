#!/bin/sh


cat toolbox.xml | \
    sed 's/Left/XXXXX/g' | \
    sed 's/Right/Left/g' | \
    sed 's/XXXXX/Right/g' | \
    sed 's/toolbox_undo/XXXXX/g' | \
    sed 's/toolbox_redo/toolbox_undo/g' | \
    sed 's/XXXXX/toolbox_redo/g' | \
    sed 's/toolbox_next/XXXXX/g' | \
    sed 's/toolbox_prev/toolbox_next/g' | \
    sed 's/XXXXX/toolbox_prev/g' \
    > toolbox_right.xml

