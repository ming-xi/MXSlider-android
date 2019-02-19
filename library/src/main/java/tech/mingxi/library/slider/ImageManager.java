package tech.mingxi.library.slider;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageManager {
	private final OnStateChangeListener onStateChangeListener;
	private final Context context;
	private ArrayList<Bitmap> bitmaps;
	private List<Uri> localUris;

	public ImageManager(Context context, OnStateChangeListener onStateChangeListener) {
		this.context = context;
		this.onStateChangeListener = onStateChangeListener;
		bitmaps = new ArrayList<>();
		localUris = new ArrayList<>();
	}

	public void addImage(Uri uri) {
		localUris.add(uri);
	}

	public void addBitmap(Bitmap bitmap) {
		bitmaps.add(bitmap);
		if (onStateChangeListener != null) {
			onStateChangeListener.onBitmapAdd();
		}
	}

	public void refreshBitmaps(int width, int height) {
		Log.w("MXSlider", "refreshBitmaps is called!");
		for (Bitmap bitmap : bitmaps) {
			bitmap.recycle();
		}
		bitmaps.clear();
		for (Uri uri : localUris) {
			bitmaps.add(getBitmapFromUri(uri, width, height));
		}
	}


	private Bitmap getBitmapFromUri(Uri uri, int width, int height) {
		Bitmap result = null;
		if (uri == null) {
			return result;
		}
		String scheme = uri.getScheme();
		if (scheme.equals(ContentResolver.SCHEME_FILE)) {
		} else if (scheme.equals(MXSlider.SCHEME_ASSETS)) {
			try {
				String path = uri.getPath();
				if (path.startsWith("/")) {
					path = path.replaceFirst("/+", "");
				}
				result = decodeStream(context.getAssets().open(path), width, height);
			} catch (IOException e) {
				Log.e(Util.DEBUG_TAG, "", e);
			}
		} else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {

		} else if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {

		} else {
			throw new IllegalArgumentException("Scheme '" + scheme + "' not supported!");
		}
		return result;
	}

	private Bitmap decodeStream(InputStream stream, int width, int height) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(stream, null, options);
		try {
			// for a decode problem on Android 7.0 and above...
			// what are you doing, Google???
			// see https://stackoverflow.com/questions/39316069/bitmapfactory-decodestream-from-assets-returns-null-on-android-7
			stream.reset();
		} catch (IOException e) {
			Log.e(Util.DEBUG_TAG, "", e);
		}
		options.inSampleSize = calculateInSampleSize(options, width, height);
		options.inJustDecodeBounds = false;
		return cropAndScaleBitmap(BitmapFactory.decodeStream(stream, null, options), width, height);
	}

	private Bitmap cropAndScaleBitmap(Bitmap bitmap, int width, int height) {
		int w;
		int h;
		if (width * 1.0 / height >= bitmap.getWidth() * 1.0 / bitmap.getHeight()) {//crop top and bottom
			w = bitmap.getWidth();
			h = (int) (w * height * 1.0 / width);
		} else {//crop left and right
			h = bitmap.getHeight();
			w = (int) (h * width * 1.0 / height);
		}
		Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - w) / 2, (bitmap.getHeight() - h) / 2, w, h);
		bitmap.recycle();
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, true);
		croppedBitmap.recycle();
		return scaledBitmap;
	}

	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	public int getImageCount() {
		return localUris.size();
	}

	public Bitmap getImage(int index) {
		if (index >= 0 && index < bitmaps.size()) {
			return bitmaps.get(index);
		} else {
			return null;
		}
	}

	interface OnStateChangeListener {
		void onBitmapAdd();
	}
}
