package my.example.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.view.View.OnTouchListener;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener,OnScaleGestureListener
								,OnTouchListener{
	/**
	 * 第一次加载图片到控件，则初始化
	 */
	private  boolean isOnce;
	/**
	 * 图片初始缩放倍数
	 */
	private float initScale;
	/**
	 * 允许图片放大的最大倍数
	 */
	private float maxScale;
	/**
	 * 用于对图片进行缩放平移的矩阵
	 */
	private Matrix scaleMatrix;
	/**
	 * 用于监听缩放手势
	 */
	private ScaleGestureDetector mScaleGestureDetector;
	/**
	 * 手指在屏幕上滑动距离大于该值时，则对图片进行移动
	 */
    private float slop;
    /**
     * 用于判断是移动，还是缩放
     */
    private int mode=0;
    /**
     * 手指按下时的横坐标
     */
    private float lastX;
    /**
     * 手指按下时的纵坐标
     */
    private float lastY;
    /**
     * 用于监听双击事件
     */
    private GestureDetector gestureDetector;
    /**
     * 用于判断当双击图片时，图片应该放大还是缩小
     */
    private boolean isIncrease=true;
    /**
     * 图片自动放大的过程中每次放大的倍数
     */
    private static final float UP_SCALE=1.5f;
    /**
     * 图片自动缩小的过程中每次缩小的倍数
     */
    private static final float DOWN_SCALE=0.7f;
	
	public ZoomImageView(Context context) {
		this(context,null);
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mScaleGestureDetector=new ScaleGestureDetector(context,this);
		setOnTouchListener(this);
		slop=ViewConfiguration.get(context).getScaledTouchSlop();
		gestureDetector=new GestureDetector(new GestureListner());
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}
	
	
	/**
	 * 当控件依附到Window时调用该方法
	 */
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}
	
	/**
	 * 当控件detachedWindow时调用该方法
	 */
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().dispatchOnGlobalLayout();
	}
	/**
	 * 当布局第一次发生变化时，初始图片的显示位置
	 */
	@Override
	public void onGlobalLayout() {
		if(!isOnce){
			int width=getWidth();
			int height=getHeight();
			Drawable d=getDrawable();
			int dw=d.getIntrinsicWidth();
			int dh=d.getIntrinsicHeight();
			float scale=1;
			if(dw > width && dh<height){
				scale=width*1.0f/dw;
			}else if(dh > height && dw<width){
				scale=height*1.0f/dh;
			}else{
				scale=Math.min(width*1.0f/dw, height*1.0f/dh);
			}
			initScale=scale;
			maxScale=initScale*4;
			scaleMatrix=new Matrix();
			scaleMatrix.postTranslate(width*1.0f/2-dw*1.0f/2, height*1.0f/2-dh*1.0f/2);
			scaleMatrix.postScale(initScale, initScale, width*1.0f/2, height*1.0f/2);			
			setImageMatrix(scaleMatrix);
			isOnce=true;
		}
	}
	
	/**
	 * 获取图片当前的缩放倍数
	 * @return
	 */
	private float getScale(){
		float[] scale =new float[9];
		scaleMatrix.getValues(scale);
		return scale[Matrix.MSCALE_X];
	}
	
	private RectF getMatrixRectF(){
		Matrix matrix=scaleMatrix;
		RectF rect=new RectF();
		Drawable d=getDrawable();
		rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		matrix.mapRect(rect);
		return rect;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		if(getDrawable()==null)
			return true;
		float scale=getScale();
		float scaleFactor=detector.getScaleFactor();
		if((scale < maxScale && scaleFactor>1.0f) || 
				(scale > initScale && scaleFactor<1.0f)){
			if(scale*scaleFactor > maxScale){
				scaleFactor=maxScale/scale;
			}else if(scale*scaleFactor < initScale){
				scaleFactor=initScale/scale;
			}
			scaleMatrix.postScale(scaleFactor, scaleFactor,detector.getFocusX(),detector.getFocusY());
			checkBorder();
			setImageMatrix(scaleMatrix);
		}
		return true;
	}

	private void checkBorder() {
		float moveX=0;
		float moveY=0;
		int width=getWidth();
		int height=getHeight();
		RectF rect=getMatrixRectF();
		if(rect.width()>=width){
			if(rect.left>0){
				moveX=-rect.left;
			}
			if(rect.right<width){
				moveX=width-rect.right;
			}
		}else{
			moveX=width/2f-rect.right+rect.width()/2f;
		}
		if(rect.height()>=height){
			if(rect.top>0){
				moveY=-rect.top;
			}
			if(rect.bottom<height){
				moveY=height-rect.bottom;
			}
		}else{
			moveY=height/2f-rect.bottom+rect.height()/2f;
		}
		scaleMatrix.postTranslate(moveX, moveY);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
			mScaleGestureDetector.onTouchEvent(event);
			gestureDetector.onTouchEvent(event);
			switch(event.getAction() & MotionEvent.ACTION_MASK){
				case MotionEvent.ACTION_DOWN:
					lastX=event.getX();					
					lastY=event.getY();
					mode=1;
					break;
				case MotionEvent.ACTION_MOVE:
					if(mode==1){
						float moveX=event.getX();
						float moveY=event.getY();
						float distanceX=moveX-lastX;
						float distanceY=moveY-lastY;
						float distance=(float) Math.sqrt(distanceX*distanceX+distanceY*distanceY);
						if(distance > slop){
							RectF rect=getMatrixRectF();
							if(rect.width()<getWidth()+1.0f){
								distanceX=0;
							}
							if(rect.height()<getHeight()+1.0f){
								distanceY=0;
							}
							scaleMatrix.postTranslate(distanceX, distanceY);
							checkBorderWhenTranslate();
							setImageMatrix(scaleMatrix);
						}
						lastX=moveX;
						lastY=moveY;
					}
					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					lastX=0;
					lastY=0;
					mode=2;
					break;
			}
		return true;
	}

	private void checkBorderWhenTranslate() {
		RectF rect=getMatrixRectF();
		int width=getWidth();
		int height=getHeight();
		float moveX=0;
		float moveY=0;
		if(rect.width()>=width){
			if(rect.left>0 ){
				moveX=-rect.left;
			}
			if(rect.right<width){
				moveX=width-rect.right;
			}
		}
		if(rect.height()>=height){
			if(rect.top>0){
				moveY=-rect.top;
			}
			if(rect.bottom<height){
				moveY=height-rect.bottom;
			}
		}
		scaleMatrix.postTranslate(moveX, moveY);		
	}

	public class GestureListner extends SimpleOnGestureListener{

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			float x=e.getX();
			float y=e.getY();
			
			postDelayed(new MyRunnable(x,y), 100);
			return true;
		}
		
	}
	
	public class MyRunnable implements Runnable{
		float centerX;
		float centerY; 
		public MyRunnable(float x,float y){
			this.centerX=x;
			this.centerY=y;
		}

		@Override
		public void run() {
			float Currentscale=getScale();
			if(Currentscale<maxScale && isIncrease){
				if(Currentscale*UP_SCALE>maxScale){					
					scaleMatrix.postScale(maxScale/Currentscale, maxScale/Currentscale, centerX, centerY);
					checkBorder();
					setImageMatrix(scaleMatrix);
					isIncrease=false;
				}else{
					scaleMatrix.postScale(UP_SCALE, UP_SCALE, centerX, centerY);
					checkBorder();
					setImageMatrix(scaleMatrix);
					postDelayed(this, 100);
				}
			}else if(Currentscale>initScale && !isIncrease){
				if(Currentscale*DOWN_SCALE<initScale){
					scaleMatrix.postScale(initScale/Currentscale, initScale/Currentscale, centerX, centerY);
					checkBorder();
					setImageMatrix(scaleMatrix);
					isIncrease=true;
				}else{
					scaleMatrix.postScale(DOWN_SCALE, DOWN_SCALE, centerX, centerY);
					checkBorder();
					setImageMatrix(scaleMatrix);
					postDelayed(this, 100);
				}
			}
		}
		
	}
	
}
