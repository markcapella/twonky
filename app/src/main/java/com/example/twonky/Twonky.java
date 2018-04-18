// *********************************************************************************************************************
// ***                                                                                                               ***
// ***                TWONKY                                                                                         ***
// ***                                                                                                               ***
// *** TWiG Software Services, by Mark James Capella                                                                 ***
// ***                                                                                                               ***
// ***    From my original book : Games Apples Play (c) 1983                                                         ***
// ***                                                                                                               ***
// ***    4.11 - Change development IDE from Ubuntu / Eclipse to WIN / intelliJ ... soooo much happier :P            ***
// ***    4.14 - Game play complete - in Theory                                                                      ***
// ***    4.2  - Game restructuring, paragraph renames, complete comments in place, unused debug code removed        ***
// ***                       - Custom Twonky Icon                                                                    ***
// ***                       - Replace portrait mode only                                                            ***
// ***                       - Doctor end of game feel                                                               ***
// ***                       - Thorough User Testing, release to first victim                                        ***
// ***    4.3  - Refactored all modules, changed all references of Twig Twonky to just Twonky                        ***
// ***    4.31 - Fixed bug in randomizePlayerPosition() caused by DUH use of || instead of &&                        ***
// ***           Added rough background for screen                                                                   ***
// ***    4.32 - Partial save while modifying GUI                                                                    ***
// ***    4.33 - Partial save GUI layout done except for button renderings                                           ***
// ***    4.34 - Arrow buttons rendered, all game functionality, Custom Twonky logo done, end-of-game cleaned        ***
// ***                        - Need to render move and shoot buttons and graphic switch on/off logic                ***
// ***                        - Still need to replace portrait mode protection                                       ***
// ***                        - Still need to hammer unit testing, then release to Reggie for victim testing         ***
// ***    4.4  - Rendered all buttons, completed button show / hide / graphic change, reset portrait mode            ***
// ***                        - Need to unit test, check into possible memory leak I thought I saw, send it          ***
// ***                          off to Reggie for Pilot / Victim test  :P                                            ***
// ***    4.41 - Misc code cleanup, exit button added on game over, foot and shoot icons brightened slightly         ***
// ***    4.42 - Added android:launchMode="singleInstance" to prevent multiple instances confusion after install     ***
// ***    4.43 - Retro fit a checkbox into the initial / Help screen                                                 ***
// ***                                                                                                               ***
// *********************************************************************************************************************

package com.example.twonky;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.Bundle;

import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;


// *********************************************************************************************************************
// *** Main game activity, definitions, etc                                                                          ***
// *********************************************************************************************************************

public class Twonky extends Activity {

    // debug flag, set to true to enable toast / flow / status messages
    public static final boolean twigDebug = true;

    // Dungeon array
    public static final int MX = 15;
    public static final int MY = 15;
    public static final int MR = 20; // Max relocation squares
    public static final int MB = 20; // Max blocked squares

    final Context context = this;
    final Random random = new Random();

    private ScrollView scrollView;
    private TextView logView;
    private Button button0;
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;

    // Define persistent data from time the utility is installed
    static final boolean init_helpFlg = true;
    private boolean pers_helpFlg;

    // Define persistent data from time the utility starts to time the utility ends
    static final String init_logView = null;
    private String pers_logView;

    static final int init_gameStatus = 0;
    private int pers_gameStatus;
    private int[][] pers_MA = new int[MX][MY];

    static final int init_MA = 0;
    static final int MA_BLOCKED = 1;
    static final int MA_RELOCATION = 2;
    static final int MA_SUPER = 3;

    static final int MA_OBJECTIVE = 4;
    public int pers_XO;
    public int pers_YO;

    static final int MA_TWONKY = 5;
    public int pers_XT;
    public int pers_YT;

    static final int MA_PLAYER = 6;
    public int pers_XP;
    public int pers_YP;

    static final int init_SP = 0;
    public int pers_SP;
    
    static final int init_ST = 0;
    public int pers_ST;

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onCreate%28android.os.Bundle%29              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.twonky);

        // Grab reference to our XML elements
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        logView = (TextView) findViewById(R.id.logView);
        button0 = (Button) findViewById(R.id.button0);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);

        onCreate_init (savedInstanceState);
        addButtonListeners();
    }

// *********************************************************************************************************************
// *** Disable the hardware back button, not needed for the game                                                     ***
// *********************************************************************************************************************

    public void onBackPressed() {
    }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Load persistent program values at onCreate, during cold or warm starts                                        ***
// ***                                                                                                               ***
// *********************************************************************************************************************

    void onCreate_init(Bundle savedInstanceState) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);

        if (savedInstanceState == null) {
            log("onCreate() Cold");
            onCreate_init_cold(prefs);
            displayGameStatus();
            processPlayerTurn();
        } else {
            log("onCreate() Warm");
            onCreate_init_warm(prefs);
        }
    }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Initialize data that needs to be persistent across swap outs by Android Operating System                      ***
// ***                                                                                                               ***
// *********************************************************************************************************************

    public void onCreate_init_cold(SharedPreferences prefs) {

        // Prefs reset first time game is run after install
        pers_helpFlg = prefs.getBoolean("pers_helpFlg", init_helpFlg);

        // Prefs reset every-time new game started
        pers_logView = init_logView;
        logView.setText(pers_logView);

        pers_gameStatus = init_gameStatus;

        // Cold start gets help screen
        if (pers_helpFlg)
            do_help();

        // Pre-clear the maze
        for (int x = 0; x < MX; x++)
            for (int y = 0; y < MY; y++)
                pers_MA[x][y] = init_MA;

        // Set the blocked squares
        for (int I = 0; I < MR;) {
            int X = random.nextInt(MX);
            int Y = random.nextInt(MY);
            if (pers_MA[X][Y] == init_MA) {
                pers_MA[X][Y] = MA_BLOCKED;
                I++;
            }
        }

        // Set the relocation squares
        for (int I = 0; I < MR;) {
            int X = random.nextInt(MX);
            int Y = random.nextInt(MY);
            if (pers_MA[X][Y] == init_MA) {
                pers_MA[X][Y] = MA_RELOCATION;
                I++;
            }
        }

        // Set the Super kill
        while (true) {
            int X = random.nextInt(MX);
            int Y = random.nextInt(MY);
            if (pers_MA[X][Y] == init_MA) {
                pers_MA[X][Y] = MA_SUPER;
                break;
            }
        }

        // Set the Objective
        while (true) {
            pers_XO = random.nextInt(MX);
            pers_YO = random.nextInt(MY);
            if (pers_MA[pers_XO][pers_YO] == init_MA) {
               pers_MA[pers_XO][pers_YO] = MA_OBJECTIVE;
               break;
            }
        }

        // Init maze Space values where Twonky and Player previously occupied
        // Allows for Them to move over objects
        // (Twonky does, Player isn't actually allowed - could be code-cleaned).
        pers_ST = init_ST;
        pers_SP = init_SP;

        // Set the Twonky
        randomizeTwonkyLocation();

        // Set the Player
        randomizePlayerLocation();
    }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Restore data that needs to be persistent across swap outs by Android Operating System                         ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onCreate_init_warm(SharedPreferences prefs) {

      // Prefs reloaded every-time game resumes play
      pers_helpFlg = prefs.getBoolean("pers_helpFlg", init_helpFlg);
      pers_logView  = prefs.getString("pers_logView", init_logView);
      logView.setText(pers_logView);

      pers_gameStatus = prefs.getInt("pers_gameStatus", init_gameStatus);

      for (int x = 0; x < MX; x++)
         for (int y = 0; y < MY; y++)
             pers_MA[x][y] = prefs.getInt("pers_MA_" + x + "_" + y, init_MA);

      pers_XO = prefs.getInt("pers_XO", init_MA);
      pers_YO = prefs.getInt("pers_YO", init_MA);

      pers_XT = prefs.getInt("pers_XT", init_MA);
      pers_YT = prefs.getInt("pers_YT", init_MA);

      pers_XP = prefs.getInt("pers_XP", init_MA);
      pers_YP = prefs.getInt("pers_YP", init_MA);

      pers_ST = prefs.getInt("pers_ST", init_ST);
      pers_SP = prefs.getInt("pers_SP", init_SP);
   }

// *********************************************************************************************************************
// *** Inflate the Android standard hardware menu button View                                                        ***
// *********************************************************************************************************************

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.twonky, menu);
        return super.onCreateOptionsMenu(menu);
    }

// *********************************************************************************************************************
// *** Process user select menu View items                                                                           ***
// *********************************************************************************************************************

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_help:
                do_help();
                break;

            case R.id.menu_about:
                do_about();
                break;

            case R.id.menu_exit:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

// *********************************************************************************************************************
// *** Display / Process the About Screen menu View item                                                             ***
// *********************************************************************************************************************

   void do_about() {
      String aboutMsg = context.getString(R.string.app_name) + "\n";
      try {
         aboutMsg += getPackageManager().getPackageInfo(getPackageName(), 0).versionName + "\n";
      } catch (NameNotFoundException e) {}

      aboutMsg += "\n" + context.getString(R.string.aboutalert_message);

      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.aboutalert_title)
                             .setMessage(aboutMsg)
                            .setPositiveButton(R.string.aboutalert_button_OK,
                                                 new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog, int id) { }
                                                 })
                                .setCancelable(true);

      alertDialogBuilder.create().show();
   }

// *********************************************************************************************************************
// *** Display / Process 1 of 3 Help Screen menu View items                                                          ***
// *********************************************************************************************************************

    void do_help() {

        // Setup Helpbox's checkbox and listener
        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setChecked(pers_helpFlg);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pers_helpFlg = isChecked;
            }
        });

       AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.helpalert_title);
      alertDialogBuilder.setMessage(R.string.helpalert_message1)
                              .setView(checkBoxView)
                        .setNegativeButton(R.string.helpalert_button_MORE,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                                 do_help2();
                                              }
                                           })
                        .setPositiveButton(R.string.helpalert_button_PLAY,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) { }
                                           })
                        .setCancelable(true);

      alertDialogBuilder.create().show();
    }

// *********************************************************************************************************************
// *** Display / Process 2 of 3 Help Screen menu View items                                                          ***
// *********************************************************************************************************************

    void do_help2() {

        // Setup Helpbox's checkbox and listener
        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setChecked(pers_helpFlg);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pers_helpFlg = isChecked;
            }
        });

       AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.helpalert_title);
      alertDialogBuilder.setMessage(R.string.helpalert_message2)
                              .setView(checkBoxView)
                        .setNegativeButton(R.string.helpalert_button_MORE,
                                           new DialogInterface.OnClickListener() {
                                                           public void onClick(DialogInterface dialog, int id) {
                                                   do_help3();
                                              }
                                           })
                        .setPositiveButton(R.string.helpalert_button_PLAY,
                                           new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                               }
                                           })
                        .setCancelable(true);

      alertDialogBuilder.create().show();
    }

// *********************************************************************************************************************
// *** Display / Process 3 of 3 Help Screen menu View items                                                          ***
// *********************************************************************************************************************

   void do_help3() {

        // Setup Helpbox's checkbox and listener
        View checkBoxView = View.inflate(this, R.layout.checkbox, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setChecked(pers_helpFlg);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pers_helpFlg = isChecked;
            }
        });

       AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
      alertDialogBuilder.setTitle(R.string.helpalert_title);
      alertDialogBuilder.setMessage(R.string.helpalert_message3)
                              .setView(checkBoxView)
                              .setPositiveButton(R.string.helpalert_button_PLAY,
                                           new DialogInterface.OnClickListener() {
                                               public void onClick(DialogInterface dialog, int id) { }
                                           })
                        .setCancelable(true);

        alertDialogBuilder.create().show();
     }

// *********************************************************************************************************************
// *** Button Listener / Input Processing / Callback Loop                                                            ***
// *********************************************************************************************************************

   public void addButtonListeners() {
        button0.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
            finish();
            }
        });
        button1.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                processKeyedInput("1");
            }
        });
        button2.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                processKeyedInput("2");
            }
        });
        button3.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                processKeyedInput("3");
            }
        });
        button4.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                processKeyedInput("4");
            }
        });
    }

// *********************************************************************************************************************
// *** Handle Input from loop based on what status we're in / what question we expected to be answered               ***
// *********************************************************************************************************************

    public void processKeyedInput(String choice) {
        log("processKeyedInput() Starts status = " + pers_gameStatus);

        // Process keyboard if game is active
        if (pers_gameStatus != 90) {

          // Print the keyed input field and process it
          switch (pers_gameStatus) {
             case 02:
                  processPlayerChoice(choice);
                  break;
             case 12:
                  processPlayerMoveChoice(choice);
                  break;
             case 22:
                  processPlayerShootChoice(choice);
                  break;
          }
        }
    }

// *********************************************************************************************************************
// *** Ask the Player what he wants to do                                                                              ***
// *********************************************************************************************************************

   public void processPlayerTurn() {

        print("\nMOVE OR SHOOT (M/S) : ");

        button1.setVisibility(View.INVISIBLE);
        button2.setVisibility(View.VISIBLE);
        button2.setBackgroundResource(R.drawable.raygun);
        button3.setVisibility(View.INVISIBLE);
        button4.setBackgroundResource(R.drawable.foot);
        button4.setVisibility(View.VISIBLE);

        pers_gameStatus = 02;
   }

// *********************************************************************************************************************
// *** Do what the Player wants to do                                                                                ***
// *********************************************************************************************************************

   public void processPlayerChoice(String choice) {
        log("processPlayerChoice() Starts status = " + pers_gameStatus + " choice = " + choice);

        button1.setVisibility(View.VISIBLE);
        button1.setBackgroundResource(R.drawable.button_up);
        button2.setVisibility(View.VISIBLE);
        button2.setBackgroundResource(R.drawable.button_right);
        button3.setVisibility(View.VISIBLE);
        button3.setBackgroundResource(R.drawable.button_down);
        button4.setVisibility(View.VISIBLE);
        button4.setBackgroundResource(R.drawable.button_left);

       if (choice == "4") {
          print("M\n");
          getPlayerMoveChoice();
         return;
      }

      if (choice == "2") {
          print("S\n");
          getPlayerShootChoice();
         return;
      }
   }

// *********************************************************************************************************************
// *** Player wants to move, ask which direction                                                                     ***
// *********************************************************************************************************************

   public void getPlayerMoveChoice() {

      print("\nFORWARD, BACKWARD,\n");
      print("RIGHT OR LEFT (F/B/R/L) : ");

        pers_gameStatus = 12;
   }

// *********************************************************************************************************************
// *** See which direction he picked to move                                                                         ***
// *********************************************************************************************************************

    public void processPlayerMoveChoice(String choice) {
        log("processPlayerTurn() Starts");

      if (choice == "1") {
          print("F\n");
          processPlayerMoveDirection(0, -1);
            return;
        }
      if (choice == "3") {
          print("B\n");
          processPlayerMoveDirection(0, 1);
            return;
        }
      if (choice == "2") {
          print("R\n");
          processPlayerMoveDirection(-1, 0);
            return;
      }
      if (choice == "4") {
          print("L\n");
          processPlayerMoveDirection(1, 0);
            return;
        }
    }

// *********************************************************************************************************************
// *** See what happens when we try to move the Player in that direction                                             ***
// *********************************************************************************************************************

    public void processPlayerMoveDirection(int X, int Y) {

        if (moveLeavesMaze(X, Y)) {
            print("\nTHAT WOULD TAKE YOU OUT OF THE MAZE\n");
            print("MOVE NOT ALLOWED\n");
            processTwonkyTurn();
            return;
        }
        if (moveIsBlocked(X, Y)) {
            print("\nTHAT SPACE IS BLOCKED\n");
            print("MOVE NOT ALLOWED\n");
            processTwonkyTurn();
            return;
        }
      if (moveGoesToSuper(X, Y)) {
            print("\nYOU FOUND THE SUPER KILL SQUARE!!!\n");
            print("MOVE ALLOWED BUT,\n");
            print("YOU\'VE BEEN KILLED!!!\n");
            pers_gameStatus = 90;
            return;
        }
        if (moveGoesToRelocation(X, Y)) {
            print("\n..... YOU\'VE BEEN RELOCATED .....\n");
            randomizePlayerLocation();
            processTwonkyTurn();
            return;
        }
        if (moveGoesToObjective(X, Y)) {
            print("\nYOU FOUND THE OBJECTIVE !!!\n");
            print("MOVE ALLOWED AND,\n");
            print("YOU WIN A TRIP OFF THIS PLANET !!!\n");
            pers_gameStatus = 90;
            return;
        }
        if (moveTouchesTwonky(X, Y)) {
            print("MOVE ALLOWED.\n\n");
            displayTwonkyWinsMessage();
            pers_gameStatus = 90;
            return;
        }

        print("MOVE ALLOWED\n");
        pers_MA[pers_XP][pers_YP] = pers_SP;
        pers_XP += X; pers_YP += Y;
        pers_SP = pers_MA[pers_XP][pers_YP];
        pers_MA[pers_XP][pers_YP] = MA_PLAYER;
        processTwonkyTurn();
    }

// *********************************************************************************************************************
// *** Check if move would leave the maze                                                                            ***
// *********************************************************************************************************************

    public Boolean moveLeavesMaze(int X, int Y) {
      X = X + pers_XP; Y = Y + pers_YP;
        return ((X < 0) || (X >= MX) || (Y < 0) || (Y >= MY));
   }

// *********************************************************************************************************************
// *** Check if move is blocked                                                                                      ***
// *********************************************************************************************************************

    public Boolean moveIsBlocked(int X, int Y) {
      X = X + pers_XP; Y = Y + pers_YP;
        return (pers_MA[X][Y] == MA_BLOCKED);
   }

// *********************************************************************************************************************
// *** Check if move is Super instant death                                                                          ***
// *********************************************************************************************************************

    public Boolean moveGoesToSuper(int X, int Y) {
      X = X + pers_XP; Y = Y + pers_YP;
        return(pers_MA[X][Y] == MA_SUPER);
    }

// *********************************************************************************************************************
// *** Check if move relocates us                                                                                    ***
// *********************************************************************************************************************

    public Boolean moveGoesToRelocation(int X, int Y) {
      X = X + pers_XP; Y = Y + pers_YP;
        return (pers_MA[X][Y] == MA_RELOCATION);
   }

// *********************************************************************************************************************
// *** Check if move gets us safely home                                                                             ***
// *********************************************************************************************************************

    public Boolean moveGoesToObjective(int X, int Y) {
      X = X + pers_XP; Y = Y + pers_YP;
        return (pers_MA[X][Y] == MA_OBJECTIVE);
   }

// *********************************************************************************************************************
// *** Check if move has us walking into the Twonky                                                                  ***
// *********************************************************************************************************************

    public Boolean moveTouchesTwonky(int X, int Y) {
      X = X + pers_XP; Y = Y + pers_YP;
        return (pers_MA[X][Y] == MA_TWONKY);
   }

// *********************************************************************************************************************
// *** Player wants to shoot, ask which direction                                                                    ***
// *********************************************************************************************************************

    public void getPlayerShootChoice() {

      print("\nFORWARD, BACKWARD,\n");
      print("RIGHT OR LEFT (F/B/R/L) : ");

       pers_gameStatus = 22;
   }

// *********************************************************************************************************************
// *** See which direction he picked to shoot                                                                         ***
// *********************************************************************************************************************

   public void processPlayerShootChoice(String choice) {

      if (choice == "1") {
          print("F\n");
          processPlayerShootDirection(0, -1);
            return;
        }
      if (choice == "3") {
          print("B\n");
          processPlayerShootDirection(0, 1);
            return;
        }
      if (choice == "2") {
          print("R\n");
          processPlayerShootDirection(-1, 0);
         return;
      }
      if (choice == "4") {
          print("L\n");
          processPlayerShootDirection(1, 0);
         return;
      }

   }

// *********************************************************************************************************************
// *** See what happens when we try to shoot in that direction                                                       ***
// *********************************************************************************************************************

    public void processPlayerShootDirection(int X, int Y) {

        int SX = pers_XP;
        int SY = pers_YP;

        while (true) {
           SX = SX + X;
           SY = SY + Y;
            print("ZAP--");

            if (shotLeavesMaze(SX, SY)) {
                print("FIZZLE...\n");
                print("THE SHOT LEFT THE MAZE.\n");
                print("THE SHOT MISSED THE TWONKY!\n");
                break;
            }

            if (shotIsBlocked(SX, SY)) {
                print("BLAST !!\n");
                print("THE SHOT HIT A WALL\n");
                print("THE SHOT MISSED\n");
                break;
            }

            if (shotTouchesTwonky(SX, SY)) {
                print("\nOUCH !!!\n");
                print("THE SHOT HIT THE TWONKY\n");
                print("THE TWONKY RETREATS\n");
                randomizeTwonkyLocation();
                break;
            }
        }

        processTwonkyTurn();
    }

// *********************************************************************************************************************
// *** Check if move would leave the maze                                                                            ***
// *********************************************************************************************************************

    public Boolean shotLeavesMaze(int X, int Y) {
        return ((X < 0) || (X >= MX) || (Y < 0) || (Y >= MY));
    }

// *********************************************************************************************************************
// *** Check if shot is blocked                                                                                      ***
// *********************************************************************************************************************

    public Boolean shotIsBlocked(int X, int Y) {
        return (pers_MA[X][Y] == MA_BLOCKED);
    }

// *********************************************************************************************************************
// *** Check if shot has us ZAPPING into the Twonky                                                                  ***
// *********************************************************************************************************************

    public Boolean shotTouchesTwonky(int X, int Y) {
        return (pers_MA[X][Y] == MA_TWONKY);
    }

// *********************************************************************************************************************
// *** Pick a random Twonky location on an empty space                                                               ***
// ***    Used to initialize his position, and to move him after being ZAPPED                                        ***
// *********************************************************************************************************************

    public void randomizeTwonkyLocation() {

       log("Randomizing Twonky Location from " + pers_XT + ", " + pers_YT);

        int new_XT;
        int new_YT;
       while (true) {
         new_XT = random.nextInt(MX);
         new_YT = random.nextInt(MY);
         if (pers_MA[new_XT][new_YT] == init_MA) {
            break;
         }
      }

       log("Randomizing Twonky Location to " + new_XT + ", " + new_YT);
        pers_MA[pers_XT][pers_YT] = pers_ST;
        pers_XT = new_XT; pers_YT = new_YT;
        pers_ST = pers_MA[pers_XT][pers_YT];
        pers_MA[pers_XT][pers_YT] = MA_TWONKY;
    }

// *********************************************************************************************************************
// *** Pick a random Player location on an empty space, not with 2 units of the Twonky                               ***
// ***    Used to initialize his position, and to move him after being Relocated                                     ***
// *********************************************************************************************************************

    public void randomizePlayerLocation() {

       log("Randomizing Player Location from " + pers_XP + ", " + pers_YP);

        int new_XP;
        int new_YP;
      while (true) {
         new_XP = random.nextInt(MX);
         new_YP = random.nextInt(MY);
         if ((pers_MA[new_XP][new_YP] == init_MA) &&
              (distanceToTwonkyRelocated(new_XP, new_YP)  >= 2))
             break;
      }

       log("Randomizing Player Location to " + new_XP + ", " + new_YP);
        pers_MA[pers_XP][pers_YP] = pers_SP;
        pers_XP = new_XP; pers_YP = new_YP;
        pers_SP = pers_MA[pers_XP][pers_YP];
        pers_MA[pers_XP][pers_YP] = MA_PLAYER;
    }

// *********************************************************************************************************************
// *** Determine what the Twonky wants to do                                                                         ***
// *********************************************************************************************************************

   public void processTwonkyTurn() {

        displayGameStatus();
        print("\nTHE TWONKY MOVES ...\n");
        if (distanceToTwonky() < 2) {
            displayTwonkyWinsMessage();
           pers_gameStatus = 90;
           return;
        }

        if (pers_XP < pers_XT)
            processTwonkyMoveDirection(-1, 0);
        else
            if (pers_XP > pers_XT)
                processTwonkyMoveDirection(1, 0);
            else
                if (pers_YP < pers_YT)
                    processTwonkyMoveDirection(0, -1);
                else
                    if (pers_YP > pers_YT)
                        processTwonkyMoveDirection(0, 1);

        displayGameStatus();
        if (distanceToTwonky() < 2) {
            displayTwonkyWinsMessage();
           pers_gameStatus = 90;
           return;
        }

        processPlayerTurn();
   }

// *********************************************************************************************************************
// *** See what happens when we try to move the Player in that direction                                             ***
// *********************************************************************************************************************

   public void processTwonkyMoveDirection(int X, int Y) {
        pers_MA[pers_XT][pers_YT] = pers_ST;
        pers_XT += X; pers_YT += Y;
        pers_ST = pers_MA[pers_XT][pers_YT];
        pers_MA[pers_XT][pers_YT] = MA_TWONKY;
    }

// *********************************************************************************************************************
// *** Set our Player into zombie zone ... all input will be ignored until the he hardware exits                     ***
// *********************************************************************************************************************

   public void displayTwonkyWinsMessage() {

        button0.setVisibility(View.VISIBLE);
        button0.setBackgroundResource(R.drawable.button);

        button1.setVisibility(View.INVISIBLE);
        button2.setVisibility(View.INVISIBLE);
        button3.setVisibility(View.INVISIBLE);
        button4.setVisibility(View.INVISIBLE);

        print("\n<<<<< S C H L O O R P ! ! ! >>>>>\n");
        print("YOU\'VE BEEN ABSORBED BY THE TWONKY !!!\n");
        print("YOU LOSE.");
    }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onRestart%28%29                              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onRestart() {
       super.onRestart();
      log("onRestart()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onStart%28%29                                ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onStart() {
      super.onStart();
      log("onStart()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// ***  http://developer.android.com/reference/android/app/Activity.html#onResume%28%29                              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onResume() {
      super.onResume();
      log("onResume()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onPause%28%29                                ***
// ***                                                                                                               ***
// *** Save data that needs to be persistent across swap outs by Android Operating System                            ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   protected void onPause() {

      super.onPause();
      log("onPause()");

      SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();

      // prefs saved every time game paused during play
      editor.putBoolean("pers_helpFlg", pers_helpFlg);

      pers_logView = logView.getText().toString();
      editor.putString("pers_logView", pers_logView);

      editor.putInt("pers_gameStatus",  pers_gameStatus);

      for (int x = 0; x < MX; x++)
         for (int y = 0; y < MY; y++)
           editor.putInt("pers_MA_" + x + "_" + y, pers_MA[x][y]);

      editor.putInt("pers_XO", pers_XO);
      editor.putInt("pers_YO", pers_YO);

      editor.putInt("pers_XT", pers_XT);
      editor.putInt("pers_YT", pers_YT);

      editor.putInt("pers_XP", pers_XP);
      editor.putInt("pers_YP", pers_YP);

      editor.putInt("pers_ST", pers_ST);
      editor.putInt("pers_SP", pers_SP);

      editor.commit();
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onStop%28%29                                 ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onStop() {
      super.onStop();
      log("onStop()");
   }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** http://developer.android.com/reference/android/app/Activity.html#onDestroy%28%29                              ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   public void onDestroy() {

      super.onDestroy();
      log("onDestroy()");

      // If menu exit, toast a silly message
      Toast.makeText(context, R.string.exit_toast, Toast.LENGTH_LONG).show();
   }

// *********************************************************************************************************************
// *** Display Status to screen, internal log, etc                                                                   ***
// *********************************************************************************************************************

    public void displayGameStatus() {

        displayBoardToLog();  // Internal testing

      print("\nTHE TWONKY IS " + String.valueOf(distanceToTwonky()) + " UNITS AWAY\n");
      print("THE OBJECTIVE IS " + String.valueOf(distanceToObjective()) + " UNITS_AWAY\n");
   }

// *********************************************************************************************************************
// *** Determine current distance to the Twonky                                                                      ***
// *********************************************************************************************************************

    public double distanceToTwonky() {
       return Math.sqrt(Math.pow(pers_XT - pers_XP, 2) + Math.pow(pers_YT - pers_YP, 2));
   }

// *********************************************************************************************************************
// *** Determine current distance to the Objective                                                                   ***
// *********************************************************************************************************************

    public double distanceToObjective() {
       return Math.sqrt(Math.pow(pers_XO - pers_XP, 2) + Math.pow(pers_YO - pers_YP, 2));
    }

// *********************************************************************************************************************
// *** Determine distance to Twonky from possible new (relocated) position                                           ***
// *********************************************************************************************************************

    public double distanceToTwonkyRelocated(int X, int Y) {
       return Math.sqrt(Math.pow(pers_XT - X, 2) + Math.pow(pers_YT - Y, 2));
    }

// *********************************************************************************************************************
// *** Display the board to the LOGCAT log for debugging                                                             ***
// *********************************************************************************************************************

    public void displayBoardToLog() {

          for (int x = 0; x < MX; x++) {
            String msg = "TWONKYBOARD ";

            for (int y = 0; y < MY; y++) {
                if (pers_MA[x][y] == init_MA)
                    msg += ". ";
                if (pers_MA[x][y] == MA_BLOCKED)
                    msg += "X ";
                if (pers_MA[x][y] == MA_RELOCATION)
                    msg += "? ";
                if (pers_MA[x][y] == MA_SUPER)
                    msg += "! ";
                if (pers_MA[x][y] == MA_OBJECTIVE)
                    msg += "O ";
                if (pers_MA[x][y] == MA_TWONKY)
                    msg += "T ";
                if (pers_MA[x][y] == MA_PLAYER)
                    msg += "P ";
            }
            log(msg);
        }
        log("\n");
    }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Global Shared function to toast a message if in debug mode                                                    ***
// ***                                                                                                               ***
// *********************************************************************************************************************

    void log(String msg) {
       if (twigDebug)
          Log.w(context.getString(R.string.app_name), context.getString(R.string.app_name) + " " + msg);
    }

// *********************************************************************************************************************
// ***                                                                                                               ***
// *** Print a message to the screen log                                                                             ***
// ***                                                                                                               ***
// *********************************************************************************************************************

   private void print(String msg) {

       logView.append(msg);

      // Force scroll to bottom-most
       scrollView.post(new Runnable() {
          public void run() {
             scrollView.fullScroll(ScrollView.FOCUS_DOWN);
         }
      });

   }

}
