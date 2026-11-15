package fr.mavoixecrite;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.io.*;

/** Non-destructive conversation checks. The original draft is restored on exit. */
public class SmokeRunner extends Instrumentation {
    private final StringBuilder results=new StringBuilder();private int checks;private MainActivity main;private String originalDraft;private String fixtureId;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;results.append("PASS ").append(label).append('\n');}
    void click(String text){runOnMainSync(()->{Button b=find(main.getWindow().getDecorView(),text);if(b==null)throw new AssertionError("Bouton absent : "+text);b.performClick();});waitForIdleSync();}
    void capture(String name)throws Exception{waitForIdleSync();SystemClock.sleep(700);Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new IOException("Pas de capture");File file=new File(getTargetContext().getExternalFilesDir(null),name+".png");try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    int count(String text)throws Exception{for(PhraseStore.Phrase p:new PhraseStore(getTargetContext()).history())if(p.text.equals(text))return p.count;return 0;}
    void awaitSpeech(String text,int before)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){if(count(text)>before)return;SystemClock.sleep(100);}throw new AssertionError("La lecture n’a pas commencé : "+text);}
    boolean ime(){if(Build.VERSION.SDK_INT<30)return false;WindowInsets i=main.getWindow().getDecorView().getRootWindowInsets();return i!=null && i.isVisible(WindowInsets.Type.ime());}
    boolean speakFullyVisible(){boolean[] ok={false};runOnMainSync(()->{View b=main.findViewById(R.id.speak_message);Rect r=new Rect();ok[0]=b.getGlobalVisibleRect(r)&&r.height()==b.getHeight()&&r.width()==b.getWidth();});return ok[0];}
    @Override public void onStart(){Bundle report=new Bundle();int result=Activity.RESULT_OK;try{
        fixtureId="smoke-"+java.util.UUID.randomUUID();CardStore.Card fixture=new CardStore.Card(fixtureId,"Discussion","Bonjour, je m’appelle Camille.","chat");fixture.favorite=true;new CardStore(getTargetContext()).save(fixture);
        main=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));waitForIdleSync();EditText input=main.findViewById(R.id.message_input);originalDraft=input.getText().toString();runOnMainSync(()->input.setText(""));
        check(find(main.getWindow().getDecorView(),"Oui")!=null,"phrases essentielles accessibles");check(!main.findViewById(R.id.speak_message).isEnabled(),"Parler désactivé sans texte");
        for(String family:new String[]{"Essentiel","Besoins","Discussion"}){Button b=find(main.getWindow().getDecorView(),family);check(b!=null && b.getPaint().measureText(family)<=b.getWidth()-b.getPaddingLeft()-b.getPaddingRight()+1,"famille lisible sans coupure : "+family);}
        long end=SystemClock.uptimeMillis()+15000;while(SystemClock.uptimeMillis()<end && !main.speech.initialized)SystemClock.sleep(100);
        check(main.speech.initialized,"initialisation du service vocal terminée");boolean available=main.speech.available();results.append("VOIX_FR_HORS_LIGNE ").append(main.speech.voices().size()).append('\n');
        if(available){int before=count("Bonjour");click("Bonjour");awaitSpeech("Bonjour",before);check(main.findViewById(R.id.message_input)!=null,"case parlante sans changer de page");check(count("Bonjour")==before+1,"une lecture commencée mémorisée une fois");}else results.append("NON_TESTÉ : synthèse réelle, aucune voix française prête\n");
        check(find(main.getWindow().getDecorView(),"Bonjour, je m’appelle Camille.")!=null,"favori personnel visible à l’accueil");capture("refonte-01-accueil");runOnMainSync(()->{input.setText("bobjour ");input.requestFocus();((InputMethodManager)main.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});waitForIdleSync();
        end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end && !input.getText().toString().equals("bonjour "))SystemClock.sleep(100);check(input.getText().toString().equals("bonjour "),"correction bobjour vers bonjour hors ligne");SystemClock.sleep(700);check(speakFullyVisible(),"Parler entièrement visible après correction");capture("refonte-04-correction");click("Annuler la correction");check(input.getText().toString().equals("bobjour "),"annulation de la correction");runOnMainSync(()->input.setText("Bonjour, merci de patienter."));
        end=SystemClock.uptimeMillis()+6000;while(SystemClock.uptimeMillis()<end && !ime())SystemClock.sleep(100);
        if(Build.VERSION.SDK_INT>=30)check(ime(),"clavier Android ouvert");
        boolean[] visible={false};runOnMainSync(()->{Rect r=new Rect();View b=main.findViewById(R.id.speak_message);WindowInsets i=main.getWindow().getDecorView().getRootWindowInsets();int keyboard=Build.VERSION.SDK_INT>=30?i.getInsets(WindowInsets.Type.ime()).bottom:0;visible[0]=b.getGlobalVisibleRect(r)&&r.height()==b.getHeight()&&r.width()==b.getWidth()&&r.bottom<=main.getWindow().getDecorView().getHeight()-keyboard;});check(visible[0],"bouton Parler visible au-dessus du clavier");
        EditorInfo info=new EditorInfo();runOnMainSync(()->input.onCreateInputConnection(info));check((info.imeOptions&EditorInfo.IME_MASK_ACTION)==EditorInfo.IME_ACTION_SEND && "Parler".contentEquals(info.actionLabel),"action Parler transmise au clavier");
        if(available){int before=count("Bonjour, merci de patienter.");runOnMainSync(()->input.onEditorAction(EditorInfo.IME_ACTION_SEND));awaitSpeech("Bonjour, merci de patienter.",before);check(input.getText().toString().equals("Bonjour, merci de patienter."),"message conservé après lecture");check(ime(),"clavier conservé après lecture");}
        capture("refonte-02-clavier");runOnMainSync(()->((InputMethodManager)main.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0));waitForIdleSync();SystemClock.sleep(500);click("Besoins");check(find(main.getWindow().getDecorView(),"J’ai soif")!=null,"phrases de besoin accessibles");check(find(main.getWindow().getDecorView(),"Bonjour, je m’appelle Camille.")!=null,"favori conservé dans une autre famille");check(find(main.getWindow().getDecorView(),"Je voudrais …")!=null,"phrase à compléter accessible");capture("refonte-03-besoins");click("＋ Ajouter une case");capture("refonte-05-personnaliser");android.view.accessibility.AccessibilityNodeInfo tree=getUiAutomation().getRootInActiveWindow();for(android.view.accessibility.AccessibilityNodeInfo n:tree.findAccessibilityNodeInfosByText("Annuler"))if("Annuler".contentEquals(n.getText())){n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);break;}waitForIdleSync();click("Essentiel");
        check(getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions==null,"aucune permission réseau ou microphone");
        DisplayActivity display=(DisplayActivity)startActivitySync(new Intent(getTargetContext(),DisplayActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK).putExtra("message",CardStore.DEFAULT_PRESENTATION).putExtra("interlocutor",true));waitForIdleSync();check(find(display.getWindow().getDecorView(),"Oui")!=null && find(display.getWindow().getDecorView(),"Non")!=null,"réponses Oui et Non du mode interlocuteur");capture("refonte-06-interlocuteur");runOnMainSync(display::finishAndRemoveTask);waitForIdleSync();
    }catch(Throwable error){result=Activity.RESULT_CANCELED;results.append("FAIL ").append(android.util.Log.getStackTraceString(error));}
    finally{if(main!=null){runOnMainSync(()->{EditText input=main.findViewById(R.id.message_input);if(input!=null && originalDraft!=null)input.setText(originalDraft);main.finishAndRemoveTask();});waitForIdleSync();}if(fixtureId!=null)try{new CardStore(getTargetContext()).delete(fixtureId);}catch(IOException e){results.append("Nettoyage de la case de test incomplet\n");}}
    report.putString("stream","\n"+results+"\n"+checks+" vérifications réussies\n");finish(result,report);}
    static Button find(View v,String s){if(v instanceof Button && (((Button)v).getText().toString().equals(s)||("Compléter : "+s).contentEquals(v.getContentDescription()==null?"":v.getContentDescription())))return (Button)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=find(g.getChildAt(i),s);if(b!=null)return b;}}return null;}
}


