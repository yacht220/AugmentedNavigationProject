package com.lenovo.android.navigator;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.dto.CoordDeflect;
import com.lenovo.minimap.search.AroundSearch;
import com.lenovo.minimap.search.RouteSearch;

/*
 * Mini service 的封装类
 */
public class ServiceProxy extends Service {
	private static final int UPDATE_INTERVAL = 500;
	
    private MinimapService service;
    private final IBinder mBinder = new ServiceBinder();
    
    public class ServiceBinder extends Binder {
        ServiceProxy getService() {
            return ServiceProxy.this;
        }
    }
    
    @Override
    public void onCreate() {
    	service = new MinimapService(this, UPDATE_INTERVAL);
    }

    @Override
    public void onDestroy() {
    	service.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public boolean isGPSDataAvailable() {
    	return service.isGPSDataAvailable();
    }
    
    public List<AroundSearch.Around> searchAround(int condition) {
    	return service.searchAround(condition);    	
    }

    public List<AroundSearch.Around> getDisplayAroundList() {
    	return service.getDisplayAroundList();
    }
    
    public List<RouteSearch.Route> searchWalkRoute(int endX, int endY) {
    	return service.searchWalkRoute(endX, endY);
    }
    
    public boolean setCurrentRoute(int index) {
    	return service.setCurrentRoute(index);
    }
    
    public RouteSearch.Route.Navigation getCurrentNavigation() {
    	return service.getCurrentNavigation();
    }
    
    public double getDistance(CoordDeflect endCoordDeflect) {
    	return service.getDistance(endCoordDeflect);
    }
    
    public boolean setCurrentNavigation(int index) {
    	return service.setCurrentNavigation(index);
    }
    
    public float[] getScreenXY(double x, double y) {
    	return service.getScreenXY(x, y);
    }
    
    public float getScreenWidth() {
    	return service.getScreenWidth();
    }
    
    public float getScreenHeight() {
    	return service.getScreenHeight();
    }
    
    public float getDirection(){
    	return service.getDirection();
    }
    
    public float getInclination(){
    	return service.getInclination();
    }
    
    public CoordDeflect getMyLocationDeflect() {
    	return service.getMyLocationDeflect();
    }
	
    /**
	 * 返回传感器原始的方向数值
	 * @return 返回传感器原始的方向数值
	 */		
	public float getSensorDirection() {
		return service.getSensorDirection();			
	}
}

