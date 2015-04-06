package com.lenovo.android.navigator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery.LayoutParams;

import com.lenovo.android.navigator.AnchorViewClone.Data;
import com.lenovo.android.navigator.AnchorViewClone.UriTimestamp;

public class SnapShot extends Activity implements
        AdapterView.OnItemSelectedListener, ViewSwitcher.ViewFactory,
        OnItemClickListener {
	
	public static final int SAVE_LIMIT = 20;

	private boolean isSaved = false;
	private ImageAdapter adpater;
	
	private ListView infoCardView;
	private LayoutInflater inflater;
	private AnchorViewClone anchorView;
	private Bitmap bitmap;
	
	private Button closeBtn;
	private Button saveBtn;
	
    private ImageSwitcher switcher;
    private Gallery gallery;
    private View.OnTouchListener touchListener;
    
    private int currentClicked = 0;
    private int currentRemoved = 0;
    
    private ArrayList<Drawable> thumbIds = new ArrayList<Drawable>();
    
    private List<Data> anchorInfos = new ArrayList<Data>();//存储当前的生成的快照
    
    public ImageRecord imageRecord = new ImageRecord(this);
    
    private List<List<Data> > imageInfos = new ArrayList<List<Data> >();
    
    private List<UriTimestamp> uriTimestamps = new ArrayList<UriTimestamp>();//存储所有的保存的图像的uri
    
	private ImageView saveView;
	private boolean isSaving;
	
	private boolean isRemoving = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	System.gc();
    	
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.snap_shot);
        
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
		infoCardView = (ListView) findViewById(R.id.info_card);
		infoCardView.setAdapter(null);		
        
		gallery = (Gallery) findViewById(R.id.gallery);
		gallery.setOnItemSelectedListener(this);
		loadImagesAndInfos();
		
        Bundle bundle = getIntent().getBundleExtra(CameraActivity.KEY_PHOTO_INFO);            
        byte[] data = bundle.getByteArray(CameraActivity.KEY_PHOTO_DATA);
        int w = bundle.getInt(CameraActivity.KEY_PHOTO_WIDTH);
        int h = bundle.getInt(CameraActivity.KEY_PHOTO_HEIGHT);
        bitmap = Util.setColorDatas(data, w, h);
        
        String buffer[] = bundle.getStringArray(CameraActivity.KEY_ANCHOR_INFO);
        anchorInfos = Data.analysis(buffer);
        imageInfos.add(anchorInfos);//将当前的生成的快照放入列表        
        Drawable drawable = new BitmapDrawable(bitmap);   
		thumbIds.add(drawable);

		updateGallery();
		
		touchListener = new View.OnTouchListener() {
    		public boolean onTouch(View v, MotionEvent event) {
				final int action = event.getAction();
				int y = (int) event.getY();

				switch (action) {
				case MotionEvent.ACTION_DOWN:
					if (y > 0 && y < v.getHeight()) {
						isRemoving = true;
					}
					break;
				case MotionEvent.ACTION_MOVE:						
					if (y < 0 && isRemoving) {
						removeCurrent();
						isRemoving = false;
					}
					break;
				case MotionEvent.ACTION_UP:
					isRemoving = false;
					break;
				}
				return false;
			}
    	};
    	
        closeBtn = (Button) findViewById(R.id.close);
        closeBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SnapShot.this.finish();
			}
		});
        
        saveBtn = (Button) findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				save();
			}
		});
        
        switcher = (ImageSwitcher) findViewById(R.id.switcher);
        switcher.setFactory(this);
        switcher.setInAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_in));
        switcher.setOutAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.fade_out));
        
        saveView = (ImageView) findViewById(R.id.save);
    	saveView.setOnTouchListener(new View.OnTouchListener() {
    		public boolean onTouch(View v, MotionEvent event) {
				final int action = event.getAction();
				Rect r = new Rect();
				v.getHitRect(r);
				
				int x = (int) event.getX() + r.left;
				int y = (int) event.getY() + r.top;

				switch (action) {
				case MotionEvent.ACTION_DOWN:
					r.set(r.left, r.top, r.left + r.width() / 2, r.top + r.height() / 2);
					if (r.contains(x, y)) {
						isSaving = true;
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (isSaving && r.contains(x, y)) {
						save();
						isSaving = false;
					}
					break;
				case MotionEvent.ACTION_UP:
					break;
				}
				return true;
			}
    	});
    	
    	int pos = gallery.getCount() - 1;
    	gallery.setSelection(pos);
    	gallery.performItemClick(gallery.getChildAt(pos), pos, pos);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
    	switcher.setImageDrawable(thumbIds.get(position));    
    	if (currentClicked != position)
    		infoCardView.setVisibility(View.INVISIBLE);
    	
    	currentClicked = position;
    	
    	showSaveBtnAndView(currentClicked == thumbIds.size() - 1 && !isSaved);
    	updateSwitcher(currentClicked);
    	highlightGallerySelected(currentClicked);
    }
    
    /*
     * 选中的高亮显示
     */
    private void highlightGallerySelected(int pos) {
    	if (thumbIds == null) {
    		return;
    	}
    	int first = gallery.getFirstVisiblePosition();
    	int highLightPos = pos - first;
    	
    	for (int i = 0; i < gallery.getChildCount(); i++) {
    		gallery.getChildAt(i).setBackgroundColor(0xFF000000);
    	}
    	for (int i = 0; i < gallery.getChildCount(); i++) {
    		if (i == highLightPos) {
    	    	gallery.getChildAt(i).setBackgroundResource(R.drawable.thumb_background);
    	    	break;
    		}
    	}
    }
    
    /*
     * 保存按钮的显示和消失
     */
    private void showSaveBtnAndView(boolean b) {
    	if (b) {
    		saveView.setVisibility(View.VISIBLE);
    		saveBtn.setVisibility(View.VISIBLE);
    	} else {
    		saveView.setVisibility(View.INVISIBLE);
    		saveBtn.setVisibility(View.INVISIBLE);
    	}
    }
    /*
     * 回收bitmap资源
     */
	private void recycleBimap(int i) {
		Bitmap bmp = ((BitmapDrawable) thumbIds.get(i)).getBitmap();
		if (bmp != null) {
			bmp.recycle();
			bmp = null;
		}				
		thumbIds.get(i).setCallback(null);	
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
		if (thumbIds != null) {
			for (int i = 0; i < thumbIds.size(); i++) {
				recycleBimap(i);		
			}
		}
		if (gallery != null) {
			Bitmap b = gallery.getDrawingCache();
			if (b != null) {
				b.recycle();
				b = null;
			}
		}
		System.gc();
	}
    
    public void onItemSelected(AdapterView parent, View v, int position, long id) {
    	currentRemoved = position;
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private void updateSwitcher(int index) {
    	((AnchorViewClone) switcher.getCurrentView()).setAnchorInfo(imageInfos.get(index));
    	switcher.getCurrentView().invalidate();
    }
    
    public View makeView() {
    	anchorView = new AnchorViewClone(this);
    	anchorView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    	anchorView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        return anchorView;
    }
    
	public void showInfoCard(boolean show) {
		infoCardView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
	}

	public void updateInfoCard(List<Data> list) {
		infoCardView.setAdapter(new InfoAdapter(this, list));
		showInfoCard(true);
		infoCardView.invalidate();
	}

	public void save() {
		Time now = new Time();
		now.setToNow();
		String uri = MediaStore.Images.Media.insertImage(getContentResolver(),
				bitmap, now.format2445(), null);
		saveImage(uri);
		
		showSaveBtnAndView(false);
		
		reload(true);		

        Toast.makeText(this, R.string.save_notification, Toast.LENGTH_LONG).show();
	}
	
	/*
	 * 保存锁定的场景和信息点
	 */
    private void saveImage(String uri) {
    	long timestamp = generateTimestamp();
    	
    	boolean inserted = imageRecord.insertUri(uri, timestamp);//存储图像    	
    	if (!inserted) {
    		Toast.makeText(this, R.string.can_not_save_image, Toast.LENGTH_LONG).show();
    		return;
    	}
        anchorView.serialize(imageRecord, timestamp);//存储信息点
        
        uriTimestamps.add(new UriTimestamp(uri, timestamp)); //加到最前端
        
        isSaved = true;//保存按钮从此不再显示, 保证在removeOldest之前调用
        if (thumbIds.size() > SAVE_LIMIT) {
        	removeOldest();
        }
    }
    
    /*
     * 重新将数据库的内容载入内存
     */
    private void reload(boolean isRemainFirst) {
		if (isRemainFirst) {
			for (int i = 0; i < thumbIds.size(); i++) {
				recycleBimap(0);   //产生崩溃的地方
				thumbIds.remove(0);
				imageInfos.remove(0);
			}
		}
    	thumbIds.clear();
		imageInfos.clear();
		uriTimestamps.clear();

		loadImagesAndInfos();
		updateGallery();
		updateSwitcher(thumbIds.size() - 1);		
		
		if (uriTimestamps.size() <= 0) {	//已经删除所有的快照， 没有快照了
		} else {
		}
		int pos = thumbIds.size() - 1;
    	gallery.performItemClick(gallery.getChildAt(pos), pos, pos);
    }
    
    /*
     * 更新gallery的内容
     */
    private void updateGallery() {
    	if (adpater != null) {
    		int c = adpater.getCount();
    		for (int i = 0; i < c; i++)
    			adpater.getItem(i).setCallback(null);
    		System.gc();
    	}
    	
		adpater = new ImageAdapter(this, thumbIds);
		gallery.setAdapter(adpater);
		
		gallery.setOnItemClickListener(this);
		gallery.setOnTouchListener(touchListener);

		highlightGallerySelected(gallery.getChildCount() - 1);
		gallery.invalidate();
    }
    
    /*
     * 载入数据
     */
    private void loadImagesAndInfos() {
    	System.gc();
    	    	
    	uriTimestamps = imageRecord.queryUriAndTimestamp();
    	if (uriTimestamps == null) {    		
    		uriTimestamps = new ArrayList<UriTimestamp>();//存储所有的保存的图像的uri	
    	}
    	for (int i = 0; i < uriTimestamps.size(); i++) {
    		String uri = uriTimestamps.get(i).uri;
    		try {
				thumbIds.add(new BitmapDrawable(MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(uri))));
			} catch (FileNotFoundException e) {
		        Toast.makeText(this, R.string.file_not_found,
		                Toast.LENGTH_LONG).show();
			} catch (IOException e) {
		        Toast.makeText(this, R.string.file_not_found,
		                Toast.LENGTH_LONG).show();
			}
			imageInfos.add(imageRecord.queryRecord(uriTimestamps.get(i).timestamp));
    	}
    }
    
    /*
     * 执行删除的具体操作
     */
    private void doRemove(int index) {
    	if (thumbIds == null || thumbIds.size() <= 0)
    		return;
    	
    	if (!isSaved) {
    		if (index == thumbIds.size() - 1) {
    			Toast.makeText(this, R.string.file_not_found,
		                Toast.LENGTH_LONG).show();
    			return;
    		}
    	}
    	
    	imageInfos.remove(index);
    	thumbIds.remove(index);

        String uri = uriTimestamps.get(index).uri;
        getContentResolver().delete(Uri.parse(uri), null, null);       //delete from MediaStore
        long timestamp = uriTimestamps.get(index).timestamp;
        imageRecord.deleteLinkAndRecord(timestamp);
        uriTimestamps.remove(index);
        reload(true);
    }

    //gallery里第一个是最早保存的, 自动删除时调用
    private void removeOldest() {
    	doRemove(0);
    }
    
    //手动删除时调用
    private void removeCurrent() {
    	if ((currentRemoved == thumbIds.size() - 1 && !isSaved) || 			//当前的快照没有保存，且要删除当前快照
    			(currentRemoved == 0 && isSaved && thumbIds.size() == 1))//只剩下最后一个
    		return;
    	doRemove(currentRemoved);
    	currentRemoved = 0;
        Toast.makeText(this, R.string.snap_shot_removed,
                Toast.LENGTH_LONG).show();
    	gallery.invalidate();
    }

    /*
     * gallery的adapter
     */
    public class ImageAdapter extends ArrayAdapter<Drawable> {
        private Context context;
        int mGalleryItemBackground;
        
        public ImageAdapter(Context c, ArrayList<Drawable> drawables) {        	
            super(c, 0, drawables);
            context = c;
        }

        public int getCount() {
            return thumbIds.size();
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(context);
            if (position == thumbIds.size() - 1 && !isSaved) {
            	i.setImageDrawable(Utilities.createIconThumbnail2(thumbIds.get(position), SnapShot.this));
            } else {
            	i.setImageDrawable(Utilities.createIconThumbnail(thumbIds.get(position), SnapShot.this));
            }
            
            i.setScaleType(ImageView.ScaleType.FIT_CENTER);            
            i.setLayoutParams(new Gallery.LayoutParams(ITEM_WIDTH, ITEM_HEIGHT));
            return i;
        }
    }
    
    private static final int ITEM_WIDTH = 38;
    private static final int ITEM_HEIGHT =  50;
    
    /*
     * 信息卡对应的InfoAdapter
     */
	public class InfoAdapter extends BaseAdapter {
		List<Data> list;

		public InfoAdapter(Context c, List<Data> l) {
			list = l;
		}

		public int getCount() {
			return list.size();
		}

		public Object getItem(int position) {
			return list.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			final Data item = (Data) getItem(position);

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.info_item2, parent, false);
			}
			TextView tv = (TextView) convertView.findViewById(R.id.info_name);
			tv.setText(item.name);

			tv = (TextView) convertView.findViewById(R.id.info_addr);
			tv.setText(item.addr);

			tv = (TextView) convertView.findViewById(R.id.info_distance);
			tv.setText(String.valueOf(item.distance));

			tv = (TextView) convertView.findViewById(R.id.info_phone);
			tv.setText(item.phone);

			return convertView;
		}
	}
        
    private static long generateTimestamp() {
    	return System.currentTimeMillis();
    }
}
