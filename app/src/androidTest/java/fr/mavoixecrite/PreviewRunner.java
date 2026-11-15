package fr.mavoixecrite;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.io.*;
public class PreviewRunner extends Instrumentation {
    void capture(String name)throws Exception{
        waitForIdleSync();SystemClock.sleep(700);
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        try(var out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle report=new Bundle();Activity a=null;try{
        if(!getTargetContext().getPackageName().endsWith(".preview"))throw new IllegalStateException("Preview package required");
        a=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity screen=a;capture("accueil");
        runOnMainSync(()->{EditText input=screen.findViewById(R.id.message_input);input.setText("Je voudrais ");input.setSelection(input.length());input.requestFocus();((InputMethodManager)screen.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});
        waitForIdleSync();SystemClock.sleep(1000);capture("ecriture");
        report.putString("stream","Captures preview OK");finish(Activity.RESULT_OK,report);
    }catch(Throwable error){report.putString("stream",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,report);}finally{if(a!=null){Activity screen=a;runOnMainSync(screen::finish);}}}
}
