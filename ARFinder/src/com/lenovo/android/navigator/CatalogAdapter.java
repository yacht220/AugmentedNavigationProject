package com.lenovo.android.navigator;

import android.content.Context;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/*
 * 类别菜单
 */
public class CatalogAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    
    private final ArrayList<ListItem> mItems = new ArrayList<ListItem>();
    
	public static final int FULL       = 1;
	public static final int DINNING    = 2;
	public static final int SHOP       = 3;
	public static final int BUS        = 4;
	public static final int GAS        = 5;

    public static class ListItem {
        public final CharSequence text;
        public final Drawable image;
        public final int actionTag;
        
        public ListItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
            text = res.getString(textResourceId);
            if (imageResourceId != -1) {
                image = res.getDrawable(imageResourceId);
            } else {
                image = null;
            }
            this.actionTag = actionTag;
        }
    }
    
    public CatalogAdapter(Context context) {
        super();

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        // Create default actions
        Resources res = context.getResources();
        
        mItems.add(new ListItem(res, R.string.full,
                R.drawable.item_full, FULL));
        
        mItems.add(new ListItem(res, R.string.dinning,
                R.drawable.item_dinning, DINNING));

        mItems.add(new ListItem(res, R.string.shop,
                R.drawable.item_shop, SHOP));
        
        mItems.add(new ListItem(res, R.string.bus,
                R.drawable.item_bus, BUS));
        
        mItems.add(new ListItem(res, R.string.gas,
                R.drawable.item_gas, GAS));
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item = (ListItem) getItem(position);
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.catalog_list_item, parent, false);
        }
        
        TextView textView = (TextView) convertView;
        textView.setTag(item);
        textView.setText(item.text);
        textView.setCompoundDrawablesWithIntrinsicBounds(item.image, null, null, null);
        
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
