package tech.mingxi.mxslider;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

import tech.mingxi.library.slider.ImageManager;
import tech.mingxi.library.slider.MXSlider;

public class MainActivity extends AppCompatActivity {
	public static final String DEBUG_TAG = "MXSlider";
	private MXSlider slider;
	;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		slider = findViewById(R.id.activity_main_slider);
		ImageManager imageManager = slider.getImageManager();
		loadBitmaps(imageManager);
//		imageManager.addBitmap(Bitmap.createBitmap(new int[]{Color.parseColor("#ffffff")}, 1, 1, Bitmap.Config.ARGB_8888));
//		imageManager.addBitmap(Bitmap.createBitmap(new int[]{Color.parseColor("#eeeeee")}, 1, 1, Bitmap.Config.ARGB_8888));
//		imageManager.addBitmap(Bitmap.createBitmap(new int[]{Color.parseColor("#dddddd")}, 1, 1, Bitmap.Config.ARGB_8888));
	}

	void loadBitmaps(ImageManager imageManager) {
		try {
			String[] list = getAssets().list("test");
			if (list != null) {
				for (String name : list) {
					Uri uri = MXSlider.getUriForAsset("/test/" + name);
					Log.i(DEBUG_TAG, "uri=" + uri);
					imageManager.addImage(uri);
				}
			}
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "", e);
		}
	}
}
