package fr.mavoixecrite;
import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.Calendar;
public class DisplayActivity extends Activity {
    private Speech speech;private TextView message;private ScrollView panel;
    private String text;private boolean rotated,interlocutor;private LinearLayout answers;private int size=64;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        text=state==null?getIntent().getStringExtra("message"):state.getString("message",getIntent().getStringExtra("message"));interlocutor=state==null?getIntent().getBooleanExtra("interlocutor",false):state.getBoolean("interlocutor");if(state==null && interlocutor){rotated=true;if(text!=null&&text.length()>60)size=getResources().getConfiguration().screenWidthDp<600?32:48;}if(text==null || text.trim().isEmpty() || text.length()>PhraseStore.MAX_TEXT){finish();return;}
        if(state!=null){rotated=state.getBoolean("rotated");size=state.getInt("size",64);}
        getSharedPreferences("conversation",MODE_PRIVATE).edit().putString("trace",text).apply();getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root=Ui.root(this);LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);root.addView(controls);
        Button back=Ui.action(this,"Retour",false,()->finish());controls.addView(back,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));
        for(String value:new String[]{"A−","A+","Retourner"}){Button b=Ui.action(this,value,false,()->{});b.setTextSize(16);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(Ui.dp(this,10),0,Ui.dp(this,10),0);b.setContentDescription(value.equals("A−")?"Réduire le texte":value.equals("A+")?"Agrandir le texte":"Retourner le message à 180 degrés");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,56));lp.leftMargin=Ui.dp(this,6);controls.addView(b,lp);b.setOnClickListener(v->{if(value.equals("Retourner")){rotated=!rotated;panel.setRotation(rotated?180:0);if(answers!=null)answers.setRotation(rotated?180:0);}else{size=Math.max(28,Math.min(96,size+(value.equals("A+")?4:-4)));message.setTextSize(size);}});}
        Button mode=Ui.button(this,root,interlocutor?"Quitter le mode interlocuteur":"Mode interlocuteur",()->{});mode.setOnClickListener(v->{interlocutor=!interlocutor;rotated=interlocutor;panel.setRotation(rotated?180:0);answers.setRotation(rotated?180:0);answers.setVisibility(interlocutor?View.VISIBLE:View.GONE);mode.setText(interlocutor?"Quitter le mode interlocuteur":"Mode interlocuteur");});
        panel=new ScrollView(this);panel.setFillViewport(true);message=new TextView(this);message.setId(R.id.display_message);message.setText(text);message.setTextColor(Ui.INK);message.setTextSize(size);message.setGravity(Gravity.CENTER);message.setPadding(12,12,12,12);panel.addView(message,new ScrollView.LayoutParams(-1,-1));panel.setRotation(rotated?180:0);root.addView(panel,new LinearLayout.LayoutParams(-1,0,1));
        answers=new LinearLayout(this);answers.setVisibility(interlocutor?View.VISIBLE:View.GONE);answers.setRotation(rotated?180:0);root.addView(answers);for(String reply:new String[]{"Oui","Non"}){Button b=Ui.action(this,reply,reply.equals("Oui"),()->answer(reply));b.setTextSize(30);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,Ui.dp(this,88),1);ap.setMargins(Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,12));answers.addView(b,ap);}
        LinearLayout actions=new LinearLayout(this);root.addView(actions);Button speak=Ui.action(this,"Parler",true,()->{if(speech!=null)speech.say(text,()->getSharedPreferences("conversation",MODE_PRIVATE).edit().putString("last",text).apply());});Ui.glyph(this,speak,"voice",android.graphics.Color.WHITE,26);actions.addView(speak,new LinearLayout.LayoutParams(0,Ui.dp(this,64),1));Button stop=Ui.action(this,"Arrêter",false,()->{if(speech!=null)speech.stop();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,64));lp.leftMargin=Ui.dp(this,10);actions.addView(stop,lp);setContentView(root);
        if(state==null)try{new PhraseStore(this).record(text,System.currentTimeMillis(),Calendar.getInstance().get(Calendar.HOUR_OF_DAY));}catch(IOException e){Toast.makeText(this,"Message affiché, mais non mémorisé : "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
private void answer(String reply){text=reply;message.setText(reply);getSharedPreferences("conversation",MODE_PRIVATE).edit().putString("trace",reply).apply();try{new PhraseStore(this).record(reply,System.currentTimeMillis(),Calendar.getInstance().get(Calendar.HOUR_OF_DAY));}catch(IOException e){Toast.makeText(this,"Réponse affichée, mais non mémorisée",Toast.LENGTH_LONG).show();}if(speech!=null)speech.say(reply,()->getSharedPreferences("conversation",MODE_PRIVATE).edit().putString("last",reply).apply());}
    @Override protected void onResume(){super.onResume();speech=new Speech(this,()->{});}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("message",text);state.putBoolean("interlocutor",interlocutor);state.putBoolean("rotated",rotated);state.putInt("size",size);super.onSaveInstanceState(state);}
    @Override protected void onPause(){if(speech!=null)speech.close();super.onPause();}
}



