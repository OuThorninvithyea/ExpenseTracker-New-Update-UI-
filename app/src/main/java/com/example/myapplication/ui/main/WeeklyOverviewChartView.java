package com.example.myapplication.ui.main;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.myapplication.utils.CurrencyHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeeklyOverviewChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<String> labels = new ArrayList<>();
    private List<Double> values = new ArrayList<>();

    public WeeklyOverviewChartView(Context context) {
        super(context);
        init();
    }

    public WeeklyOverviewChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeeklyOverviewChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        
        barPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setAlpha(60); 

        labelPaint.setTextSize(sp(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setTextSize(sp(10));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setAlpha(180); 

        applyThemeColors();
    }

    private void applyThemeColors() {
        
        int primary = getMaterialColor("colorPrimary");
        int onSurfaceVariant = getMaterialColor("colorOnSurfaceVariant");
        int outline = getMaterialColor("colorOutlineVariant");

        barPaint.setColor(primary); 
        labelPaint.setColor(onSurfaceVariant); 
        valuePaint.setColor(onSurfaceVariant); 
        gridPaint.setColor(outline != 0 ? outline : onSurfaceVariant); 
    }

    public void setData(List<String> labels, List<Double> values) {
        
        this.labels = labels != null ? labels : new ArrayList<>();
        this.values = values != null ? values : new ArrayList<>();
        
        applyThemeColors();
        
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (labels == null || values == null || labels.isEmpty() || values.isEmpty()) {
            
            return;
        }

        int count = Math.min(labels.size(), values.size());
        if (count == 0) return;

        float w = getWidth();
        float h = getHeight();

        float paddingLeft = dp(8);
        float paddingRight = dp(8);
        float paddingTop = dp(10);
        float paddingBottom = dp(26); 

        float chartLeft = paddingLeft;
        float chartRight = Math.max(chartLeft + 1, w - paddingRight);
        float chartTop = paddingTop;
        float chartBottom = Math.max(chartTop + 1, h - paddingBottom);

        float max = 0;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, values.get(i) != null ? values.get(i).floatValue() : 0f);
        }
        if (max <= 0) max = 1f; 

        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, gridPaint);

        float availableWidth = chartRight - chartLeft;
        if (availableWidth <= 0) return;
        
        float slot = availableWidth / count; 
        float barWidth = slot * 0.55f; 
        float radius = dp(8); 

        for (int i = 0; i < count; i++) {
            double v = values.get(i) != null ? values.get(i) : 0;
            
            float ratio = (float) (v / max);
            float barHeight = (chartBottom - chartTop) * ratio;

            float cx = chartLeft + slot * i + slot / 2f;
            float left = cx - barWidth / 2f;
            float right = cx + barWidth / 2f;
            float top = chartBottom - barHeight;

            RectF rect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(rect, radius, radius, barPaint);

            String valueText = v >= 1 
                ? CurrencyHelper.formatCurrency(getContext(), v).replace(".00", "") 
                : (v > 0 ? CurrencyHelper.formatCurrency(getContext(), v) : "");
            if (!valueText.isEmpty()) {
                canvas.drawText(valueText, cx, top - dp(4), valuePaint);
            }

            canvas.drawText(labels.get(i), cx, h - dp(8), labelPaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }

    private int getMaterialColor(String attrName) {
        int attrId = getContext().getResources().getIdentifier(attrName, "attr", getContext().getPackageName());
        if (attrId == 0) {
            attrId = getContext().getResources().getIdentifier(attrName, "attr", "com.google.android.material");
        }
        if (attrId == 0) {
            
            attrId = getContext().getResources().getIdentifier(attrName, "attr", "android");
        }
        if (attrId == 0) return 0;

        TypedValue typedValue = new TypedValue();
        try {
            if (getContext().getTheme().resolveAttribute(attrId, typedValue, true)) {
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return typedValue.data;
                }
                if (typedValue.resourceId != 0) {
                     return ContextCompat.getColor(getContext(), typedValue.resourceId);
                }
            }
        } catch (Exception e) {
            
        }
        return 0;
    }
}

