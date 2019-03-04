package tech.mingxi.musicplayerdemo;

import android.content.Context;

class Util {

	static int getPxFromDp(Context c, float dipValue) {
		final float scale = c.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}
}
