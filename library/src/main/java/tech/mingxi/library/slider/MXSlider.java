package tech.mingxi.library.slider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MXSlider extends View implements ImageManager.OnStateChangeListener {
	private static boolean DEBUG = false;
	private List<Rect> debugRects;
	/**
	 * duration to settling/flinging to target page
	 */
	private static final long SETTLING_DURATION = 250;
	/**
	 * absolute velocity threshold of fling in 1 second
	 */
	private static final int FLING_THRESHOLD = 3000;
	/**
	 * scheme for resources in assets folder
	 */
	static final String SCHEME_ASSETS = "assets";
	/**
	 * side-image's top margin
	 */
	private int sideTopMargin = 0;
	/**
	 * side-image's bottom margin
	 */
	private int sideBottomMargin = 0;
	/**
	 * side-image's width
	 */
	private int sideWidth = 0;

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

	private int scrollState = ViewPager.SCROLL_STATE_IDLE;

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

	/**
	 * convert a asset-path to uri
	 *
	 * @param path full path of file in assets folder
	 * @return a uri for ImageManager
	 */
	public static Uri getUriForAsset(String path) {
		if (path.startsWith("/")) {
			path = path.replaceFirst("/+", "");
		}
		return Uri.parse(SCHEME_ASSETS + ":///" + path);
	}

	private void init() {
		sideTopMargin = Util.getPxFromDp(getContext(), 96);
		sideBottomMargin = Util.getPxFromDp(getContext(), 48);
		sideWidth = Util.getPxFromDp(getContext(), 32);
		setWillNotDraw(false);
		paint = new Paint();
		paint.setFilterBitmap(true);
		paint.setDither(true);
		paint.setAntiAlias(true);
		if (DEBUG) {
			debugRects = new ArrayList<>();
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.GREEN);
		}
		setImageManager(new ImageManager(getContext(), this));
	}

	private void setImageManager(ImageManager imageManager) {
		this.imageManager = imageManager;
	}

	public ImageManager getImageManager() {
		return imageManager;
	}

	private Rect rectSrc = new Rect();
	private Rect rectDst = new Rect();
	private Rect rectView = new Rect();

	@Override
	public boolean performClick() {
		return super.performClick();
	}

	VelocityTracker velocityTracker;
	private float start_x;
	private float start_y;
	private float lastSwipePosition = 0;
	private float lastFrameSwipePosition = 0;
	private long touchStartTimestamp = 0;
	private boolean possibleClickFlag = true;
	private static final int CLICK_DISTANCE_DP_THRESHOLD = 24;
	private static final long CLICK_DURATION_THRESHOLD = 500;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		}
		velocityTracker.addMovement(event);
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
				settlingStarted = false;
			}
			scrollState = ViewPager.SCROLL_STATE_DRAGGING;
			start_x = event.getX();
			start_y = event.getY();
			lastSwipePosition = swipePosition * getWidth();
			lastFrameSwipePosition = 0;
			touchStartTimestamp = SystemClock.uptimeMillis();
			possibleClickFlag = true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			scrollState = ViewPager.SCROLL_STATE_DRAGGING;
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
			if (lastFrameSwipePosition != swipePosition) {
				lastFrameSwipePosition = swipePosition;
				invalidate();
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			float x = event.getX();
			float y = event.getY();
			velocityTracker.computeCurrentVelocity(1000);
			float velocity = velocityTracker.getXVelocity();
			scrollState = ViewPager.SCROLL_STATE_SETTLING;
			isFling = Math.abs(velocity) >= FLING_THRESHOLD;
			settlingStartSwipePosition = swipePosition;
			if (isFling) {
				settlingStartVelocity = velocity / 1000;
				int index1 = (int) swipePosition;
				if (velocity > 0) {//swipe to previous image
					settlingDisplacement = getWidth() * (swipePosition - index1);//>0
				} else {
					settlingDisplacement = getWidth() * ((swipePosition - index1) - 1);//<0
				}
				settlingAcceleration = -settlingStartVelocity * settlingStartVelocity / (2 * settlingDisplacement);
			} else {
				int index1 = (int) (swipePosition + 0.5);//slide to nearest position
				settlingDisplacement = getWidth() * (swipePosition - index1);
			}
			invalidate();
			lastSwipePosition = swipePosition * getWidth();
			if (possibleClickFlag && SystemClock.uptimeMillis() - touchStartTimestamp < CLICK_DURATION_THRESHOLD) {
				performClick();
			}
		}
		return true;
	}

	/**
	 * whether settling is triggered by user's fling
	 */
	private boolean isFling = false;
	/**
	 * whether settling parameters are initiated
	 */
	private boolean settlingStarted = false;
	/**
	 * settling acceleration
	 */
	private float settlingAcceleration = 0;
	/**
	 * settling starting velocity
	 */
	private float settlingStartVelocity = 0;
	/**
	 * settling distance to go
	 */
	private float settlingDisplacement = 0;
	/**
	 * when settling started
	 */
	private long settlingStartTimestamp = 0;
	/**
	 * when settling should end
	 */
	private long settlingEndTimestamp = 0;
	/**
	 * swipe position when settling begins
	 */
	private float settlingStartSwipePosition = 0;

//	@Override
//	protected void onDraw(Canvas canvas) {
//		super.onDraw(canvas);
//		int width = getWidth();
//		int height = getHeight();
//		//draw background left image
//		int indexCurrent = (int) swipePosition;
//		Bitmap bitmapCurrent = imageManager.getImage(indexCurrent);
//		if (bitmapCurrent != null) {
//			rectSrc.set(
//					(int) ((swipePosition - indexCurrent) * bitmapCurrent.getWidth()),
//					0,
//					bitmapCurrent.getWidth(),
//					bitmapCurrent.getHeight()
//			);
//			rectDst.set(
//					0,
//					0,
//					(int) ((1 - (swipePosition - indexCurrent)) * width),
//					height
//			);
//			canvas.drawBitmap(bitmapCurrent, rectSrc, rectDst, paint);
//			Log.w("MXSlider", "draw bitmap 1 pos=" + swipePosition);
//		}
//		if (indexCurrent * 1f != swipePosition) {
//			//draw background right image if visible
//			int indexNext = indexCurrent + 1;
//			Bitmap bitmapNext = imageManager.getImage(indexNext);
//			if (bitmapNext != null) {
//				rectSrc.set(
//						0,
//						0,
//						(int) ((1 - (indexNext - swipePosition)) * bitmapNext.getWidth()),
//						bitmapNext.getHeight()
//				);
//				rectDst.set(
//						(int) ((indexNext - swipePosition) * width),
//						0,
//						width,
//						height
//				);
//				canvas.drawBitmap(bitmapNext, rectSrc, rectDst, paint);
//				Log.w("MXSlider", "draw bitmap 2 pos=" + swipePosition);
//			}
//		}
//		if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
//			handleSettlingState();
//		}
//	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = getWidth();
		int height = getHeight();
		float offset;
		int centerWidth = width - 2 * sideWidth;
		int bitmapCenterWidth;
		int imageCount = imageManager.getImageCount();
		for (int index = 0; index < imageCount; index++) {
			//visible in view as background
			offset = index - swipePosition;
			rectDst.set(
					(int) (offset * width),
					0,
					(int) ((1 + offset) * width),
					height
			);
			if (Rect.intersects(rectDst, rectView)) {
				Bitmap bitmap = imageManager.getImage(index);
				rectSrc.set(
						0,
						0,
						bitmap.getWidth(),
						bitmap.getHeight()
				);
				if (DEBUG) {
					debugRects.add(new Rect(rectSrc));
					debugRects.add(new Rect(rectDst));
				}
				canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
			}
		}
		Log.i("MXSlider", "draw center views");
		for (int index = 0; index < imageCount; index++) {
			offset = index - swipePosition;
			rectDst.set(
					(int) (sideWidth + offset * centerWidth),
					sideTopMargin,
					(int) (sideWidth + offset * centerWidth + centerWidth),
					height - sideBottomMargin
			);
			if (Rect.intersects(rectDst, rectView)) {
				Log.i("MXSlider", String.format("dst=%s view=%s offset=%.2f", rectDst.toString(), rectView.toString(), offset));
				Bitmap bitmap = imageManager.getImage(index);
				bitmapCenterWidth = (int) (((double) bitmap.getWidth()) / width * centerWidth);
				rectSrc.set(
						(int) (bitmap.getWidth() * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset - 0.5)),
						(int) (bitmap.getHeight() * ((double) sideTopMargin) / height),
						(int) (bitmap.getWidth() * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset + 0.5)),
						(int) (bitmap.getHeight() * (1 - ((double) sideBottomMargin) / height))
				);
				if (DEBUG) {
					debugRects.add(new Rect(rectSrc));
					debugRects.add(new Rect(rectDst));
				}
				canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
			}
		}
		drawDebugFrame(canvas);
		if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
			handleSettlingState();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
//		rectView.set(left,top,right,bottom);
		getGlobalVisibleRect(rectView);
		imageManager.refreshBitmaps(getWidth(), getHeight());
	}

	private void handleSettlingState() {
		if (settlingDisplacement == 0) {
			scrollState = ViewPager.SCROLL_STATE_IDLE;
			settlingStarted = false;
			return;
		}
		if (isFling) {
			//triggered by user's fling
			if (!settlingStarted) {
				settlingStarted = true;
				settlingStartTimestamp = SystemClock.uptimeMillis();
				settlingEndTimestamp = (long) (settlingStartTimestamp - settlingStartVelocity / settlingAcceleration);
			} else {
				float timeOffset = SystemClock.uptimeMillis() - settlingStartTimestamp;
				float displacementOffset = settlingStartVelocity * timeOffset + 0.5f * settlingAcceleration * timeOffset * timeOffset;
				if (settlingStartTimestamp + timeOffset >= settlingEndTimestamp) {
					displacementOffset = settlingDisplacement;
					scrollState = ViewPager.SCROLL_STATE_IDLE;
					settlingStarted = false;
				}
				swipePosition = settlingStartSwipePosition - displacementOffset / getWidth();
				if (swipePosition < 0) {
					swipePosition = 0;
				} else if (swipePosition > imageManager.getImageCount() - 1) {
					swipePosition = imageManager.getImageCount() - 1;
				}
			}
		} else {
			//triggered by touch-up event
			if (!settlingStarted) {
				settlingStarted = true;
				settlingStartTimestamp = SystemClock.uptimeMillis();
				settlingEndTimestamp = settlingStartTimestamp + SETTLING_DURATION;
				settlingAcceleration = -2 * settlingDisplacement / (SETTLING_DURATION * SETTLING_DURATION);
				settlingStartVelocity = -settlingAcceleration * SETTLING_DURATION;
			} else {
				float timeOffset = SystemClock.uptimeMillis() - settlingStartTimestamp;
				float displacementOffset = settlingStartVelocity * timeOffset + 0.5f * settlingAcceleration * timeOffset * timeOffset;
				if (settlingStartTimestamp + timeOffset >= settlingEndTimestamp) {
					displacementOffset = settlingDisplacement;
					scrollState = ViewPager.SCROLL_STATE_IDLE;
					settlingStarted = false;
				}
				swipePosition = settlingStartSwipePosition - displacementOffset / getWidth();
				if (swipePosition < 0) {
					swipePosition = 0;
				} else if (swipePosition > imageManager.getImageCount() - 1) {
					swipePosition = imageManager.getImageCount() - 1;
				}
			}
		}
		invalidate();
	}

	private void drawDebugFrame(Canvas canvas) {
		if (!DEBUG) {
			return;
		}
		for (Rect rect : debugRects) {
			canvas.drawRect(rect, paint);
		}
		debugRects.clear();
	}

	@Override
	public void onBitmapAdd() {
		invalidate();
	}
}
