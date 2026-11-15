package fr.mavoixecrite;
import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.tts.Voice;
public class ProbeRunner extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle report=new Bundle();VoiceActivity a=null;try{
        a=(VoiceActivity)startActivitySync(new Intent(getTargetContext(),VoiceActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final VoiceActivity screen=a;long deadline=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<deadline){boolean[] done={false};runOnMainSync(()->done[0]=screen.speech!=null && screen.speech.initialized);if(done[0])break;SystemClock.sleep(100);}
        StringBuilder info=new StringBuilder();runOnMainSync(()->{info.append("Initialisé : ").append(screen.speech.initialized).append("\nMoteur prêt : ").append(screen.speech.ready).append("\nVoix françaises hors ligne : ").append(screen.speech.voices().size()).append('\n');for(Voice v:screen.speech.voices())info.append(v.getName()).append(" · ").append(v.getLocale().toLanguageTag()).append(" · qualité ").append(v.getQuality()).append('\n');});report.putString("stream",info.toString());runOnMainSync(screen::finish);finish(Activity.RESULT_OK,report);
    }catch(Throwable e){if(a!=null){VoiceActivity v=a;runOnMainSync(v::finish);}report.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,report);}}
}
