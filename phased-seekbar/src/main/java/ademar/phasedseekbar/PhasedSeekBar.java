package ademar.phasedseekbar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PhasedSeekBar extends View {

    protected static final int[] STATE_NORMAL = new int[] {};
    protected static final int[] STATE_SELECTED = new int[] { android.R.attr.state_selected };
    protected static final int[] STATE_PRESSED = new int[] { android.R.attr.state_pressed };

    protected int[] mState = STATE_SELECTED;

    protected boolean mModeIsHorizontal = true;
    protected boolean mFirstDraw = true;
    protected boolean mUpdateFromPosition = false;
    protected boolean mDrawOnOff = true;
    protected boolean mFixPoint = true;

    protected Drawable mBackgroundDrawable;
    protected RectF mBackgroundPaddingRect;

    protected int mCurrentX, mCurrentY;
    protected int mPivotX, mPivotY;
    protected int mItemHalfWidth, mItemHalfHeight;
    protected int mItemAnchorHalfWidth, mItemAnchorHalfHeight;
    protected int[][] mAnchors;
    protected int mCurrentItem;

    protected PhasedAdapter mAdapter;
    protected PhasedListener mListener;
    protected PhasedInteractionListener mInteractionListener;

    public PhasedSeekBar(Context context) {
        super(context);
        init(null, 0);
    }

    public PhasedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PhasedSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    protected void init(AttributeSet attrs, int defStyleAttr) {
        mBackgroundPaddingRect = new RectF();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.PhasedSeekBar, defStyleAttr, 0);

            setDrawOnOff(a.getBoolean(R.styleable.PhasedSeekBar_phased_draw_on_off, mDrawOnOff));
            setFixPoint(a.getBoolean(R.styleable.PhasedSeekBar_phased_fix_point, mFixPoint));
            setModeIsHorizontal(a.getInt(R.styleable.PhasedSeekBar_phased_mode, 0) != 2);

            mBackgroundPaddingRect.left = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_left, 0.0f);
            mBackgroundPaddingRect.top = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_top, 0.0f);
            mBackgroundPaddingRect.right = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_right, 0.0f);
            mBackgroundPaddingRect.bottom = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_bottom, 0.0f);

            mItemHalfWidth = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_item_width, 0.0f) / 2.0f);
            mItemHalfHeight = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_item_width, 0.0f) / 2.0f);
            mItemAnchorHalfWidth = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_anchor_width, 0.0f) / 2.0f);
            mItemAnchorHalfHeight = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_anchor_height, 0.0f) / 2.0f);

            mBackgroundDrawable = a.getDrawable(R.styleable.PhasedSeekBar_phased_base_background);

            a.recycle();
        }
    }

    protected void configure() {
        Rect rect = new Rect((int) mBackgroundPaddingRect.left,
                (int) mBackgroundPaddingRect.top,
                (int) (getWidth() - mBackgroundPaddingRect.right),
                (int) (getHeight() - mBackgroundPaddingRect.bottom));
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.setBounds(rect);
        }
        mCurrentX = mPivotX = getWidth() / 2;
        mCurrentY = mPivotY = getHeight() / 2;

        int count = getCount();
        int widthBase = rect.width() / count;
        int widthHalf = widthBase / 2;
        int heightBase = rect.height() / count;
        int heightHalf = heightBase / 2;
        mAnchors = new int[count][2];
        for (int i = 0, j = 1; i < count; i++, j++) {
            mAnchors[i][0] = mModeIsHorizontal ? widthBase * j - widthHalf + rect.left : mPivotX;
            mAnchors[i][1] = !mModeIsHorizontal ? heightBase * j - heightHalf + rect.top : mPivotY;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setFirstDraw(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFirstDraw) configure();
        if (mBackgroundDrawable != null) mBackgroundDrawable.draw(canvas);
        if (isInEditMode()) return;

        Drawable itemOff;
        Drawable itemOn;
        StateListDrawable stateListDrawable;
        int count = getCount();

        if (!mUpdateFromPosition) {
            int distance;
            int minIndex = 0;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                distance = Math.abs(mModeIsHorizontal ? mAnchors[i][0] - mCurrentX : mAnchors[i][1] - mCurrentY);
                if (minDistance > distance) {
                    minIndex = i;
                    minDistance = distance;
                }
            }

            setCurrentItem(minIndex);
            stateListDrawable = mAdapter.getItem(minIndex);
        } else {
            mUpdateFromPosition = false;
            mCurrentX = mAnchors[mCurrentItem][0];
            mCurrentY = mAnchors[mCurrentItem][1];
            stateListDrawable = mAdapter.getItem(mCurrentItem);
        }
        stateListDrawable.setState(mState);
        itemOn = stateListDrawable.getCurrent();

        for (int i = 0; i < count; i++) {
            if (!mDrawOnOff && i == mCurrentItem) continue;
            stateListDrawable = mAdapter.getItem(i);
            stateListDrawable.setState(STATE_NORMAL);
            itemOff = stateListDrawable.getCurrent();
            itemOff.setBounds(
                    mAnchors[i][0] - mItemHalfWidth,
                    mAnchors[i][1] - mItemHalfHeight,
                    mAnchors[i][0] + mItemHalfWidth,
                    mAnchors[i][1] + mItemHalfHeight);
            itemOff.draw(canvas);
        }

        itemOn.setBounds(
                mCurrentX - mItemHalfWidth,
                mCurrentY - mItemHalfHeight,
                mCurrentX + mItemHalfWidth,
                mCurrentY + mItemHalfHeight);
        itemOn.draw(canvas);

        setFirstDraw(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCurrentX = mModeIsHorizontal ? getNormalizedX(event) : mPivotX;
        mCurrentY = !mModeIsHorizontal ? getNormalizedY(event) : mPivotY;
        int action = event.getAction();
        mUpdateFromPosition = mFixPoint && action == MotionEvent.ACTION_UP;
        mState = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ? STATE_SELECTED : STATE_PRESSED;
        invalidate();

        if (mInteractionListener != null) {
            mInteractionListener.onInteracted(mCurrentX, mCurrentY, mCurrentItem, event);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                return true;
        }
        return super.onTouchEvent(event);
    }

    protected int getNormalizedX(MotionEvent event) {
        return Math.min(Math.max((int) event.getX(), mItemHalfWidth), getWidth() - mItemHalfWidth);
    }

    protected int getNormalizedY(MotionEvent event) {
        return Math.min(Math.max((int) event.getY(), mItemHalfHeight), getHeight() - mItemHalfHeight);
    }

    protected int getCount() {
        return isInEditMode() ? 3 : mAdapter.getCount();
    }

    public void setAdapter(PhasedAdapter adapter) {
        mAdapter = adapter;
    }

    public void setFirstDraw(boolean firstDraw) {
        mFirstDraw = firstDraw;
    }

    public void setListener(PhasedListener listener) {
        mListener = listener;
    }

    public void setInteractionListener(PhasedInteractionListener interactionListener) {
        mInteractionListener = interactionListener;
    }

    public void setPosition(int position) {
        position = position < 0 ? 0 : position;
        position = position >= mAdapter.getCount() ? mAdapter.getCount() - 1 : position;
        mCurrentItem = position;
        mUpdateFromPosition = true;
        invalidate();
    }

    public boolean isModeIsHorizontal() {
        return mModeIsHorizontal;
    }

    public void setModeIsHorizontal(boolean modeIsHorizontal) {
        mModeIsHorizontal = modeIsHorizontal;
    }

    public boolean isDrawOnOff() {
        return mDrawOnOff;
    }

    public void setDrawOnOff(boolean drawOnOff) {
        mDrawOnOff = drawOnOff;
    }

    public boolean isFixPoint() {
        return mFixPoint;
    }

    public void setFixPoint(boolean fixPoint) {
        mFixPoint = fixPoint;
    }

    public int getCurrentX() {
        return mCurrentX;
    }

    public int getCurrentY() {
        return mCurrentY;
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    protected void setCurrentItem(int currentItem) {
        if (mCurrentItem != currentItem && mListener != null) {
            mListener.onPositionSelected(currentItem);
        }
        mCurrentItem = currentItem;
    }
}
