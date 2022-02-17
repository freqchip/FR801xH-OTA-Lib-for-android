package com.example.bluetooth.le.activity;

import com.example.bluetooth.le.R;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class ScanDevice {
	private ImageView searchiv = null;
	private ImageView myiv = null;
	private ViewGroup line2 = null;
	private int width,heigh;
	private float density;
	private int screenwidth,screenheigh;
	private Context context;
	public ScanDevice(Context context) {
		this.context = context;
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		width = dm.widthPixels; 
		heigh = dm.heightPixels;
		density = dm.density;
		screenwidth = (int) (width/ density);
		screenheigh = (int) (heigh / density);
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.activity_main,null);
		searchiv = (ImageView)view.findViewById(R.id.searchiv);
		myiv = (ImageView)view.findViewById(R.id.myiv);
		line2 = (ViewGroup)view.findViewById(R.id.line2);
		RotateAnimation roanimation = new RotateAnimation(0f, 359f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
		LinearInterpolator lir = new LinearInterpolator();
		roanimation.setInterpolator(lir);
		roanimation.setDuration(1000);
		roanimation.setRepeatCount(-1);
		myiv.startAnimation(roanimation);
		RotateAnimation roanimationsearch = new RotateAnimation(0f, 359f,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
		roanimationsearch.setInterpolator( new LinearInterpolator());
		roanimationsearch.setDuration(2000);
		roanimationsearch.setRepeatCount(-1);
		searchiv.startAnimation(roanimationsearch);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Message message = new Message();
				myhandler.sendEmptyMessage(1);
				
			}
		}).start();
	}
	
	   private int dp2px(int dpValue){
	        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpValue,
	        		context.getResources().getDisplayMetrics());
	    }
	   private int sp2px(int spValue){
	        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,spValue,
	        		context.getResources().getDisplayMetrics());
	    }
		Handler myhandler = new Handler(){
			public void handleMessage(android.os.Message msg) {
				switch(msg.what){
				case 1:
					int linwidth = line2.getWidth();
					int linheigth = line2.getHeight();
					line2.setVisibility(View.VISIBLE);
					System.out.println("heigh　"+ screenheigh + " "+(screenheigh + linheigth) + " " + screenwidth);
					RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) line2.getLayoutParams();
					//lp.setMargins(0, screenheigh,screenwidth, screenheigh + linheigth);
					lp.setMargins(0, -1,0, -linheigth);
					line2.requestLayout();
					TranslateAnimation trananumation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,0f,Animation.RELATIVE_TO_SELF,-1f);
				    trananumation.setDuration(1000);
				    trananumation.setFillAfter(true);
				    line2.startAnimation(trananumation);
				       
				}
				
			};
			
		};
}
