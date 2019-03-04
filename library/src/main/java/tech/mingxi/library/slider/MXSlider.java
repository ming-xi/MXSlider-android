package tech.mingxi.library.slider;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import tech.mingxi.library.R;

public class MXSlider extends View implements PageManager.OnStateChangeListener {

	public enum TextPositionStyle {
		CLASSIC(0), PACKED(1);

		private final int id;

		static TextPositionStyle fromId(int id) {
			switch (id) {
				case 1:
					return PACKED;
				case 0:
				default:
					return CLASSIC;
			}
		}

		TextPositionStyle(int i) {
			id = i;
		}

		public int getId() {
			return id;
		}
	}

	public enum SideImageMeasuringStyle {
		PERCENTAGE(0), PIXEL(1);

		private final int id;

		static SideImageMeasuringStyle fromId(int id) {
			switch (id) {
				case 1:
					return PIXEL;
				case 0:
				default:
					return PERCENTAGE;
			}
		}

		SideImageMeasuringStyle(int i) {
			id = i;
		}

		public int getId() {
			return id;
		}
	}

	public static final String DEBUG_TAG = "MXSlider";

	private static final int DEFAULT_TITLE_FONT_SIZE = 32;
	private static final int DEFAULT_SUBTITLE_FONT_SIZE = 12;

	private static final float DEFAULT_SIDE_TOP_MARGIN = 108f;
	private static final float DEFAULT_SIDE_HEIGHT = 350f;
	private static final float DEFAULT_SIDE_WIDTH = 24f;

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

	private TextPositionStyle textPositionStyle;
	private SideImageMeasuringStyle sideImageMeasuringStyle;
	/**
	 * vertical position of the anchor of text
	 */
	private float textVerticalPosition = 0.45f;
	/**
	 * space between title and subtitle
	 */
	private float textVerticalSpacing = 40f;
	/**
	 * side-image's top margin's percentage in this view's Height
	 */
	private float sideTopMarginPercentage = 0.133f;
	/**
	 * side-image's height's percentage in this view's Height
	 */
	private float sideHeightPercentage = 0.431f;
	/**
	 * side-image's width's percentage in this view's Width
	 */
	private float sideWidthPercentage = 0.064f;
	/**
	 * side-image's top margin
	 */
	private float sideTopMargin;
	/**
	 * side-image's height
	 */
	private float sideHeight;
	/**
	 * side-image's width
	 */
	private float sideWidth;
	/**
	 * text's margin
	 */
	private float textMargin = 24;
	/**
	 * title's font-size
	 */
	private float titleFontSize;
	/**
	 * subtitle's font-size
	 */
	private float subtitleFontSize;
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
	 * if true, refreshBitmaps() will be triggered when onLayout() is called.
	 * it is recommended to turn this off if this view's size changes from time to time.
	 */
	private boolean refreshBitmapsOnLayout = true;
	/**
	 * when setIndex(id, true) is called, this flag will be set to true to prevent user's touch events
	 * and will be set to false when settling animation is done and user touches screen again.
	 */
	private boolean userTouchCanceled = false;
	private Paint paint;

	private int[] shadowGradientColors;

	private float[] shadowGradientPositions;
	/**
	 * shadow on images
	 */
	private LinearGradient shadowGradient;
	private PageManager pageManager;
	private OnSwipeListener onSwipeListener;

	private int scrollState = ViewPager.SCROLL_STATE_IDLE;

	public MXSlider(Context context) {
		super(context);
		init(null);
	}

	public MXSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public MXSlider(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	public MXSlider(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(attrs);
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

	private void init(AttributeSet attrs) {
		setPropertiesFromAttribute(attrs);

		setWillNotDraw(false);
		initPaint();
		if (DEBUG) {
			debugRects = new ArrayList<>();
			paint.setStyle(Paint.Style.STROKE);
		}
		setPageManager(new PageManager(getContext(), this));
	}

	private void setPropertiesFromAttribute(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MXSlider);
		if (a.hasValue(R.styleable.MXSlider_mx_textPositionStyle)) {
			textPositionStyle = TextPositionStyle.fromId(a.getInt(R.styleable.MXSlider_mx_textPositionStyle, 0));
		} else {
			textPositionStyle = TextPositionStyle.CLASSIC;
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideImageMeasuringStyle)) {
			sideImageMeasuringStyle = SideImageMeasuringStyle.fromId(a.getInt(R.styleable.MXSlider_mx_sideImageMeasuringStyle, 0));
		} else {
			sideImageMeasuringStyle = SideImageMeasuringStyle.PERCENTAGE;
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideTopMargin)) {
			sideTopMargin = a.getDimension(R.styleable.MXSlider_mx_sideTopMargin, 0);
		} else {
			sideTopMargin = Util.getPxFromDp(getContext(), DEFAULT_SIDE_TOP_MARGIN);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideWidth)) {
			sideWidth = a.getDimension(R.styleable.MXSlider_mx_sideWidth, 0);
		} else {
			sideWidth = Util.getPxFromDp(getContext(), DEFAULT_SIDE_WIDTH);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideHeight)) {
			sideHeight = a.getDimension(R.styleable.MXSlider_mx_sideHeight, 0);
		} else {
			sideHeight = Util.getPxFromDp(getContext(), DEFAULT_SIDE_HEIGHT);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideTopMarginPercentage)) {
			sideTopMarginPercentage = a.getFloat(R.styleable.MXSlider_mx_sideTopMarginPercentage, sideTopMarginPercentage);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideWidthPercentage)) {
			sideWidthPercentage = a.getFloat(R.styleable.MXSlider_mx_sideWidthPercentage, sideWidthPercentage);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_sideHeightPercentage)) {
			sideHeightPercentage = a.getFloat(R.styleable.MXSlider_mx_sideHeightPercentage, sideHeightPercentage);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_titleFontSize)) {
			titleFontSize = a.getDimension(R.styleable.MXSlider_mx_titleFontSize, 0);
		} else {
			titleFontSize = Util.getPxFromDp(getContext(), DEFAULT_TITLE_FONT_SIZE);
		}
		if (a.hasValue(R.styleable.MXSlider_mx_subtitleFontSize)) {
			subtitleFontSize = a.getDimension(R.styleable.MXSlider_mx_subtitleFontSize, 0);
		} else {
			subtitleFontSize = Util.getPxFromDp(getContext(), DEFAULT_SUBTITLE_FONT_SIZE);
		}
		refreshBitmapsOnLayout = a.getBoolean(R.styleable.MXSlider_mx_refreshBitmapsOnLayout, refreshBitmapsOnLayout);
		TypedArray shadowGradientColors;
		TypedArray shadowGradientPositions;
		if (a.hasValue(R.styleable.MXSlider_mx_shadowGradientColors) && a.hasValue(R.styleable.MXSlider_mx_shadowGradientPositions)) {
			int resourceId = a.getResourceId(R.styleable.MXSlider_mx_shadowGradientColors, 0);
			shadowGradientColors = getResources().obtainTypedArray(resourceId);
			resourceId = a.getResourceId(R.styleable.MXSlider_mx_shadowGradientPositions, 0);
			shadowGradientPositions = getResources().obtainTypedArray(resourceId);
		} else {
			if (a.hasValue(R.styleable.MXSlider_mx_shadowGradientColors) | a.hasValue(R.styleable.MXSlider_mx_shadowGradientPositions)) {
				Log.w(DEBUG_TAG, "only one of shadowGradientColors or shadowGradientPositions is set! so it will be ignored and default values will be used to prevent unexpected problems");
			}
			shadowGradientColors = getResources().obtainTypedArray(R.array.mx_slider_default_gradient_colors);
			shadowGradientPositions = getResources().obtainTypedArray(R.array.mx_slider_default_gradient_positions);
		}
		int length = Math.min(shadowGradientColors.length(), shadowGradientPositions.length());
		if (shadowGradientColors.length() != shadowGradientPositions.length()) {
			Log.w(DEBUG_TAG, "shadowGradientColors array's length not equals to shadowGradientPositions array's length!");
		}
		this.shadowGradientColors = new int[length];
		for (int i = 0; i < length; i++) {
			this.shadowGradientColors[i] = shadowGradientColors.getColor(i, Color.TRANSPARENT);
		}
		shadowGradientColors.recycle();
		this.shadowGradientPositions = new float[length];
		for (int i = 0; i < length; i++) {
			this.shadowGradientPositions[i] = shadowGradientPositions.getFloat(i, 0);
		}
		shadowGradientPositions.recycle();
		a.recycle();
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

	public float getSideTopMarginPercentage() {
		return sideTopMarginPercentage;
	}

	public void setSideTopMarginPercentage(float sideTopMarginPercentage) {
		this.sideTopMarginPercentage = sideTopMarginPercentage;
		refreshSideImageSize();
		invalidate();
	}

	public float getSideHeightPercentage() {
		return sideHeightPercentage;
	}

	public void setSideHeightPercentage(float sideHeightPercentage) {
		this.sideHeightPercentage = sideHeightPercentage;
		refreshSideImageSize();
		invalidate();
	}

	public float getSideWidthPercentage() {
		return sideWidthPercentage;
	}

	public void setSideWidthPercentage(float sideWidthPercentage) {
		this.sideWidthPercentage = sideWidthPercentage;
		refreshSideImageSize();
		invalidate();
	}

	public float getSideTopMargin() {
		return sideTopMargin;
	}

	public void setSideTopMargin(float sideTopMargin) {
		this.sideTopMargin = sideTopMargin;
		invalidate();
	}

	public float getSideHeight() {
		return sideHeight;
	}

	public void setSideHeight(float sideHeight) {
		this.sideHeight = sideHeight;
		invalidate();
	}

	public float getSideWidth() {
		return sideWidth;
	}

	public void setSideWidth(float sideWidth) {
		this.sideWidth = sideWidth;
		invalidate();
	}

	public float getTitleFontSize() {
		return titleFontSize;
	}

	public void setTitleFontSize(float titleFontSize) {
		this.titleFontSize = titleFontSize;
		invalidate();
	}

	public float getSubtitleFontSize() {
		return subtitleFontSize;
	}

	public void setSubtitleFontSize(float subtitleFontSize) {
		this.subtitleFontSize = subtitleFontSize;
		invalidate();
	}

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
		invalidate();
	}

	public boolean isRefreshBitmapsOnLayout() {
		return refreshBitmapsOnLayout;
	}

	public void setRefreshBitmapsOnLayout(boolean refreshBitmapsOnLayout) {
		this.refreshBitmapsOnLayout = refreshBitmapsOnLayout;
	}

	public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
		this.onSwipeListener = onSwipeListener;
	}

	public float getSwipePosition() {
		return swipePosition;
	}

	public TextPositionStyle getTextPositionStyle() {
		return textPositionStyle;
	}

	public void setTextPositionStyle(TextPositionStyle textPositionStyle) {
		this.textPositionStyle = textPositionStyle;
		invalidate();
	}

	public SideImageMeasuringStyle getSideImageMeasuringStyle() {
		return sideImageMeasuringStyle;
	}

	public void setSideImageMeasuringStyle(SideImageMeasuringStyle sideImageMeasuringStyle) {
		this.sideImageMeasuringStyle = sideImageMeasuringStyle;
	}

	private void refreshSideImageSize() {
		if (sideImageMeasuringStyle == SideImageMeasuringStyle.PERCENTAGE) {
			int width = getWidth();
			int height = getHeight();
			sideTopMargin = height * sideTopMarginPercentage;
			sideHeight = height * sideHeightPercentage;
			sideWidth = width * sideWidthPercentage;
		}
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
		drawShadow(canvas);
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
				paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
				paint.setStyle(Paint.Style.FILL);
				paint.setTextAlign(Paint.Align.CENTER);
				StaticLayout titleStaticLayout;
				StaticLayout subtitleStaticLayout;
				int titleHeight;
				int subtitleHeight;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					titleStaticLayout = StaticLayout.Builder.obtain(pageManager.getPage(index).getTitle(), 0, pageManager.getPage(index).getTitle().length(), new TextPaint(paint), width).build();
				} else {
					titleStaticLayout = new StaticLayout(pageManager.getPage(index).getTitle(), new TextPaint(paint), width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
				}
				titleHeight = titleStaticLayout.getHeight();
				paint.setTextSize(subtitleFontSize);
				paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					subtitleStaticLayout = StaticLayout.Builder.obtain(pageManager.getPage(index).getSubtitle(), 0, pageManager.getPage(index).getSubtitle().length(), new TextPaint(paint), width).build();
				} else {
					subtitleStaticLayout = new StaticLayout(pageManager.getPage(index).getSubtitle(), new TextPaint(paint), width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
				}
				subtitleHeight = subtitleStaticLayout.getHeight();
				canvas.save();
				switch (textPositionStyle) {
					case CLASSIC:
						canvas.translate(width / 2.0f + offset * 0.1f * width, height * textVerticalPosition - titleHeight);
						break;
					case PACKED:
						canvas.translate(width / 2.0f + offset * 0.1f * width, (height - titleHeight - subtitleHeight) * textVerticalPosition);
						break;
				}
				titleStaticLayout.draw(canvas);
				canvas.translate(0, titleHeight + textVerticalSpacing);
				subtitleStaticLayout.draw(canvas);
				canvas.restore();
				paint.setShadowLayer(0, 0, 0, 0);
				canvas.restore();
			}
		}
	}

	private void drawShadow(Canvas canvas) {
		if (shadowGradient != null) {
			paint.setShader(shadowGradient);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRect(rectView, paint);
		}
		paint.setStyle(Paint.Style.STROKE);
		paint.setShader(null);
	}

	private void drawPageImages(Canvas canvas, int width, int height) {
		float offset;
		int centerWidth = (int) (width - 2 * sideWidth);
		int bitmapCenterWidth;
		double ratio = ((double) width) / height;
		float x = 0;//new width or height for bitmap to fit this view's size
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
					if (((double) bitmap.getWidth()) / bitmap.getHeight() <= ratio) {
						//crop top and bottom
						x = (float) (bitmap.getWidth() / ratio);
						rectSrc.set(
								0,
								(int) ((bitmap.getHeight() - x) / 2.0),
								bitmap.getWidth(),
								(int) ((bitmap.getHeight() + x) / 2.0)
						);
					} else {
						//crop left and right
						x = (float) (bitmap.getHeight() * ratio);
						rectSrc.set(
								(int) ((bitmap.getWidth() - x) / 2.0),
								0,
								(int) ((bitmap.getWidth() + x) / 2.0),
								bitmap.getHeight()
						);
					}
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
					((int) sideTopMargin),
					(int) (sideWidth + offset * centerWidth + centerWidth),
					((int) (sideTopMargin + sideHeight))
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
					if (((double) bitmap.getWidth()) / bitmap.getHeight() <= ratio) {
						//crop top and bottom
						x = (float) (bitmap.getWidth() / ratio);
						bitmapCenterWidth = (int) ((double) bitmap.getWidth() / width * centerWidth);
						rectSrc.set(
								(int) (bitmap.getWidth() * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset - 0.5)),
								(int) ((bitmap.getHeight() - x) / 2.0 + x * sideTopMargin / height),
								(int) (bitmap.getWidth() * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset + 0.5)),
								(int) ((bitmap.getHeight() - x) / 2.0 + x * (((double) sideTopMargin + sideHeight) / height))
						);
					} else {
						//crop left and right
						x = (float) (bitmap.getHeight() * ratio);
						bitmapCenterWidth = (int) (x / width * centerWidth);
						rectSrc.set(
								(int) ((bitmap.getWidth() - x) / 2.0 + x * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset - 0.5)),
								(int) (bitmap.getHeight() * ((double) sideTopMargin) / height),
								(int) ((bitmap.getWidth() - x) / 2.0 + x * (-0.5 * offset + 0.5) + bitmapCenterWidth * (0.5 * offset + 0.5)),
								(int) (bitmap.getHeight() * (((double) sideTopMargin + sideHeight) / height))
						);
					}
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

	boolean firstLayout = true;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (firstLayout || refreshBitmapsOnLayout) {
			refreshBitmaps(getWidth(), getHeight());
			firstLayout = false;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		getLocalVisibleRect(rectView);
		Log.i(DEBUG_TAG, String.format("onSizeChanged w=%d h=%d oldw=%d oldh=%d rect=%s", w, h, oldw, oldh, rectView.toString()));
		if (shadowGradientColors != null && shadowGradientPositions != null) {
			shadowGradient = new LinearGradient(0, 0, 0, rectView.height(), shadowGradientColors, shadowGradientPositions, android.graphics.Shader.TileMode.CLAMP);
		}
		refreshSideImageSize();
	}

	public void refreshBitmaps(int width, int height) {
		pageManager.refreshBitmaps(width, height);
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
