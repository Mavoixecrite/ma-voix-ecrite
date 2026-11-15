package fr.mavoixecrite;
import android.app.*;import android.content.*;import android.os.Bundle;import android.speech.tts.*;import android.view.*;import android.widget.*;import java.util.*;
public class VoiceActivity extends Activity {
    Speech speech;private LinearLayout body;private ScrollView scroll;private boolean compare;private View comparisonAnchor;
    @Override public void onCreate(Bundle state){super.onCreate(state);compare=state!=null&&state.getBoolean("compare");LinearLayout root=Ui.root(this);Ui.text(this,root,"Une voix qui vous ressemble",26);Ui.text(this,root,"Choisissez un style, un accent ou réglez votre timbre en un appui.",17);scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));Ui.text(this,body,"Recherche des voix françaises…",18);Ui.button(this,root,"Retour",()->finish());setContentView(root);}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("compare",compare);super.onSaveInstanceState(state);}
    @Override protected void onResume(){super.onResume();speech=new Speech(this,()->{if(!isFinishing())render();});}
    private void settings(){try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}catch(ActivityNotFoundException e){Ui.error(this,"Ouvrez les paramètres de la tablette, puis Synthèse vocale.");}}
    private void installVoicePacks(){try{startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));}catch(ActivityNotFoundException e){settings();}}
    private Voice chosen(){return Speech.chooseVoice(speech.voices(),getSharedPreferences("voice",0).getString("name",""));}
    private void preview(Voice voice,String text){if(voice==null||speech.tts.setVoice(voice)!=TextToSpeech.SUCCESS){Ui.error(this,"Cette voix est indisponible.");return;}speech.say(text);}
    private Voice findUsable(List<Voice> voices,String country,String nameHint,int fallbackIndex){
        if(country!=null)for(Voice v:voices)if(country.equalsIgnoreCase(v.getLocale().getCountry())&&!v.getName().endsWith("-language"))return v;
        if(nameHint!=null)for(Voice v:voices)if(v.getName().contains(nameHint)&&v.getName().contains("local"))return v;
        if(nameHint!=null)for(Voice v:voices)if(v.getName().contains(nameHint))return v;
        if(voices.isEmpty())return null;
        return voices.get(Math.min(fallbackIndex,voices.size()-1));
    }
    private void applyStyle(String id,Voice target,float pitch,float rate,String sample){
        Voice active=target!=null?target:chosen();
        if(active!=null && speech.tts.setVoice(active)==TextToSpeech.SUCCESS){
            getSharedPreferences("voice",0).edit().putString("name",active.getName()).putString("style",id).putFloat("pitch",pitch).putFloat("rate",rate).apply();
        } else {
            getSharedPreferences("voice",0).edit().putString("style",id).putFloat("pitch",pitch).putFloat("rate",rate).apply();
        }
        speech.tts.setPitch(pitch);speech.tts.setSpeechRate(rate);
        render();
        preview(chosen(),sample);
    }
    private void render(){body.removeAllViews();if(!speech.ready){Ui.text(this,body,"La voix n’est pas disponible pour le moment.",20);Ui.button(this,body,"Ouvrir les réglages vocaux",this::settings);return;}
        List<Voice> voices=speech.voices();Voice selected=chosen();
        if(selected==null){Ui.text(this,body,"Il manque une voix française",22);Ui.text(this,body,"Installez les données françaises dans les réglages de la tablette, puis revenez ici. Une fois installées, les voix proposées fonctionneront sans Internet.",18);Ui.button(this,body,"Installer une voix française",this::settings);}
        else {
            voiceCard(selected,Math.max(1,voices.indexOf(selected)+1),true);
            Ui.gap(this,body,14);
            Ui.text(this,body,"Styles & Accents en 1 touche",20);
            Ui.text(this,body,"Changez instantanément d’intonation, de chaleur ou de timbre :",15);
            String currentStyle=getSharedPreferences("voice",0).getString("style","fr_nat");
            Voice vFr=findUsable(voices,"FR","fra",0);
            Voice vCa=findUsable(voices,"CA","frc",1);
            Voice vAf=findUsable(voices,"SN","frd",2);
            Voice vBe=findUsable(voices,"BE","fra",0);
            Voice vRadio=findUsable(voices,null,"frb",3);
            Voice vCalm=findUsable(voices,null,"frc",1);
            LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);body.addView(grid);
            Object[][] presets=new Object[][]{
                {"fr_nat","🇫🇷  France · Naturelle",vFr,1.0f,1.0f,"Bonjour, voici ma voix naturelle et claire."},
                {"ca_qc","🍁  Québec / Canada",vCa,1.08f,0.96f,"Bonjour ! Comment ça va aujourd’hui ? Prenez votre temps."},
                {"af_warm","🌍  Afrique · Chaleureuse",vAf,0.78f,0.90f,"Bonjour à vous, j’espère que vous allez bien aujourd’hui."},
                {"be_ch","🇧🇪  Belgique / Suisse · Douce",vBe,0.93f,0.92f,"Bonjour, merci de votre écoute et de votre patience."},
                {"radio","🎙️  Voix Grave & Forte",vRadio,0.80f,0.98f,"S’il vous plaît, écoutez-moi un instant."},
                {"calm","🧘  Très Articulée & Posée",vCalm,0.96f,0.82f,"Je parle doucement pour qu’on me comprenne bien."}
            };
            for(int i=0;i<presets.length;i+=2){
                LinearLayout row=new LinearLayout(this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=Ui.dp(this,8);grid.addView(row,rp);
                for(int j=i;j<Math.min(i+2,presets.length);j++){
                    Object[] p=presets[j];String id=(String)p[0];String title=(String)p[1];Voice v=(Voice)p[2];float pitch=(Float)p[3];float rate=(Float)p[4];String sample=(String)p[5];
                    Button b=Ui.action(this,title,id.equals(currentStyle),()->applyStyle(id,v,pitch,rate,sample));
                    b.setTextSize(15);b.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,8));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,62),1);
                    if(j==i)lp.rightMargin=Ui.dp(this,6);else lp.leftMargin=Ui.dp(this,6);
                    row.addView(b,lp);
                }
            }
            Ui.gap(this,body,16);Ui.text(this,body,"Hauteur de la voix (Timbre)",20);LinearLayout tones=new LinearLayout(this);body.addView(tones);float current=getSharedPreferences("voice",0).getFloat("pitch",1f);
            float[] pitchVals=new float[]{.76f,.88f,1f,1.14f};
            String[] pitchLabels=new String[]{"Très grave","Chaleureuse","Naturelle","Plus claire"};
            for(int i=0;i<pitchVals.length;i++){float pitch=pitchVals[i];String label=pitchLabels[i];Button b=Ui.action(this,label,Math.abs(current-pitch)<.03f,()->{if(speech.tts.setPitch(pitch)==TextToSpeech.SUCCESS){getSharedPreferences("voice",0).edit().putFloat("pitch",pitch).putString("style","custom").apply();preview(selected,"Bonjour, voici le ton de ma voix.");render();}else Ui.error(this,"Ce moteur ne permet pas ce réglage.");});b.setTextSize(14);b.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1);lp.rightMargin=Ui.dp(this,5);tones.addView(b,lp);}
            Ui.text(this,body,"Vitesse de lecture",20);LinearLayout speeds=new LinearLayout(this);body.addView(speeds);float rateNow=getSharedPreferences("voice",0).getFloat("rate",1f);
            float[] rateVals=new float[]{.82f,.92f,1f,1.14f};
            String[] rateLabels=new String[]{"Très posée","Calme","Normale","Plus vive"};
            for(int i=0;i<rateVals.length;i++){float rate=rateVals[i];String label=rateLabels[i];Button b=Ui.action(this,label,Math.abs(rateNow-rate)<.03f,()->{if(speech.tts.setSpeechRate(rate)==TextToSpeech.SUCCESS){getSharedPreferences("voice",0).edit().putFloat("rate",rate).putString("style","custom").apply();preview(selected,"Voici ma vitesse de lecture.");render();}else Ui.error(this,"Ce moteur ne permet pas ce réglage.");});b.setTextSize(14);b.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1);lp.rightMargin=Ui.dp(this,5);speeds.addView(b,lp);}
            if(voices.size()>1){Ui.gap(this,body,14);comparisonAnchor=Ui.button(this,body,compare?"Masquer les autres voix":"Comparer les voix hors-ligne ("+voices.size()+")",()->{compare=!compare;render();if(compare)scroll.post(()->scroll.smoothScrollTo(0,comparisonAnchor.getTop()));});if(compare)for(int i=0;i<voices.size();i++)if(!voices.get(i).getName().equals(selected.getName()))voiceCard(voices.get(i),i+1,false);}
        }
        Ui.gap(this,body,14);
        Ui.button(this,body,"📥 Installer d’autres voix (Québec, Belgique, Afrique…)",this::installVoicePacks);
        Ui.button(this,body,"Réglages du moteur vocal",()->{List<TextToSpeech.EngineInfo> engines=speech.tts.getEngines();String[] names=new String[engines.size()+1];for(int i=0;i<engines.size();i++)names[i]=engines.get(i).label;names[names.length-1]="Réglages Android";new AlertDialog.Builder(this).setTitle("Service vocal").setItems(names,(d,index)->{if(index==engines.size()){settings();return;}getSharedPreferences("voice",0).edit().putString("engine",engines.get(index).name).remove("name").apply();speech.close();speech=new Speech(this,this::render);}).setNegativeButton("Fermer",null).show();});
        Ui.text(this,body,"Astuce : dans « Installer d’autres voix », vous pouvez télécharger gratuitement Français (Canada / Québec), Français (Belgique) ou Français (Suisse) pour les utiliser hors ligne.",14);
    }
    private void voiceCard(Voice v,int number,boolean selected){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,14));card.setBackground(Ui.borderedShape(android.graphics.Color.WHITE,20,selected?Ui.TEAL:Ui.CARD_BORDER,selected?2f:1.4f,this));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=Ui.dp(this,12);body.addView(card,cp);Ui.text(this,card,selected?"Votre voix actuelle":region(v.getLocale())+" · voix "+number,22);Ui.text(this,card,region(v.getLocale())+" · voix "+number+" (Hors ligne)",15);LinearLayout row=new LinearLayout(this);card.addView(row);Button listen=Ui.action(this,"Écouter",selected,()->preview(v,"Bonjour, merci de patienter. Je communique avec cette application. Comment allez-vous aujourd’hui ?"));row.addView(listen,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));if(!selected){Button use=Ui.action(this,"Choisir",false,()->{if(speech.tts.setVoice(v)==TextToSpeech.SUCCESS){getSharedPreferences("voice",0).edit().putString("name",v.getName()).apply();compare=false;render();scroll.post(()->scroll.fullScroll(View.FOCUS_UP));}else Ui.error(this,"Cette voix est indisponible.");});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,56),1);lp.leftMargin=Ui.dp(this,8);row.addView(use,lp);}}
    static String region(Locale locale){switch(locale.getCountry()){case "CA":return "Canada / Québec";case "FR":return "France";case "BE":return "Belgique";case "CH":return "Suisse";case "SN":return "Sénégal";case "CI":return "Côte d’Ivoire";case "CM":return "Cameroun";default:return locale.getCountry().isEmpty()?"Français":locale.getDisplayCountry(Locale.FRENCH);}}
    @Override protected void onPause(){if(speech!=null)speech.close();super.onPause();}
}
