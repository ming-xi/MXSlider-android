package tech.mingxi.musicplayerdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.functions.Consumer;
import tech.mingxi.library.slider.MXSlider;
import tech.mingxi.library.slider.Page;

public class MainActivity extends AppCompatActivity {
	public static final String DEBUG_TAG = "MXSlider";
	private static final String[] testTitles = new String[]{
			"MAST\nIN\nTHE\nMIST\n～霧の港～",
			"Return\nof\nthe Ancients",
			"熏しの隠れ家",
			"銀の意志\nSuper Arrange Ver",
			"Weight of the World the End of YoRHa",
	};
	private static final String[] testAuthors = new String[]{"" +
			"菅野よう子",
			"Glenn Stafford / Derek Duke / Tracy Bush",
			"山根ミチル",
			"Falcom Sound Team jdk",
			"YoRHa",
	};
	private ImageButton b_play;
	private ImageButton b_prev;
	private ImageButton b_next;
	private ImageButton b_random;
	private ImageButton b_loop;
	private ProgressBar pb_progress;
	private MediaPlayer mediaPlayer;
	private MXSlider slider;
	private int playPosition = 0;
	private int progressBarMax;
	private Timer timer;
	private boolean inited = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		setContentView(R.layout.activity_main);
		slider = findViewById(R.id.activity_main_slider);
		b_play = findViewById(R.id.activity_main_play);
		b_prev = findViewById(R.id.activity_main_prev);
		b_next = findViewById(R.id.activity_main_next);
		b_random = findViewById(R.id.activity_main_random);
		b_loop = findViewById(R.id.activity_main_loop);
		pb_progress = findViewById(R.id.activity_main_progress);
		progressBarMax = getResources().getInteger(R.integer.progress_max);
		slider.getPageManager().addAllPages(generatePages());
		slider.setOnSwipeListener(new MXSlider.OnSwipeListener() {
			@Override
			public void onSwipePositionChange(float position) {
//				Log.i(DEBUG_TAG, String.format("pos = %.2f", position));
			}

			@Override
			public void onIndexChange(int index) {
				Log.i(DEBUG_TAG, String.format("index = %d", index));
				setDataSourceIndex(index);
			}
		});
		b_play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer.isPlaying()) {
					pausePlayer();
				} else {
					resumePlayer();
				}
			}
		});
		b_prev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				slider.setIndex(slider.getIndex() - 1, true);
			}
		});
		b_next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				slider.setIndex(slider.getIndex() + 1, true);
			}
		});
		b_random.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				//seek to the end of current playing music in order to test auto playing next music.
//				mediaPlayer.seekTo(mediaPlayer.getDuration() - 3000);
				slider.setTextPositionStyle(slider.getTextPositionStyle() == MXSlider.TextPositionStyle.CLASSICAL ? MXSlider.TextPositionStyle.PACKED : MXSlider.TextPositionStyle.CLASSICAL);
			}
		});
		b_loop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				slider.setLoop(!slider.isLoop());
			}
		});
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				inited = true;
				startPlayer();
			}
		});
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.i(DEBUG_TAG, String.format("onError what = %d extra = %d", what, extra));
				return true;
			}
		});
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (slider.getIndex() >= slider.getPageManager().getPageCount()) {
					return;
				}
				slider.setIndex(slider.getIndex() + 1, true);
			}
		});
		setDataSourceIndex(0);
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (mediaPlayer.isPlaying()) {
					pb_progress.setProgress((int) (((float) mediaPlayer.getCurrentPosition()) * progressBarMax / mediaPlayer.getDuration()));
				}
			}
		}, 0, 250);
	}

	private void setDataSourceIndex(int index) {
		final MusicItem page = (MusicItem) slider.getPageManager().getPage(index);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String path = page.musicUri.getPath();
					if (path.startsWith("/")) {
						path = path.replaceFirst("/+", "");
					}
					AssetFileDescriptor descriptor = getAssets().openFd(path);
					try {
						mediaPlayer.reset();
						mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
						mediaPlayer.prepare();

					} catch (IOException e) {
						Log.e(DEBUG_TAG, "", e);
					}
					descriptor.close();
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "", e);
				}

			}
		}).start();
	}

	protected void onDestroy() {
		mediaPlayer.release();
		timer.cancel();
		timer = null;
		mediaPlayer = null;
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		resumePlayer();

	}

	@Override
	protected void onPause() {
		super.onPause();
		pausePlayer();

	}

	private void startPlayer() {
		if (!mediaPlayer.isPlaying()) {
			playPosition = 0;
			mediaPlayer.start();
		}
	}

	private void resumePlayer() {
		if (inited && !mediaPlayer.isPlaying()) {
			mediaPlayer.seekTo(playPosition);
			mediaPlayer.start();
			b_play.setImageResource(R.drawable.ic_pause_white_24dp);
		}
	}

	private void pausePlayer() {
		if (inited && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			playPosition = mediaPlayer.getCurrentPosition();
			b_play.setImageResource(R.drawable.ic_play_arrow_white_24dp);
		}
	}

	private List<MusicItem> generatePages() {
		List<MusicItem> pages = new ArrayList<>();
		for (int i = 0; i < testTitles.length && i < testAuthors.length; i++) {
			pages.add(new MusicItem(testTitles[i % testTitles.length], testAuthors[i % testAuthors.length], Uri.parse("assets:///test/" + i + ".jpg"), Uri.parse("assets:///test/" + i + ".mp3")));
		}
//      uncomment following line if you want to test all types of uris(file://, content:// and android.resource://)
//		testAllTypeOfUris(pages);
		return pages;
	}

	/**
	 * This method will change test pages in order to test all supported uri schemes
	 *
	 * @param pages generated pages
	 */
	@SuppressLint("CheckResult")
	private void testAllTypeOfUris(List<MusicItem> pages) {
		pages.get(0).coverUri = Uri.parse("android.resource://tech.mingxi.musicplayerdemo/drawable/ic_launcher");
		RxPermissions rxPermissions = new RxPermissions(this);
		if (rxPermissions.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
			String[] what = new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN,
					MediaStore.Images.ImageColumns._ID,
					MediaStore.Images.ImageColumns.MIME_TYPE,
					MediaStore.Images.ImageColumns.DATA};

			String where = MediaStore.Images.Media.MIME_TYPE + "='image/jpeg'" +
					" OR " + MediaStore.Images.Media.MIME_TYPE + "='image/png'";
			Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					what,
					where,
					null,
					MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
			if (cursor.moveToNext()) {
				pages.get(1).coverUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));
				cursor.close();
			}
			pages.get(2).coverUri = Uri.fromFile(new File("/storage/emulated/0/Download/23-52-55-718746bed9eaa9022589869223b09295.png"));
		} else {
			rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE).subscribe(new Consumer<Boolean>() {
				@Override
				public void accept(Boolean granted) throws Exception {
					Toast.makeText(getBaseContext(), "reopen APP to see changes", Toast.LENGTH_LONG).show();
				}
			});
		}
	}


	static class MusicItem implements Page {
		private String title;
		private String author;
		private Uri coverUri;
		private Uri musicUri;

		public MusicItem(String title, String author, Uri coverUri, Uri musicUri) {
			this.title = title;
			this.author = author;
			this.coverUri = coverUri;
			this.musicUri = musicUri;
		}

		@Override
		public Uri getLocalUri() {
			return coverUri;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String getSubtitle() {
			return author;
		}

		@Override
		public String toString() {
			return "MusicItem{" +
					"title='" + title + '\'' +
					", author='" + author + '\'' +
					", coverUri=" + coverUri +
					'}';
		}
	}

}
