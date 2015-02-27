package org.egokituz.arduino2android.activities;

import org.egokituz.arduino2android.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * Activity showing some general information about this app.
 * 
 * @author Xabier Gardeazabal
 *
 */
public class HelpActivity extends Activity {

	//variable for selection intent
	private final int PICKER = 1;
	//variable to store the currently selected image
	private int currentPic = 0;
	//gallery object
	private Gallery picGallery;
	//image view for larger display
	private ImageView picView;

	//adapter for gallery view
	private PicAdapter imgAdapt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		//get the large image view
		picView = (ImageView) findViewById(R.id.picture);

		//get the gallery view
		picGallery = (Gallery) findViewById(R.id.gallery);

		//create a new adapter
		imgAdapt = new PicAdapter(this);

		//set the gallery adapter
		picGallery.setAdapter(imgAdapt);
		

		//set the click listener for each item in the thumbnail gallery
		picGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				//set the larger image view to display the chosen bitmap calling method of adapter class
		        picView.setImageBitmap(imgAdapt.getPic(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			//check if we are returning from picture selection
			if (requestCode == PICKER) {
				//import the image
				//the returned picture URI
				Uri pickedUri = data.getData();
				//declare the bitmap
				Bitmap pic = null;
				 
				//declare the path string
				String imgPath = "";
				
				//retrieve the string using media data
				String[] medData = { MediaStore.Images.Media.DATA };
				//query the data
				Cursor picCursor = managedQuery(pickedUri, medData, null, null, null);
				if(picCursor!=null)
				{
				    //get the path string
				    int index = picCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				    picCursor.moveToFirst();
				    imgPath = picCursor.getString(index);
				}
				else
				    imgPath = pickedUri.getPath();
			}
		}
		//superclass method
		super.onActivityResult(requestCode, resultCode, data);

	}





	////////////////////////////////////////////////


	public class PicAdapter extends BaseAdapter {
		//use the default gallery background image
		int defaultItemBackground;

		//gallery context
		private Context galleryContext;

		//array to store bitmaps to display
		private Bitmap[] imageBitmaps;

		//placeholder bitmap for empty spaces in gallery
		Bitmap placeholder;

		public PicAdapter(Context c) {

			//instantiate context
			galleryContext = c;

			//create bitmap array
			imageBitmaps  = new Bitmap[5];

			//decode the placeholder image
			//placeholder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
			imageBitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.architecture);
			imageBitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.data_flow_diagram);
			imageBitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.bluetoothv3);
			imageBitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.arduino_uno_wiring);
			imageBitmaps[4] = BitmapFactory.decodeResource(getResources(), R.drawable.arduino_mega_wiring);

			//set placeholder as all thumbnail images in the gallery initially
			//for(int i=0; i<imageBitmaps.length; i++)
			//	imageBitmaps[i]=placeholder;

			//get the styling attributes - use default Andorid system resources
			TypedArray styleAttrs = galleryContext.obtainStyledAttributes(R.styleable.PicGallery);

			//get the background resource
			defaultItemBackground = styleAttrs.getResourceId(
					R.styleable.PicGallery_android_galleryItemBackground, 0);

			//recycle attributes
			styleAttrs.recycle();
		}

		//return number of data items i.e. bitmap images
		public int getCount() {
			return imageBitmaps.length;
		}

		//return item at specified position
		public Object getItem(int position) {
			return position;
		}

		//return item ID at specified position
		public long getItemId(int position) {
			return position;
		}
		
		//return bitmap at specified position for larger display
		public Bitmap getPic(int posn)
		{
		    //return bitmap at posn index
		    return imageBitmaps[posn];
		}

		//get view specifies layout and display options for each thumbnail in the gallery
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			//create the view
			ImageView imageView = new ImageView(galleryContext);
			//specify the bitmap at this position in the array
			imageView.setImageBitmap(imageBitmaps[position]);
			//set layout options
			imageView.setLayoutParams(new Gallery.LayoutParams(300, 200));
			//scale type within view area
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			//set default gallery item background
			imageView.setBackgroundResource(defaultItemBackground);
			//return the view
			return imageView;
		}

	}
}
