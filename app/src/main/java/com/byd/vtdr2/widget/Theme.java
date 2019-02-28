package com.byd.vtdr2.widget;

//import com.byd.appstore.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.byd.vtdr2.R;

public class Theme {
	/** 整形常量采用此种做法，可以随时添加，增强代码可维护性*/
	public static final int NORMAL = 0;
	public static final int SPORT = NORMAL + 1;
	public static final int THUMB_NORMAL = SPORT + 1;
	public static final int THUMB_SPORT = THUMB_NORMAL + 1;
	public static final int TEXT_COLOR_BLUE = THUMB_SPORT+1;
	public static final int TEXT_COLOR_BROWN = TEXT_COLOR_BLUE+1;
	public static final int BACKGROUND = TEXT_COLOR_BROWN+1;
	public static final int SRC = BACKGROUND + 1;

	public static final int HAD_NORMAL = SRC + 1;
	public static final int HAD_SPORT = HAD_NORMAL + 1;

	public static final int STAR_NORMAL = HAD_SPORT + 1;
	public static final int STAR_SPORT = STAR_NORMAL + 1;

	public static final int BLACKGOLD_NORMAL = STAR_SPORT + 1;
	public static final int BLACKGOLD_SPORT = BLACKGOLD_NORMAL + 1;

	public static final int EYESHOT_NORMAL = BLACKGOLD_SPORT + 1;
	public static final int EYESHOT_SPORT = EYESHOT_NORMAL + 1;

	public static final int BUSSINESS_NORMAL = EYESHOT_SPORT + 1;
	public static final int BUSSINESS_SPORT = BUSSINESS_NORMAL + 1;

	public static final int ELEGANT_NORMAL = BUSSINESS_SPORT + 1;
	public static final int ELEGANT_SPORT = ELEGANT_NORMAL + 1;

	private static final int COUNT = ELEGANT_SPORT + 1;


	private int[] mThemes;

	public Theme() {
		mThemes = new int[COUNT];
	}
	/** #获得我们自定义的自定义属性*/
	public void set(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.theme);//int n = a.getIndexCount();
		mThemes[NORMAL] = a.getResourceId(R.styleable.theme_normal, 0);
		mThemes[SPORT] = a.getResourceId(R.styleable.theme_sport, 0);
		mThemes[THUMB_NORMAL] = a.getResourceId(R.styleable.theme_thumb_normal, 0);
		mThemes[THUMB_SPORT] = a.getResourceId(R.styleable.theme_thumb_sport, 0);
		mThemes[TEXT_COLOR_BLUE] = a.getResourceId(R.styleable.theme_text_color_normal, 0);
		mThemes[TEXT_COLOR_BROWN] = a.getResourceId(R.styleable.theme_text_color_sport, 0);
		mThemes[BACKGROUND] = a.getResourceId(R.styleable.theme_background, 0);
		mThemes[SRC] = a.getResourceId(R.styleable.theme_src, 0);

		mThemes[HAD_NORMAL] = a.getResourceId(R.styleable.theme_hadnormal, 0);
		mThemes[HAD_SPORT] = a.getResourceId(R.styleable.theme_hadsport, 0);
		mThemes[STAR_NORMAL] = a.getResourceId(R.styleable.theme_starnormal, 0);
		mThemes[STAR_SPORT] = a.getResourceId(R.styleable.theme_starsport, 0);
		mThemes[BLACKGOLD_NORMAL] = a.getResourceId(R.styleable.theme_blackgoldnormal, 0);
		mThemes[BLACKGOLD_SPORT] = a.getResourceId(R.styleable.theme_blackgoldsport, 0);
		mThemes[EYESHOT_NORMAL] = a.getResourceId(R.styleable.theme_eyeshotnormal, 0);
		mThemes[EYESHOT_SPORT] = a.getResourceId(R.styleable.theme_eyeshotsport, 0);

		mThemes[BUSSINESS_NORMAL] = a.getResourceId(R.styleable.theme_bussinessnormal, 0);
		mThemes[BUSSINESS_SPORT] = a.getResourceId(R.styleable.theme_bussinesssport, 0);
		mThemes[ELEGANT_NORMAL] = a.getResourceId(R.styleable.theme_elegantnormal, 0);
		mThemes[ELEGANT_SPORT] = a.getResourceId(R.styleable.theme_elegantsport, 0);
        a.recycle();
	}
	/** 方法名一样，参数不同，重载*/
	public void set(Theme theme) {
		mThemes = theme.mThemes.clone();
	}

	public Theme add(int theme, int resId) {
		mThemes[theme] = resId;
		return this;
	}

	public int get(int theme) {
		return mThemes[theme];
	}
}


/*	
 //此处可以用switch()语句实现
public void set(Context context, AttributeSet attrs){
	TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.theme);
	int n = array.getIndexCount();
	for(int i=0;i<n;i++){
		int attr = array.getIndex(i);
		switch (attr) {
		case R.styleable.theme_normal:
			mThemes[NORMAL]=array.getResourceId(attr, 0);				
			break;
		case R.styleable.theme_sport:
			mThemes[SPORT]=array.getResourceId(attr, 0);				
			break;
		case R.styleable.theme_thumb_normal:
			mThemes[THUMB_NORMAL]=array.getResourceId(attr, 0);				
			break;
		case R.styleable.theme_thumb_sport:
			mThemes[THUMB_SPORT]=array.getResourceId(attr, 0);				
			break;
		case R.styleable.theme_text_color_normal:
			mThemes[TEXT_COLOR_BLUE]=array.getResourceId(attr, 0);				
			break;
		case R.styleable.theme_text_color_sport:
			mThemes[TEXT_COLOR_BROWN]=array.getResourceId(attr, 0);				
			break;
		case R.styleable.theme_background:
			mThemes[BACKGROUND] =array.getResourceId(attr, 0);	
		case R.styleable.theme_src:	
			mThemes[SRC] = array.getResourceId(attr, 0);	
		default:
			break;
		}
	}
	array.recycle();
}*/
