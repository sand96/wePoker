package edu.vub.at.nfcpoker.ui;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.tekle.oss.android.animation.AnimationFactory;
import com.tekle.oss.android.animation.AnimationFactory.FlipDirection;

import mobisocial.nfc.Nfc;
import mobisocial.nfc.addon.BluetoothConnector;
import edu.vub.at.nfcpoker.Card;
import edu.vub.at.nfcpoker.R;
import edu.vub.at.nfcpoker.ui.tools.PageCurlView;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.tekle.oss.android.animation.AnimationFactory;
import com.tekle.oss.android.animation.AnimationFactory.FlipDirection;

import edu.vub.at.commlib.CommLib;
import edu.vub.at.commlib.CommLibConnectionInfo;
import edu.vub.at.nfcpoker.Card;
import edu.vub.at.nfcpoker.ConcretePokerServer.GameState;
import edu.vub.at.nfcpoker.R;
import edu.vub.at.nfcpoker.comm.Message.ReceiveHoleCardsMessage;
import edu.vub.at.nfcpoker.comm.Message.ReceivePublicCards;
import edu.vub.at.nfcpoker.comm.PokerServer;

public class ClientActivity extends Activity {

	// Game state
	//public static GameState GAME_STATE = GameState.INIT;
	private static int currentBet = 0;
	private static int currentMoney = 0;
	
    // Interactivity
    private static final boolean useIncognitoMode = true;
    private static final boolean useIncognitoLight = false;
    private static final boolean useIncognitoProxmity = true;
    private boolean incognitoMode;
    private long incognitoLight;
    private long incognitoProximity;
    private Timer incognitoDelay;
    private SensorManager sensorManager;
	
	// Enums
//	public enum GameState {
//	    INIT, NFCPAIRING, HOLE, HOLE_NEXT, FLOP, FLOP_NEXT, TURN, TURN_NEXT, RIVER, RIVER_NEXT
//	}
	
    @Override
    @SuppressWarnings("unused")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client);
        
        // Settings
        
        // Game state
        
        // Interactivity
        incognitoMode = false;
        incognitoLight = -1;
        incognitoProximity = -1;
        incognitoDelay = new Timer();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        
        
        // UI
        /*final Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, AsciiNdefMessage.CreateNdefMessage(UUID));
        startActivity(intent);*/
        final ImageButton buttonAddBlack = (ImageButton) findViewById(R.id.AddBlack);
        buttonAddBlack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (canBet()) incrementBetAmount(100);
            }
        });
        final ImageButton buttonAddGreen = (ImageButton) findViewById(R.id.AddGreen);
        buttonAddGreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (canBet()) incrementBetAmount(25);
            }
        });
        final ImageButton buttonAddBlue = (ImageButton) findViewById(R.id.AddBlue);
        buttonAddBlue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (canBet()) incrementBetAmount(10);
            }
        });
        final ImageButton buttonAddRed = (ImageButton) findViewById(R.id.AddRed);
        buttonAddRed.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (canBet()) incrementBetAmount(5);
            }
        });
        final ImageButton buttonAddWhite = (ImageButton) findViewById(R.id.AddWhite);
        buttonAddWhite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (canBet()) incrementBetAmount(1);
            }
        });

        final ViewFlipper viewFlipper1 = (ViewFlipper) findViewById(R.id.viewFlipper1);
        final ImageButton btn = (ImageButton) findViewById(R.id.Card1Front);
        // TODO EGB CHANGE THIS FOR THE CARDS THAT THE SERVER SENDS YOU
        short x = 3;
        Card c = new Card(x,(short) (x+1));
        int id = getResources().getIdentifier("edu.vub.at.nfcpoker:drawable/" + c.toString(), null, null);
        btn.setImageResource(id);
        AnimationFactory.flipTransition(viewFlipper1, FlipDirection.LEFT_RIGHT);
        
        final ViewFlipper viewFlipper2 = (ViewFlipper) findViewById(R.id.viewFlipper2);
        AnimationFactory.flipTransition(viewFlipper2, FlipDirection.LEFT_RIGHT);

        
        /*
        ArrayList<Bitmap> mPages1 = new ArrayList<Bitmap>();
		mPages1.add(BitmapFactory.decodeResource(getResources(), R.drawable.backside));
		mPages1.add(BitmapFactory.decodeResource(getResources(), R.drawable.clubs_10c));

        final PageCurlView card1 = (PageCurlView) findViewById(R.id.Card1);
        card1.setPages(mPages1);

        ArrayList<Bitmap> mPages2 = new ArrayList<Bitmap>();
        mPages2.add(BitmapFactory.decodeResource(getResources(), R.drawable.backside));
        mPages2.add(BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_10d));
		
        final PageCurlView card2 = (PageCurlView) findViewById(R.id.Card2);
        card2.setPages(mPages2);
        */
        
        listenToGameServer();
    }
    
    private void listenToGameServer() {
    	final TimerTask tt = new TimerTask() {
    		public void run() {
    			try {
    				Log.v("AMBIENTPOKER", "Looking for server...");
    				CommLibConnectionInfo clci = CommLib.discover(PokerServer.class);
    				Log.v("AMBIENTPOKER", "Discovered server at " + clci.getAddress());
    				Client c = clci.connect(new Listener() {
    					@Override
    					public void received(Connection c, Object m) {
    						super.received(c, m);
					
    						Log.v("AMBIENTPOKER", "Received message " + m.toString());
					
    						if (m instanceof GameState) {
    							GameState newGameState = (GameState) m;
    							switch (newGameState) {
    							case STOPPED:
    								Log.v("AMBIENTPOKER", "Game state changed to STOPPED");
    								break;
    							case WAITING_FOR_PLAYERS:
    								Log.v("AMBIENTPOKER", "Game state changed to WAITING_FOR_PLAYERS");
    								break;
    							case PREFLOP:
    								Log.v("AMBIENTPOKER", "Game state changed to PREFLOP");
    								break;
    							case FLOP:
    								Log.v("AMBIENTPOKER", "Game state changed to FLOP");
    								break;
    							case TURN:
    								Log.v("AMBIENTPOKER", "Game state changed to TURN");
    								break;
    							case RIVER:
    								Log.v("AMBIENTPOKER", "Game state changed to RIVER");
    								break;
    							case END_OF_ROUND:
    								Log.v("AMBIENTPOKER", "Game state changed to END_OF_ROUND");
    								break;
    							}
    						}
    						
    						if (m instanceof ReceivePublicCards) {
    							ReceivePublicCards newPublicCards = (ReceivePublicCards) m;
    							Log.v("AMBIENTPOKER", "Received public cards: ");
    							Card[] cards = newPublicCards.cards;
    							for (int i = 0; i < cards.length; i++) {
    								Log.v("AMBIENTPOKER", cards[i].toString() + ", ");
    							}
    						}
					
    						if (m instanceof ReceiveHoleCardsMessage) {
    							ReceiveHoleCardsMessage newHoleCards = (ReceiveHoleCardsMessage) m;
    							Log.v("AMBIENTPOKER", "Received hand cards: " + newHoleCards.toString());
    						}
						
    					}
    				});
    			} catch (IOException e) {
    				Log.e("AMBIENTPOKER", "Could not discover server: ");
    				e.printStackTrace();
    			}
    		}
    	};
    	
    	Timer timer = new Timer();
    	timer.schedule(tt, 1000);
    		
    }

    
    @Override
    protected void onResume()
    {
        if (useIncognitoMode) {
	        incognitoMode = false;
	        incognitoLight = -1;
	        incognitoProximity = -1;
	        incognitoDelay = new Timer();

	        if (useIncognitoLight) {
		        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		        if (lightSensor != null) {
			        sensorManager.registerListener(incognitoSensorEventListener, 
			        		lightSensor, 
			        		SensorManager.SENSOR_DELAY_NORMAL);
			        incognitoLight = 0;
		        }
	        }
	        if (useIncognitoProxmity) {
	        	Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		        if (proximitySensor != null) {
			        sensorManager.registerListener(incognitoSensorEventListener, 
			        		proximitySensor, 
			        		SensorManager.SENSOR_DELAY_NORMAL);
			        incognitoProximity = 0;
		        }
	        }
        }
        super.onResume();
    }
    
    @Override
    protected void onPause()
    {
    	sensorManager.unregisterListener(incognitoSensorEventListener);
        super.onPause();
    }

    // Game
    private void incrementBetAmount(int value) {
    	currentBet += value;
        final EditText textCurrentBet = (EditText) findViewById(R.id.currentBet);
        textCurrentBet.setText(""+currentBet);
    }
    
    private boolean canViewCards() {
//    	return ((GAME_STATE == GameState.HOLE) ||
//    			(GAME_STATE == GameState.HOLE_NEXT) ||
//    			(GAME_STATE == GameState.FLOP) ||
//    			(GAME_STATE == GameState.FLOP_NEXT) ||
//    			(GAME_STATE == GameState.TURN) ||
//    			(GAME_STATE == GameState.TURN_NEXT) ||
//    			(GAME_STATE == GameState.RIVER) ||
//    	    	(GAME_STATE == GameState.RIVER_NEXT));
    	return true;
    }
    
    private boolean canBet() {
//    	return ((GAME_STATE == GameState.HOLE) ||
//    			(GAME_STATE == GameState.FLOP) ||
//    			(GAME_STATE == GameState.TURN) ||
//    			(GAME_STATE == GameState.RIVER));
    	return true;
    }
    
    // Interactivity
    SensorEventListener incognitoSensorEventListener = new SensorEventListener() {
    	@Override
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    		
    	}

    	@Override
    	public void onSensorChanged(SensorEvent event) {
    		if (event.sensor.getType()==Sensor.TYPE_LIGHT){
    			float currentReading = event.values[0];
    			if (currentReading < 10) {
    				if (incognitoLight == 0) incognitoLight = System.currentTimeMillis();
        			Log.d("Light SENSOR", "It's dark!" + currentReading);
    			} else {
    				incognitoLight = 0;
        			Log.d("Light SENSOR", "It's bright!" + currentReading);
    			}
    		}
    		if (event.sensor.getType()==Sensor.TYPE_PROXIMITY){
    			float currentReading = event.values[0];
    			if (currentReading < 1) {
    				if (incognitoProximity == 0) incognitoProximity = System.currentTimeMillis();
        			Log.d("Proximity SENSOR", "I found a hand!" + currentReading);
    			} else {
    				incognitoProximity = 0;
        			Log.d("Proximity SENSOR", "All clear!" + currentReading);
    			}
    		}
    		if ((incognitoLight != 0) && (incognitoProximity != 0)) {
    			if (!incognitoMode) {
    				incognitoMode = true;
    				incognitoDelay = new Timer();
    				incognitoDelay.schedule(new TimerTask() {
    					public void run() {
    						runOnUiThread(new Runnable() {
    							@Override
    							public void run() {
    								showCards();
    							}
    						});
    					}}, 750);
    			}
    		} else {
    			if (incognitoDelay != null) {
    				incognitoDelay.cancel();
    				incognitoDelay = null;
    			}
				if (incognitoMode) {
					incognitoMode = false;
	    			runOnUiThread(new Runnable() {
	    	            @Override
	    	            public void run() {
	    	            	hideCards();
	    	            }
	    	        });
				}
    		}
    	}
    };
    
    // UI
    private void showCards() {
    	if (canViewCards()) {
    		
    	}
    }
    
    private void hideCards() {
    	if (canViewCards()) {
    		
    	}
    }
}
