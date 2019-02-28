package com.byd.vtdr2.widget;

//import com.byd.appstore.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import com.byd.vtdr2.R;

import skin.support.widget.SkinCompatBackgroundHelper;
import skin.support.widget.SkinCompatTextHelper;

/**#
 * 该主题运用在可点击的字眼，点击时候具有发光的效果
 *
 */
public class ThemeLightButton extends LightButton implements ITheme {
	private final static  String TAG="ThemeLightButton";
	private Theme mTheme;
	private Context myContext;
	private SkinCompatTextHelper mTextHelper;
	private SkinCompatBackgroundHelper mBackgroundTintHelper;

	public ThemeLightButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		myContext = context;
		mTheme = new Theme();
		mTheme.set(context, attrs);
		ThemeManager.getInstance().add(this);
//		//        SkinCompatSupportable初始化
//		mBackgroundTintHelper = new SkinCompatBackgroundHelper(this);
//		mBackgroundTintHelper.loadFromAttributes(attrs, 0);
//		mTextHelper = SkinCompatTextHelper.create(this);
//		mTextHelper.loadFromAttributes(attrs, 0);
	}

	@Override
	public void setTheme(Theme theme) {
		mTheme.set(theme);
		onThemeChanged(ThemeManager.getInstance().getTheme());
	}
	@Override
	public void onThemeChanged(int theme) {
//		int res = mTheme.get(theme);
		//if(res > 0) {
			//setLight(getResources().getColor(res+4), 35);
			

			ColorStateList colorStateList;
			if(!isEnabled()){
				colorStateList = getResources().getColorStateList(R.color.service_item_confirm_color);
			}else{
				switch (theme) {
					case Theme.NORMAL:
						setLight(getResources().getColor(R.color.normal_color), 10);//设置发光的颜色以及半径
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_steco);
						break;
					case Theme.SPORT:
						setLight(getResources().getColor(R.color.sport_color), 10);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_stsport);
						break;
					case Theme.HAD_NORMAL:
						setLight(getResources().getColor(R.color.lightone), 10);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_hadeco);
						break;
					case Theme.HAD_SPORT:
						setLight(getResources().getColor(R.color.hadsport_color), 8);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_hadsport);
						break;
//					case Theme.STAR_NORMAL:
//						setLight(getResources().getColor(R.color.starnormal_color), 10);
//						break;
//					case Theme.STAR_SPORT:
//						setLight(getResources().getColor(R.color.starsport_color), 8);
//						break;
					case Theme.BLACKGOLD_NORMAL:
						setLight(getResources().getColor(R.color.blackgoldnormal_color), 10);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_blackgoldeco);
						break;
					case Theme.BLACKGOLD_SPORT:
						setLight(getResources().getColor(R.color.blackgoldsport_color), 8);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_blackgoldsport);
						break;
					case Theme.EYESHOT_NORMAL:
						setLight(getResources().getColor(R.color.eyeshotnormal_color), 10);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_eyeshoteco);
						break;
					case Theme.EYESHOT_SPORT:
						setLight(getResources().getColor(R.color.eyeshotsport_color), 8);
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_eyeshotsport);
						break;
//					case Theme.BUSSINESS_NORMAL:
//						setLight(getResources().getColor(R.color.bussinessnormal_color), 8);
//						break;
//					case Theme.BUSSINESS_SPORT:
//						setLight(getResources().getColor(R.color.bussinesssport_color), 8);
//						break;
					default:
						setLight(getResources().getColor(R.color.normal_color), 10);//设置发光的颜色以及半径
						colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_steco);

						break;
				}
			}
			setTextColor(colorStateList);//设置文本的颜色
	}

//	@Override
//	public void applySkin() {
//		if (mBackgroundTintHelper != null) {
//			mBackgroundTintHelper.applySkin();
//		}
//		if (mTextHelper != null) {
//			mTextHelper.applySkin();
//		}
//	}
}
