package tech.mingxi.library.slider;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class ImageManager {
	private final OnStateChangeListener onStateChangeListener;
	private ArrayList<Bitmap> bitmaps;

	public ImageManager(OnStateChangeListener onStateChangeListener) {
		this.onStateChangeListener = onStateChangeListener;
		bitmaps = new ArrayList<>();
	}

	public void addBitmap(Bitmap bitmap) {
		bitmaps.add(bitmap);
		if (onStateChangeListener != null) {
			onStateChangeListener.onBitmapAdd();
		}
	}

	public int getImageCount() {
		return bitmaps.size();
	}

	public Bitmap getImage(int index) {
		if (index < bitmaps.size()) {
			return bitmaps.get(index);
		} else {
			return null;
		}
	}

	interface OnStateChangeListener {
		void onBitmapAdd();
	}
}
