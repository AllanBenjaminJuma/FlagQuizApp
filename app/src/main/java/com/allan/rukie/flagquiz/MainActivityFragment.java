package com.allan.rukie.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList;
    private List<String>quizCountriesList;
    private Set<String> regionsSet;
    private String correctAnswer;
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows;
    private SecureRandom random;
    private Handler handler;
    private Animation shakeAnimation;

    private LinearLayout quizLinearLayout;
    private TextView questionNumberTextView;
    private ImageView flagImageView;
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;




    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       // return inflater.inflate(R.layout.fragment_main, container, false);
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList= new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView)view.findViewById(R.id.questionNumberTextview);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);

        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        for(LinearLayout row : guessLinearLayouts){
            for(int column = 0; column < row.getChildCount(); column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }
            questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));
                return view;
    }
    public void updateGuessRows(SharedPreferences sharedPreferences){

        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);

        guessRows = Integer.parseInt(choices) / 2;

        for(LinearLayout layout : guessLinearLayouts)layout.setVisibility(View.GONE);

        //display appropriate guess button LinearLayouts
        for(int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }
    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public  void resetQuiz(){

        //use assetManager to get image files for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try {
            //loop through each region
            for(String region : regionsSet){
                //get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for(String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }
        catch (IOException exception){
            Log.e(TAG, "Error loading image file names", exception);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //add FLAGS_IN_QUIZ random file names to the quizCountriesList
        while(flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            //get the random file name
            String filename = fileNameList.get(randomIndex);

            //if the region is enabled and it hasn't already been chosen
            if(!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);
                ++flagCounter;


            }
        }
        loadNextFlag();
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void loadNextFlag(){

        //get filename of next flag and remove it from the list

        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;  //update correct answer
        answerTextView.setText(""); //clear answerTextView

        //display current question number

        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //extract region from next images's name

        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //use AssetManeger to load next image from assets folder

        AssetManager assets = getActivity().getAssets();

        //get an InputStream to the asset representing the next flag and try to use the InputStream

        try{
            InputStream stream = assets.open(region + "/" + nextImage + ".png");

            //load the asset as drawable and and display on the flagImageView

            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false);
        }
        catch (IOException exception){

            Log.e(TAG, "Error loading" + nextImage, exception);
        }
        Collections.shuffle(fileNameList);

        //put the correct answer at the end of fileNameList

        int correct = fileNameList.indexOf(correctAnswer);

        fileNameList.add(fileNameList.remove(correct));

        //add 2,4, 6 or 8 guess buttons based on value of guessRows

        for(int row = 0; row < guessRows; row++){

            //place buttons in currentTableRow

            for(int column = 0;
                    column < guessLinearLayouts[row].getChildCount();column++){

                //get reference to button to configure

                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //get country name and set it as newGuessButton's text

                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //randomly replace one button with correct answer

        int row = random.nextInt(guessRows);    //pick random row
        int column = random.nextInt(2); //pick random column
        LinearLayout randomRow = guessLinearLayouts[row];   //get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private String getCountryName(String name) {

        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void animate(boolean animateOut) {
        //prevent animation into the the UI for the first flag
        if(correctAnswers == 0)
            return;

        //calculate centre x and centre y
        int centreX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;

        int centreY = (quizLinearLayout.getTop()) + quizLinearLayout.getBottom()/ 2;

        //calculate animation radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        //if the quizLinearLayout should animate out rather in
        if(animateOut){
            //create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centreX, centreY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter(){
                //called when type animation finishes
               @Override
                public void onAnimationEnd(Animator animation){

                    loadNextFlag();
                }
            });
        }else{//if the quizLinearLayout should animate in
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centreX, centreY, 0, radius);
        }
        animator.setDuration(500);
        animator.start();
    }

    public View.OnClickListener guessButtonListener = new View.OnClickListener() { //called when a guess button is touched
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {

            Button guessButton = ((Button)v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses; //increment number of guesses the user has made

            if(guess.equals(answer)){

                ++correctAnswers;

                //display the correct answer in green text

                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));

                disableButtons(); //disable all guess buttons

                //if user correctly identified FLAGS_IN_QUIZ

                if(correctAnswers == FLAGS_IN_QUIZ){

                    //dialogFragment to display quiz stats and new quiz starts

                 DialogFragment quizResults = new DialogFragment(){
                        //create an alert dialog and return it

                        @Override
                        public Dialog onCreateDialog(Bundle bundle) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses, (1000/ (double) totalGuesses)));

                            //Reset quiz button

                            builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener(){

                                public void onClick(DialogInterface dialog, int id){
                                    resetQuiz();
                                }
                            }
                            );
                            return  builder.create();
                        }

                    };

                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                }
                else{
                    //answer correct but quiz not over
                    //load next flag after a 2 second delay

                    handler.postDelayed(
                            new Runnable() {
                                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void run() {
                                    animate(true);//animate flag off the screen
                                }
                            },2000);//2000 milliseconds for 2 second delay
                }
            }
            else{
                //incorrect answer

                flagImageView.startAnimation(shakeAnimation);

                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);
            }

        }
    };
    private void disableButtons(){
            for(int row = 0; row < guessRows; row++){

                LinearLayout guessRow = guessLinearLayouts[row];
                for(int i = 0; i<guessRow.getChildCount(); i++)
                    guessRow.getChildAt(i).setEnabled(false);
            }
    }
}
