package de.dfki.android.gestureTrainer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.dfki.android.gesture.R;
import de.dfki.ccaal.gestures.IGestureRecognitionListener;
import de.dfki.ccaal.gestures.IGestureRecognitionService;
import de.dfki.ccaal.gestures.classifier.Distribution;

public class GestureTrainer extends Activity {

	IGestureRecognitionService recognitionService;
	String activeTrainingSet;
	public static Button pressedTrainOK;
	public static RelativeLayout mylayout;
	

	private final ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			recognitionService = IGestureRecognitionService.Stub.asInterface(service);
			try {
				recognitionService.startClassificationMode(activeTrainingSet);
				recognitionService.registerListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			recognitionService = null;
		}
	};

	IBinder gestureListenerStub = new IGestureRecognitionListener.Stub() {

		@Override
		public void onGestureLearned(String gestureName) throws RemoteException {
			Toast.makeText(GestureTrainer.this, String.format("Gesto %s natrenovane", gestureName), Toast.LENGTH_SHORT).show();
			System.err.println("Gesture %s learned");
		}

		@Override
		public void onTrainingSetDeleted(String trainingSet) throws RemoteException {
			Toast.makeText(GestureTrainer.this, String.format("Trenovacia mnozina %s vymazana", trainingSet), Toast.LENGTH_SHORT).show();
			System.err.println(String.format("Training set %s deleted", trainingSet));
		}

		@Override
		public void onGestureRecognized(final Distribution distribution) throws RemoteException {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					RelativeLayout layout = (RelativeLayout)findViewById(R.id.relative);
					if (distribution.getBestDistance() < 8){						
						layout.setBackgroundColor(0xFF00FF00);
						Toast.makeText(GestureTrainer.this, String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()), Toast.LENGTH_LONG).show();
					}else{
						layout.setBackgroundColor(0xFFFFFFFF);
						Toast.makeText(GestureTrainer.this, String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()), Toast.LENGTH_LONG).show();
					}
						
						//layout.main.
						//R.id.relative.setBackgroundColor(0xFF00FF00);
						//R.class.setBackgroundColor(Color.argb(255, 255, 255, 255));
					//
					//System.err.println(String.format("%s: %f", distribution.getBestMatch(), distribution.getBestDistance()));
				}
			});
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final TextView activeTrainingSetText = (TextView) findViewById(R.id.activeTrainingSet);
		final EditText trainingSetText = (EditText) findViewById(R.id.trainingSetName);
		final EditText editText = (EditText) findViewById(R.id.gestureName);
		activeTrainingSet = editText.getText().toString();
		pressedTrainOK = (Button) findViewById(R.id.TrainOK);
		mylayout = (RelativeLayout)findViewById(R.id.relative);
		final Button startTrainButton = (Button) findViewById(R.id.trainButton);
		final Button deleteTrainingSetButton = (Button) findViewById(R.id.deleteTrainingSetButton);
		final Button changeTrainingSetButton = (Button) findViewById(R.id.startNewSetButton);
	



		startTrainButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (recognitionService != null) {
					try {
						if (!recognitionService.isLearning()) {
							startTrainButton.setText("Stop Training");
							editText.setEnabled(false);
							deleteTrainingSetButton.setEnabled(false);
							changeTrainingSetButton.setEnabled(false);
							trainingSetText.setEnabled(false);
							//Zaciatok trenovania							
							
							//recognitionService.onPushToGesture(true);
							recognitionService.startLearnMode(activeTrainingSet, editText.getText().toString());
						} else {
							startTrainButton.setText("Start Training");
							editText.setEnabled(true);
							deleteTrainingSetButton.setEnabled(true);
							changeTrainingSetButton.setEnabled(true);
							trainingSetText.setEnabled(true);
							recognitionService.stopLearnMode();
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		changeTrainingSetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				activeTrainingSet = trainingSetText.getText().toString();
				activeTrainingSetText.setText(activeTrainingSet);

				if (recognitionService != null) {
					try {
						recognitionService.startClassificationMode(activeTrainingSet);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		deleteTrainingSetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				AlertDialog.Builder builder = new AlertDialog.Builder(GestureTrainer.this);
				builder.setMessage("You really want to delete the training set?").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (recognitionService != null) {
							try {
								recognitionService.deleteTrainingSet(activeTrainingSet);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				builder.create().show();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.edit_gestures:
			Intent editGesturesIntent = new Intent().setClass(this, GestureOverview.class);
			editGesturesIntent.putExtra("trainingSetName", activeTrainingSet);
			startActivity(editGesturesIntent);
			return true;

		default:
			return false;
		}
	}

	@Override
	protected void onPause() {
		try {
			recognitionService.unregisterListener(IGestureRecognitionListener.Stub.asInterface(gestureListenerStub));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		recognitionService = null;
		unbindService(serviceConnection);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Intent bindIntent = new Intent("de.dfki.ccaal.gestures.GESTURE_RECOGNIZER");
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		super.onResume();
	}
}