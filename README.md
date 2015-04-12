android-quill
=============

### Handwriting note-taking app for Android tablets
**[Forked/Exported from code.google.com/p/android-quill]**

A key design goal is quick response to pen strokes and 100% vector graphics. Developed on a Lenovo ThinkPad Tablet and a Galaxy Note.

### Features
  * Active pen (digitizer) support on ThinkPad Tablet, HTC Jetstream, and HTC Flyer, Galaxy Note.
  * Very fast response to pen strokes.
  * "Fountain pen" mode supports pen pressure data, more pressure = thicker line (requires active pen).
  * Pinch-to-zoom.
  * Double-finger tap to zoom.
  * Two-finger move gesture.
  * Pen strokes are vector art, zoom does not pixelate your writing.
  * "Pen only" mode (optional) disables touch input while writing.
  * Android 3.x hardware accelerated graphics.
  * Open source (GPL), so your notes are not stuck in an opaque file format.
  * PDF export (save to SD card, Evernote, Share).
  * PNG (raster image) export.
  * Can backup/restore your data.
  * Ruled/Quad background paper
  * Tagging for pages so you can group them together.
  * Switch between multiple notebooks.
  * Undo/redo.
  * Requires Honeycomb or later (Ice Cream Sandwich, Jelly Bean)
  * Desktop companion program at https://github.com/vbraun/QuillDesktop to view/convert quill notebooks into svg, pdf, ps.

The n-trig active pen has been tested on the ThinkPad Tablet, HTC Jetstream, and the HTC Flyer (Honeycomb). The Samsung S-pen has been tested on the Galaxy Note (ICS). The basic functionality should work on any Android tablet, but distinguishing pen from finger data and pressure sensitivity might not work on others. Please let me know your results if you try it on another tablet. See http://code.google.com/p/android-quill/wiki/SupportedDevices for more details.

There is a thread on the XDA Developers forum http://forum.xda-developers.com/showthread.php?t=1378625 and at the Lenovo forum http://forum.lenovo.com/t5/ThinkPad-slate-tablets if you want to discuss anything.

Also available on the Android Market (https://play.google.com/store/apps/details?id=com.write.Quill).

### Dependencies
  * [Android Color Picker aka AmbilWarna library](https://github.com/yukuku/ambilwarna)
  * [Android File Picker](http://code.google.com/p/android-file-picker)

### Reviews
http://www.youtube.com/watch?v=k1yxYXMPXA0  
http://the-gadgeteer.com/2011/11/16/lenovo-thinkpad-tablet-review (contains above video)

![LarsWallinDrawing](../master/doc/screenshot-quill-android.png "An amazing drawing by Lars Wallin done in Quill")  
An amazing drawing by Lars Wallin done in Quill.

## LICENSE

Quill is released under the [GNU General Public License v3.0](../master/LICENSE).
