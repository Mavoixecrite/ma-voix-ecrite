package fr.mavoixecrite;

import android.content.Context;
import android.view.*;
import android.widget.ScrollView;
import java.util.function.IntConsumer;

/** Horizontal navigation belongs only to the phrase area, never the keyboard/composer. */
final class FamilyScrollView extends ScrollView {
    private final IntConsumer change;
    private final float threshold,slop;
    private float startX,startY;
    private boolean horizontal,vertical;

    FamilyScrollView(Context context,IntConsumer change){super(context);this.change=change;float density=getResources().getDisplayMetrics().density;threshold=56*density;slop=Math.max(24*density,ViewConfiguration.get(context).getScaledTouchSlop()*2);}
    private void track(MotionEvent event){
        if(event.getPointerCount()>1){horizontal=false;vertical=true;return;}
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){startX=event.getX();startY=event.getY();horizontal=false;vertical=false;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&!horizontal&&!vertical){float dx=Math.abs(event.getX()-startX),dy=Math.abs(event.getY()-startY);if(dy>slop&&dy>dx)vertical=true;else if(dx>slop&&dx>dy*1.5f){horizontal=true;MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();}}
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){track(event);return horizontal||super.onInterceptTouchEvent(event);}
    @Override public boolean onTouchEvent(MotionEvent event){
        track(event);
        if(horizontal){if(event.getActionMasked()==MotionEvent.ACTION_UP){float dx=event.getX()-startX,dy=event.getY()-startY;if(Math.abs(dx)>=threshold&&Math.abs(dx)>Math.abs(dy)*1.5f)change.accept(dx<0?1:-1);horizontal=false;}else if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)horizontal=false;return true;}
        return super.onTouchEvent(event);
    }
}
