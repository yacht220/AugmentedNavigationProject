package com.lenovo.android.navigator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;

import android.view.MotionEvent;

import android.widget.ImageView;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.search.AroundSearch;

public class AnchorView extends ImageView 
	implements CameraActivity.PositionListener {
	
	public static final int DISPLAY_MAX = MinimapService.SEARCH_SIZE; // 需要显示的POI数量
	private static final int AUTO_SELECTION_TIMEOUT = 200;
    private static final int offset = 81; //和信息卡的高度相等
	
	private List<AnchorInfo> anchorInfos = new ArrayList<AnchorInfo>();
	
    public  List<AroundSearch.Around> arounds; // 所有搜索到的POI
    private List<AroundSearch.Around> tempArounds; // doInBackground()中的临时兴趣点list
    private boolean isSearchAroundEnabled = true; // 激活周边搜索flag
    private int condition;
    private int max;
    private AroundSearch.Around displayBuffer[]; // 需要显示的POI
        
    private int touchX;
    private int touchY;
    private boolean isTouch = false;
     
	private boolean hasManualSelection = false;//记录有手动选择的标志
	private boolean hasAutoSelection = false;
    private Runnable autoSelect;    
    
	public boolean lock = false;
	
	
	
    public AnchorView(Context context, AttributeSet attrs) {
        super(context,attrs);
        
        /*
         * 自动选择
         */
        autoSelect = new Runnable() {
        	public void run() {
        		AnchorView.this.removeCallbacks(this);
        		AnchorView.this.postDelayed(this, AUTO_SELECTION_TIMEOUT); 
        		Rect r = new Rect();
        		//判断手动选择的点是否还在屏幕中，如果不在则退选
        		if (hasManualSelection) {
        			AnchorView.this.getHitRect(r);
        			for (int i = 0; i < anchorInfos.size(); i++) {
        				AnchorInfo info = anchorInfos.get(i);
        				//手动选择的点不在屏幕显示区域内， 退选
            			if (info.getSelected() && !r.contains(screenX(info.rect), screenY(info.rect))) {
            				info.setSelected(false);
            				hasManualSelection = false;
            				((CameraActivity) getContext()).showInfoCard(false);//信息卡随之消失
            			} else if (info.getSelected()) {//手动选择的还在屏幕里
            				return;
            			}
        			}
        		}
        		final ImageView focusView = (ImageView) ((CameraActivity) getContext()).focusView;        		
        		focusView.getHitRect(r);
    			for (int i = 0; i < anchorInfos.size(); i++) {
    				anchorInfos.get(i).setSelected(false);
    			}
        			
        		//检查选择框中有目标
        		int selectedIndex = -1;
        		float distance = Float.MAX_VALUE;
        		Rect absoluteRect = new Rect();	
        		
        		for (int i = 0; i < anchorInfos.size(); i++) {        			
        			
        			absoluteRect.set(anchorInfos.get(i).rect);
        			absoluteRect.offset(0, offset);
        			if (r.intersect(absoluteRect)) { //在选择区域, 计算距离中心的距离	
        				
        				float x = r.centerX() - anchorInfos.get(i).data.getScreenX();
        				float y = 135 - anchorInfos.get(i).data.getScreenY();//174-81+(83/2)=135， 选择区的中心在AnchorView中的位置
        				float d = x * x + y * y;
        				if (d < distance) {
        					distance = d;
        					selectedIndex = i;
        				}
        			}
        		}
        		if (selectedIndex >= 0) {		//有目标被选中, 下标从零开始
        			updateSelection(selectedIndex);
        			hasAutoSelection = true;
        		} else {						//没有目标被选中
        			hasAutoSelection = false;        			
        		}
        		if (!hasSelection()) {//自动，手动都没有，信息卡显示取消
        			((CameraActivity) getContext()).showInfoCard(false);
        		}
        	}
        };   
        postDelayed(autoSelect, AUTO_SELECTION_TIMEOUT);
    }
    
    /*
     * 更新信息卡内容，并显示
     */
    public void updateSelection(int index) {
    	// TODO 更新信息卡内容，并显示
    	anchorInfos.get(index).setSelected(true);
		updateInfoCard(anchorInfos.get(index).getData());
		((CameraActivity) getContext()).showInfoCard(true);
    }
	
    /*
     * 当前有选择的气泡
     */
	public boolean hasSelection() {
		return hasManualSelection || hasAutoSelection;
	}
	
	public List<AnchorInfo> getAnchorInfos() {
		return anchorInfos;
	}
	
	public List<AroundSearch.Around> getAllInfos() {
		return arounds;
	}
        
    public void setCondition(int condition) {
    	this.condition = condition;
    }
    
    /*
     * 激活周边搜索
     */
    public void enableSearchAround(){
    	this.isSearchAroundEnabled = true;
    }
 
    /*
     * displayBuffer 里的内容是最终要显示的气泡
     */
	private void sortByDistance(ServiceProxy service) {
		List<AroundSearch.Around> list = arounds;
		if (list == null || list.size() <= 0) {
			return;
		}
		int length = list.size();
		
		max = (length > DISPLAY_MAX ? DISPLAY_MAX : length);
		displayBuffer = new AroundSearch.Around[max];
		
		for (int i = 0; i < max; i++) {
			displayBuffer[i] = list.get(i);
		}
	}
	      
	/*
	 * 信息点对应的数据结构
	 */
    public static class AnchorInfo {
    	public Rect rect;
    	public int x;
    	public int y;
    	public boolean isSelected;
    	AroundSearch.Around data;

    	public AnchorInfo(Rect r, int x, int y, AroundSearch.Around d, boolean isSelected) {
    		this.rect = r;
    		this.x = x;
    		this.y = y;
    		this.data = d;
    		this.isSelected = isSelected;
    	}
    	
    	public AnchorInfo(AnchorInfo anchor) {
    		this.rect = new Rect(anchor.rect);
    		this.x = anchor.x;
    		this.y = anchor.y;
    		this.data = anchor.data;
    		this.isSelected = anchor.isSelected;
    	}
    	
    	public AnchorInfo clone() {
    		return new AnchorInfo(this);
    	}
    	
    	public boolean inInRegion(int px, int py) {
    		return rect.contains(px, py);
    	}
    	
    	public boolean equals(AnchorInfo a) {
    		return ((a.x == x) && (a.y == y));
    	}
    	
    	public boolean equals(AroundSearch.Around p) {
    		return ((p.getCoord20X() == x) && (p.getCoord20Y() == y));
    	}
    	
    	public boolean getSelected() {
    		return isSelected;
    	}
    	
    	public void setSelected(boolean b) {
    		this.isSelected = b;
    	}

    	public AroundSearch.Around getData() {
    		return this.data;
    	}
    }
    
    private static int screenX(Rect r) {
        return r.left + r.width() / 2;
    }

    private static int screenY(Rect r) {
        return r.bottom;
    }

    /*
     * 计算显示区域
     */
    private Rect calculateRegion(Drawable d, float windowX, float windowY) {
    	int xx = (int) windowX;
    	int yy = (int) windowY;
    	int w = d.getIntrinsicWidth();
    	int h = d.getIntrinsicHeight();
        
        int top = yy - h;
        int bottom = yy;

        if (top < 0) {
            top = 0;
            bottom = h;
        }
        if (bottom > getHeight()) {
            top = getHeight() - h;
            bottom = getHeight();
        }
    	return new Rect(xx - w / 2, top, xx + w / 2, bottom);
    }
    
    private SearchAroundTask searchAroundTask = null;
    
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		CameraActivity activity = (CameraActivity) this.getContext();
		ServiceProxy service = activity.mBoundService;
		
		if(activity.getMode() == CameraActivity.MODE_CAMERA && activity.isGPSAvailable) {		
			if (!lock) {
				if (searchAroundTask == null || searchAroundTask.getStatus() != AsyncTask.Status.RUNNING) {
			        if (service != null) {
						searchAroundTask = new SearchAroundTask(service, this);
						searchAroundTask.execute(condition);					
						sortByDistance(service);
			        }
				}
				int i = max - 1;
				List<AnchorInfo> infos = new ArrayList<AnchorInfo>();
	    		while (i >= 0) {
	    			boolean isSelected = selected(displayBuffer[i]);
	    			Drawable drawable = Const.getDrawableByDistance(displayBuffer[i].getRealTimeDistance(), isSelected);
	    			Rect r = calculateRegion(drawable, displayBuffer[i].getScreenX(), 
	    					displayBuffer[i].getScreenY() - offset);//绘制的矩形区域
	    			int x = displayBuffer[i].getCoord20X();
			    	int y = displayBuffer[i].getCoord20Y();

			    	AroundSearch.Around d = displayBuffer[i];
			    	infos.add(new AnchorInfo(r, x, y, d, isSelected));
	    			i--;
				}
	    		anchorInfos = infos;
			}
			else {
				if (searchAroundTask != null) {
					searchAroundTask.cancel(true); //长按，要锁定屏幕, 停止数据更新
					searchAroundTask = null;
				}
			}

			/*
			 * 重叠数的显示
			 */
			int i = max - 1;
    		int catalog[][] = getOverlapBubbleInfo();
    		for (i = 0; i < catalog[1].length; i++) {
                if (catalog[1][i] > 0) {
	    			Rect r = calculateRegion(Const.bgNumber, 
    						anchorInfos.get(i).rect.right + 4,
    						anchorInfos.get(i).rect.top + 10);
		    		Const.draw(canvas, Const.bgNumber, r);
    				canvas.drawText(catalog[1][i]+"", 
    						anchorInfos.get(i).rect.right,
    						anchorInfos.get(i).rect.top + 4, Const.getPaint());
    			}
    		}
			
    		if (isTouch) {
    			isTouch = false;
    			touchSelect(catalog[0]);
    		}
    		
    		i = max - 1;
    		while (i >= 0) {    			
    			Drawable drawable = Const.getDrawableByDistance(displayBuffer[i].getRealTimeDistance(), anchorInfos.get(i).getSelected());
		    	Const.draw(canvas, drawable, anchorInfos.get(i).rect);
    			i--;
			}
		}
	}
	
	/*
	 * 查找重叠的算法
	 */
	public int[][] getOverlapBubbleInfo() {
		int size = anchorInfos.size();
		int catalog[][] = new int[2][size];
		int[][] result = new int[size][size];
		
		for (int i = size - 1; i >= 0; i--) {	//计算互为重叠的气泡， 标记为1
			for (int j = 0; j < i; j++) {
				if (Const.isOverlap(anchorInfos.get(i).rect, anchorInfos.get(j).rect)) {
					result[i][j] = 1;
				}
			}
            result[i][i] = 1;
		}
		for (int i = 0; i < size; i++) {			//按列扫描result
			for (int j = i + 1 ; j < size; j++) {   //从当前行的下个开始计算 j = i+1
				if (result[j][i] > 0) {				//表示找到一个重叠的气泡
					for (int k = 0; k < size; k++) {
						result[j][k] = 0;				//清除该行， 因为这个气泡被认为和当前气泡重叠
					}
					result[i][i] += 1;					//重叠的气泡数目，
					catalog[0][j] = i + 1;				//记录那些气泡和当前气泡重叠， 并付给只有重叠的气泡才相等的值； 不同的重叠群这个值是不一样的
					catalog[0][i] = i + 1;				//别忘了自己
				}
			}
		}
		for (int i = 0; i < size; i++) {
			if (result[i][i] > 1) {
				catalog[1][i] = result[i][i];		//重叠的数目
			}
		}
		return catalog;
	}

	/*
	 * 判断是否为选中
	 */
	private boolean selected(AroundSearch.Around newData) {
		for (int i = 0; i < anchorInfos.size(); i++) {
			AnchorInfo tmp = anchorInfos.get(i);
			if (tmp.getSelected() && tmp.equals(newData))
				return true;
		}
		return false;
	}
	
	/*
	 * 	在自动被选取的信息点被选中时,如果单击无信息
	 *	点实景区域,取消该点的选取,并且3秒内丌再进行点的自动选取
	 */	
	private void threeSecondUnselect() {
		removeCallbacks(autoSelect);
		postDelayed(autoSelect, 3000);
	}
	
	/*
	 * 信息点的退选
	 */
	public void unSelectAll() {
		hasAutoSelection = false;
		hasManualSelection = false;
		((CameraActivity) getContext()).showInfoCard(false);//信息卡消失
		for (int i = 0; i < anchorInfos.size(); i++) {	
			anchorInfos.get(i).setSelected(false);
		}
	}
	
	/*
	 * 信息点的点击处理
	 */
	private void touchSelect(int catalog[]) {	
		// TODO 信息点的点击处理
		hasAutoSelection = false;
		threeSecondUnselect();
		
		hasManualSelection = false;
		((CameraActivity) getContext()).showInfoCard(false);//信息卡消失
		for (int i = 0; i < anchorInfos.size(); i++) {		
			if (anchorInfos.get(i).isSelected) {
				anchorInfos.get(i).setSelected(false);
			}
		}
		
		for (int i = 0; i < anchorInfos.size(); i++) {
			if (anchorInfos.get(i).inInRegion(touchX, touchY)) {				
				int value = catalog[i];
				if (value == 0) { 		// 0 means this bubble is single one
					updateSelection(i);
				} else {				// show multi-selection view
					performMultiSelection(value, catalog, anchorInfos);
				}
				hasManualSelection = true;
				return;
			}
		}
		if (!hasManualSelection) {	//没有选择气泡
            postDelayed(checkLongClickTimer, 2000);
        }
	}
	
	private void performMultiSelection(int value, int catalog[], List<AnchorInfo> anchorInfos) {
		((CameraActivity)getContext()).performMultiSelection(value, catalog, anchorInfos);
	}
	
	public void updateInfoCard(AroundSearch.Around info) {
		List<AroundSearch.Around> l = new ArrayList<AroundSearch.Around>();
		l.add(info);
		((CameraActivity)getContext()).updateInfoCard(l, info);
	}
	
	/*
	 * 手势进入锁定场景
	 */
	private Runnable checkLongClickTimer = new Runnable() {
    	public void run() {
    		onLongTouchDown();
    	}
    };
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	// TODO onTouchEvent
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	touchX = x;
            	touchY = y;
            	isTouch = true;
            	return true;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
	            onLongTouchUp();
                break;
        }
        return false;
    }
	
	// 将anchorInfo中的数据传给SnapShot能够处理的格式
	public String[] AnchorInfo2StringArray() {
		if (anchorInfos == null || anchorInfos.size() <= 0) {
			return null;
		}
		int length = anchorInfos.size();
		String result[] = new String[length * 6];
		for (int i = 0; i < length; i++) {
			int j = i * 6;
			AroundSearch.Around info = anchorInfos.get(i).getData();
			result[j] = info.getRealTimeDistance() + "";
			result[j + 1] = screenX(anchorInfos.get(i).rect) + "";
			result[j + 2] = screenY(anchorInfos.get(i).rect) + "";
			result[j + 3] = info.getName();
			result[j + 4] = info.getAddress();
			result[j + 5] = info.getPhoneNumber();
		}
		return result;
	}

	/*
	 * 查找信息点的任务
	 */
    private class SearchAroundTask extends AsyncTask<Integer, Void, List<AroundSearch.Around> > {
        private WeakReference<AnchorView> av;
        private WeakReference<ServiceProxy> service;

        SearchAroundTask(ServiceProxy service, AnchorView av) {
            this.av = new WeakReference<AnchorView>(av);
        	this.service = new WeakReference<ServiceProxy>(service);
        }

        @Override
		protected void onPreExecute() {
		}

        @Override
        protected List<AroundSearch.Around> doInBackground(Integer... params) {        	
        	// 只搜索一次
        	if(isSearchAroundEnabled == true){
        		tempArounds = service.get().searchAround(params[0]);
        		while(tempArounds == null){
        			tempArounds = service.get().searchAround(params[0]);
        		}
        		isSearchAroundEnabled = false;
        	}        	
        	return tempArounds;
        }
        
        @Override
        protected void onPostExecute(List<AroundSearch.Around> result) {
        	av.get().arounds = result;
        }
	}
    
    /*
     * 用手势进入锁定界面
     */
	private void onLongTouchDown() {		
		CameraActivity activity = (CameraActivity) getContext();
		activity.lockBtn.setPressed(true);
		if (!lock)
			((CameraActivity) getContext()).takePicture(); //产生锁定背景
		lock = true;
	}
	
	private void onLongTouchUp() {
		removeCallbacks(checkLongClickTimer);
		lock = false;
		
		CameraActivity activity = (CameraActivity) getContext();
		activity.lockBtn.setPressed(false);
	}
	
    public void onDirectionChanged(float value) {    	
    }
    
    public boolean isRunOneTime = false;
    public void onGestureChanged(float value) {
    	if (Math.abs(value) < 10 && lock) {
    		removeCallbacks(checkLongClickTimer);
    		if (!isRunOneTime) {
    			((CameraActivity) getContext()).cameraView.stopCameraPreview();    		
    			((CameraActivity) getContext()).startSnapShotActivity();
    			isRunOneTime = true;
    		}
    	}
    }
}

