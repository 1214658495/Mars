package com.byd.vtdr2.widget;

//import com.byd.appstore.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import com.byd.vtdr2.R;

import skin.support.widget.SkinCompatBackgroundHelper;
import skin.support.widget.SkinCompatCompoundButtonHelper;
import skin.support.widget.SkinCompatTextHelper;

public class ThemeLightRadioButton extends LightRadioButton implements ITheme {
    private Theme mTheme;
    private Context myContext;
    private SkinCompatTextHelper mTextHelper;
    private SkinCompatCompoundButtonHelper mCompoundButtonHelper;
    private SkinCompatBackgroundHelper mBackgroundTintHelper;
    public ThemeLightRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTheme = new Theme();
        mTheme.set(context, attrs);
        ThemeManager.getInstance().add(this);
        myContext = context;
/*//        SkinCompatSupportable初始化
        mCompoundButtonHelper = new SkinCompatCompoundButtonHelper(this);
//        mCompoundButtonHelper.loadFromAttributes(attrs, R.attr.radioButtonStyle);
        mBackgroundTintHelper = new SkinCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, 0);
        mTextHelper = SkinCompatTextHelper.create(this);
        mTextHelper.loadFromAttributes(attrs, 0);*/
    }

    @Override
    public void setTheme(Theme theme) {
        mTheme.set(theme);
        onThemeChanged(ThemeManager.getInstance().getTheme());
    }

    @Override
    public void onThemeChanged(int theme) {
//	方法一，此法需要在xml文件中调用其属性	
//		int res = mTheme.get(theme);
//		if(res > 0) {	
////			setLight(getResources().getColor(res+4), 15);
//			setLight(getResources().getColor(R.color.lightone), 25);
//		
//			ColorStateList colorStateList = getResources().getColorStateList(res);
//			setTextColor(colorStateList);
//		}

//方法二
        ColorStateList colorStateList;
        switch (theme) {
            case Theme.NORMAL:
                setLight(getResources().getColor(R.color.lightone), 30);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_steco);
                break;
            case Theme.SPORT:
                setLight(getResources().getColor(R.color.sport_color), 12);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_stsport);
                break;
            case Theme.HAD_NORMAL:
                setLight(getResources().getColor(R.color.lightone), 30);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_hadeco);
                break;
            case Theme.HAD_SPORT:
                setLight(getResources().getColor(R.color.hadsport_color), 8);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_hadsport);
                break;
//            case Theme.STAR_NORMAL:
//                setLight(getResources().getColor(R.color.starnormal_color), 8);
//                break;
//            case Theme.STAR_SPORT:
//                setLight(getResources().getColor(R.color.starsport_color), 8);
//                break;
            case Theme.BLACKGOLD_NORMAL:
                setLight(getResources().getColor(R.color.blackgoldnormal_color), 1);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_blackgoldeco);
                break;
            case Theme.BLACKGOLD_SPORT:
                setLight(getResources().getColor(R.color.blackgoldsport_color), 1);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_blackgoldsport);
                break;
            case Theme.EYESHOT_NORMAL:
                setLight(getResources().getColor(R.color.eyeshotnormal_color), 1);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_eyeshoteco);
                break;
            case Theme.EYESHOT_SPORT:
                setLight(getResources().getColor(R.color.eyeshotsport_color), 1);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_eyeshotsport);
                break;
//            case Theme.BUSSINESS_NORMAL:
//                setLight(getResources().getColor(R.color.bussinessnormal_color), 8);
//                break;
//            case Theme.BUSSINESS_SPORT:
//                setLight(getResources().getColor(R.color.bussinesssport_color), 8);
//                break;
            default:
                setLight(getResources().getColor(R.color.lightone), 30);
                colorStateList = getResources().getColorStateList(R.color.check_selector_buttoncolor_steco);
                break;
        }
        setTextColor(colorStateList);
    }

  /*  @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setTextAppearance(int resId) {
        setTextAppearance(getContext(), resId);
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }
//    使用第三方库实现换肤
    @Override
    public void applySkin() {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySkin();
        }
        if (mTextHelper != null) {
            mTextHelper.applySkin();
        }
    }*/
}
