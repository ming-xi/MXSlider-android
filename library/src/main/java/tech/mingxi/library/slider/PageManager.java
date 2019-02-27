package tech.mingxi.library.slider;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PageManager {
	private final OnStateChangeListener onStateChangeListener;
	private final Context context;
	private List<Page> pages;
	private ArrayList<Bitmap> bitmaps;
	private boolean loop = false;

	public PageManager(Context context, OnStateChangeListener onStateChangeListener) {
		this.context = context;
		this.onStateChangeListener = onStateChangeListener;
		pages = new ArrayList<>();
		bitmaps = new ArrayList<>();
	}

	public void addPage(Page page) {
		pages.add(page);
		if (onStateChangeListener != null) {
			onStateChangeListener.onPageAdd();
		}
	}

	public void addAllPages(Collection<? extends Page> page) {
		pages.addAll(page);
		if (onStateChangeListener != null) {
			onStateChangeListener.onPageAdd();
		}
	}

	public void refreshBitmaps(int width, int height) {
		Log.w(MXSlider.DEBUG_TAG, "refreshBitmaps is called!");
		for (Bitmap bitmap : bitmaps) {
			bitmap.recycle();
		}
		bitmaps.clear();
		for (Page page : pages) {
			bitmaps.add(getBitmapFromUri(page.getLocalUri(), width, height));
		}
	}


	private Bitmap getBitmapFromUri(Uri uri, int width, int height) {
		Log.i(MXSlider.DEBUG_TAG, "getBitmapFromUri start uri = " + uri.toString());
		Bitmap result = null;
		if (uri == null) {
			return result;
		}
		String scheme = uri.getScheme();
		if (scheme.equals(ContentResolver.SCHEME_FILE)) {
			try {
				String path = uri.getPath();
				InputStream stream = new FileInputStream(path);
				BitmapFactory.Options option = getOptionFromStream(stream);
				stream.close();
				stream = new FileInputStream(path);
				result = decodeStream(stream, option, width, height);
			} catch (Exception e) {
				Log.e(MXSlider.DEBUG_TAG, "", e);
			}
		} else if (scheme.equals(MXSlider.SCHEME_ASSETS)) {
			try {
				String path = uri.getPath();
				if (path.startsWith("/")) {
					path = path.replaceFirst("/+", "");
				}
				InputStream stream = context.getAssets().open(path);
				BitmapFactory.Options option = getOptionFromStream(stream);
				stream.close();
				stream = context.getAssets().open(path);
				result = decodeStream(stream, option, width, height);
			} catch (Exception e) {
				Log.e(MXSlider.DEBUG_TAG, "", e);
			}
		} else if (scheme.equals(ContentResolver.SCHEME_CONTENT) || scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
			try {
				InputStream stream = context.getContentResolver().openInputStream(uri);
				BitmapFactory.Options option = getOptionFromStream(stream);
				stream.close();
				stream = context.getContentResolver().openInputStream(uri);
				result = decodeStream(stream, option, width, height);
			} catch (Exception e) {
				Log.e(MXSlider.DEBUG_TAG, uri.toString(), e);
			}
		} else {
			throw new IllegalArgumentException("Scheme '" + scheme + "' not supported!");
		}
		Log.i(MXSlider.DEBUG_TAG, "getBitmapFromUri end uri = " + uri.toString());
		return result;
	}

	private BitmapFactory.Options getOptionFromStream(InputStream stream) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(stream, null, options);
		return options;
	}

	private Bitmap decodeStream(InputStream stream, BitmapFactory.Options options2, int width, int height) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = calculateInSampleSize(options2, width, height);
		options.inJustDecodeBounds = false;
		return cropAndScaleBitmap(BitmapFactory.decodeStream(stream, null, options), width, height);
	}

//	private Bitmap decodeResource(@DrawableRes int res_id, int width, int height) {
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inJustDecodeBounds = true;
//		BitmapFactory.decodeResource(context.getResources(), res_id, options);
//		BitmapFactory.Options options2 = new BitmapFactory.Options();
//		options2.inSampleSize = calculateInSampleSize(options, width, height);
//		options2.inJustDecodeBounds = false;
//		return cropAndScaleBitmap(BitmapFactory.decodeResource(context.getResources(), res_id, options2), width, height);
//	}

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
		if (bitmap != croppedBitmap) {
			bitmap.recycle();
		}
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, true);
		if (croppedBitmap != scaledBitmap) {
			croppedBitmap.recycle();
		}
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

	public int getPageCount() {
		return pages.size();
	}

	public Page getPage(int index) {
		if (loop) {
			index = getFixedIndex(index);
		} else {
			if (index < 0 || index > getPageCount()) {
				return null;
			}
		}
		return pages.get(index);
	}

	public Bitmap getBitmap(int index) {
		if (loop) {
			index = getFixedIndex(index);
		} else {
			if (index < 0 || index > getPageCount()) {
				return null;
			}
		}
		return bitmaps.get(index);
	}

	private int getFixedIndex(int index) {
		if (index < 0 || index > getPageCount() - 1) {
			if (index > 0) {
				index = index % getPageCount();
			} else {
				if (index % getPageCount() == 0) {
					index = 0;
				} else {
					index = getPageCount() + index % getPageCount();
				}
			}
		}
		return index;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	public boolean getLoop() {
		return loop;
	}

	interface OnStateChangeListener {
		void onPageAdd();
	}
}
