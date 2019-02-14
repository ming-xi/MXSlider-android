package tech.mingxi.mxslider;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import tech.mingxi.library.slider.ImageManager;
import tech.mingxi.library.slider.MXSlider;

public class MainActivity extends AppCompatActivity {

	private MXSlider slider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		slider = findViewById(R.id.activity_main_slider);
		ImageManager imageManager = slider.getImageManager();
		imageManager.addBitmap(Bitmap.createBitmap(new int[]{Color.parseColor("#ffffff")}, 1, 1, Bitmap.Config.ARGB_8888));
		imageManager.addBitmap(Bitmap.createBitmap(new int[]{Color.parseColor("#eeeeee")}, 1, 1, Bitmap.Config.ARGB_8888));
		imageManager.addBitmap(Bitmap.createBitmap(new int[]{Color.parseColor("#dddddd")}, 1, 1, Bitmap.Config.ARGB_8888));
	}
}
