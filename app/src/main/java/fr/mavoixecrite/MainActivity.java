package fr.mavoixecrite;
import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity implements Ui.ImeObserver {
    private final android.os.Handler draftHandler=new android.os.Handler(android.os.Looper.getMainLooper());private final Runnable draftCheckpoint=()->getSharedPreferences("draft",MODE_PRIVATE).edit().putString("text",this.draft).apply();
    private EditText input;private LinearLayout suggestions,cards;
    private boolean keyboardVisible;private LinearLayout composeCaption,composeFooter,composeActions;private HorizontalScrollView suggestionStrip;
    private CardStore customCards;private LinearLayout favoritesBox,correctionBar;private Button repeatButton;
    private boolean editing,applyingCorrection,voiceHelpVisible;private String lastRequested="";private String beforeCorrection="",afterCorrection="",skipCorrectionText="",lastSpoken="";
    private final ExecutorService correctionWorker=Executors.newSingleThreadExecutor();private volatile FrenchCorrector corrector;private int correctionGeneration;
    private PhraseStore store;private String page="home",draft="",category="Essentiel";
    private TextView warning,status;private Button speakButton;private Button activeCard;
    private LinearLayout contentRoot,mainHeader,categoryTabs,reflexBar,adaptiveWordsBox; private ScrollView mainScroll,writingHelperScroll; private TextView adaptiveWordsLabel;
    private LinearLayout tracePanel,traceHistoryBox;private TextView traceMessage;
    Speech speech; private PhraseLibrary phraseLibrary;
    private static final int EXPORT=101,IMPORT=102;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        if(android.os.Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        draft=state==null?getSharedPreferences("draft",MODE_PRIVATE).getString("text",""):state.getString("draft","");
        category=getSharedPreferences("options",MODE_PRIVATE).getString("category","Essentiel");if(!Arrays.asList(CardStore.CATEGORIES).contains(category))category="Essentiel";
        if(state!=null){page=state.getString("page","home");category=state.getString("category","Essentiel");}lastSpoken=getSharedPreferences("conversation",MODE_PRIVATE).getString("last","");if(state!=null){beforeCorrection=state.getString("beforeCorrection","");afterCorrection=state.getString("afterCorrection","");skipCorrectionText=state.getString("skipCorrectionText","");}correctionWorker.execute(()->{try{corrector=FrenchCorrector.load(getApplicationContext());}catch(IOException ignored){}});render();}
    private void reload(){try{store=new PhraseStore(this);customCards=new CardStore(this);if(warning!=null)warning.setVisibility(View.GONE);}catch(IOException e){store=null;if(warning!=null){warning.setText(getString(R.string.storage_error,e.getMessage()));warning.setVisibility(View.VISIBLE);}}}
    private void rememberDraft(){if(input!=null)draft=input.getText().toString();}
    @Override protected void onSaveInstanceState(Bundle state){rememberDraft();state.putString("draft",draft);state.putString("page",page);state.putString("category",category);state.putString("beforeCorrection",beforeCorrection);state.putString("afterCorrection",afterCorrection);state.putString("skipCorrectionText",skipCorrectionText);super.onSaveInstanceState(state);}
    @Override protected void onPause(){if(phraseLibrary!=null)phraseLibrary.close();draftHandler.removeCallbacks(draftCheckpoint);correctionGeneration++;rememberDraft();if(speech!=null)speech.close();if(!getSharedPreferences("draft",MODE_PRIVATE).edit().putString("text",draft).commit())Toast.makeText(this,"Le brouillon n’a pas pu être sauvegardé",Toast.LENGTH_LONG).show();super.onPause();}
    @Override protected void onResume(){super.onResume();renderTrace();lastSpoken=getSharedPreferences("conversation",MODE_PRIVATE).getString("last",lastSpoken);if(repeatButton!=null){repeatButton.setEnabled(!lastSpoken.isEmpty());repeatButton.setAlpha(lastSpoken.isEmpty()?.4f:1f);}reload();refresh();speech=new Speech(this,()->{if(status!=null)status.setText(speech.available()?"Prête à vous donner la parole":"Choisissez votre voix dans les options");});speech.setFeedback(text->{if(status!=null)status.setText(text);if(speech.initialized&&!speech.available())showVoiceHelp();});}
    @Override protected void onDestroy(){draftHandler.removeCallbacks(draftCheckpoint);correctionGeneration++;correctionWorker.shutdownNow();super.onDestroy();}
    private void navigate(String next){rememberDraft();input=null;page=next;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),0);render();}
    private LinearLayout header(LinearLayout root,String title){boolean small=getResources().getConfiguration().screenWidthDp<600;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(Ui.dp(this,small?52:72));root.addView(row,new LinearLayout.LayoutParams(-1,-2));
        int mSize=Ui.dp(this,small?38:48), mPad=Ui.dp(this,small?8:10);
        ImageView mark=new ImageView(this);Ui.Glyph glyph=new Ui.Glyph("voice",android.graphics.Color.WHITE);mark.setImageDrawable(glyph);mark.setBackground(Ui.shape(Ui.TEAL,small?12:16,this));mark.setPadding(mPad,mPad,mPad,mPad);row.addView(mark,new LinearLayout.LayoutParams(mSize,mSize));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(Ui.dp(this,small?10:14),0,Ui.dp(this,6),0);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView name=Ui.label(this,title,small?20:26,Ui.INK);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));words.addView(name);
        if(page.equals("home") && !small)words.addView(Ui.label(this,"Touchez pour parler · glissez pour changer de famille",15,Ui.MUTED));return row;}
    private void render(){
        suggestions=null;cards=null;status=null;activeCard=null;tracePanel=null;traceHistoryBox=null;traceMessage=null;reflexBar=null;writingHelperScroll=null;adaptiveWordsBox=null;adaptiveWordsLabel=null;LinearLayout root=Ui.root(this);contentRoot=root;LinearLayout head=header(root,page.equals("history")?"Vos phrases":"Ma voix");mainHeader=head;
        boolean wide=getResources().getConfiguration().screenWidthDp>=600;
        boolean small=!wide;
        if(page.equals("history")){Button back=Ui.action(this,"Retour",false,()->navigate("home"));head.addView(back);}
        else {
            int bh=Ui.dp(this,small?42:52), bp=Ui.dp(this,small?10:14), bm=Ui.dp(this,small?5:8);
            Button call=Ui.action(this,wide?"🔔 Appeler":"🔔",false,this::ringAndCall);call.setTextSize(small?14:15);call.setTextColor(Ui.URGENT_BORDER);call.setBackground(Ui.urgentBackground(this));call.setContentDescription("Appeler quelqu’un à l’aide avec une sonnerie");call.setPadding(bp,0,bp,0);LinearLayout.LayoutParams callLp=new LinearLayout.LayoutParams(-2,bh);callLp.rightMargin=bm;head.addView(call,callLp);
            Button cardBtn=Ui.action(this,wide?"Ma carte":"Carte",false,()->{String message=customCards==null?CardStore.DEFAULT_PRESENTATION:customCards.presentation();startActivity(new Intent(this,DisplayActivity.class).putExtra("message",message).putExtra("interlocutor",true));});cardBtn.setTextSize(small?13:15);cardBtn.setPadding(bp,0,bp,0);LinearLayout.LayoutParams cLp=new LinearLayout.LayoutParams(-2,bh);cLp.rightMargin=bm;head.addView(cardBtn,cLp);
            Button voiceBtn=Ui.action(this,"Voix",false,()->startActivity(new Intent(this,VoiceActivity.class)));voiceBtn.setTextSize(small?13:15);voiceBtn.setContentDescription("Choisir ma voix ou mon accent");voiceBtn.setPadding(bp,0,bp,0);LinearLayout.LayoutParams vLp=new LinearLayout.LayoutParams(-2,bh);vLp.rightMargin=bm;head.addView(voiceBtn,vLp);
            Button menu=Ui.action(this,wide?"Options":"⋮",false,()->{});menu.setTextSize(small?15:15);menu.setContentDescription("Options : personnaliser, historique et sauvegarde");menu.setPadding(bp,0,bp,0);head.addView(menu,new LinearLayout.LayoutParams(-2,bh));menu.setOnClickListener(v->openMenu(menu));
        }
        warning=Ui.text(this,root,"",16);warning.setTextColor(0xffA33B27);warning.setVisibility(View.GONE);reload();
        if(page.equals("home"))buildTrace(root);
        ScrollView scroll=page.equals("home")?new FamilyScrollView(this,this::swipeCategory):new ScrollView(this);scroll.setId(R.id.family_scroll);mainScroll=scroll;scroll.setFillViewport(false);scroll.setClipToPadding(false);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page.equals("history")){renderHistory(body);setContentView(root);return;}
        Ui.gap(this,body,small?6:16);favoritesBox=new LinearLayout(this);favoritesBox.setOrientation(LinearLayout.VERTICAL);body.addView(favoritesBox);renderFavorites();if(editing){Ui.text(this,body,"Touchez une case pour la modifier",18);Ui.button(this,body,"Terminer la personnalisation",()->{editing=false;redraw();});}LinearLayout chips=new LinearLayout(this);categoryTabs=chips;body.addView(chips);
        for(String name:new String[]{"Essentiel","Besoins","Discussion"}){Button b=Ui.action(this,name,category.equals(name),()->{category=name;getSharedPreferences("options",MODE_PRIVATE).edit().putString("category",name).apply();renderCategories(chips);renderCards();});b.setTextSize(small?15:18);b.setSingleLine(true);b.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,small?42:50),1);lp.rightMargin=Ui.dp(this,6);chips.addView(b,lp);}
        reflexBar=new LinearLayout(this);reflexBar.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.topMargin=Ui.dp(this,small?6:10);body.addView(reflexBar,rlp);
        String[][] reflexes=new String[][]{{"✓ Oui","Oui"},{"✕ Non","Non"},{"♥ Merci","Merci"},{small?"⏳ Instant":"⏳ Un instant","Un instant s’il vous plaît"}};
        int[] reflexColors=new int[]{0xffDFEDE2,0xffF3E4DB,0xffFFFFFF,0xffFFFFFF};
        for(int i=0;i<reflexes.length;i++){String label=reflexes[i][0],utterance=reflexes[i][1];int color=reflexColors[i];Button rb=Ui.action(this,label,false,()->speak(utterance));rb.setTextSize(small?13:15);rb.setSingleLine(true);rb.setPadding(Ui.dp(this,small?4:8),0,Ui.dp(this,small?4:8),0);rb.setBackground(Ui.touch(this,color,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,small?38:44),1);if(i<reflexes.length-1)lp.rightMargin=Ui.dp(this,small?4:6);reflexBar.addView(rb,lp);}
        Ui.gap(this,body,small?8:14);cards=new LinearLayout(this);cards.setOrientation(LinearLayout.VERTICAL);body.addView(cards);renderCards();Ui.gap(this,body,small?6:12);
        buildWritingHelper(root);
        LinearLayout compose=new LinearLayout(this);compose.setOrientation(LinearLayout.VERTICAL);compose.setPadding(Ui.dp(this,small?12:18),Ui.dp(this,small?8:10),Ui.dp(this,small?12:18),Ui.dp(this,small?10:14));compose.setBackground(Ui.borderedShape(android.graphics.Color.WHITE,small?20:24,Ui.CARD_BORDER,1.6f,this));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=Ui.dp(this,small?4:8);root.addView(compose,cp);
        LinearLayout caption=new LinearLayout(this);composeCaption=caption;caption.setGravity(Gravity.CENTER_VERTICAL);compose.addView(caption);TextView hint=Ui.label(this,small?"ÉCRIRE":"ÉCRIRE UN MESSAGE LIBRE",12,Ui.TEAL);hint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));hint.setLetterSpacing(.08f);caption.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        repeatButton=Ui.action(this,"Répéter",false,this::repeatLast);repeatButton.setTextSize(small?13:14);repeatButton.setPadding(Ui.dp(this,small?10:12),0,Ui.dp(this,small?10:12),0);repeatButton.setEnabled(!lastSpoken.isEmpty());repeatButton.setAlpha(lastSpoken.isEmpty()?.4f:1f);LinearLayout.LayoutParams repLp=new LinearLayout.LayoutParams(-2,Ui.dp(this,small?36:42));repLp.rightMargin=Ui.dp(this,6);caption.addView(repeatButton,repLp);
        Button clear=Ui.action(this,"Effacer",false,()->{if(input!=null && input.length()>0)new AlertDialog.Builder(this).setTitle("Effacer votre message ?").setNegativeButton("Garder",null).setPositiveButton("Effacer",(d,w)->input.setText("")).show();});clear.setTextSize(small?13:14);clear.setPadding(Ui.dp(this,small?10:12),0,Ui.dp(this,small?10:12),0);caption.addView(clear,new LinearLayout.LayoutParams(-2,Ui.dp(this,small?36:42)));
        input=new ComposerInput(this);input.setId(R.id.message_input);input.setHint(small?"Touchez ici pour écrire…":"Touchez ici pour écrire votre phrase au clavier…");input.setHintTextColor(Ui.MUTED);input.setTextColor(Ui.INK);input.setTextSize(small?18:23);input.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));input.setMinLines(1);input.setMinimumHeight(Ui.dp(this,small?44:52));input.setMaxLines(3);input.setGravity(Gravity.TOP);input.setBackground(Ui.borderedShape(0xffF8F9F5,14,Ui.CARD_BORDER,1.2f,this));input.setPadding(Ui.dp(this,12),Ui.dp(this,small?6:8),Ui.dp(this,12),Ui.dp(this,small?8:10));
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_AUTO_CORRECT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PhraseStore.MAX_TEXT)});input.setImeOptions(EditorInfo.IME_ACTION_SEND|EditorInfo.IME_FLAG_NO_EXTRACT_UI);input.setImeActionLabel("Parler",EditorInfo.IME_ACTION_SEND);input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEND){speakDraft();return true;}return false;});
        LinearLayout.LayoutParams inputLp=new LinearLayout.LayoutParams(-1,-2);inputLp.topMargin=Ui.dp(this,small?4:6);inputLp.bottomMargin=Ui.dp(this,small?4:6);compose.addView(input,inputLp);input.setText(draft);input.setSelection(input.length());
        HorizontalScrollView suggestionScroll=new HorizontalScrollView(this);suggestionStrip=suggestionScroll;suggestionScroll.setHorizontalScrollBarEnabled(false);suggestions=new LinearLayout(this);suggestionScroll.addView(suggestions);compose.addView(suggestionScroll);suggestionScroll.setTag("suggestions");
        correctionBar=new LinearLayout(this);correctionBar.setGravity(Gravity.CENTER_VERTICAL);compose.addView(correctionBar);renderCorrection();
        int actH=Ui.dp(this,small?48:60);
        LinearLayout actions=new LinearLayout(this);composeActions=actions;actions.setGravity(Gravity.CENTER_VERTICAL);compose.addView(actions);speakButton=Ui.action(this,"Parler",true,this::speakDraft);speakButton.setId(R.id.speak_message);speakButton.setTextSize(small?16:18);speakButton.setSingleLine(true);speakButton.setPadding(Ui.dp(this,small?12:16),0,Ui.dp(this,small?12:16),0);Ui.glyph(this,speakButton,"voice",android.graphics.Color.WHITE,small?20:26);if(small)speakButton.setCompoundDrawablePadding(Ui.dp(this,8));actions.addView(speakButton,new LinearLayout.LayoutParams(0,actH,1));
        Button show=Ui.action(this,"Afficher en grand",false,()->show(input.getText().toString()));show.setTextSize(small?13:15);show.setSingleLine(true);show.setPadding(Ui.dp(this,small?10:14),0,Ui.dp(this,small?10:14),0);LinearLayout.LayoutParams showLp=new LinearLayout.LayoutParams(-2,actH);showLp.leftMargin=Ui.dp(this,small?6:8);actions.addView(show,showLp);
        Button stop=Ui.action(this,"Arrêter",false,()->{correctionGeneration++;if(speech!=null)speech.stop();});stop.setTextSize(small?13:15);stop.setSingleLine(true);stop.setPadding(Ui.dp(this,small?10:14),0,Ui.dp(this,small?10:14),0);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,actH);sp.leftMargin=Ui.dp(this,small?6:8);actions.addView(stop,sp);
        LinearLayout footer=new LinearLayout(this);composeFooter=footer;footer.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,-2);flp.topMargin=Ui.dp(this,small?2:4);compose.addView(footer,flp);status=Ui.label(this,"Un appui sur une case parle immédiatement",small?12:14,Ui.MUTED);status.setMaxLines(1);status.setEllipsize(TextUtils.TruncateAt.END);footer.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){draft=s.toString();draftHandler.removeCallbacks(draftCheckpoint);draftHandler.postDelayed(draftCheckpoint,400);correctionGeneration++;if(!applyingCorrection){skipCorrectionText="";beforeCorrection="";afterCorrection="";renderCorrection();if(s.length()>0 && " .,!?:;".indexOf(s.charAt(s.length()-1))>=0 && BaseInputConnection.getComposingSpanStart(input.getText())<0)correctDraft(false);}refresh();}public void afterTextChanged(Editable e){}});refresh();setContentView(root);root.setFocusableInTouchMode(true);root.requestFocus();
    }
    @Override public void onImeVisibility(boolean visible){keyboardVisible=visible;if(!page.equals("home") || contentRoot==null || mainHeader==null || mainScroll==null)return;mainHeader.setVisibility(visible?View.GONE:View.VISIBLE);mainScroll.setVisibility(visible?View.GONE:View.VISIBLE);if(writingHelperScroll!=null)writingHelperScroll.setVisibility(visible?View.VISIBLE:View.GONE);contentRoot.setGravity(Gravity.TOP);updateWritingLayout();}
    private void insertWords(String chunk){
        if(input==null || chunk==null || chunk.trim().isEmpty())return;
        String current=input.getText().toString();
        int start=Math.max(0,input.getSelectionStart()),end=Math.max(start,input.getSelectionEnd());
        String before=current.substring(0,start),after=current.substring(end);
        // Complete the word at the cursor without deleting the remainder of the message.
        String prefix=before.substring(before.lastIndexOf(' ')+1);
        if(start==end && start<current.length() && prefix.length()>=2 && FrenchCorrector.key(chunk).startsWith(FrenchCorrector.key(prefix))){
            while(!after.isEmpty() && Character.isLetterOrDigit(after.charAt(0)))after=after.substring(1);
        }
        String inserted=stitchFrenchGrammar(before,chunk.trim());
        if(inserted.endsWith(" ") && after.startsWith(" "))after=after.substring(1);
        String next=inserted+after;
        if(next.length()>PhraseStore.MAX_TEXT){Toast.makeText(this,"Message trop long pour cet ajout",Toast.LENGTH_SHORT).show();return;}
        input.setText(next);
        input.setSelection(inserted.length());
        input.requestFocus();
    }
    static String stitchFrenchGrammar(String current,String add){
        if(add==null || add.trim().isEmpty())return current;
        String trimCur=current.trim();
        // If the user is mid-word (no trailing space) and taps a completion matching that prefix, replace the partial word!
        if(!current.isEmpty() && !current.endsWith(" ") && !current.endsWith("’") && !current.endsWith("'")){
            int lastSp=current.lastIndexOf(' ');
            String partial=current.substring(lastSp+1);
            String normPartial=FrenchCorrector.key(partial);
            String normAdd=FrenchCorrector.key(add);
            if(normPartial.length()>=2 && (normAdd.startsWith(normPartial) || normAdd.contains(" "+normPartial))){
                String before=lastSp<0?"":current.substring(0,lastSp+1);
                return stitchFrenchGrammar(before,add);
            }
        }
        if(trimCur.isEmpty()||trimCur.endsWith(".")||trimCur.endsWith("?")||trimCur.endsWith("!")){
            String prefix=trimCur.isEmpty()?"":(current.endsWith(" ")?current:current+" ");
            return prefix+Character.toUpperCase(add.charAt(0))+add.substring(1)+" ";
        }
        String lowCur=trimCur.replace('\'','’').toLowerCase(Locale.ROOT);
        String lowAdd=add.replace('\'','’').toLowerCase(Locale.ROOT);
        boolean keepCap=add.startsWith("Je ")||add.startsWith("J’");
        String mid=keepCap?add:(Character.toLowerCase(add.charAt(0))+add.substring(1));
        if(lowCur.endsWith("est-ce que")){
            if(lowAdd.matches("^(fermer|ouvrir|allumer|éteindre|approcher|appeler|attendre|répéter|venir|donner|aider|m’aider|me donner|boire|manger|sortir|dormir|changer).*"))
                return trimCur+" tu peux "+mid+" ";
            if(lowAdd.matches("^(de l’eau|à manger|mes médicaments|un café|mon téléphone|mes lunettes).*"))
                return trimCur+" je peux avoir "+mid+" ";
            if(lowAdd.matches("^(le médecin|l’infirmière|ma famille).*"))
                return trimCur+" "+mid+" va venir ";
        }
        if(lowCur.endsWith("pouvez-vous")||lowCur.endsWith("tu peux")||lowCur.endsWith("vous pouvez")){
            if(lowAdd.matches("^(de l’eau|à manger|mes médicaments|mon téléphone|mes lunettes|un mouchoir|un café|la couverture|un oreiller).*"))
                return trimCur+" me donner "+mid+" ";
            if(lowAdd.matches("^(le médecin|l’infirmière|ma famille).*"))
                return trimCur+" appeler "+mid+" ";
            if(lowAdd.equals("la porte"))return trimCur+" fermer la porte ";
            if(lowAdd.equals("la fenêtre"))return trimCur+" ouvrir la fenêtre ";
            if(lowAdd.equals("la lumière")||lowAdd.equals("la télévision"))return trimCur+" éteindre "+mid+" ";
            if(lowAdd.equals("appelez"))
                return trimCur+" appeler ";
        }
        if(lowCur.endsWith("je voudrais")||lowCur.endsWith("j’aimerais")){
            if(lowAdd.equals("à manger"))return trimCur+" manger ";
            if(lowAdd.equals("appelez"))return trimCur+" appeler ";
            if(lowAdd.matches("^(le médecin|l’infirmière|ma famille).*"))return trimCur+" voir "+mid+" ";
        }
        if(lowCur.endsWith("j’ai besoin de")||lowCur.endsWith("besoin de")){
            String base=trimCur.substring(0,trimCur.length()-2).trim();
            if(lowAdd.equals("de l’eau")||lowAdd.equals("eau"))return base+" d’eau ";
            if(lowAdd.equals("à manger"))return trimCur+" manger ";
            if(lowAdd.startsWith("le "))return base+" du "+mid.substring(3)+" ";
            if(lowAdd.startsWith("les "))return base+" des "+mid.substring(4)+" ";
            if(lowAdd.matches("^[aeéèêiîoôuùûhyœ].*"))return base+" d’"+mid+" ";
        }
        if(lowCur.endsWith("j’ai mal à")||lowCur.endsWith("mal à")){
            String base=trimCur.substring(0,trimCur.length()-1).trim();
            if(lowAdd.startsWith("au ")||lowAdd.startsWith("aux ")||lowAdd.startsWith("à ")||lowAdd.equals("partout")||lowAdd.equals("beaucoup")||lowAdd.startsWith("depuis "))
                return base+" "+mid+" ";
            if(lowAdd.startsWith("le "))return base+" au "+mid.substring(3)+" ";
            if(lowAdd.startsWith("les "))return base+" aux "+mid.substring(4)+" ";
        }
        if(lowCur.endsWith("où est") && lowAdd.matches("^(mes |les |des |vos |nos ).*")){
            String base=trimCur.substring(0,trimCur.length()-3)+"sont";
            return base+" "+mid+" ";
        }
        // Two consecutive noun phrases -> join naturally with "et"
        String nounPattern=".*\\b(mon téléphone|mes lunettes|mes médicaments|ma tablette|un café|de l’eau|d’eau|un verre d’eau|un mouchoir|la couverture|un oreiller|la télécommande|le médecin|l’infirmière|ma famille)$";
        if(lowCur.matches(nounPattern) && lowAdd.matches("^(mon |ma |mes |un |une |du |de l’|le |la |les |l’).*") && !lowAdd.startsWith("un peu") && !lowAdd.startsWith("un moment") && !lowAdd.startsWith("un instant")){
            return trimCur+" et "+mid+" ";
        }
        boolean needsSpace=!current.endsWith(" ")&&!current.endsWith("’")&&!current.endsWith("'")&&!add.equals("?");
        return FrenchCorrector.polishGrammar(current+(needsSpace?" ":"")+mid+" ");
    }
    private void jumpToFamily(String fam){
        category=fam;
        getSharedPreferences("options",MODE_PRIVATE).edit().putString("category",fam).apply();
        if(categoryTabs!=null)renderCategories(categoryTabs);
        if(cards!=null)renderCards();
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),0);
        onImeVisibility(false);
    }
    private void buildWritingHelper(LinearLayout root){
        boolean small=getResources().getConfiguration().screenWidthDp<600;
        writingHelperScroll=new ScrollView(this);writingHelperScroll.setFillViewport(false);writingHelperScroll.setVisibility(View.GONE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,small?10:14),Ui.dp(this,small?6:8),Ui.dp(this,small?10:14),Ui.dp(this,small?6:8));box.setBackground(Ui.borderedShape(0xffEBF2EE,20,Ui.CARD_BORDER,1.4f,this));
        writingHelperScroll.addView(box);root.addView(writingHelperScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams topLp=new LinearLayout.LayoutParams(-1,-2);topLp.bottomMargin=Ui.dp(this,5);box.addView(top,topLp);
        String[][] fastNav=new String[][]{{"◀ Essentiel","Essentiel"},{"◀ Besoins","Besoins"},{"◀ Discussion","Discussion"}};
        for(String[] nf:fastNav){String lbl=nf[0],fam=nf[1];Button b=Ui.action(this,lbl,false,()->jumpToFamily(fam));b.setTextSize(small?11:13);b.setSingleLine(true);b.setPadding(Ui.dp(this,small?4:8),0,Ui.dp(this,small?4:8),0);b.setBackground(Ui.touch(this,0xffE3F0EC,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);lp.rightMargin=Ui.dp(this,small?4:5);top.addView(b,lp);}
        Button backspaceWord=Ui.action(this,small?"⌫":"⌫ Mot",false,this::deleteLastWord);backspaceWord.setContentDescription("Supprimer le dernier mot");backspaceWord.setTextSize(small?12:13);backspaceWord.setPadding(Ui.dp(this,small?8:10),0,Ui.dp(this,small?8:10),0);backspaceWord.setBackground(Ui.touch(this,android.graphics.Color.WHITE,12));LinearLayout.LayoutParams bwlp=new LinearLayout.LayoutParams(-2,Ui.dp(this,48));bwlp.rightMargin=Ui.dp(this,small?4:5);top.addView(backspaceWord,bwlp);
        Button closeIme=Ui.action(this,small?"✕":"✕ Clavier",false,()->{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),0);onImeVisibility(false);});closeIme.setContentDescription("Fermer le clavier et retrouver les cases");closeIme.setTextSize(small?12:13);closeIme.setPadding(Ui.dp(this,small?8:10),0,Ui.dp(this,small?8:10),0);top.addView(closeIme,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));

        LinearLayout quickSpeak=new LinearLayout(this);quickSpeak.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams qsp=new LinearLayout.LayoutParams(-1,-2);qsp.bottomMargin=Ui.dp(this,small?4:6);box.addView(quickSpeak,qsp);
        String[][] instant=new String[][]{{small?"✓ Oui":"✓ Dire Oui","Oui"},{small?"✕ Non":"✕ Dire Non","Non"},{small?"♥ Merci":"♥ Dire Merci","Merci"},{small?"⏳ Attends":"⏳ « Attends, j’écris »","Un instant s’il vous plaît, je suis en train d’écrire."}};
        int[] instantColors=new int[]{0xffDFEDE2,0xffF3E4DB,0xffFFFFFF,0xffFFFFFF};
        for(int i=0;i<instant.length;i++){String lbl=instant[i][0],utt=instant[i][1];int col=instantColors[i];Button b=Ui.action(this,lbl,false,()->speak(utt));b.setTextSize(small?12:13);b.setSingleLine(true);b.setPadding(Ui.dp(this,small?4:6),0,Ui.dp(this,small?4:6),0);b.setBackground(Ui.touch(this,col,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);if(i<instant.length-1)lp.rightMargin=Ui.dp(this,small?4:5);quickSpeak.addView(b,lp);}

        LinearLayout adaptiveHead=new LinearLayout(this);adaptiveHead.setGravity(Gravity.CENTER_VERTICAL);box.addView(adaptiveHead);
        adaptiveWordsLabel=Ui.label(this,small?"★ Suite suggérée & mots habituels :":"★ Vos mots habituels & suite suggérée (évolue avec vous) :",small?12:13,Ui.TEAL);adaptiveWordsLabel.setSingleLine(true);adaptiveWordsLabel.setEllipsize(TextUtils.TruncateAt.END);adaptiveWordsLabel.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));adaptiveHead.addView(adaptiveWordsLabel,new LinearLayout.LayoutParams(0,-2,1));
        Button addWordBtn=Ui.action(this,"＋ Mot perso",false,this::promptAddPersonalWord);addWordBtn.setTextSize(small?11:12);addWordBtn.setPadding(Ui.dp(this,small?6:8),0,Ui.dp(this,small?6:8),0);addWordBtn.setBackground(Ui.touch(this,android.graphics.Color.WHITE,10));adaptiveHead.addView(addWordBtn,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));
        adaptiveWordsBox=new LinearLayout(this);adaptiveWordsBox.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams awp=new LinearLayout.LayoutParams(-1,-2);awp.topMargin=Ui.dp(this,4);awp.bottomMargin=Ui.dp(this,4);box.addView(adaptiveWordsBox,awp);

        Ui.text(this,box,"Débuts de phrase (touchez pour insérer) :",small?12:13);
        String[] allStarters=new String[]{"Je voudrais","J’ai besoin de","Pouvez-vous","Est-ce que","J’ai mal à","Où est","Je ne peux pas","Appelez"};
        int sCols=small?2:4;
        for(int idx=0;idx<allStarters.length;idx+=sCols){
            LinearLayout r=new LinearLayout(this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=Ui.dp(this,small?4:5);box.addView(r,rp);
            int end=Math.min(idx+sCols,allStarters.length);
            for(int i=idx;i<end;i++){String w=allStarters[i];Button b=Ui.action(this,w+" …",false,()->insertWords(w));b.setTextSize(small?13:14);b.setSingleLine(true);b.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),0);b.setBackground(Ui.touch(this,android.graphics.Color.WHITE,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);if(i<end-1)lp.rightMargin=Ui.dp(this,small?5:6);r.addView(b,lp);}
        }
        Ui.text(this,box,"Mots utiles en 1 touche :",small?12:13);
        String[] allConn=new String[]{"s’il vous plaît","maintenant","tout à l’heure","aujourd’hui","demain","de l’eau","à manger","mes médicaments","le médecin","ma famille"};
        int cCols=small?2:5;
        for(int idx=0;idx<allConn.length;idx+=cCols){
            LinearLayout r=new LinearLayout(this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=Ui.dp(this,4);box.addView(r,rp);
            int end=Math.min(idx+cCols,allConn.length);
            for(int i=idx;i<end;i++){String w=allConn[i];Button b=Ui.action(this,"+ "+w,false,()->insertWords(w));b.setTextSize(small?14:16);b.setMaxLines(2);b.setEllipsize(TextUtils.TruncateAt.END);b.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));b.setBackground(Ui.touch(this,0xffF8F9F5,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);if(i<end-1)lp.rightMargin=Ui.dp(this,5);r.addView(b,lp);}
        }
        // Keep useful completions above navigation on short phone screens.
        box.removeView(adaptiveHead);box.addView(adaptiveHead,0);
        box.removeView(adaptiveWordsBox);box.addView(adaptiveWordsBox,1);
        refreshWritingHelper();
    }
    private static final Set<String> STOP_WORDS=new HashSet<>(Arrays.asList("je","tu","il","elle","on","nous","vous","ils","elles","le","la","les","un","une","des","du","de","à","au","aux","en","et","ou","mais","pour","par","sur","dans","avec","sans","ce","cet","cette","ces","mon","ma","mes","ton","ta","tes","son","sa","ses","notre","votre","leur","est","sont","suis","ai","as","a","avons","avez","ont","oui","non","merci","bonjour","bonsoir","voudrais","besoin","mal","pas","plus","très","bien","comment","allez","fait","faire","dire","être","avoir","tout","tous","peu","ça","cela","qui","que","quoi","où","quand","s’il","j’ai","c’est","n’ai","d’un","d’une","qu’on","plaît","appelez","appeler","manger","boire","médicaments","médecin","famille","maintenant","heure","aujourd’hui","demain","faim","soif","aide","reposer","toilettes","instant","train","écrire","parler","entends","laissez","temps","communiquer","communique","application","question","écouter","écoute","comprends","répéter","content","voir"));
    private List<String> pinnedPersonalWords(){
        String raw=getSharedPreferences("personal_words",MODE_PRIVATE).getString("list","");
        List<String> out=new ArrayList<>();
        for(String part:raw.split("\n"))if(!part.trim().isEmpty())out.add(part.trim());
        return out;
    }
    private void savePinnedPersonalWords(List<String> words){
        getSharedPreferences("personal_words",MODE_PRIVATE).edit().putString("list",TextUtils.join("\n",words)).apply();
        refreshWritingHelper();
    }
    private void promptAddPersonalWord(){
        EditText et=new EditText(this);et.setHint("Ex : Xavier, un café, mes gouttes, la télé…");et.setTextSize(20);et.setSingleLine(true);et.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,12));
        new AlertDialog.Builder(this).setTitle("Ajouter un mot ou prénom habituel").setMessage("Ce mot restera toujours proposé en haut de votre aide à l’écriture (appui long dessus pour le retirer).").setView(et).setNegativeButton("Annuler",null).setPositiveButton("Ajouter",(d,w)->{
            String val=et.getText().toString().trim();
            if(!val.isEmpty()){List<String> list=pinnedPersonalWords();if(!list.contains(val)){list.add(0,val);savePinnedPersonalWords(list);}}
        }).show();
    }
    private static final List<String> AAC_VOCAB=Arrays.asList(
        "médicaments","médecin","infirmière","famille","téléphone","lunettes","tablette","télécommande","télévision",
        "toilettes","couverture","oreiller","mouchoir","fauteuil","fenêtre","lumière","porte","traitement","antidouleur",
        "compote","yaourt","tisane","café","chocolat","soupe","gorge","ventre","épaule","poitrine","fatigué","reposer",
        "dormir","changer de position","s’il vous plaît","maintenant","aujourd’hui","demain","tout à l’heure","merci beaucoup"
    );
    private List<String> computeAdaptiveWords(String draftText){
        LinkedHashSet<String> result=new LinkedHashSet<>();
        String clean=draftText.replace('\'','’').trim().toLowerCase(Locale.ROOT);
        if(clean.isEmpty())result.addAll(pinnedPersonalWords());
        int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        // 0. Live prefix auto-completion when user is mid-word (no trailing space)
        if(!draftText.isEmpty() && !draftText.endsWith(" ") && !draftText.endsWith("’") && !draftText.endsWith("'")){
            int lastSp=clean.lastIndexOf(' ');
            String partial=FrenchCorrector.key(clean.substring(lastSp+1));
            if(partial.length()>=2){
                for(String p:pinnedPersonalWords())if(FrenchCorrector.key(p).startsWith(partial))result.add(p);
                for(String v:AAC_VOCAB)if(FrenchCorrector.key(v).startsWith(partial) && !FrenchCorrector.key(v).equals(partial))result.add(v);
            }
        }
        // 1. Learned next-word transitions from user's PhraseStore history when draft has words
        if(!clean.isEmpty() && store!=null){
            String[] draftTokens=clean.split("\\s+");
            String lastToken=draftTokens[draftTokens.length-1];
            for(PhraseStore.Phrase p:store.history()){
                String normP=p.text.replace('\'','’').replaceAll("[.,!?;:]","").trim();
                String[] tokens=normP.split("\\s+");
                for(int i=0;i<tokens.length-1;i++){
                    if(tokens[i].equalsIgnoreCase(lastToken)){
                        String nextWord=tokens[i+1];
                        if(nextWord.length()>2){
                            String candidate=i+2<tokens.length && tokens[i+1].length()<=3 ? tokens[i+1]+" "+tokens[i+2] : nextWord;
                            result.add(candidate);
                        }
                    }
                }
            }
        }
        // 2. Smart multi-step contextual completions (Step 1 -> Step 2 -> Step 3)
        if(clean.endsWith("je voudrais")||clean.endsWith("j’aimerais")){
            result.addAll(Arrays.asList("boire","manger","me reposer","aller aux toilettes","mon téléphone","mes lunettes","voir le médecin","un café","de l’eau","changer de position"));
        } else if(clean.endsWith("j’ai besoin de")||clean.endsWith("besoin de")){
            result.addAll(Arrays.asList("votre aide","me reposer","mes médicaments","mes lunettes","mon téléphone","calme","boire","changer de position","l’infirmière","un mouchoir"));
        } else if(clean.endsWith("j’ai mal à")||clean.endsWith("j’ai mal")||clean.endsWith("mal à")){
            result.addAll(Arrays.asList("la gorge","la tête","au ventre","au dos","à l’épaule","à la poitrine","beaucoup","depuis ce matin","côté gauche","côté droit"));
        } else if(clean.endsWith("pouvez-vous")||clean.endsWith("tu peux")||clean.endsWith("vous pouvez")){
            result.addAll(Arrays.asList("m’aider","fermer la porte","allumer la lumière","éteindre la lumière","approcher ma tablette","me donner de l’eau","appeler ma famille","répéter","attendre un instant","ouvrir la fenêtre"));
        } else if(clean.endsWith("est-ce que")){
            result.addAll(Arrays.asList("tu peux m’aider","tu peux fermer la porte","vous pouvez venir","le médecin va passer","ma famille arrive","c’est bientôt l’heure","je peux boire","je peux manger","on peut ouvrir la fenêtre","tout va bien"));
        } else if(clean.endsWith("appelez")||clean.endsWith("appeler")){
            result.addAll(Arrays.asList("l’infirmière","le médecin","ma famille","un proche","un soignant","s’il vous plaît"));
        } else if(clean.endsWith("où est")||clean.endsWith("où sont")){
            result.addAll(Arrays.asList("mon téléphone","ma famille","le médecin","l’infirmière","mes lunettes","mon traitement","la télécommande"));
        } else if(clean.endsWith("je ne peux pas")){
            result.addAll(Arrays.asList("parler","avaler","bouger","dormir","attendre","respirer bien","manger cela","boire cela"));
        } else if(clean.matches(".*\\b(boire|eau|verre|soif|café|thé|tisane)$")){
            result.addAll(Arrays.asList("un verre d’eau","un café","une tisane","un thé","frais","tiède","avec une paille","un peu","s’il vous plaît","maintenant"));
        } else if(clean.matches(".*\\b(manger|faim|compote|soupe|yaourt|repas)$")){
            result.addAll(Arrays.asList("une compote","un yaourt","une soupe","un peu","du pain","c’est trop chaud","c’est trop froid","plus tard","s’il vous plaît","merci"));
        } else if(clean.matches(".*\\b(gorge|tête|ventre|dos|épaule|poitrine|dents|jambes|bras)$")){
            result.addAll(Arrays.asList("beaucoup","un peu","depuis ce matin","depuis tout à l’heure","ça brûle","ça tire","côté gauche","côté droit","un antidouleur","s’il vous plaît"));
        } else if(clean.matches(".*\\b(médecin|infirmière|soignant|famille|proche)$")){
            result.addAll(Arrays.asList("s’il vous plaît","quand vient-il ?","j’ai une question","pour mon traitement","maintenant","aujourd’hui","demain","merci"));
        } else if(clean.matches(".*\\b(reposer|dormir|fatigué|position|lit)$")){
            result.addAll(Arrays.asList("un moment","éteignez la lumière","fermez la porte","baissez le son","redressez le lit","la couverture","un oreiller","j’ai froid","j’ai chaud","merci"));
        } else if(clean.matches(".*\\b(toilettes|laver|bassin)$")){
            result.addAll(Arrays.asList("maintenant","c’est urgent","avec votre aide","s’il vous plaît","merci"));
        } else if(clean.matches(".*\\b(téléphone|tablette|lunettes|télécommande|mouchoir|médicaments|couverture|oreiller)$")){
            result.addAll(Arrays.asList("s’il vous plaît","sur la table","pour le brancher","approchez-le","je ne le trouve pas","maintenant","merci"));
        } else if(clean.matches(".*\\b(porte|fenêtre|lumière|télévision|son)$")){
            result.addAll(Arrays.asList("s’il vous plaît","un peu","complètement","maintenant","merci"));
        }
        // 3. Pinned personal words
        result.addAll(pinnedPersonalWords());
        // 4. Automatically learned personal words from PhraseStore (ranked by count & current hour) and CardStore
        Map<String,Integer> scores=new HashMap<>();
        if(store!=null){
            for(PhraseStore.Phrase p:store.history()){
                int weight=p.count*3+(hour>=0&&hour<24?p.hours[hour]*5:0)+2;
                String[] tokens=p.text.replace('\'','’').replaceAll("[.,!?;:«»…]","").trim().split("\\s+");
                for(String rawTok:tokens){
                    String t=rawTok.replaceFirst("^(?i)(j’|l’|d’|s’|n’|m’|t’|c’|qu’)","");
                    String low=t.toLowerCase(Locale.ROOT);
                    if(t.length()>=4 && !low.matches(".*\\d.*") && !STOP_WORDS.contains(low) && !STOP_WORDS.contains(rawTok.toLowerCase(Locale.ROOT)))scores.put(t,scores.getOrDefault(t,0)+weight);
                }
            }
        }
        if(customCards!=null){
            for(CardStore.Card c:customCards.all()){
                if(c.id.startsWith("default-")||c.id.startsWith("template-"))continue;
                for(String rawTok:c.text.replace('\'','’').replaceAll("[.,!?;:«»…]","").trim().split("\\s+")){
                    String t=rawTok.replaceFirst("^(?i)(j’|l’|d’|s’|n’|m’|t’|c’|qu’)","");
                    String low=t.toLowerCase(Locale.ROOT);
                    if(t.length()>=3 && !low.matches(".*\\d.*") && !STOP_WORDS.contains(low))scores.put(t,scores.getOrDefault(t,0)+10);
                }
            }
        }
        List<Map.Entry<String,Integer>> sorted=new ArrayList<>(scores.entrySet());
        sorted.sort((a,b)->{int rank=Integer.compare(b.getValue(),a.getValue());return rank!=0?rank:a.getKey().compareToIgnoreCase(b.getKey());});
        for(Map.Entry<String,Integer> e:sorted)result.add(e.getKey());
        // 5. Fallback useful everyday words so the 2 rows (10 slots) are always full
        result.addAll(Arrays.asList("mon téléphone","mes lunettes","un café","la télévision","la lumière","la couverture","un mouchoir","fatigué","froid","chaud"));
        String prefix="";
        if(!draftText.isEmpty() && !Character.isWhitespace(draftText.charAt(draftText.length()-1))){
            String token=clean.substring(clean.lastIndexOf(' ')+1);
            if(FrenchCorrector.key(token).length()>=2)prefix=FrenchCorrector.key(token);
        }
        boolean hasCompletion=false;
        if(!prefix.isEmpty())for(String word:result)if(FrenchCorrector.key(word).startsWith(prefix)&&!FrenchCorrector.key(word).equals(prefix)){hasCompletion=true;break;}
        List<String> finalWords=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(String w:result){
            String key=FrenchCorrector.key(w);
            if(hasCompletion && (!key.startsWith(prefix)||key.equals(prefix)))continue;
            if(!clean.contains(w.toLowerCase(Locale.ROOT)) && seen.add(key))finalWords.add(w);
            if(finalWords.size()>=10)break;
        }
        return finalWords;
    }
    private void refreshWritingHelper(){
        if(adaptiveWordsBox==null)return;
        adaptiveWordsBox.removeAllViews();
        boolean small=getResources().getConfiguration().screenWidthDp<600;
        String draftText=input!=null?input.getText().toString():"";
        if(adaptiveWordsLabel!=null){
            boolean hasDraft=!draftText.trim().isEmpty();
            adaptiveWordsLabel.setText(hasDraft?(small?"★ Suite suggérée :":"★ Suite suggérée & vos mots habituels :"):(small?"★ Vos mots habituels :":"★ Vos mots habituels (appris avec l’usage · appui long pour retirer) :"));
        }
        List<String> words=computeAdaptiveWords(draftText);
        List<String> pinned=pinnedPersonalWords();
        int cols=small?2:4;
        for(int rIdx=0;rIdx<2 && rIdx*cols<words.size();rIdx++){
            LinearLayout row=new LinearLayout(this);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=Ui.dp(this,4);adaptiveWordsBox.addView(row,rp);
            int end=Math.min((rIdx+1)*cols,words.size());
            for(int i=rIdx*cols;i<end;i++){
                String w=words.get(i);
                boolean isPinned=pinned.contains(w);
                Button b=Ui.action(this,(isPinned?"★ ":"+ ")+w,false,()->insertWords(w));
                b.setTextSize(small?14:16);b.setMaxLines(2);b.setEllipsize(TextUtils.TruncateAt.END);b.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));
                b.setBackground(Ui.touch(this,isPinned?0xffDFEDE2:android.graphics.Color.WHITE,12));b.setContentDescription("Ajouter : "+w+". Appui long pour gérer ce mot.");
                b.setOnLongClickListener(v->{
                    new AlertDialog.Builder(this).setTitle(w).setMessage(isPinned?"Retirer ce mot de vos mots épinglés ?":"Épingler ce mot pour qu’il reste toujours en premier ?").setNegativeButton("Annuler",null).setPositiveButton(isPinned?"Retirer":"Épingler",(d,which)->{
                        List<String> list=pinnedPersonalWords();
                        if(isPinned)list.remove(w);else if(!list.contains(w))list.add(0,w);
                        savePinnedPersonalWords(list);
                    }).show();
                    return true;
                });
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);
                if(i<end-1)lp.rightMargin=Ui.dp(this,5);
                row.addView(b,lp);
            }
        }
    }
    private void deleteLastWord(){
        if(input==null)return;
        String current=input.getText().toString().trim();
        if(current.isEmpty())return;
        int lastSpace=current.lastIndexOf(' ');
        String next=lastSpace<0?"":current.substring(0,lastSpace+1);
        input.setText(next);
        input.setSelection(next.length());
        input.requestFocus();
    }
    private void ringAndCall(){
        try{android.media.ToneGenerator tone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,100);tone.startTone(android.media.ToneGenerator.TONE_SUP_INTERCEPT,650);draftHandler.postDelayed(tone::release,750);}catch(RuntimeException ignored){}
        speak("S’il vous plaît, j’ai besoin de vous.");
    }
    private static String resolveIcon(CardStore.Card item){
        if(item.text.equals("J’ai faim"))return "food";
        if(item.text.equals("Je voudrais aller aux toilettes"))return "wc";
        if(item.text.equals("J’ai besoin de me reposer"))return "bed";
        if(item.text.equals("J’ai besoin d’aide"))return "bell";
        return item.icon;
    }
    private void updateWritingLayout(){renderTrace();boolean small=getResources().getConfiguration().screenWidthDp<600;if(input!=null)input.setMaxLines(keyboardVisible?2:3);if(composeCaption!=null)composeCaption.setVisibility(keyboardVisible&&small?View.GONE:View.VISIBLE);if(composeActions!=null)composeActions.setVisibility(View.VISIBLE);if(composeFooter!=null)composeFooter.setVisibility(keyboardVisible||small?View.GONE:View.VISIBLE);if(suggestionStrip!=null)suggestionStrip.setVisibility(correctionBar!=null&&correctionBar.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}
    private void renderCategories(LinearLayout chips){for(int i=0;i<chips.getChildCount();i++){Button b=(Button)chips.getChildAt(i);boolean chosen=b.getText().toString().equals(category);b.setTextColor(chosen?android.graphics.Color.WHITE:Ui.TEAL);b.setBackground(Ui.touch(this,chosen?Ui.TEAL:0xffE9EEE7,16));}}
    private void renderCards(){cards.removeAllViews();activeCard=null;if(reflexBar!=null)reflexBar.setVisibility(!editing&&!category.equals("Essentiel")?View.VISIBLE:View.GONE);if(customCards==null){Ui.text(this,cards,"Vos cases sont temporairement indisponibles.",18);return;}
        List<CardStore.Card> entries=customCards.in(category);boolean small=getResources().getConfiguration().screenWidthDp<600;int columns=getResources().getConfiguration().screenWidthDp>=900?3:2;
        for(int i=0;i<entries.size();i+=columns){LinearLayout row=new LinearLayout(this);row.setBaselineAligned(false);cards.addView(row);for(int j=i;j<Math.min(i+columns,entries.size());j++){
            CardStore.Card item=entries.get(j);String text=item.text;String icon=resolveIcon(item);boolean urgent=text.equals("J’ai besoin d’aide")||text.startsWith("J’ai mal");
            int fill=text.equals("Oui")?0xffDFEDE2:text.equals("Non")?0xffF3E4DB:urgent?Ui.URGENT_BG:android.graphics.Color.WHITE;
            Button card=Ui.action(this,text,false,()->{});card.setTextColor(Ui.INK);card.setTextSize(small?16:24);card.setMaxLines(small?3:4);card.setEllipsize(TextUtils.TruncateAt.END);card.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);card.setBackground(urgent?Ui.urgentBackground(this):Ui.touch(this,fill,small?16:20));card.setMinHeight(Ui.dp(this,small?74:getResources().getConfiguration().screenHeightDp<900?104:120));card.setMinimumHeight(Ui.dp(this,small?74:104));card.setPadding(Ui.dp(this,small?12:20),Ui.dp(this,small?10:14),Ui.dp(this,small?10:16),Ui.dp(this,small?10:14));card.setTag(fill);
            int iconColor=urgent?Ui.URGENT_BORDER:Ui.TEAL;
            Ui.glyph(this,card,icon,iconColor,small?22:30);if(small)card.setCompoundDrawablePadding(Ui.dp(this,8));
            card.setTag(R.id.choice_card,item.template());if(item.template())styleChoice(card,item,small);
            card.setOnClickListener(v->{if(editing){editCard(item);return;}if(activeCard!=null)activeCard.setBackground(Boolean.TRUE.equals(activeCard.getTag(R.id.choice_card))?Ui.choiceBackground(this):Ui.touch(this,(Integer)activeCard.getTag(),small?16:20));activeCard=card;card.setBackground(item.template()?Ui.choiceBackground(this):Ui.touch(this,0xffCBE0D6,small?16:20));card.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);useCard(item);});card.setOnLongClickListener(v->{cardOptions(item);return true;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.rightMargin=Ui.dp(this,small?4:6);lp.leftMargin=Ui.dp(this,small?4:6);lp.bottomMargin=Ui.dp(this,small?8:12);row.addView(card,lp);
        }if(entries.size()-i<columns)for(int j=entries.size()-i;j<columns;j++)row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));}
        Button library=Ui.button(this,cards,"Plus de phrases · choisir une situation ›",()->library().open(category));library.setTextSize(small?14:18);library.setMinHeight(Ui.dp(this,small?44:56));library.setMinimumHeight(Ui.dp(this,small?44:56));library.setBackground(Ui.choiceBackground(this));Ui.glyph(this,library,"choice",Ui.TEAL,small?18:22);
        Button add=Ui.button(this,cards,"＋ Ajouter une case",()->editCard(null));add.setTextSize(small?14:16);add.setMinHeight(Ui.dp(this,small?42:56));add.setMinimumHeight(Ui.dp(this,small?42:56));
    }
    private void swipeCategory(int delta){int index=Arrays.asList(CardStore.CATEGORIES).indexOf(category);int next=Math.max(0,Math.min(CardStore.CATEGORIES.length-1,index+delta));if(next==index||categoryTabs==null||customCards==null)return;category=CardStore.CATEGORIES[next];getSharedPreferences("options",MODE_PRIVATE).edit().putString("category",category).apply();renderCategories(categoryTabs);renderCards();mainScroll.scrollTo(0,0);mainScroll.announceForAccessibility(category);mainScroll.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
    private void styleChoice(Button button,CardStore.Card card,boolean small){String title=card.text.replace("…","").trim();String hint=small?"Choisir ›":"Choisir une réponse ›";SpannableString label=new SpannableString(title+"\n"+hint);int start=title.length()+1;label.setSpan(new android.text.style.RelativeSizeSpan(.75f),start,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setSpan(new android.text.style.ForegroundColorSpan(Ui.TEAL),start,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);button.setText(label);button.setMaxLines(small?3:4);button.setContentDescription("Compléter : "+card.text);button.setBackground(Ui.choiceBackground(this));Ui.glyph(this,button,"choice",Ui.TEAL,small?22:30);if(small)button.setCompoundDrawablePadding(Ui.dp(this,8));}
    private static final String FEEDBACK_EMAIL = "mavoixecrite@proton.me";
    private void openMenu(View anchor){PopupMenu menu=new PopupMenu(this,anchor);menu.getMenu().add(0,1,0,"Choisir ma voix");menu.getMenu().add(0,5,1,"Modifier les cases");menu.getMenu().add(0,6,2,"Ma carte de présentation");menu.getMenu().add(0,7,3,"Mode interlocuteur");menu.getMenu().add(0,2,4,"Historique");menu.getMenu().add(0,8,5,"Correction automatique").setCheckable(true).setChecked(correctionEnabled());menu.getMenu().add(0,3,6,"Sauvegarder mes données");menu.getMenu().add(0,4,7,"Restaurer une sauvegarde");menu.getMenu().add(0,11,8,"Donner mon avis / Suggérer une idée");menu.getMenu().add(0,10,9,"À propos & mention légale");menu.setOnMenuItemClickListener(item->{switch(item.getItemId()){case 1:startActivity(new Intent(this,VoiceActivity.class));break;case 2:navigate("history");break;case 3:exportFile();break;case 4:importFile();break;case 5:editing=!editing;redraw();break;case 6:editPresentation();break;case 7:String message=input!=null&&!input.getText().toString().trim().isEmpty()?input.getText().toString():!lastSpoken.isEmpty()?lastSpoken:customCards==null?CardStore.DEFAULT_PRESENTATION:customCards.presentation();startActivity(new Intent(this,DisplayActivity.class).putExtra("message",message).putExtra("interlocutor",true));break;case 8:getSharedPreferences("options",MODE_PRIVATE).edit().putBoolean("correct",!correctionEnabled()).apply();break;case 11:openFeedbackDialog();break;case 10:showAboutDialog();break;}return true;});menu.show();}
    private void openFeedbackDialog(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(this,20),Ui.dp(this,10),Ui.dp(this,20),Ui.dp(this,6));
        Ui.text(this,form,"Votre retour est volontaire. Ne donnez ni nom, ni diagnostic, ni phrase personnelle. Rien ne part automatiquement. Vous pourrez relire le texte avant de choisir de le transmettre. Choisissez le sujet :",15);
        final String[][] cats=new String[][]{
            {"IDEE","💡 Idée d’amélioration (mots, phrases, fonctions)"},
            {"BUG","🐞 Problème d’affichage ou de voix"},
            {"USAGE","🩺 Retour d’usage (patient, aidant, orthophoniste)"},
            {"CONTRIB","🤝 Proposer mon aide (développement, conseils)"}
        };
        RadioGroup rg=new RadioGroup(this);
        for(int i=0;i<cats.length;i++){
            RadioButton rb=new RadioButton(this);rb.setId(1000+i);rb.setText(cats[i][1]);rb.setTextSize(15);rb.setTextColor(Ui.INK);rb.setPadding(Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6));
            rg.addView(rb);
            if(i==0)rb.setChecked(true);
        }
        form.addView(rg);
        EditText details=new EditText(this);details.setHint("Essai : installation réussie ? Phrase facile à trouver ? Message compris ? Réutilisée spontanément ? Autre idée ou problème…");details.setTextSize(17);details.setMinLines(3);details.setMaxLines(6);details.setGravity(Gravity.TOP);details.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);details.setBackground(Ui.borderedShape(0xffF8F9F5,12,Ui.CARD_BORDER,1.2f,this));details.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10));
        LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,-2);dlp.topMargin=Ui.dp(this,8);form.addView(details,dlp);
        ScrollView sv=new ScrollView(this);sv.addView(form);
        new AlertDialog.Builder(this).setTitle("Donner mon avis / Idée").setView(sv).setNegativeButton("Annuler",null).setPositiveButton("Préparer un e-mail",(d,w)->{
            int checkedId=rg.getCheckedRadioButtonId();
            int idx=Math.max(0,Math.min(cats.length-1,checkedId-1000));
            String tag=cats[idx][0], label=cats[idx][1];
            String userMsg=details.getText().toString().trim();
            int sw=getResources().getConfiguration().screenWidthDp;
            String formFactor=sw<600?"Téléphone":"Tablette";
            String subject="[MAVOIX-RETOUR] ["+tag+"] Retour Ma Voix Écrite ("+formFactor+")";
            String body="[MAVOIX-RETOUR] ["+tag+"]\n"
                +"VERSION: "+BuildConfig.VERSION_NAME+"\n"
                +"FORMAT: "+formFactor+"\n\n"
                +"Message :\n"+(userMsg.isEmpty()?"(Complétez votre message ici)":userMsg)+"\n";
Intent email=new Intent(Intent.ACTION_SENDTO,android.net.Uri.parse("mailto:"+FEEDBACK_EMAIL));
            email.putExtra(Intent.EXTRA_SUBJECT,subject);
            email.putExtra(Intent.EXTRA_TEXT,body);
            try{startActivity(email);}catch(ActivityNotFoundException e){
                Intent fallback=new Intent(Intent.ACTION_SEND).setType("message/rfc822");
                fallback.putExtra(Intent.EXTRA_EMAIL,new String[]{FEEDBACK_EMAIL});
                fallback.putExtra(Intent.EXTRA_SUBJECT,subject);
                fallback.putExtra(Intent.EXTRA_TEXT,body);
                try{startActivity(Intent.createChooser(fallback,"Envoyer votre retour"));}catch(ActivityNotFoundException ex){
                    Toast.makeText(this,"Aucune messagerie trouvée. Vous pouvez écrire à : "+FEEDBACK_EMAIL,Toast.LENGTH_LONG).show();
                }
            }
        }).show();
    }
    private void showAboutDialog(){
        String msg="Ma Voix Écrite (v"+BuildConfig.VERSION_NAME+" — Libre & Gratuite)\n\n"
            +"• Initiative citoyenne créée pour un ami opéré de la gorge, réalisée par un novice avec l’aide de l’IA et ouverte à toute contribution.\n"
            +"• Gratuite, sans publicité et sans compte. Écriture et phrases disponibles hors ligne.\n"
            +"• Voix française hors ligne à installer dans Android si nécessaire. Styles selon les voix disponibles.\n• Aucun accès au microphone, aucune capture audio.\n• Historique et personnalisation stockés sur lʼappareil. Une sauvegarde ou un retour ne sort que si vous le partagez.\n• Équipe Ma Voix Écrite : projet bénévole en essai, bénéfice clinique non évalué.\n• Code MIT ; dictionnaire Lexique 3.83 sous CC BY-SA 4.0, attributions fournies avec les sources.\n\n"
            +"Mention d’usage :\n"
            +"Application d’entraide et de confort de communication au quotidien. "
            +"Ce logiciel libre (licence MIT) n’est pas un dispositif médical certifié ni un système d’appel d’urgence médicalisé.";
        new AlertDialog.Builder(this).setTitle("À propos de Ma Voix").setMessage(msg).setPositiveButton("Fermer",null).setNegativeButton("Donner mon avis",(d,w)->openFeedbackDialog()).setNeutralButton("Partager", (d,w)->{
            Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"Ma Voix Écrite — Application gratuite pour parler avec sa tablette ou son téléphone").putExtra(Intent.EXTRA_TEXT,"Découvrez Ma Voix Écrite : une application Android 100 % gratuite, hors-ligne et sans publicité créée pour aider les adultes privés de voix après une opération de la gorge à communiquer par phrases ou texte. Voix hors ligne à installer si nécessaire. Projet bénévole en essai ; retours volontaires bienvenus.\n\nTéléchargement direct & QR Code :\nhttps://mavoixecrite.github.io/ma-voix-ecrite/");
            try{startActivity(Intent.createChooser(share,"Partager Ma Voix"));}catch(ActivityNotFoundException ignored){}
        }).show();
    }
    private void renderHistory(LinearLayout body){Ui.text(this,body,"Retrouvez les mots déjà utilisés",20);if(store==null)return;if(store.history().isEmpty())Ui.text(this,body,"Les phrases prononcées ou affichées apparaîtront ici.",18);
        for(PhraseStore.Phrase p:store.history()){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=Ui.dp(this,10);body.addView(row,rp);Button phrase=Ui.action(this,p.text,false,()->{draft=p.text;beforeCorrection="";afterCorrection="";skipCorrectionText="";input=null;navigate("home");});phrase.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);phrase.setBackground(Ui.touch(this,android.graphics.Color.WHITE,16));row.addView(phrase,new LinearLayout.LayoutParams(0,-2,1));Button delete=Ui.action(this,"",false,()->new AlertDialog.Builder(this).setTitle("Oublier cette phrase ?").setMessage(p.text).setNegativeButton("Garder",null).setPositiveButton("Supprimer",(d,w)->{try{store.delete(p.text);render();}catch(IOException e){Ui.error(this,"La phrase n’a pas été supprimée : "+e.getMessage());}}).show());delete.setContentDescription("Supprimer la phrase : "+p.text);Ui.glyph(this,delete,"trash",Ui.MUTED,24);row.addView(delete,new LinearLayout.LayoutParams(Ui.dp(this,56),Ui.dp(this,56)));}}
    private void speakDraft(){correctDraft(true);}
    private String visibleMessage(){return getSharedPreferences("conversation",MODE_PRIVATE).getString("trace","");}
    private void rememberVisible(String text){getSharedPreferences("conversation",MODE_PRIVATE).edit().putString("trace",text).apply();renderTrace();}
    private void buildTrace(LinearLayout root){boolean small=getResources().getConfiguration().screenWidthDp<600;tracePanel=new LinearLayout(this);tracePanel.setId(R.id.trace_panel);tracePanel.setOrientation(LinearLayout.VERTICAL);tracePanel.setPadding(Ui.dp(this,small?10:14),Ui.dp(this,small?2:4),Ui.dp(this,small?10:14),Ui.dp(this,small?6:12));tracePanel.setBackground(Ui.shape(0xffE7F0EB,small?14:20,this));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=Ui.dp(this,small?4:8);root.addView(tracePanel,tp);LinearLayout caption=new LinearLayout(this);caption.setGravity(Gravity.CENTER_VERTICAL);tracePanel.addView(caption);TextView capLbl=Ui.label(this,small?"Dernier message":"Dernier message & récents",small?12:14,Ui.MUTED);capLbl.setSingleLine(true);capLbl.setEllipsize(TextUtils.TruncateAt.END);caption.addView(capLbl,new LinearLayout.LayoutParams(0,-2,1));for(String action:new String[]{"Agrandir","Masquer"}){Button b=Ui.action(this,action,false,()->{if(action.equals("Agrandir"))show(visibleMessage());else{getSharedPreferences("conversation",MODE_PRIVATE).edit().remove("trace").apply();renderTrace();}});b.setTextSize(small?12:14);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(Ui.dp(this,small?5:8),0,Ui.dp(this,small?5:8),0);b.setBackgroundColor(android.graphics.Color.TRANSPARENT);caption.addView(b,new LinearLayout.LayoutParams(-2,Ui.dp(this,small?32:48)));}traceMessage=Ui.label(this,"",small?18:28,Ui.INK);traceMessage.setId(R.id.trace_message);traceMessage.setMaxLines(small?1:2);traceMessage.setEllipsize(TextUtils.TruncateAt.END);traceMessage.setOnClickListener(v->show(visibleMessage()));traceMessage.setContentDescription("Dernier message. Touchez pour afficher en grand.");tracePanel.addView(traceMessage,new LinearLayout.LayoutParams(-1,-2));traceHistoryBox=new LinearLayout(this);traceHistoryBox.setOrientation(LinearLayout.VERTICAL);tracePanel.addView(traceHistoryBox,new LinearLayout.LayoutParams(-1,-2));renderTrace();}
    private void renderTrace(){if(tracePanel==null||traceMessage==null)return;boolean small=getResources().getConfiguration().screenWidthDp<600;String text=visibleMessage();traceMessage.setText(text);traceMessage.setContentDescription("Dernier message : "+text+". Touchez pour afficher en grand.");tracePanel.setVisibility(text.isEmpty()||keyboardVisible?View.GONE:View.VISIBLE);if(traceHistoryBox!=null){traceHistoryBox.removeAllViews();if(!small&&!text.isEmpty()&&store!=null){List<PhraseStore.Phrase> recent=store.history();int shown=0;Set<String> seen=new HashSet<>();seen.add(text.replace('\'','’').trim().toLowerCase(Locale.ROOT));for(PhraseStore.Phrase p:recent){String norm=p.text.replace('\'','’').trim().toLowerCase(Locale.ROOT);if(!seen.add(norm))continue;if(shown==0)Ui.gap(this,traceHistoryBox,6);TextView item=Ui.label(this,"↺  "+p.text,16,Ui.MUTED);item.setSingleLine(true);item.setEllipsize(TextUtils.TruncateAt.END);item.setPadding(0,Ui.dp(this,3),0,Ui.dp(this,3));item.setOnClickListener(v->speak(p.text,"written"));traceHistoryBox.addView(item);if(++shown>=3)break;}}}}
private void speak(String text){speak(text,"choice");}
    private void speak(String text,String source){String value=text.trim();lastRequested=value;if(value.isEmpty())return;rememberVisible(value);if(speech!=null)speech.say(value,()->learn(value));}
    private void showVoiceHelp(){if(voiceHelpVisible||isFinishing())return;voiceHelpVisible=true;AlertDialog help=new AlertDialog.Builder(this).setTitle("Une voix française est nécessaire").setMessage("Choisissez une voix installée sur cet appareil pour parler sans Internet. Vous pouvez aussi afficher votre message par écrit.").setPositiveButton("Choisir ma voix",(d,w)->startActivity(new Intent(this,VoiceActivity.class))).setNegativeButton("Afficher le texte",(d,w)->show(lastRequested)).create();help.setOnDismissListener(d->voiceHelpVisible=false);help.show();}
    private void learn(String text){lastSpoken=text;getSharedPreferences("conversation",MODE_PRIVATE).edit().putString("last",text).apply();if(repeatButton!=null){repeatButton.setEnabled(true);repeatButton.setAlpha(1f);}try{new PhraseStore(this).record(text,System.currentTimeMillis(),Calendar.getInstance().get(Calendar.HOUR_OF_DAY));reload();refresh();}catch(IOException e){if(status!=null)status.setText(R.string.spoken_not_saved);Toast.makeText(this,R.string.spoken_not_saved,Toast.LENGTH_LONG).show();}}
    private void show(String text){text=text.trim();if(text.isEmpty()){Toast.makeText(this,"Écrivez d’abord un message",Toast.LENGTH_SHORT).show();return;}rememberVisible(text);startActivity(new Intent(this,DisplayActivity.class).putExtra("message",text));}
    private void refresh(){if(suggestions==null || input==null)return;updateWritingLayout();refreshWritingHelper();suggestions.removeAllViews();if(speakButton!=null){speakButton.setEnabled(input.getText().toString().trim().length()>0);speakButton.setAlpha(speakButton.isEnabled()?1f:.42f);}if(store==null)return;
        if(input.length()==0){List<PhraseStore.Phrase> recent=store.history();for(int i=0;i<Math.min(5,recent.size());i++){PhraseStore.Phrase p=recent.get(i);Button b=Ui.action(this,"↺  "+p.text,false,()->{input.setText(p.text);input.setSelection(input.length());});b.setTextSize(14);b.setSingleLine(true);b.setMaxWidth(Ui.dp(this,280));b.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,44));lp.rightMargin=Ui.dp(this,6);lp.bottomMargin=Ui.dp(this,8);suggestions.addView(b,lp);}return;}
        List<PhraseStore.Phrase> found=store.suggestions(input.getText().toString(),Calendar.getInstance().get(Calendar.HOUR_OF_DAY));for(int i=0;i<Math.min(3,found.size());i++){PhraseStore.Phrase p=found.get(i);Button b=Ui.action(this,"↺ "+p.text,false,()->{input.setText(p.text);input.setSelection(input.length());});b.setTextSize(15);b.setSingleLine(true);b.setMaxWidth(Ui.dp(this,280));b.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,48));lp.rightMargin=Ui.dp(this,6);lp.bottomMargin=Ui.dp(this,8);suggestions.addView(b,lp);}
    }
    static class ComposerInput extends EditText {
        ComposerInput(android.content.Context c){super(c);}
        @Override public InputConnection onCreateInputConnection(EditorInfo out){InputConnection connection=super.onCreateInputConnection(out);out.imeOptions=(out.imeOptions&~EditorInfo.IME_MASK_ACTION&~EditorInfo.IME_FLAG_NO_ENTER_ACTION)|EditorInfo.IME_ACTION_SEND|EditorInfo.IME_FLAG_NO_EXTRACT_UI;out.actionLabel="Parler";out.actionId=EditorInfo.IME_ACTION_SEND;return connection;}
    }
    private void redraw(){rememberDraft();input=null;render();}
    private PhraseLibrary library(){if(phraseLibrary!=null)phraseLibrary.close();return phraseLibrary=new PhraseLibrary(this,this::speak,text->{try{CardStore.Card card=new CardStore.Card(UUID.randomUUID().toString(),category,text,"chat");customCards.save(card);renderCards();Toast.makeText(this,"Phrase ajoutée dans "+category,Toast.LENGTH_SHORT).show();}catch(IOException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}},()->{CardStore.Card card=new CardStore.Card(UUID.randomUUID().toString(),"Besoins","Je voudrais parler de …","chat");card.choices.add("mon traitement");editCard(card);},()->{if(speech!=null)speech.stop();},(spokenPart,fullSentence)->{rememberVisible(fullSentence);if(speech!=null)speech.say(spokenPart,()->learn(fullSentence));});}
    private void useCard(CardStore.Card card){if(card.id.equals("template-pain")&&card.text.equals("J’ai mal …")&&card.choices.equals(Arrays.asList("à la tête","à la gorge","au ventre","au dos"))){library().pain();return;}if(card.template())new AlertDialog.Builder(this).setTitle(card.text).setItems(card.choices.toArray(new String[0]),(d,index)->speak(card.completed(card.choices.get(index)))).setNegativeButton("Annuler",null).show();else speak(card.text);}
    private void renderFavorites(){if(favoritesBox==null)return;favoritesBox.removeAllViews();if(customCards==null||customCards.favorites().isEmpty())return;Ui.text(this,favoritesBox,"Vos favoris",16);HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);scroll.addView(row);favoritesBox.addView(scroll);for(CardStore.Card c:customCards.favorites()){Button b=Ui.action(this,c.text,false,()->{if(editing)editCard(c);else useCard(c);});if(c.template()){b.setText("Choisir ›  "+c.text.replace("…","").trim());b.setContentDescription("Compléter : "+c.text);b.setBackground(Ui.choiceBackground(this));Ui.glyph(this,b,"choice",Ui.TEAL,20);}b.setTextSize(17);b.setSingleLine(true);b.setMaxWidth(Ui.dp(this,280));b.setEllipsize(TextUtils.TruncateAt.END);b.setOnLongClickListener(v->{cardOptions(c);return true;});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=Ui.dp(this,8);lp.bottomMargin=Ui.dp(this,12);row.addView(b,lp);}}
    private void cardOptions(CardStore.Card card){String[] actions={"Modifier la case",card.favorite?"Retirer des favoris":"Ajouter aux favoris","Déplacer avant","Déplacer après","Supprimer la case"};new AlertDialog.Builder(this).setTitle(card.text).setItems(actions,(d,index)->{if(index==0){editCard(card);return;}if(index==4){new AlertDialog.Builder(this).setTitle("Supprimer cette case ?").setMessage(card.text).setNegativeButton("Garder",null).setPositiveButton("Supprimer",(x,y)->{try{customCards.delete(card.id);redraw();}catch(IOException e){Ui.error(this,e.getMessage());}}).show();return;}try{if(index==1)customCards.favorite(card.id);else customCards.move(card.id,index==2?-1:1);redraw();}catch(IOException e){Ui.error(this,e.getMessage());}}).setNegativeButton("Fermer",null).show();}
    private void editCard(CardStore.Card existing){if(customCards==null){Ui.error(this,"Les cases ne peuvent pas être chargées.");return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),0);Ui.text(this,form,"Famille : "+(existing==null?category:existing.category),16);
        EditText phrase=new EditText(this);phrase.setHint("La phrase à dire");phrase.setTextSize(21);phrase.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);phrase.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PhraseStore.MAX_TEXT)});if(existing!=null)phrase.setText(existing.text);form.addView(phrase);
        CheckBox favorite=new CheckBox(this);favorite.setText("Garder dans mes favoris");favorite.setChecked(existing!=null&&existing.favorite);form.addView(favorite);
        CheckBox template=new CheckBox(this);template.setText("Phrase à compléter");template.setChecked(existing!=null&&existing.template());form.addView(template);
        TextView help=Ui.text(this,form,"Les points seront remplacés par votre choix. Écrivez un choix par ligne, par exemple : un café, puis un verre d’eau.",16);
        EditText choices=new EditText(this);choices.setHint("Un choix par ligne");choices.setMinLines(3);choices.setGravity(Gravity.TOP);choices.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);if(existing!=null)choices.setText(android.text.TextUtils.join("\n",existing.choices));form.addView(choices);help.setVisibility(template.isChecked()?View.VISIBLE:View.GONE);choices.setVisibility(template.isChecked()?View.VISIBLE:View.GONE);template.setOnCheckedChangeListener((v,checked)->{if(checked && !phrase.getText().toString().contains("…") && !phrase.getText().toString().contains("...")){String current=phrase.getText().toString().trim();phrase.setText(current.isEmpty()?"…":current+" …");phrase.setSelection(current.length());}help.setVisibility(checked?View.VISIBLE:View.GONE);choices.setVisibility(checked?View.VISIBLE:View.GONE);});
        ScrollView scroll=new ScrollView(this);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null||customCards.all().stream().noneMatch(c->c.id.equals(existing.id))?"Nouvelle case":"Modifier la case").setView(scroll).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=phrase.getText().toString().trim();CardStore.Card card=new CardStore.Card(existing==null?UUID.randomUUID().toString():existing.id,existing==null?category:existing.category,template.isChecked()?text.replace("...","…"):text,existing==null?"chat":existing.icon);card.favorite=favorite.isChecked();if(template.isChecked())for(String choice:choices.getText().toString().split("\n"))if(!choice.trim().isEmpty())card.choices.add(choice.trim());if(template.isChecked()&&card.choices.isEmpty()){choices.setError("Ajoutez au moins un choix");return;}try{customCards.save(card);dialog.dismiss();redraw();}catch(IOException e){phrase.setError(e.getMessage());}}));dialog.show();}
    private void editPresentation(){if(customCards==null)return;EditText text=new EditText(this);text.setText(customCards.presentation());text.setTextSize(20);text.setMinLines(3);text.setGravity(Gravity.TOP);text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PhraseStore.MAX_TEXT)});text.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,12));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Ma carte de présentation").setView(text).setNegativeButton("Annuler",null).setNeutralButton("Enregistrer",null).setPositiveButton("Afficher",null).create();dialog.setOnShowListener(d->{for(int which:new int[]{AlertDialog.BUTTON_NEUTRAL,AlertDialog.BUTTON_POSITIVE})dialog.getButton(which).setOnClickListener(v->{try{String value=text.getText().toString().trim();customCards.presentation(value);dialog.dismiss();if(which==AlertDialog.BUTTON_POSITIVE)startActivity(new Intent(this,DisplayActivity.class).putExtra("message",value).putExtra("interlocutor",true));}catch(IOException e){text.setError(e.getMessage());}});});dialog.show();}
    private void repeatLast(){if(!lastSpoken.isEmpty())speak(lastSpoken);}
    private boolean correctionEnabled(){return getSharedPreferences("options",MODE_PRIVATE).getBoolean("correct",true);}
    private void renderCorrection(){if(correctionBar==null)return;correctionBar.removeAllViews();if(beforeCorrection.isEmpty() || beforeCorrection.equals(afterCorrection)){correctionBar.setVisibility(View.GONE);updateWritingLayout();return;}correctionBar.setVisibility(View.VISIBLE);TextView label=Ui.label(this,"Petite faute corrigée",14,Ui.MUTED);correctionBar.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button undo=Ui.action(this,"Annuler la correction",false,()->{if(input==null)return;String previous=beforeCorrection;correctionGeneration++;if(speech!=null)speech.stop();applyingCorrection=true;input.setText(previous);input.setSelection(input.length());applyingCorrection=false;skipCorrectionText=previous;beforeCorrection="";afterCorrection="";renderCorrection();});undo.setTextSize(14);correctionBar.addView(undo,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));updateWritingLayout();}
    private void correctDraft(boolean thenSpeak){if(input==null)return;String snapshot=input.getText().toString();if(snapshot.trim().isEmpty())return;if(!correctionEnabled() || snapshot.equals(skipCorrectionText)){if(thenSpeak)speak(snapshot,"written");return;}final int generation=++correctionGeneration;final Set<String> personal=FrenchCorrector.protectedWords(customCards);
        if(thenSpeak && status!=null)status.setText(R.string.preparing_text);
        correctionWorker.execute(()->{FrenchCorrector.Result result;try{FrenchCorrector dictionary=corrector==null?FrenchCorrector.load(getApplicationContext()):corrector;corrector=dictionary;result=dictionary.correct(snapshot,personal);}catch(IOException e){result=new FrenchCorrector.Result(snapshot,false);}final FrenchCorrector.Result ready=result;runOnUiThread(()->{if(isDestroyed()||input==null||generation!=correctionGeneration||!input.getText().toString().equals(snapshot))return;if(ready.changed){beforeCorrection=snapshot;afterCorrection=ready.text;applyingCorrection=true;int cursor=input.getSelectionStart();input.setText(ready.text);input.setSelection(Math.min(Math.max(0,cursor+ready.text.length()-snapshot.length()),input.length()));applyingCorrection=false;renderCorrection();}if(thenSpeak)speak(ready.text,"written");});});}

    private void exportFile(){if(store==null){Ui.error(this,"Les données sont illisibles. Aucune sauvegarde vide ne sera créée.");return;}
        try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"MaVoixEcrite-sauvegarde.json");startActivityForResult(i,EXPORT);}catch(ActivityNotFoundException e){Ui.error(this,"Aucun sélecteur de fichiers disponible.");}}
    private void importFile(){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}catch(ActivityNotFoundException e){Ui.error(this,"Aucun sélecteur de fichiers disponible.");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK || data==null || data.getData()==null)return;
        if(request==EXPORT){try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new IOException("Fichier inaccessible");out.write(DataFile.snapshot(this).toString().getBytes(StandardCharsets.UTF_8));out.flush();Toast.makeText(this,"Sauvegarde terminée",Toast.LENGTH_LONG).show();}catch(Exception e){Ui.error(this,"Sauvegarde non terminée : "+e.getMessage());}}
        if(request==IMPORT){try(InputStream in=getContentResolver().openInputStream(data.getData())){String json=PhraseStore.readBounded(in);org.json.JSONObject backup=DataFile.checkedBackup(json);int count=backup.optString("format").equals("MaVoixEcrite")?PhraseStore.parse(json).size():PhraseStore.parse(backup.optJSONObject("phrases").toString()).size();
            new AlertDialog.Builder(this).setTitle("Restaurer "+count+" phrases ?").setMessage("Cela remplacera les phrases et, si la sauvegarde les contient, vos cases personnelles. Sauvegardez vos données actuelles si nécessaire.").setNegativeButton("Annuler",null).setPositiveButton("Remplacer",(d,w)->{try{if(store==null){Ui.error(this,"Les données locales sont illisibles. Conservez-les avant toute restauration.");return;}DataFile.restore(this,json);reload();redraw();Toast.makeText(this,"Restauration terminée",Toast.LENGTH_LONG).show();}catch(IOException e){Ui.error(this,"Restauration impossible : "+e.getMessage());}}).show();
        }catch(IOException e){Ui.error(this,"Sauvegarde invalide, aucune phrase remplacée : "+e.getMessage());}}
    }
}

















