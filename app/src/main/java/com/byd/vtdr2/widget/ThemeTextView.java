package com.byd.vtdr2.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.AttributeSet;

import com.byd.vtdr2.R;

import skin.support.widget.SkinCompatTextView;

//此处未继承LightTextView
public class ThemeTextView extends SkinCompatTextView implements ITheme {
	private Theme mTheme;
	
	private int color;
	private float radius;

	private Rect bounds;
	private Context myContext;

	
	public ThemeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		mTheme = new Theme();
		mTheme.set(context, attrs);
		ThemeManager.getInstance().add(this);
		myContext = context;
	}

	@Override
	protected void onDraw(Canvas canvas) {
//		if(isPressed()){
			getPaint().setShadowLayer(radius * 0.2f, 0, 0, color);
			super.onDraw(canvas);

			getPaint().setShadowLayer(radius * 0.4f, 0, 0, color);
			super.onDraw(canvas);

			getPaint().setShadowLayer(radius * 0.6f, 0, 0, color);
			super.onDraw(canvas);

			getPaint().setShadowLayer(radius * 0.8f, 0, 0, color);
			super.onDraw(canvas);

			bounds.left = getCompoundPaddingLeft();
			bounds.right = getWidth() - getCompoundPaddingRight();
			bounds.top = getCompoundPaddingTop();
			bounds.bottom = getHeight() - getCompoundPaddingBottom();

			canvas.save();
			/** Op.DIFFERENCE:A-B/INTERSECT:A和B交集/REVERSE_DIFFERENCE:B-A/XOR:异或/UNION并集*/
			canvas.clipRect(bounds, Op.DIFFERENCE);
			canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
			canvas.restore();
			
			getPaint().setShadowLayer(radius, 0, 0, color);
			super.onDraw(canvas);
//			
//			getPaint().setMaskFilter(new BlurMaskFilter(5, Blur.SOLID));
//			super.onDraw(canvas);
			

//		}
		
		getPaint().setShadowLayer(radius, 0, 0, color);
		super.onDraw(canvas);
	}

	private void init() {
		bounds = new Rect();
		setLayerType(LAYER_TYPE_SOFTWARE, null);
		setLight(Constants.COLOR_DEFAULT_BLUE, Constants.DEFAULT_RADIUS);
	}

//	@Override
	public void setLight(int color, float radius) {
		this.color = color;
		this.radius = radius;
		invalidate();
	}

	@Override
	public void setTheme(Theme theme) {
		mTheme.set(theme);
		onThemeChanged(ThemeManager.getInstance().getTheme());
	}

//	@SuppressLint("NewApi")
	@Override
	public void onThemeChanged(int theme) {
		ColorStateList colorStateList;
		switch (theme) {
			case Theme.NORMAL:
				setLight(getResources().getColor(R.color.lightone), 20);
//                colorStateList = getResources().getColorStateList(R.color.check_selector_eco);
				break;
			case Theme.SPORT:
				setLight(getResources().getColor(R.color.sport_color), 3);
//                colorStateList = getResources().getColorStateList(R.color.check_selector_sport);
				break;
			case Theme.HAD_NORMAL:
				setLight(getResources().getColor(R.color.lightone), 30);
				break;
			case Theme.HAD_SPORT:
				setLight(getResources().getColor(R.color.hadsport_color), 5);
				break;
//			case Theme.STAR_NORMAL:
//				setLight(getResources().getColor(R.color.starnormal_color), 8);
//				break;
//			case Theme.STAR_SPORT:
//				setLight(getResources().getColor(R.color.starsport_color), 8);
//				break;
			case Theme.BLACKGOLD_NORMAL:
				setLight(getResources().getColor(R.color.blackgoldnormal_color), 8);
				break;
			case Theme.BLACKGOLD_SPORT:
				setLight(getResources().getColor(R.color.blackgoldsport_color), 8);
				break;
			case Theme.EYESHOT_NORMAL:
				setLight(getResources().getColor(R.color.eyeshotnormal_color), 8);
				break;
			case Theme.EYESHOT_SPORT:
				setLight(getResources().getColor(R.color.eyeshotsport_color), 8);
				break;
			/*case Theme.BUSSINESS_NORMAL:
				setLight(getResources().getColor(R.color.bussinessnormal_color), 8);
				break;
			case Theme.BUSSINESS_SPORT:
				setLight(getResources().getColor(R.color.bussinesssport_color), 8);
				break;*/
			default:
				setLight(getResources().getColor(R.color.lightone), 20);
				colorStateList = getResources().getColorStateList(R.color.check_selector_eco);
				break;
		}
//		setTextColor(colorStateList);
	}
}

