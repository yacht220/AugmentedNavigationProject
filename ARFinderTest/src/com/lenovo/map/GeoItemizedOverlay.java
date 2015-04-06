package com.lenovo.map;

import android.graphics.drawable.Drawable;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class GeoItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private int id;
	private OverlayItem mOverlays = null;

	public GeoItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public void addOverlay(OverlayItem overlay) {
		this.mOverlays = overlay;
		populate();
	}

	protected OverlayItem createItem(int i) {
		return this.mOverlays;
	}

	@Override
	public int size() {
		return 1;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeoItemizedOverlay other = (GeoItemizedOverlay) obj;
		if (id != other.id)
			return false;
		return true;
	}
}