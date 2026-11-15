package fr.mavoixecrite;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.util.*;
import org.json.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk={26,35})
public class PersonalDataTest {
 Context c;
 @Before public void reset(){c=RuntimeEnvironment.getApplication();for(String name:new String[]{DataFile.NAME,DataFile.NAME+".new","phrases-v2.json"})new File(c.getFilesDir(),name).delete();c.getSharedPreferences("MainActivity",0).edit().clear().commit();}
 @Test public void defaultTemplatesComplete(){CardStore s;try{s=new CardStore(c);}catch(IOException e){throw new AssertionError(e);}CardStore.Card card=s.in("Besoins").stream().filter(x->x.id.equals("template-pain")).findFirst().get();assertEquals("J’ai mal au dos",card.completed("au dos"));}
 @Test public void personalCardsPersistPerFamily()throws Exception{CardStore s=new CardStore(c);CardStore.Card a=new CardStore.Card("mine","Discussion","Bonjour Xavier","chat");a.favorite=true;s.save(a);CardStore again=new CardStore(c);assertTrue(again.in("Discussion").stream().anyMatch(x->x.text.equals("Bonjour Xavier")));assertFalse(again.in("Besoins").stream().anyMatch(x->x.id.equals("mine")));assertEquals("mine",again.favorites().get(0).id);}
 @Test public void editDeleteAndOrderPersist()throws Exception{CardStore s=new CardStore(c);CardStore.Card a=new CardStore.Card("mine","Essentiel","Autre phrase","chat");s.save(a);s.move("mine",-1);assertEquals("mine",new CardStore(c).in("Essentiel").get(5).id);a.text="Phrase modifiée";s.save(a);assertTrue(new CardStore(c).all().stream().anyMatch(x->x.text.equals("Phrase modifiée")));s.delete("mine");assertFalse(new CardStore(c).all().stream().anyMatch(x->x.id.equals("mine")));}
 @Test public void presentationPersists()throws Exception{CardStore s=new CardStore(c);s.presentation("Bonjour, je m’appelle Camille.");assertEquals("Bonjour, je m’appelle Camille.",new CardStore(c).presentation());}
 @Test public void invalidTemplateDoesNotReplaceData()throws Exception{CardStore s=new CardStore(c);String before=s.toJson().toString();CardStore.Card a=new CardStore.Card("bad","Besoins","Je voudrais", "chat");a.choices.add("un café");assertThrows(IOException.class,()->s.save(a));assertEquals(before,s.toJson().toString());}
 @Test public void favoritesSurvivePhraseLearning()throws Exception{CardStore s=new CardStore(c);s.favorite("default-0");new PhraseStore(c).record("Bonjour",1,0);assertEquals("Oui",new CardStore(c).favorites().get(0).text);}
 @Test public void backupRestoresPhrasesAndCardsTogether()throws Exception{PhraseStore s=new PhraseStore(c);s.record("Bonjour",1,0);CardStore cards=new CardStore(c);cards.save(new CardStore.Card("mine","Essentiel","Camille", "chat"));String backup=DataFile.snapshot(c).toString();cards.delete("mine");s.delete("Bonjour");DataFile.restore(c,backup);assertEquals("Bonjour",new PhraseStore(c).history().get(0).text);assertTrue(new CardStore(c).all().stream().anyMatch(x->x.id.equals("mine")));}
 @Test public void corruptCardsInBackupDoNotReplacePhrases()throws Exception{new PhraseStore(c).record("À garder",1,0);String before=DataFile.snapshot(c).toString();JSONObject invalid=new JSONObject(before);invalid.getJSONObject("cards").remove("items");assertThrows(IOException.class,()->DataFile.restore(c,invalid.toString()));assertEquals(before,DataFile.snapshot(c).toString());}
 @Test public void oldPhraseBackupKeepsPersonalCards()throws Exception{PhraseStore s=new PhraseStore(c);s.record("Ancienne phrase",1,0);String legacy=s.exportJson();CardStore cards=new CardStore(c);cards.save(new CardStore.Card("mine","Essentiel","Camille", "chat"));s.record("Nouvelle phrase",2,0);DataFile.restore(c,legacy);assertEquals(1,new PhraseStore(c).history().size());assertTrue(new CardStore(c).all().stream().anyMatch(x->x.id.equals("mine")));}
 @Test public void writeFailurePreservesBothSections()throws Exception{new PhraseStore(c).record("À garder",1,0);String before=DataFile.snapshot(c).toString();File blocked=new File(c.getFilesDir(),DataFile.NAME+".new");assertTrue(blocked.mkdir());try{assertThrows(IOException.class,()->new CardStore(c).presentation("Nouvelle présentation"));assertEquals(before,DataFile.snapshot(c).toString());}finally{blocked.delete();}}
 @Test public void labelsForFrenchRegions(){assertEquals("Canada / Québec",VoiceActivity.region(Locale.CANADA_FRENCH));assertEquals("Sénégal",VoiceActivity.region(new Locale("fr","SN")));assertTrue(Speech.usable(new android.speech.tts.Voice("fr-ca",Locale.CANADA_FRENCH,400,200,false,Collections.emptySet())));}
}
