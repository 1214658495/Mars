package com.byd.vtdr2.widget;

//import com.byd.appstore.R;

import android.content.Context;
import android.util.AttributeSet;

import com.byd.vtdr2.R;

import skin.support.widget.SkinCompatCheckBox;

//此处未继承LightCheckbox
public class ThemeCheckBox extends SkinCompatCheckBox implements ITheme {
    private Theme mTheme;

    public ThemeCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTheme = new Theme();
        mTheme.set(context, attrs);
        ThemeManager.getInstance().add(this);
    }

    @Override
    public void setTheme(Theme theme) {
        // TODO Auto-generated method stub
        mTheme.set(theme);
        onThemeChanged(ThemeManager.getInstance().getTheme());
    }

    @Override
    public void onThemeChanged(int theme) {
        // TODO Auto-generated method stub
	/*	int res = mTheme.get(theme);
		Drawable selector = getResources().getDrawable(R.drawable.selector_file_sort);
		Drawable drawable = DrawableCompat.wrap(selector);
		if(res > 0) {
			setTextColor(getResources().getColor(res));
			DrawableCompat.setTint(drawable, getResources().getColor(res));
			setButtonDrawable(drawable);
		}*/
        switch (theme) {
            case Theme.SPORT:
//			setButtonDrawable(getResources().getDrawable(R.drawable.selector_file_sort));
                setTextColor(getResources().getColor(R.color.sport_color));
                break;
            case Theme.HAD_NORMAL:
//			setButtonDrawable(getResources().getDrawable(R.drawable.selector_file_sort));
                setTextColor(getResources().getColor(R.color.grey));
                break;
            case Theme.HAD_SPORT:
//			setButtonDrawable(getResources().getDrawable(R.drawable.selector_file_sort));
//                setTextColor(getResources().getColor(R.color.hadsport_color));
                setTextColor(getResources().getColor(R.color.grey));
                break;
//            case Theme.STAR_NORMAL:
////			setButtonDrawable(getResources().getDrawable(R.drawable.selector_file_sort));
//                setTextColor(getResources().getColor(R.color.grey));
//                break;
//            case Theme.STAR_SPORT:
////			setButtonDrawable(getResources().getDrawable(R.drawable.selector_file_sort));
////                setTextColor(getResources().getColor(R.color.hadsport_color));
//                setTextColor(getResources().getColor(R.color.grey));
//                break;
            case Theme.BLACKGOLD_NORMAL:
            case Theme.BLACKGOLD_SPORT:
            case Theme.EYESHOT_NORMAL:
            case Theme.EYESHOT_SPORT:
                setTextColor(getResources().getColor(R.color.white));
                break;
//            case Theme.BUSSINESS_NORMAL:
//
//                break;
//            case Theme.BUSSINESS_SPORT:
//
//                break;
            default:
//			setButtonDrawable(getResources().getDrawable(R.drawable.pic_check_box_up));
                setTextColor(getResources().getColor(R.color.light));
                break;
        }
    }

}
