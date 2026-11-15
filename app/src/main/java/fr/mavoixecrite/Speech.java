package fr.mavoixecrite;
import android.app.Activity;
import android.speech.tts.*;
import android.widget.Toast;
import java.util.*;
import java.util.function.Consumer;

/** Lifecycle-owned speech. Only explicit requests are queued; the latest tap replaces the last. */
class Speech {
    final Activity activity; TextToSpeech tts; boolean ready,closed,initialized;
    private Consumer<String> feedback;
    private Runnable idleListener;
    private Request pending,active;
    private long sequence;
    static final class Request { final String text,id;final Runnable started;boolean notified;Request(String t,String i,Runnable r){text=t;id=i;started=r;} }
    Speech(Activity a,Runnable onReady){this(a,onReady,true);}
    Speech(Activity a,Runnable onReady,boolean initialize){activity=a;if(!initialize)return;
        String engine=a.getSharedPreferences("voice",0).getString("engine",null);
        tts=new TextToSpeech(a,status->{if(closed)return;initialized=true;ready=status==TextToSpeech.SUCCESS;
            if(ready){List<Voice> voices=voices();String chosen=a.getSharedPreferences("voice",0).getString("name","");
                Voice selected=chooseVoice(voices,chosen);
                if(selected!=null && tts.setVoice(selected)==TextToSpeech.SUCCESS)a.getSharedPreferences("voice",0).edit().putString("name",selected.getName()).apply();
                tts.setSpeechRate(a.getSharedPreferences("voice",0).getFloat("rate",1f));tts.setPitch(a.getSharedPreferences("voice",0).getFloat("pitch",1f));
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    public void onStart(String id){a.runOnUiThread(()->{if(closed || active==null || !active.id.equals(id))return;Request r=active;if(!r.notified){r.notified=true;r.started.run();}report("Je dis : « "+r.text+" »",false);});}
                    public void onDone(String id){a.runOnUiThread(()->{if(!closed && active!=null && active.id.equals(id)){String text=active.text;active=null;report("« "+text+" »",false);if(idleListener!=null)idleListener.run();}});}
                    public void onError(String id){a.runOnUiThread(()->{if(!closed && active!=null && active.id.equals(id)){active=null;report("La lecture a échoué. Vérifiez la voix et le volume multimédia.",true);if(idleListener!=null)idleListener.run();}});}
                });
            }onReady.run();if(pending!=null){Request r=pending;pending=null;play(r);}
        },engine);
    }
    void setFeedback(Consumer<String> observer){feedback=observer;}
    void setOnIdle(Runnable listener){idleListener=listener;}
    boolean speaking(){return active!=null || pending!=null;}
    static Voice chooseVoice(List<Voice> voices,String preferred){return voices.stream().filter(v->v.getName().equals(preferred)).findFirst().orElse(voices.isEmpty()?null:voices.get(0));}
    private void report(String text,boolean error){if(feedback!=null)feedback.accept(text);if(error)Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    static boolean usable(Voice v){return v.getLocale().getLanguage().equals("fr") && !v.isNetworkConnectionRequired() && (v.getFeatures()==null || !v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED));}
    List<Voice> voices(){List<Voice> list=new ArrayList<>();if(tts!=null && tts.getVoices()!=null)for(Voice v:tts.getVoices())if(usable(v))list.add(v);
        list.sort(Comparator.comparing((Voice v)->v.getName().endsWith("-language")).thenComparing(Comparator.comparingInt(Voice::getQuality).reversed()).thenComparing(v->!v.getLocale().getCountry().equals("FR")).thenComparing(Voice::getName));return list;}
    boolean available(){
        if(!ready || tts==null)return false;
        if(tts.getVoice()!=null && usable(tts.getVoice()))return true;
        List<Voice> v=voices();
        if(!v.isEmpty() && tts.setVoice(v.get(0))==TextToSpeech.SUCCESS)return true;
        return false;
    }
    void say(String text){say(text,()->{});}
    void say(String text,Runnable started){
        if(closed)return;String value=text.trim();if(value.isEmpty())return;
        Request r=new Request(value,"message-"+(++sequence),started);
        if(!initialized){pending=r;report("Préparation de la voix…",false);return;}play(r);
    }
    private void play(Request r){
        if(!available()){report("Choisissez une voix française dans le menu Voix.",true);if(idleListener!=null)idleListener.run();return;}
        active=r;report("« "+r.text+" »",false);
        if(tts.speak(r.text,TextToSpeech.QUEUE_FLUSH,null,r.id)==TextToSpeech.ERROR){active=null;report("Lecture indisponible. Vérifiez la voix choisie.",true);if(idleListener!=null)idleListener.run();}
    }
    void stop(){boolean wasActive=active!=null||pending!=null;pending=null;active=null;if(tts!=null)tts.stop();report("Lecture arrêtée",false);if(wasActive&&idleListener!=null)idleListener.run();}
    void close(){closed=true;ready=false;pending=null;active=null;if(tts!=null){tts.stop();tts.shutdown();}}
}
