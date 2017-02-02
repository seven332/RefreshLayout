/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.refreshlayout;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;


/**
 * Custom progress bar that shows a cycle of colors as widening circles that
 * overdraw each other. When finished, the bar is cleared from the inside out as
 * the main cycle continues. Before running, this can also indicate how close
 * the user is to triggering something (e.g. how far they need to pull down to
 * trigger a refresh).
 */
final class SwipeProgressBar {

    private static final boolean SUPPORT_CLIP_RECT_DIFFERENCE =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;

    // Default progress animation colors are grays.
    private static final int COLOR1 = 0xB3000000;
    private static final int COLOR2 = 0x80000000;
    private static final int COLOR3 = 0x4d000000;
    private static final int COLOR4 = 0x1a000000;

    // The duration per color of the animation cycle.
    private static final int ANIMATION_DURATION_MS_PER_COLOR = 500;

    // The duration of the animation to clear the bar.
    private static final int FINISH_ANIMATION_DURATION_MS = 1000;

    // Interpolator for varying the speed of the animation.
    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();

    private final Paint mPaint = new Paint();
    private final RectF mClipRect = new RectF();
    private float mTriggerPercentage;
    private long mStartTime;
    private long mFinishTime;
    private boolean mRunning;

    // Colors used when rendering the animation,
    private int[] mColors;
    private int mAnimationDuration;
    private View mParent;

    private Rect mBounds = new Rect();

    SwipeProgressBar(View parent) {
        mParent = parent;
        setColorScheme(COLOR1, COLOR2, COLOR3, COLOR4);
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     *
     * @param colors the colors for scheme
     */
    void setColorScheme(int... colors) {
        if (colors == null || colors.length <= 0) {
            throw new IllegalStateException("colors == null || colors.length <= 0");
        }
        mColors = colors;
        mAnimationDuration = colors.length * ANIMATION_DURATION_MS_PER_COLOR;
    }

    /**
     * Update the progress the user has made toward triggering the swipe
     * gesture. and use this value to update the percentage of the trigger that
     * is shown.
     */
    void setTriggerPercentage(float triggerPercentage) {
        mTriggerPercentage = triggerPercentage;
        mStartTime = 0;
        ViewCompat.postInvalidateOnAnimation(
                mParent, mBounds.left, mBounds.top, mBounds.right, mBounds.bottom);
    }

    /**
     * Start showing the progress animation.
     */
    void start() {
        if (!mRunning) {
            mTriggerPercentage = 0;
            mStartTime = AnimationUtils.currentAnimationTimeMillis();
            mRunning = true;
            mParent.postInvalidate();
        }
    }

    /**
     * Stop showing the progress animation.
     */
    void stop() {
        if (mRunning) {
            mTriggerPercentage = 0;
            mFinishTime = AnimationUtils.currentAnimationTimeMillis();
            mRunning = false;
            mParent.postInvalidate();
        }
    }

    /**
     * @return Return whether the progress animation is currently running.
     */
    boolean isRunning() {
        return mRunning || mFinishTime > 0;
    }

    void draw(Canvas canvas) {
        // API < 18 do not support clipRect(Region.Op.DIFFERENCE).
        // So draw twice for finish animation
        if (draw(canvas, true)) {
            draw(canvas, false);
        }
    }

    private boolean draw(Canvas canvas, boolean first) {
        Rect bounds = mBounds;
        final int width = bounds.width();
        final int cx = bounds.centerX();
        final int cy = bounds.centerY();
        final int colors = mColors.length;
        boolean drawTriggerWhileFinishing = false;
        boolean drawAgain = false;
        int restoreCount = canvas.save();
        canvas.clipRect(bounds);

        if (mRunning || (mFinishTime > 0)) {
            long now = AnimationUtils.currentAnimationTimeMillis();
            long elapsed = (now - mStartTime) % mAnimationDuration;
            long iterations = (now - mStartTime) / ANIMATION_DURATION_MS_PER_COLOR;
            float rawProgress = (elapsed / (mAnimationDuration / (float) colors));

            // If we're not running anymore, that means we're running through
            // the finish animation.
            if (!mRunning) {
                // If the finish animation is done, don't draw anything, and
                // don't repost.
                if ((now - mFinishTime) >= FINISH_ANIMATION_DURATION_MS) {
                    mFinishTime = 0;
                    return false;
                }

                // Otherwise, use a 0 opacity alpha layer to clear the animation
                // from the inside out. This layer will prevent the circles from
                // drawing within its bounds.
                long finishElapsed = (now - mFinishTime) % FINISH_ANIMATION_DURATION_MS;
                float finishProgress = (finishElapsed / (FINISH_ANIMATION_DURATION_MS / 100f));
                float pct = (finishProgress / 100f);
                // Radius of the circle is half of the screen.
                float clearRadius = width / 2 * INTERPOLATOR.getInterpolation(pct);
                if (SUPPORT_CLIP_RECT_DIFFERENCE) {
                    mClipRect.set(cx - clearRadius, bounds.top, cx + clearRadius, bounds.bottom);
                    canvas.clipRect(mClipRect, Region.Op.DIFFERENCE);
                } else {
                    if (first) {
                        // First time left
                        drawAgain = true;
                        mClipRect.set(bounds.left, bounds.top, cx - clearRadius, bounds.bottom);
                    } else {
                        // Second time right
                        mClipRect.set(cx + clearRadius, bounds.top, bounds.right, bounds.bottom);
                    }
                    canvas.clipRect(mClipRect);
                }
                // Only draw the trigger if there is a space in the center of
                // this refreshing view that needs to be filled in by the
                // trigger. If the progress view is just still animating, let it
                // continue animating.
                drawTriggerWhileFinishing = true;
            }

            // First fill in with the last color that would have finished drawing.
            if (iterations == 0) {
                canvas.drawColor(mColors[0]);
            } else {
                int index = colors - 1;
                float left = 0.0f;
                float right = 1.0f;
                for (int i = 0; i < colors; ++i) {
                    if ((rawProgress >= left && rawProgress < right) || i == colors - 1) {
                        canvas.drawColor(mColors[index]);
                        break;
                    }
                    index = (index + 1) % colors;
                    left += 1.0f;
                    right += 1.0f;
                }
            }

            // Then draw up to 4 overlapping concentric circles of varying radii, based on how far
            // along we are in the cycle.
            // progress 0-50 draw mColor2
            // progress 25-75 draw mColor3
            // progress 50-100 draw mColor4
            // progress 75 (wrap to 25) draw mColor1
            if (colors > 1) {
                if ((rawProgress >= 0.0f && rawProgress <= 1.0f)) {
                    float pct = (rawProgress + 1.0f) / 2;
                    drawCircle(canvas, cx, cy, mColors[0], pct);
                }
                float left = 0.0f;
                float right = 2.0f;
                for (int i = 1; i < colors; ++i) {
                    if (rawProgress >= left && rawProgress <= right) {
                        float pct = (rawProgress - i + 1.0f) / 2;
                        drawCircle(canvas, cx, cy, mColors[i], pct);
                    }
                    left += 1.0f;
                    right += 1.0f;
                }
                if ((rawProgress >= colors - 1.0f && rawProgress <= colors)) {
                    float pct = (rawProgress - colors + 1.0f) / 2;
                    drawCircle(canvas, cx, cy, mColors[0], pct);
                }
            }
            if (mTriggerPercentage > 0 && drawTriggerWhileFinishing) {
                // There is some portion of trigger to draw. Restore the canvas,
                // then draw the trigger. Otherwise, the trigger does not appear
                // until after the bar has finished animating and appears to
                // just jump in at a larger width than expected.
                canvas.restoreToCount(restoreCount);
                restoreCount = canvas.save();
                canvas.clipRect(bounds);
                drawTrigger(canvas, cx, cy);
            }
            // Keep running until we finish out the last cycle.
            ViewCompat.postInvalidateOnAnimation(
                    mParent, bounds.left, bounds.top, bounds.right, bounds.bottom);
        } else {
            // Otherwise if we're in the middle of a trigger, draw that.
            if (mTriggerPercentage > 0 && mTriggerPercentage <= 1.0) {
                drawTrigger(canvas, cx, cy);
            }
        }
        canvas.restoreToCount(restoreCount);
        return drawAgain;
    }

    private void drawTrigger(Canvas canvas, int cx, int cy) {
        mPaint.setColor(mColors[0]);
        canvas.drawCircle(cx, cy, cx * mTriggerPercentage, mPaint);
    }

    /**
     * Draws a circle centered in the view.
     *
     * @param canvas the canvas to draw on
     * @param cx the center x coordinate
     * @param cy the center y coordinate
     * @param color the color to draw
     * @param pct the percentage of the view that the circle should cover
     */
    private void drawCircle(Canvas canvas, float cx, float cy, int color, float pct) {
        mPaint.setColor(color);
        canvas.save();
        canvas.translate(cx, cy);
        float radiusScale = INTERPOLATOR.getInterpolation(pct);
        canvas.scale(radiusScale, radiusScale);
        canvas.drawCircle(0, 0, cx, mPaint);
        canvas.restore();
    }

    /**
     * Set the drawing bounds of this SwipeProgressBar.
     */
    void setBounds(int left, int top, int right, int bottom) {
        mBounds.left = left;
        mBounds.top = top;
        mBounds.right = right;
        mBounds.bottom = bottom;
    }
}
