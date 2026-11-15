package fr.mavoixecrite;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.speech.tts.Voice;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,35})
public class AppTest {
    Context context;
    @Before public void reset() {context=RuntimeEnvironment.getApplication();for(String name:new String[]{"phrases-v2.json","phrases-v2.json.bak","phrases-v2.json.new","mavoix-data-v3.json","mavoix-data-v3.json.new"})new File(context.getFilesDir(),name).delete();for(String name:new String[]{"MainActivity","draft","voice","conversation","options"})context.getSharedPreferences(name,0).edit().clear().commit();context.getSharedPreferences("options",0).edit().putBoolean("correct",false).commit();}
    @Test public void accentAndCaseMatch(){assertEquals(2,PhraseStore.match("eCO","École demain"));}
    @Test public void typoMatches(){assertEquals(1,PhraseStore.match("bonjpur","Bonjour à tous"));}
    @Test public void shortInputDoesNotFuzz(){assertEquals(0,PhraseStore.match("bo","Non merci"));}
    @Test public void unrelatedInputNotSuggested(){assertEquals(0,PhraseStore.match("garage","Bonjour"));}
    @Test public void failedWritePreservesPreviousDocument()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Conserver",1,0);String before=s.exportJson();File blocked=new File(context.getFilesDir(),DataFile.NAME+".new");assertTrue(blocked.mkdir());try{assertThrows(IOException.class,()->s.record("Échec",2,0));assertEquals(before,s.exportJson());assertEquals(before,new PhraseStore(context).exportJson());}finally{blocked.delete();}}
    @Test public void learnerPersists()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Merci",1,10);s.record("Merci",2,10);assertEquals(2,new PhraseStore(context).history().get(0).count);}
    @Test public void habitsRank()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Bonjour",1,8);s.record("Bonsoir",2,20);assertEquals("Bonjour",s.suggestions("",8).get(0).text);assertEquals("Bonsoir",s.suggestions("",20).get(0).text);}
    @Test public void exactPrefixBeforeFuzzy()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Bonjour",1,8);for(int i=0;i<5;i++)s.record("Bonheur",i,8);assertEquals("Bonjour",s.suggestions("bonj",8).get(0).text);}
    @Test public void historyIsRecent()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Un",1,8);s.record("Deux",2,8);assertEquals("Deux",s.history().get(0).text);}
    @Test public void deletePersists()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Privé",1,8);s.delete("Privé");assertTrue(new PhraseStore(context).history().isEmpty());}
    @Test public void backupRoundTrip()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Ça va ?\nOui !",4,14);String backup=s.exportJson();s.delete("Ça va ?\nOui !");s.restore(backup);assertEquals("Ça va ?\nOui !",new PhraseStore(context).history().get(0).text);assertEquals(1,s.history().get(0).hours[14]);}
    @Test public void invalidRestorePreserves()throws Exception{PhraseStore s=new PhraseStore(context);s.record("À conserver",1,8);String before=s.exportJson();assertThrows(IOException.class,()->s.restore("{}"));assertEquals(before,new PhraseStore(context).exportJson());}
    @Test public void duplicateImportRejected()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Oui",1,8);org.json.JSONObject root=new org.json.JSONObject(s.exportJson());org.json.JSONArray a=root.getJSONArray("phrases");a.put(a.getJSONObject(0));assertThrows(IOException.class,()->s.restore(root.toString()));}
    @Test public void invalidCountersRejected()throws Exception{PhraseStore s=new PhraseStore(context);s.record("Oui",1,8);org.json.JSONObject root=new org.json.JSONObject(s.exportJson());root.getJSONArray("phrases").getJSONObject(0).put("count",-1);assertThrows(IOException.class,()->s.restore(root.toString()));}
    @Test public void oversizedBackupRejected(){byte[] b=new byte[PhraseStore.MAX_BYTES+1];assertThrows(IOException.class,()->PhraseStore.readBounded(new ByteArrayInputStream(b)));}
    @Test public void blankAndLongMessagesRejected()throws Exception{PhraseStore s=new PhraseStore(context);assertThrows(IOException.class,()->s.record("  ",1,0));assertThrows(IOException.class,()->s.record(String.join("",Collections.nCopies(2001,"a")),1,0));assertTrue(s.history().isEmpty());}
    @Test public void migrationPreservesOldCounts()throws Exception{context.getSharedPreferences("MainActivity",0).edit().putString("phrases_v1","{\"Merci\":9}").commit();PhraseStore s=new PhraseStore(context);assertEquals(9,s.history().get(0).count);s.record("Merci",5,1);assertEquals(10,new PhraseStore(context).history().get(0).count);}
    @Test public void corruptionIsNotSilentlyOverwritten()throws Exception{File file=new File(context.getFilesDir(),"phrases-v2.json");try(FileOutputStream out=new FileOutputStream(file)){out.write("broken".getBytes(StandardCharsets.UTF_8));}assertThrows(IOException.class,()->new PhraseStore(context));assertEquals("broken",new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));}
    @Test public void suggestionsAreBounded()throws Exception{PhraseStore s=new PhraseStore(context);for(int i=0;i<8;i++)s.record("Phrase "+i,i,0);assertEquals(5,s.suggestions("",0).size());}
    @Test public void onlyOfflineFrenchVoiceAllowed(){assertTrue(Speech.usable(voice(Locale.FRANCE,false,Collections.emptySet())));assertFalse(Speech.usable(voice(Locale.FRANCE,true,Collections.emptySet())));assertFalse(Speech.usable(voice(Locale.US,false,Collections.emptySet())));assertFalse(Speech.usable(voice(Locale.FRANCE,false,Collections.singleton("notInstalled"))));}
    Voice voice(Locale locale,boolean network,Set<String> features){return new Voice("test",locale,400,200,network,features);}
    static class FakeSpeech extends Speech {
        String spoken;Runnable whenStarted;
        FakeSpeech(Activity a){super(a,()->{},false);}
        @Override void say(String text,Runnable started){spoken=text;whenStarted=started;}
    }
    @Test public void quickPhraseSpeaksWithoutChangingPage()throws Exception{try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();a.speech.close();FakeSpeech fake=new FakeSpeech(a);a.speech=fake;find(a.getWindow().getDecorView(),"Oui").performClick();assertEquals("Oui",fake.spoken);assertNull(Shadows.shadowOf(a).getNextStartedActivity());assertTrue(new PhraseStore(context).history().isEmpty());fake.whenStarted.run();assertEquals("Oui",new PhraseStore(context).history().get(0).text);}}
    @Test public void speakKeepsDraftAndScreen()throws Exception{try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();a.speech.close();FakeSpeech fake=new FakeSpeech(a);a.speech=fake;EditText input=a.findViewById(R.id.message_input);input.setText("Bonjour, ça va ?");find(a.getWindow().getDecorView(),"Parler").performClick();assertEquals("Bonjour, ça va ?",fake.spoken);assertEquals("Bonjour, ça va ?",input.getText().toString());assertNull(Shadows.shadowOf(a).getNextStartedActivity());}}
    @Test public void blankDraftCannotSpeak(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){Button speak=c.get().findViewById(R.id.speak_message);assertFalse(speak.isEnabled());((EditText)c.get().findViewById(R.id.message_input)).setText("  ");assertFalse(speak.isEnabled());((EditText)c.get().findViewById(R.id.message_input)).setText("Merci");assertTrue(speak.isEnabled());}}
    @Test public void keyboardActionSpeaksWithoutLosingFocus(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();a.speech.close();FakeSpeech fake=new FakeSpeech(a);a.speech=fake;EditText input=a.findViewById(R.id.message_input);input.requestFocus();input.setText("Merci");android.view.inputmethod.EditorInfo info=new android.view.inputmethod.EditorInfo();input.onCreateInputConnection(info);assertEquals(android.view.inputmethod.EditorInfo.IME_ACTION_SEND,info.imeOptions&android.view.inputmethod.EditorInfo.IME_MASK_ACTION);assertEquals("Parler",info.actionLabel);input.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);assertEquals("Merci",fake.spoken);assertTrue(input.hasFocus());}}
    @Test public void needsCategorySpeaksDirectly(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();a.speech.close();FakeSpeech fake=new FakeSpeech(a);a.speech=fake;find(a.getWindow().getDecorView(),"Besoins").performClick();find(a.getWindow().getDecorView(),"J’ai soif").performClick();assertEquals("J’ai soif",fake.spoken);assertNotNull(a.findViewById(R.id.message_input));}}
    @Test public void draftSurvivesRecreation(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){((EditText)c.get().findViewById(R.id.message_input)).setText("Mon message en cours");c.recreate();assertEquals("Mon message en cours",((EditText)c.get().findViewById(R.id.message_input)).getText().toString());}}
    @Test public void displayedMessageLearnedOnceOnRotation()throws Exception{Intent i=new Intent(context,DisplayActivity.class).putExtra("message","Merci beaucoup");try(ActivityController<DisplayActivity> c=Robolectric.buildActivity(DisplayActivity.class,i).setup()){assertEquals(1,new PhraseStore(context).history().get(0).count);find(c.get().getWindow().getDecorView(),"Retourner").performClick();c.recreate();assertEquals(1,new PhraseStore(context).history().get(0).count);assertEquals("Merci beaucoup",((TextView)c.get().findViewById(R.id.display_message)).getText().toString());assertEquals(180f,((View)c.get().findViewById(R.id.display_message).getParent()).getRotation(),0f);}}
    @Test public void voiceScreenWithoutEngineDoesNotCrash(){try(ActivityController<VoiceActivity> c=Robolectric.buildActivity(VoiceActivity.class).setup()){assertNotNull(find(c.get().getWindow().getDecorView(),"Retour"));}}
    static Button find(View view,String text){if(view instanceof Button && (((Button)view).getText().toString().equals(text)||("Compléter : "+text).contentEquals(view.getContentDescription()==null?"":view.getContentDescription())))return (Button)view;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){Button b=find(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
}






