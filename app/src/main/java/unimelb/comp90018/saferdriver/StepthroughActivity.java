package unimelb.comp90018.saferdriver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ViewFlipper;

public class StepthroughActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private ViewFlipper viewFlipper;
    //Defines an instance of the gesture detector
    GestureDetector detector;
    //Custom Animation
    private Animation enter_left_to_right;
    private Animation exit_left_to_right;

    private Animation enter_right_to_left;
    private Animation exit_right_to_left;
    //Define the minimum distance between two points of gesture
    final int FLIP_DISTANCE=50;

    private ImageButton skip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //Request full screen
        getWindow().requestFeature(Window.FEATURE_NO_TITLE); //The request has no title bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stepthrough);

        detector=new GestureDetector(this);
        //Get ViewFlipper instance
        viewFlipper = (ViewFlipper) this.findViewById(R.id.flipper);
        /** Two animations from left to right */
        enter_left_to_right = AnimationUtils.loadAnimation(this,
                R.anim.enter_left_to_right);
        exit_left_to_right = AnimationUtils.loadAnimation(this,
                R.anim.exit_left_to_right);
        /** Two animations from right to left */
        enter_right_to_left = AnimationUtils.loadAnimation(this,
                R.anim.enter_right_to_left);
        exit_right_to_left = AnimationUtils.loadAnimation(this,
                R.anim.exit_right_to_left);

        skip = (ImageButton) findViewById(R.id.skip_button);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StepthroughActivity.this,StartActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Hand in the touch time on the Activity, GestureDetector processor
        return detector.onTouchEvent(event);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        if (e1.getX()-e2.getX()>FLIP_DISTANCE) {
            showPreviousView();
            return true;
        }

        else if (e2.getX()-e1.getX()>FLIP_DISTANCE) {
            showNextView();
            return true;
        }
        return false;
    }

    private void showNextView() {
        // animation effect
        viewFlipper.setInAnimation(enter_left_to_right);
        viewFlipper.setOutAnimation(enter_left_to_right);
        viewFlipper.showPrevious();
    }

    /**
     * Show the next page
     */
    private void showPreviousView() {
        // animation effect
        viewFlipper.setInAnimation(enter_right_to_left);
        viewFlipper.setOutAnimation(exit_right_to_left);
        viewFlipper.showNext();
    }

}
