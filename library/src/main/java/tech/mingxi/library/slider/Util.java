package tech.mingxi.library.slider;

import android.content.Context;

class Util {
	public static final String DEBUG_TAG = "MXSlider";

	static double getDistanceBetween(float x1, float y1, float x2, float y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	static int getPxFromDp(Context c, float dipValue) {
		final float scale = c.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}
}
