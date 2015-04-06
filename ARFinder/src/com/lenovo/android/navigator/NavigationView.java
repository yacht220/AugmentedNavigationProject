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

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;



import com.lenovo.minimap.dto.CoordDeflect;
import com.lenovo.minimap.search.RouteSearch;
import com.lenovo.minimap.search.RouteSearch.Route.Navigation;

/*
 * 实景导航主界面
 */
public class NavigationView extends View {
	private static final int RADIUS = 50;
	private static final int OFFSET_GROUND = 60;
	
	private static final String step_prefix_prompt = " 距离下个目标点 ";
	private static final String finish_prefix_prompt = " 距离终点 ";
	private static final String suffix_prompt = " 米 ";
	private static final String finish_prompt = "你已进入终点区域，请注意寻找目标";
	
	public int navIndex = 0;
	
	private static final boolean isDebug = true;
	private static final String LOG_CAT = "NavigationView";

	private Drawable stepAnchor;
	private Drawable finishAnchor;

	private ArrayList<NameAndDistance> navigationList;
	private ArrayList<String> directions;
	private boolean passed[];
	
	private boolean isNeedToVibrate = true;
	
	public ArrayList<NameAndDistance> getNavigationList() {
		return navigationList;
	}
	
	private ServiceProxy service;

	public NavigationView(Context context, AttributeSet attrs) {
		super(context, attrs);

		stepAnchor = context.getResources().getDrawable(R.drawable.step);
		finishAnchor = context.getResources().getDrawable(R.drawable.finish_point);	
	}

	public static class NameAndDistance {
		public String name;
		public double distance;

		public NameAndDistance(String name, double distance) {
			this.name = name;
			this.distance = distance;
		}
	}

	/*
	 * 查询导航路径
	 */
	private class SearchNavigationTask extends
			AsyncTask<Integer, Void, List<Navigation>> {

		private WeakReference<ServiceProxy> service;
		private WeakReference<NavigationView> nv;

		SearchNavigationTask(ServiceProxy service, NavigationView nv) {
			this.service = new WeakReference<ServiceProxy>(service);
			this.nv = new WeakReference<NavigationView>(nv);
		}

		@Override
		protected void onPreExecute() {
			navIndex = 0;
			isNeedToVibrate = true;
			((CameraActivity) (nv.get().getContext()))
					.showDialog(CameraActivity.DIALOG_WAITING);
		}

		@Override
		protected List<Navigation> doInBackground(Integer... params) {
			int endX = params[0];
			int endY = params[1];
			List<RouteSearch.Route> routes = service.get().searchWalkRoute(
					endX, endY);
			if (routes == null) {
				return null;
			}
			RouteSearch.Route route = routes.get(0);
			List<Navigation> navigations = route.getNavigations();
			return navigations;
		}

		@Override
		protected void onPostExecute(List<Navigation> navigations) {
			if (navigations == null) {
				((CameraActivity) (nv.get().getContext()))
						.showDialog(CameraActivity.DIALOG_SEARCH_NAVIGATION_FAILED);
				((CameraActivity) nv.get().getContext())
						.removeDialog(CameraActivity.DIALOG_WAITING);
				((CameraActivity) nv.get().getContext()).exitNavigation();
				return;
			}
			navIndex = 0;
			navigationList = new ArrayList<NameAndDistance>();
			directions = new ArrayList<String>();
			for (int i = 0; i < navigations.size(); i++) {
				String name = (navigations.get(i).getRouteName().length() == 0 ? ""
						: "沿" + navigations.get(i).getRouteName())
						+ "向前"
						+ navigations.get(i).getNavigationLength()
						+ "米 --- "
						+ navigations.get(i).getNavigationActionText();
				int distance = navigations.get(i).getNavigationLength();
				navigationList.add(new NameAndDistance(name, distance));
				directions.add(navigations.get(i).getNavigationActionText());
			}
			passed = new boolean[navigations.size()];

			((CameraActivity) (nv.get().getContext()))
					.removeDialog(CameraActivity.DIALOG_WAITING);
			((CameraActivity) (nv.get().getContext())).showNavigationDlg(
					navigationList, navIndex);
		}
	}

	private SearchNavigationTask task;
	public int endX;//重置导航时使用
	public int endY;

	public ArrayList<NameAndDistance> beginNavigate(ServiceProxy service,
			int endX, int endY) {
		this.endX = endX;
		this.endY = endY;
		
		task = new SearchNavigationTask(service, this);
		task.execute(endX, endY);
		task = null;
		return navigationList;
	}

	/*
	 * 距离目标点的距离
	 */
    private int getTargetDistance() {
		int end = service.getCurrentNavigation().getCoordDeflects().size() - 1;
		CoordDeflect endPoint = service.getCurrentNavigation().getCoordDeflects().get(end);
		return (int) service.getDistance(endPoint);
    }

	private boolean isInRange() {
		return getTargetDistance() <= RADIUS;
	}
	
	/*
	 * 出了目标范围重新设置振动
	 */
	private void setNeedToVibarate() {
		if (getTargetDistance() > RADIUS) {
			isNeedToVibrate = true;
		}
	}
	
	public void setServiceProxy(ServiceProxy service) {
		this.service = service;
	}

	/*
	 * 计算Drawable d 的绘制区域
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

	private void draw(Canvas canvas, Drawable d, Rect r) {
		d.setBounds(r);
		d.draw(canvas);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		CameraActivity activity = (CameraActivity) this.getContext();

		if (activity.getMode() == CameraActivity.MODE_NAVIGATION
				&& navigationList != null && navigationList.size() > 0) {

			CoordDeflect endPoint = service.getCurrentNavigation().getCoordDeflects().get(service.getCurrentNavigation().getCoordDeflectSize() - 1);
			float point[] = service.getScreenXY(endPoint.getX(), endPoint.getY());
			
			Rect r;//
			// 标记点, 终点
			if (0 <= point[0] && point[0] < service.getScreenWidth()) {
				if (isInRange() && !passed[navIndex] && navIndex == navigationList.size() - 1) { // 导航即将结束
					passed[navIndex] = true;
				    r = calculateRegion(finishAnchor, point[0], point[1] + OFFSET_GROUND);
					draw(canvas, finishAnchor, r);
				} else if (isInRange() && !passed[navIndex]) {
					passed[navIndex] = true;
					navIndex++; // 下个导航端
					service.setCurrentNavigation(navIndex);
				    r = calculateRegion(stepAnchor, point[0], point[1] + OFFSET_GROUND);
					draw(canvas, stepAnchor, r);
				} else if (navIndex == navigationList.size() - 1) {					
					r = calculateRegion(finishAnchor, point[0], point[1] + OFFSET_GROUND);
					draw(canvas, finishAnchor, r);
				} else {
					r = calculateRegion(stepAnchor, point[0], point[1] + OFFSET_GROUND);
					draw(canvas, stepAnchor, r);
				}
			} else if (point[0] < 0) {
				if (navIndex == navigationList.size() - 1) {		//终点在显示区域左边		
					r = calculateRegion(Const.finishLeft, Const.finishLeft.getIntrinsicWidth() / 2 + 2, point[1] + OFFSET_GROUND);
					draw(canvas, Const.finishLeft, r);
				} else {											//阶段标记点在显示区域左边
					r = calculateRegion(Const.stepLeft, Const.stepLeft.getIntrinsicWidth() / 2 + 2, point[1] + OFFSET_GROUND);
					draw(canvas, Const.stepLeft, r);
				}
			} else if (point[0] > service.getScreenWidth()) {
				if (navIndex == navigationList.size() - 1) {		//终点在显示区域右边		
					r = calculateRegion(Const.finishRight, service.getScreenWidth() - Const.finishRight.getIntrinsicWidth() / 2 - 2, point[1] + OFFSET_GROUND);
					draw(canvas, Const.finishRight, r);
				} else {											//阶段标记点在显示区域右边
					r = calculateRegion(Const.stepRight, service.getScreenWidth() - Const.stepRight.getIntrinsicWidth() / 2 - 2, point[1] + OFFSET_GROUND);
					draw(canvas, Const.stepRight, r);
				}
			}
			
			//进入区域， 振动提示
			if (isNeedToVibrate && isInRange() && navIndex == navigationList.size() - 1) {
				Util.vibrate(getContext());
				Util.vibrate(getContext());
				isNeedToVibrate = false;
			} else if (isNeedToVibrate && isInRange()) {
				Util.vibrate(getContext());
				isNeedToVibrate = false;
			}
			setNeedToVibarate();  //如果走出范围, 设置能否振动
			
			//根据不同的情况显示不同的提示信息
			if (navIndex == navigationList.size() - 1) {
				if (isInRange())
					showNotifyImage(R.drawable.finish_flag, finish_prompt);
				else {
					int id = getNotifyImageId(point[0]);
					showNotifyImage(id, finish_prefix_prompt + getTargetDistance() + suffix_prompt);
				}
			} else {
				int id = getNotifyImageId(point[0]);
				showNotifyImage(id, step_prefix_prompt + getTargetDistance() + suffix_prompt);				
			}
		}
	}
	
	public boolean isNavigationFinished() {
		return (navIndex == navigationList.size() - 1 && isInRange());
	}

	private void showNotifyImage(int id, String text) {
		final TextView tv = ((CameraActivity) getContext()).navigationPromptText;	
		tv.setText(text);

		final ImageView iv = (ImageView) ((CameraActivity) getContext()).navigationPromptImage;
		iv.setImageResource(id);	
	}
	/*
	 * 根据标记点或终点的x值， 显示的提示图片
	 */
	private int getNotifyImageId(float x) {
		if (x <= 0)
			return R.drawable.turn_left;
		else if (x > service.getScreenWidth())
			return R.drawable.turn_right;
		else 
			return R.drawable.forward;
	}
}
