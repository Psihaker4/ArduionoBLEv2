package com.example.psycho.arduionoble;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;


public class ControlCircleLayout extends FrameLayout {

    private final static String TAG = "mine";

    private OnCommandListener onCommandListener;

    private ImageView controlCircle;

    public ControlCircleLayout(@NonNull Context context) {
        super(context);
    }

    public ControlCircleLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setupLayout(){
        controlCircle = (ImageView) findViewById(R.id.control_circle);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int xCommand = 0;
        int yCommand = 0;
        int command = 0;

        float x = event.getX();
        float y = event.getY();

        float r;
        float d = controlCircle.getWidth()/2;
        float maxR = getWidth()/2;
        float angle;

        float xC = x - maxR;
        float yC = y - maxR;

        r = (float) Math.sqrt(xC * xC + yC * yC);

        angle = (float) Math.toDegrees(Math.atan(yC / xC));

        angle = xC < 0 ? -180 + angle: yC > 0 ? angle -360: angle;

        if (r > getWidth() / 2-d) {
            r = getWidth()/2-d;
            x = (float) (r*Math.cos(Math.toRadians(angle)))+getWidth()/2;
            y = (float) (r*Math.sin(Math.toRadians(angle)))+getHeight()/2;

        }
        if(r>maxR*0.25) {
            if (-angle < 1.5 * 45 || -angle > 6.5 * 45) {
                command = 4;
            } else if (-angle > 2.5 * 45 && -angle < 5.5 * 45) {
                command = 3;
            } else if (-angle > 0.5 * 45 && -angle < 3.5 * 45) {
                command = 1;
            } else if (-angle > 4.5 * 45 && -angle < 7.5 * 45) {
                command = 2;
            }
        } else {
            command = 0;
        }

        if(event.getAction()==MotionEvent.ACTION_DOWN){
            controlCircle.setImageDrawable(getResources().getDrawable(R.drawable.control_circle_solid));
        } else if(event.getAction()==MotionEvent.ACTION_MOVE) {
            controlCircle.animate()
                    .x(x - d)
                    .y(y - d)
                    .setDuration(0)
                    .start();
        } else if(event.getAction() == MotionEvent.ACTION_UP){
            command = 0;
            controlCircle.setImageDrawable(getResources().getDrawable(R.drawable.control_circle));
            controlCircle.animate()
                    .x(maxR-d)
                    .y(maxR-d)
                    .setDuration(100)
                    .start();
        }

        Log.d(TAG, "onTouchEvent: "+command);
        if(event.getAction()!=MotionEvent.ACTION_DOWN) {
            onCommandListener.onChange(command);
        }

        return true;
    }

    interface OnCommandListener {
        void onChange(int d);
    }

    public void setOnCommandListener(OnCommandListener onCommandListener) {
        this.onCommandListener = onCommandListener;
    }
}

