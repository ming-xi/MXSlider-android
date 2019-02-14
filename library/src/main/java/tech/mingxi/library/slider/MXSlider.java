package tech.mingxi.library.slider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import tech.mingxi.library.Util;

public class MXSlider extends View implements ImageManager.OnStateChangeListener {
	/**
	 * duration to settling/flinging to target page
	 */
	private static final long SETTLING_DURATION = 250;
	/**
	 * absolute velocity threshold of fling in 1 second
	 */
	private static final int FLING_THRESHOLD = 3000;
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

	VelocityTracker velocityTracker;
	private float start_x;
	private float start_y;
	private float lastSwipePosition = 0;
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
			invalidate();
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

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = getWidth();
		int height = getHeight();
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
		if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
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
	}

	@Override
	public void onBitmapAdd() {
		invalidate();
	}
}
