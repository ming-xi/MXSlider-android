package tech.mingxi.library;

import android.content.Context;

public class Util {
	public static double getDistanceBetween(float x1, float y1, float x2, float y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	public static int getPxFromDp(Context c, float dipValue) {
		final float scale = c.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}
}
