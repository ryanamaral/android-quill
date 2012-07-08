package com.write.Quill.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import junit.framework.Assert;

import name.vbraun.view.write.GraphicsImage;
import name.vbraun.view.write.GraphicsImage.FileType;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

public class Util {
	private static final String TAG = "Util";
	
	public final static int IMAGE_MAX_SIZE = 2048;

	public static int scalePow2(int height, int width) {
        int scale = 1;
        int size = Math.max(height, width);
        if (size > IMAGE_MAX_SIZE) {
            scale = (int)Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / 
            			(double) size) / Math.log(0.5)));
        }
        return scale;
	}
	
	public static Bitmap getBitmap(ContentResolver contentResolver, Uri uri) {
		InputStream in = null;
		try {
			in = contentResolver.openInputStream(uri);
			
			//Decode image size
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;

	        BitmapFactory.decodeStream(in, null, o);
	        in.close();

	        int scale = Util.scalePow2(o.outHeight, o.outWidth);
	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize = scale;
	        in = contentResolver.openInputStream(uri);
	        Bitmap b = BitmapFactory.decodeStream(in, null, o2);
	        in.close();
			
			return b;
		} catch (FileNotFoundException e) {
			Log.e(TAG, "file " + uri.toString() + " not found");
		} catch (IOException e) {
			Log.e(TAG, "file " + uri.toString() + " not found");
		}
		return null;
	}
	
    // Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static Bitmap rotateAndCrop(Bitmap b, int degrees, Rect crop) {
    	if (b == null) return b;
    	Bitmap b2 = null;
        int scale = Util.scalePow2(b.getHeight(), b.getWidth());
        if (scale != 1 && crop != null) {
        	crop.left *= scale;
        	crop.right*= scale;
        	crop.bottom *= scale;
        	crop.top *= scale;
        }
    	try {
    		if (degrees != 0) {
    			Matrix m = new Matrix();
    			m.setRotate(degrees, 0, 0);
				RectF r_rot = new RectF(0,0,b.getWidth(),b.getHeight());
				m.mapRect(r_rot);
				m.postTranslate(-r_rot.left, -r_rot.top);

//				r_rot.set(0,0,b.getWidth(),b.getHeight());
//				m.mapRect(r_rot);
//				Log.d(TAG, "rotated bitmap = "+r_rot.toString());

				if (crop == null)
    				b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
    			else {
    				Matrix minv = new Matrix();
    				m.invert(minv);
//      			minv.postScale(scale, scale);
    				RectF r = new RectF();
    				r.set(crop);
    				minv.mapRect(r);
    				Log.d(TAG, "crop = "+crop.toString());
    				r.round(crop);
    				Log.d(TAG, "bitmap "+b.getDensity() + " " + b.getWidth() + " x "+b.getHeight());
    				Log.d(TAG, "inv rotated crop = "+crop.toString());
    				b2 = Bitmap.createBitmap(b, crop.left, crop.top, crop.width(), crop.height(), m, true);
    			}
    		} else {
    			if (crop != null) {
    				Log.d(TAG, "crop = "+crop.toString());
    				Log.d(TAG, "bitmap "+b.getDensity() + " " + b.getWidth() + " x "+b.getHeight());
    				b2 = Bitmap.createBitmap(b, crop.left, crop.top, crop.width(), crop.height());
//  				b2 = Bitmap.createBitmap(b, scale*crop.left, scale*crop.top, 
//  						scale*crop.width(), scale*crop.height());
    			} else
    				b2 = b;
    		}
    	} catch (OutOfMemoryError ex) {
    		// We have no memory to rotate. Return the original bitmap.
    		b2 = b;
    	}
    	Assert.assertNotNull(b2);
        if (b == b2) {
        	return b;
        } else {
        	Log.d(TAG, "b != b2, recycling b");
            b.recycle();
            return b2;
        }
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
    	return rotateAndCrop(b, degrees, null);
    }
	
	public static void copyfile(File source, File dest) {
		try {
			InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
	}

	
	public static String getImageFileName(UUID uuid) {
		return GraphicsImage.getImageFileName(uuid, FileType.FILETYPE_JPG);
	}



}
