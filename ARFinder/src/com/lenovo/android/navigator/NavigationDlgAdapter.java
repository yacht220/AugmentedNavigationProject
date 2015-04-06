package com.lenovo.android.navigator;

import java.util.ArrayList;

import android.content.Context;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import android.widget.TextView;

import com.lenovo.android.navigator.NavigationView.NameAndDistance;

/*
 * 导航确认对话框的Adapter
 */
public class NavigationDlgAdapter extends BaseAdapter {
    private LayoutInflater mInflater;    
    private ArrayList<NameAndDistance> mItems;
    private Drawable stepMark;
    private Drawable stepMarkSelected;
    private Drawable finishMark;

    public NavigationDlgAdapter(Context context, ArrayList<NameAndDistance> items) {
        super();
        mItems = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        stepMarkSelected = context.getResources().getDrawable(R.drawable.nav_point_selected);
        stepMark = context.getResources().getDrawable(R.drawable.nav_point);
        finishMark = context.getResources().getDrawable(R.drawable.nav_finish);        
    }

    public View getView(int position, View convertView, ViewGroup parent) {
    	int currentNav = (Integer) parent.getTag();
    	
        NameAndDistance item = (NameAndDistance) getItem(position);
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.navigation_item, parent, false);
        }
        TextView textView = (TextView) convertView;
        if (currentNav == position) {
    		textView.setTextColor(Color.RED);
    		textView.setCompoundDrawablesWithIntrinsicBounds(stepMarkSelected, null, null, null);
    	} else {
    		textView.setTextColor(Color.BLACK);
    		textView.setCompoundDrawablesWithIntrinsicBounds(stepMark, null, null, null);
    	}
        textView.setText(item.name + Util.formatDistance(item.distance));
        if (position == getCount() - 1)
        	textView.setCompoundDrawablesWithIntrinsicBounds(finishMark, null, null, null); 
        else 
        	textView.setCompoundDrawablesWithIntrinsicBounds(stepMark, null, null, null);        

        return convertView;
    }
    
    public int getCount() {
        return mItems.size();
    }

    public Object getItem(int position) {
        return mItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }
}
