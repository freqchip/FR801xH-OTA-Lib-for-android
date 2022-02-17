package com.example.bluetooth.le;

import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

public class OpAdapte extends PagerAdapter {
	List<View> views;
	
	public OpAdapte(List<View> view) {
		super();
		views = view;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return views.size();
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		// TODO Auto-generated method stub
		return arg0 == arg1;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		((ViewPager)container).removeView(views.get(position));  
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		 ((ViewPager)container).addView(views.get(position));
		return views.get(position);
	}

}
