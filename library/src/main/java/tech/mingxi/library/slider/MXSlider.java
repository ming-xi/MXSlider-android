package tech.mingxi.library.slider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.IntRange;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MXSlider extends View implements PageManager.OnStateChangeListener {
	public static final String DEBUG_TAG = "MXSlider";
	private static boolean DEBUG = false;
	private List<Rect> debugRects;
	/**
	 * duration to settling/flinging to target page
	 */
	private static final long SETTLING_DURATION = 250;
	/**
	 * absolute velocity threshold of fling in 1 second
	 */
	private static final int FLING_THRESHOLD = 2000;
	/**
	 * scheme for resources in assets folder
	 */
	static final String SCHEME_ASSETS = "assets";
	/**
	 * side-image's top margin
	 */
	private int sideTopMargin = 108;
	/**
	 * side-image's height
	 */
	private int sideHeight = 350;
	/**
	 * side-image's width
	 */
	private int sideWidth = 24;
	/**
	 * title's font-size
	 */
	private float titleFontSize = 32;
	/**
	 * subtitle's font-size
	 */
	private float subtitleFontSize = 12;
	/**
	 * swipe position in [0,size-1]. e.g. 1.5f means user swiped to the position where second image
	 * and third image are both displayed half of themselves.
	 */
	private float swipePosition = 0;
	/**
	 * whether user can slide infinitely or not
	 */
	private boolean loop = true;
	/**
	 * when setIndex(index, true) is called, this flag will be set to true to prevent user's touch events
	 * and will be set to false when settling animation is done and user touches screen again.
	 */
	private boolean userTouchCanceled = false;
	private Paint paint;
	/**
	 * shadow on images
	 */
	private LinearGradient shadowGradient;
	private PageManager pageManager;
	private OnSwipeListener onSwipeListener;

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
	 * @return a uri for PageManager
	 */
	public static Uri getUriForAsset(String path) {
		if (path.startsWith("/")) {
			path = path.replaceFirst("/+", "");
		}
		return Uri.parse(SCHEME_ASSETS + ":///" + path);
	}

	private void init() {
		titleFontSize = Util.getPxFromDp(getContext(), titleFontSize);
		subtitleFontSize = Util.getPxFromDp(getContext(), subtitleFontSize);
		sideTopMargin = Util.getPxFromDp(getContext(), sideTopMargin);
		sideHeight = Util.getPxFromDp(getContext(), sideHeight);
		sideWidth = Util.getPxFromDp(getContext(), sideWidth);
		setWillNotDraw(false);
		initPaint();
		if (DEBUG) {
			debugRects = new ArrayList<>();
			paint.setStyle(Paint.Style.STROKE);
		}
		setPageManager(new PageManager(getContext(), this));
	}

	private void initPaint() {
		paint = new Paint();
		paint.setFilterBitmap(true);
		paint.setDither(true);
		paint.setAntiAlias(true);
	}

	private void setPageManager(PageManager pageManager) {
		this.pageManager = pageManager;
		pageManager.setLoop(loop);
	}

	public PageManager getPageManager() {
		return pageManager;
	}

	public void setIndex(int index, boolean animated) {
		if (animated) {
			userTouchCanceled = true;
			scrollState = ViewPager.SCROLL_STATE_SETTLING;
			isFling = false;
			settlingStartSwipePosition = swipePosition;
			settlingDisplacement = getWidth() * (swipePosition - index);
		} else {
			swipePosition = index;
		}
		invalidate();
		triggerSwipeListener();
	}

	public void setIndex(int index) {
		setIndex(index, false);
	}

	public int getIndex() {
		return ((int) swipePosition);
	}

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
				if (userTouchCanceled) {
					return false;
				}
				settlingStarted = false;
			} else {
				userTouchCanceled = false;
			}
			scrollState = ViewPager.SCROLL_STATE_DRAGGING;
			start_x = event.getX();
			start_y = event.getY();
			lastSwipePosition = swipePosition * getWidth();
			lastFrameSwipePosition = 0;
			touchStartTimestamp = SystemClock.uptimeMillis();
			possibleClickFlag = true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (userTouchCanceled) {
				return false;
			}
			scrollState = ViewPager.SCROLL_STATE_DRAGGING;
			float x = event.getX();
			float y = event.getY();
			if (possibleClickFlag && Util.getDistanceBetween(start_x, start_y, x, y) > Util.getPxFromDp(getContext(), CLICK_DISTANCE_DP_THRESHOLD)) {
				//move, not click. So clear click flag
				possibleClickFlag = false;
			}
			swipePosition = (lastSwipePosition + start_x - x) / getWidth();
			fixSwipePosition();
			triggerSwipeListener();
			if (lastFrameSwipePosition != swipePosition) {
				lastFrameSwipePosition = swipePosition;
				invalidate();
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
			if (userTouchCanceled) {
				return false;
			}
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
				int index1 = (int) (swipePosition >= 0 ? swipePosition + 0.5 : swipePosition - 0.5);//slide to nearest position
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

	private void fixSwipePosition() {
		if (loop) {
			if (swipePosition < 0) {
				if (swipePosition % pageManager.getPageCount() == 0) {
					swipePosition = 0;
				} else {
					swipePosition = pageManager.getPageCount() + swipePosition % pageManager.getPageCount();
				}
			} else if (swipePosition > pageManager.getPageCount() - 1) {
				swipePosition = swipePosition % pageManager.getPageCount();
			}
		} else {
			if (swipePosition < 0) {
				swipePosition = 0;
			} else if (swipePosition > pageManager.getPageCount() - 1) {
				swipePosition = pageManager.getPageCount() - 1;
			}
		}
	}

	public int getSideTopMargin() {
		return sideTopMargin;
	}

	public void setSideTopMargin(int sideTopMargin) {
		this.sideTopMargin = sideTopMargin;
	}

	public int getSideHeight() {
		return sideHeight;
	}

	public void setSideHeight(int sideHeight) {
		this.sideHeight = sideHeight;
	}

	public int getSideWidth() {
		return sideWidth;
	}

	public void setSideWidth(int sideWidth) {
		this.sideWidth = sideWidth;
	}

	public float getTitleFontSize() {
		return titleFontSize;
	}

	public void setTitleFontSize(float titleFontSize) {
		this.titleFontSize = titleFontSize;
	}

	public float getSubtitleFontSize() {
		return subtitleFontSize;
	}

	public void setSubtitleFontSize(float subtitleFontSize) {
		this.subtitleFontSize = subtitleFontSize;
	}

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
		invalidate();
	}

	public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
		this.onSwipeListener = onSwipeListener;
	}

	public float getSwipePosition() {
		return swipePosition;
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

	private Rect rectSrc = new Rect();
	private Rect rectDst = new Rect();
	private Rect rectClip = new Rect();
	private Rect rectView = new Rect();

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = getWidth();
		int height = getHeight();
		drawPageImages(canvas, width, height);
		drawShadow(canvas, width, height);
		drawPageText(canvas, width, height);
		if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
			handleSettlingState();
		}
	}

	private void drawPageText(Canvas canvas, int width, int height) {
		float offset;
		//try to draw 5 text around swipePosition
		for (int index = (int) swipePosition - 2; index < swipePosition + 2; index++) {
			if (!loop && (index < 0 || index > pageManager.getPageCount() - 1)) {
				continue;
			}
			offset = index - swipePosition;
			if (Math.abs(offset) < 1) {
				canvas.save();
				canvas.skew(-(float) width / height, 0);
				float factor = 0.02f;
				canvas.translate(2 * width * offset, 0);
				rectClip.set((int) (factor * width), 0, (int) (2 * (1 - factor) * width), height);
				if (DEBUG) {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(index % 2 == 0 ? 0x3F00FF00 : 0x3FFF0000);
					canvas.drawRect(rectClip, paint);
					paint.setStyle(Paint.Style.STROKE);
				}
				paint.setStyle(Paint.Style.FILL);
				canvas.clipRect(rectClip, Region.Op.INTERSECT);
				canvas.translate(-(2 * width * offset), 0);
				canvas.skew((float) width / height, 0);
				paint.setShadowLayer(2, 2, 2, Color.parseColor("#7F000000"));
				paint.setTextSize(titleFontSize);
				paint.setColor(Color.WHITE);
				paint.setStyle(Paint.Style.FILL);
				paint.setTextAlign(Paint.Align.CENTER);
				StaticLayout staticLayout = new StaticLayout(pageManager.getPage(index).getTitle(), new TextPaint(paint), width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
				canvas.save();
				canvas.translate(width / 2.0f + offset * 0.1f * width, (height - staticLayout.getHeight()) * 0.34f);
				staticLayout.draw(canvas);
				paint.setTextSize(subtitleFontSize);
				canvas.translate(0, staticLayout.getHeight() + 40);
				staticLayout = new StaticLayout(pageManager.getPage(index).getSubtitle(), new TextPaint(paint), width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
				staticLayout.draw(canvas);
				canvas.restore();
				paint.setShadowLayer(0, 0, 0, 0);
				canvas.restore();
			}
		}
	}

	private void drawShadow(Canvas canvas, int width, int height) {
		paint.setShader(shadowGradient);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(rectView, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setShader(null);
	}

	private static int argb(
			@IntRange(from = 0, to = 255) int alpha,
			@IntRange(from = 0, to = 255) int red,
			@IntRange(from = 0, to = 255) int green,
			@IntRange(from = 0, to = 255) int blue) {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	private void drawPageImages(Canvas canvas, int width, int height) {
		float offset;
		int centerWidth = width - 2 * sideWidth;
		int bitmapCenterWidth;
		//draw background images
		//try to draw 5 images around swipePosition
		for (int index = (int) swipePosition - 2; index < swipePosition + 2; index++) {
			if (!loop && (index < 0 || index > pageManager.getPageCount() - 1)) {
				continue;
			}
			offset = index - swipePosition;
			rectDst.set(
					(int) (offset * width),
					0,
					(int) ((1 + offset) * width),
					height
			);
			if (Rect.intersects(rectDst, rectView)) {
				Bitmap bitmap = pageManager.getBitmap(index);
				if (bitmap == null || bitmap.isRecycled()) {
					paint.setColor(Color.GRAY);
					paint.setStyle(Paint.Style.FILL);
					canvas.drawRect(rectDst, paint);
					paint.setStyle(Paint.Style.STROKE);
				} else {
					rectSrc.set(
							0,
							0,
							bitmap.getWidth(),
							bitmap.getHeight()
					);
					canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
				}
				if (DEBUG) {
					debugRects.add(new Rect(rectDst));
				}
				paint.setColor(Color.BLACK);
				paint.setAlpha((int) (0.75 * Math.abs(offset) * 255));
				canvas.drawRect(rectDst, paint);
				paint.setAlpha(255);
			}
		}
		//draw center images
		//try to draw 5 images around swipePosition
		for (int index = (int) swipePosition - 2; index < swipePosition + 2; index++) {
			if (!loop && (index < 0 || index > pageManager.getPageCount() - 1)) {
				continue;
			}
			offset = index - swipePosition;
			rectDst.set(
					(int) (sideWidth + offset * centerWidth),
					sideTopMargin,
					(int) (sideWidth + offset * centerWidth + centerWidth),
					sideTopMargin + sideHeight
			);
			if (DEBUG) {
				debugRects.add(new Rect(rectDst));
			}
			if (Rect.intersects(rectDst, rectView)) {
				Bitmap bitmap = pageManager.getBitmap(index);
				if (bitmap == null || bitmap.isRecycled()) {
					paint.setColor(Color.GRAY);
					paint.setStyle(Paint.Style.FILL);
					canvas.drawRect(rectDst, paint);
					paint.setStyle(Paint.Style.STROKE);
				} else {
					bitmapCenterWidth = (int) (((double) bitmap.getWidth()) / width * centerWidth);
					rectSrc.set(
							(int) (bitmap.getWidth() * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset - 0.5)),
							(int) (bitmap.getHeight() * ((double) sideTopMargin) / height),
							(int) (bitmap.getWidth() * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset + 0.5)),
							(int) (bitmap.getHeight() * (((double) sideTopMargin + sideHeight) / height))
					);
					canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
				}
			}
		}
		drawDebugFrame(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		getGlobalVisibleRect(rectView);
		shadowGradient = new LinearGradient(0, 0, 0, rectView.height(),
				new int[]{0x1F000000, 0x3F000000, 0xAF000000, 0xFF000000},
				new float[]{0, 0.5f, 0.75f, 1}
				, android.graphics.Shader.TileMode.CLAMP);
		pageManager.refreshBitmaps(getWidth(), getHeight());
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
				fixSwipePosition();
				triggerSwipeListener();
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
				fixSwipePosition();
				triggerSwipeListener();
			}
		}
		invalidate();
	}

	private int lastIndex = 0;

	private void triggerSwipeListener() {
		if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
			int index = (int) swipePosition;
			if (lastIndex != index) {
				if (onSwipeListener != null) {
					onSwipeListener.onIndexChange(index);
				}
				lastIndex = index;
			}
		}
		if (onSwipeListener != null) {
			onSwipeListener.onSwipePositionChange(swipePosition);
		}
	}

	private void drawDebugFrame(Canvas canvas) {
		if (!DEBUG) {
			return;
		}
		paint.setStyle(Paint.Style.STROKE);
		for (Rect rect : debugRects) {
			canvas.drawRect(rect, paint);
		}
		debugRects.clear();
	}

	@Override
	public void onPageAdd() {
		invalidate();
	}

	public interface OnSwipeListener {
		void onSwipePositionChange(float position);

		void onIndexChange(int index);
	}
}
