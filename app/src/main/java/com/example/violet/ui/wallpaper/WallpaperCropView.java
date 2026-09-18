package com.example.violet.ui.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Interactive touch view for cropping and adjusting the Start screen wallpaper.
 * Features 1-finger panning, 2-finger pinch-to-zoom, bounds containment, and
 * dynamic aspect ratio switching based on whether parallax motion is enabled.
 */
public class WallpaperCropView extends View {

    private Bitmap sourceBitmap;
    private final Matrix imageMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    private final RectF cropRect = new RectF();
    private final RectF initialViewportRect = new RectF();
    private final RectF imageBounds = new RectF();

    private boolean parallaxEnabled = true;

    private float minScale = 1.0f;
    private float maxScale = 5.0f;
    private float currentScale = 1.0f;

    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    private ScaleGestureDetector scaleDetector;

    // Paints
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WallpaperCropView(Context context) {
        super(context);
        init(context);
    }

    public WallpaperCropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WallpaperCropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        maskPaint.setColor(0xA6000000); // 65% black dimming
        maskPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));

        guideLinePaint.setColor(0x80FFFFFF);
        guideLinePaint.setStyle(Paint.Style.STROKE);
        guideLinePaint.setStrokeWidth(dpToPx(1.0f));
        guideLinePaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(4)}, 0));

        guideTextPaint.setColor(0x99FFFFFF);
        guideTextPaint.setTextSize(dpToPx(11));
        guideTextPaint.setAntiAlias(true);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float targetScale = currentScale * factor;

                if (targetScale < minScale) {
                    factor = minScale / currentScale;
                    currentScale = minScale;
                } else if (targetScale > maxScale) {
                    factor = maxScale / currentScale;
                    currentScale = maxScale;
                } else {
                    currentScale = targetScale;
                }

                imageMatrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                clampImageToBounds();
                invalidate();
                return true;
            }
        });
    }

    public void setParallaxEnabled(boolean enabled) {
        this.parallaxEnabled = enabled;
        if (getWidth() > 0 && getHeight() > 0) {
            setupCropAndImage();
        }
    }

    public void setSourceBitmap(Bitmap bitmap) {
        this.sourceBitmap = bitmap;
        if (getWidth() > 0 && getHeight() > 0) {
            setupCropAndImage();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setupCropAndImage();
    }

    private void setupCropAndImage() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float heightFactor = parallaxEnabled ? 1.8f : 1.0f;
        float screenAspect = (float) dm.widthPixels / (float) (dm.heightPixels * heightFactor);

        float padding = dpToPx(24);
        float availableW = getWidth() - padding * 2;
        float availableH = getHeight() - padding * 2;

        float boxW = availableW;
        float boxH = boxW / screenAspect;

        if (boxH > availableH) {
            boxH = availableH;
            boxW = boxH * screenAspect;
        }

        float left = (getWidth() - boxW) / 2f;
        float top = (getHeight() - boxH) / 2f;
        cropRect.set(left, top, left + boxW, top + boxH);

        if (parallaxEnabled) {
            float initialH = boxH / 1.8f;
            initialViewportRect.set(left, top, left + boxW, top + initialH);
        } else {
            initialViewportRect.set(cropRect);
        }

        if (sourceBitmap != null && !sourceBitmap.isRecycled()) {
            imageMatrix.reset();

            float scaleX = cropRect.width() / sourceBitmap.getWidth();
            float scaleY = cropRect.height() / sourceBitmap.getHeight();
            minScale = Math.max(scaleX, scaleY);
            currentScale = minScale;

            imageMatrix.postScale(minScale, minScale);

            // Center image within crop frame initially
            float scaledW = sourceBitmap.getWidth() * minScale;
            float scaledH = sourceBitmap.getHeight() * minScale;
            float dx = cropRect.left + (cropRect.width() - scaledW) / 2f;
            float dy = cropRect.top + (cropRect.height() - scaledH) / 2f;

            imageMatrix.postTranslate(dx, dy);
            clampImageToBounds();
        }

        invalidate();
    }

    private void clampImageToBounds() {
        if (sourceBitmap == null || cropRect.isEmpty()) return;

        imageBounds.set(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        imageMatrix.mapRect(imageBounds);

        float dx = 0;
        float dy = 0;

        if (imageBounds.width() < cropRect.width()) {
            dx = cropRect.centerX() - imageBounds.centerX();
        } else {
            if (imageBounds.left > cropRect.left) {
                dx = cropRect.left - imageBounds.left;
            } else if (imageBounds.right < cropRect.right) {
                dx = cropRect.right - imageBounds.right;
            }
        }

        if (imageBounds.height() < cropRect.height()) {
            dy = cropRect.centerY() - imageBounds.centerY();
        } else {
            if (imageBounds.top > cropRect.top) {
                dy = cropRect.top - imageBounds.top;
            } else if (imageBounds.bottom < cropRect.bottom) {
                dy = cropRect.bottom - imageBounds.bottom;
            }
        }

        if (dx != 0 || dy != 0) {
            imageMatrix.postTranslate(dx, dy);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sourceBitmap == null) return false;

        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                activePointerId = event.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex != -1 && !scaleDetector.isInProgress()) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    imageMatrix.postTranslate(dx, dy);
                    clampImageToBounds();
                    invalidate();

                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newPointerIndex = (pointerIndex == 0) ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (sourceBitmap != null && !sourceBitmap.isRecycled()) {
            canvas.drawBitmap(sourceBitmap, imageMatrix, bitmapPaint);
        }

        // Dim outside crop box (top, bottom, left, right)
        canvas.drawRect(0, 0, getWidth(), cropRect.top, maskPaint);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), maskPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, maskPaint);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, maskPaint);

        // White border around crop area
        canvas.drawRect(cropRect, borderPaint);

        if (parallaxEnabled) {
            // Dashed horizontal guideline indicating initial Start viewport
            Path path = new Path();
            path.moveTo(initialViewportRect.left, initialViewportRect.bottom);
            path.lineTo(initialViewportRect.right, initialViewportRect.bottom);
            canvas.drawPath(path, guideLinePaint);

            canvas.drawText("Start screen top", cropRect.left + dpToPx(8), initialViewportRect.bottom - dpToPx(6), guideTextPaint);
            canvas.drawText("Parallax scroll region", cropRect.left + dpToPx(8), cropRect.bottom - dpToPx(8), guideTextPaint);
        }
    }

    @Nullable
    public Bitmap getCroppedBitmap() {
        if (sourceBitmap == null || sourceBitmap.isRecycled() || cropRect.isEmpty()) {
            return null;
        }

        if (!imageMatrix.invert(inverseMatrix)) {
            return null;
        }

        RectF srcRect = new RectF();
        inverseMatrix.mapRect(srcRect, cropRect);

        // Clamp to source bitmap bounds
        int left = Math.max(0, (int) srcRect.left);
        int top = Math.max(0, (int) srcRect.top);
        int right = Math.min(sourceBitmap.getWidth(), (int) srcRect.right);
        int bottom = Math.min(sourceBitmap.getHeight(), (int) srcRect.bottom);

        int width = right - left;
        int height = bottom - top;

        if (width <= 0 || height <= 0) {
            return null;
        }

        try {
            return Bitmap.createBitmap(sourceBitmap, left, top, width, height);
        } catch (Throwable t) {
            return null;
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
