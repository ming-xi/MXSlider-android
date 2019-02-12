package tech.mingxi.library.slider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import tech.mingxi.library.Util;

public class MXSlider extends View implements ImageManager.OnStateChangeListener {
	/**
	 * prev image's top margin
	 */
	private float prevTopMargin = 0;
	/**
	 * prev image's bottom margin
	 */
	private float prevBottomMargin = 0;
	/**
	 * prev image's width
	 */
	private float prevWidth = 0;
	/**
	 * next image's top margin
	 */
	private float nextTopMargin = 0;
	/**
	 * next image's bottom margin
	 */
	private float nextBottomMargin = 0;
	/**
	 * next image's width
	 */
	private float nextWidth = 0;
	/**
	 * swipe position in [0,size-1]. e.g. 1.5f means user swiped to the position where second image
	 * and third image are both displayed half of themselves.
	 */
	private float swipePosition = 0;
	/**
	 * whether user can slide infinitely or not
	 */
	private boolean loop = true;
	private Paint paint;
	private ImageManager imageManager;

	public MXSlider(Context context) {
		super(context);
		init();
	}

	public MXSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MXSlider(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public MXSlider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		setWillNotDraw(false);
		paint = new Paint();
		paint.setAntiAlias(true);
		setImageManager(new ImageManager(this));
	}

	private void setImageManager(ImageManager imageManager) {
		this.imageManager = imageManager;
	}

	public ImageManager getImageManager() {
		return imageManager;
	}

	private Rect rectSrc = new Rect();
	private Rect rectDst = new Rect();

	@Override
	public boolean performClick() {
		return super.performClick();
	}


	private float start_x;
	private float start_y;
	private float lastSwipePosition = 0;
	private long touchStartTimestamp = 0;
	private boolean possibleClickFlag = true;
	private static final int CLICK_DISTANCE_DP_THRESHOLD = 24;
	private static final long CLICK_DURATION_THRESHOLD = 500;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			start_x = event.getX();
			start_y = event.getY();
			lastSwipePosition = swipePosition * getWidth();
			touchStartTimestamp = SystemClock.uptimeMillis();
			possibleClickFlag = true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			float x = event.getX();
			float y = event.getY();
			if (possibleClickFlag && Util.getDistanceBetween(start_x, start_y, x, y) > Util.getPxFromDp(getContext(), CLICK_DISTANCE_DP_THRESHOLD)) {
				//move, not click. So clear click flag
				possibleClickFlag = false;
			}
			swipePosition = (lastSwipePosition + start_x - x) / getWidth();
			if (swipePosition < 0) {
				swipePosition = 0;
			} else if (swipePosition > imageManager.getImageCount() - 1) {
				swipePosition = imageManager.getImageCount() - 1;
			}
			invalidate();
			Log.d("MXSlider", String.format("x=%.1f,y=%.1f,pos=%.1f", x, y, swipePosition));
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			float x = event.getX();
			float y = event.getY();
			lastSwipePosition = swipePosition * getWidth();
			if (possibleClickFlag && SystemClock.uptimeMillis() - touchStartTimestamp < CLICK_DURATION_THRESHOLD) {
				performClick();
			}
		}
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = getWidth();
		int height = getHeight();
		double ratio = width * 1.0 / height;
//		int index = (int) Math.floor(swipePosition + 0.5f);
//
//		Bitmap current = imageManager.getImage(index);
//		Bitmap prev = imageManager.getImage(index - 1);
//		Bitmap next = imageManager.getImage(index + 1);
//		if (current != null) {
//			rectSrc.set(0, 0, );
//			canvas.drawBitmap(current, );
//		}

		//draw background left image
		int index1 = (int) swipePosition;
		Bitmap bitmap1 = imageManager.getImage(index1);
		rectSrc.set(
				(int) ((swipePosition - index1) * bitmap1.getWidth()),
				0,
				bitmap1.getWidth(),
				bitmap1.getHeight()
		);
		rectDst.set(
				0,
				0,
				(int) ((1 - (swipePosition - index1)) * width),
				height
		);
		canvas.drawBitmap(bitmap1, rectSrc, rectDst, paint);
		if (index1 * 1f != swipePosition) {
			//draw background right image if visible
			int index2 = index1 + 1;
			Bitmap bitmap2 = imageManager.getImage(index2);
			rectSrc.set(
					0,
					0,
					(int) ((1 - (index2 - swipePosition)) * bitmap2.getWidth()),
					bitmap2.getHeight()
			);
			rectDst.set(
					(int) ((index2 - swipePosition) * width),
					0,
					width,
					height
			);
			canvas.drawBitmap(bitmap2, rectSrc, rectDst, paint);
		}
	}

	@Override
	public void onBitmapAdd() {
		invalidate();
	}
}
