package fr.mavoixecrite;
import android.content.Context;import android.speech.tts.Voice;import android.os.Looper;import android.widget.EditText;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.android.controller.ActivityController;import org.robolectric.annotation.Config;
import java.util.*;import java.io.*;import java.time.Duration;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk={26,35})
public class UsabilityTest {
 Context c;
 @Before public void reset(){c=RuntimeEnvironment.getApplication();for(String n:new String[]{DataFile.NAME,DataFile.NAME+".new","phrases-v2.json"})new File(c.getFilesDir(),n).delete();for(String n:new String[]{"draft","options","voice","MainActivity","conversation"})c.getSharedPreferences(n,0).edit().clear().commit();c.getSharedPreferences("options",0).edit().putBoolean("correct",false).commit();}
 Voice voice(String name){return new Voice(name,Locale.FRANCE,400,200,false,Collections.emptySet());}
 @Test public void missingPreviousVoiceFallsBack(){Voice a=voice("installed");assertSame(a,Speech.chooseVoice(Arrays.asList(a,voice("other")),"removed"));}
 @Test public void existingPreferredVoiceIsKept(){Voice a=voice("first"),b=voice("chosen");assertSame(b,Speech.chooseVoice(Arrays.asList(a,b),"chosen"));}
 @Test public void noVoiceIsNotInvented(){assertNull(Speech.chooseVoice(Collections.emptyList(),"removed"));}
 @Test public void draftCheckpointBeforePause(){try(ActivityController<MainActivity> ctl=Robolectric.buildActivity(MainActivity.class).setup()){EditText input=ctl.get().findViewById(R.id.message_input);input.setText("Mon brouillon est conservé.");Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));assertEquals("Mon brouillon est conservé.",c.getSharedPreferences("draft",0).getString("text",""));}}
 @Test public void lastFamilyIsRemembered(){try(ActivityController<MainActivity> ctl=Robolectric.buildActivity(MainActivity.class).setup()){AppTest.find(ctl.get().getWindow().getDecorView(),"Besoins").performClick();}try(ActivityController<MainActivity> next=Robolectric.buildActivity(MainActivity.class).setup()){assertNotNull(AppTest.find(next.get().getWindow().getDecorView(),"J’ai soif"));}}
}
