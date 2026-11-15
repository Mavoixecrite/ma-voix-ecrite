package fr.mavoixecrite;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

final class CardStore {
    static final String[] CATEGORIES={"Essentiel","Besoins","Discussion"};
    static final String DEFAULT_PRESENTATION="Je ne peux pas parler, mais je vous entends. Laissez-moi le temps d’écrire.";
    static final int MAX_CARDS=200;
    static final class Card {
        String id,category,text,icon;boolean favorite;final List<String> choices=new ArrayList<>();
        Card(String id,String family,String text,String icon){this.id=id;category=family;this.text=text;this.icon=icon;}
        boolean template(){return !choices.isEmpty();}
        String completed(String choice){int at=text.indexOf('…');if(at<0)return text;String before=text.substring(0,at),after=text.substring(at+1);return (before+(!before.isEmpty()&&Character.isLetterOrDigit(before.charAt(before.length()-1))?" ":"")+choice+(!after.isEmpty()&&Character.isLetterOrDigit(after.charAt(0))?" ":"")+after).trim();}
    }
    final Context context;private List<Card> cards;private String presentation=DEFAULT_PRESENTATION;
    CardStore(Context context)throws IOException{this.context=context;JSONObject root=DataFile.read(context);if(root==null)cards=defaults();else{JSONObject json=root.optJSONObject("cards");cards=parse(json);presentation=json.optString("presentation",DEFAULT_PRESENTATION);}}
    static List<Card> defaults(){List<Card> list=new ArrayList<>();
        String[][] rows={{"Essentiel","Oui","yes"},{"Essentiel","Non","no"},{"Essentiel","Merci","heart"},{"Essentiel","Bonjour","sun"},{"Essentiel","S’il vous plaît","chat"},{"Essentiel","Un instant","clock"},{"Besoins","J’ai soif","cup"},{"Besoins","J’ai faim","cup"},{"Besoins","J’ai mal","help"},{"Besoins","J’ai besoin d’aide","help"},{"Besoins","Je voudrais aller aux toilettes","chat"},{"Besoins","J’ai besoin de me reposer","clock"},{"Discussion","Comment allez-vous ?","chat"},{"Discussion","Je vais bien","sun"},{"Discussion","Je ne comprends pas","chat"},{"Discussion","Pouvez-vous répéter ?","chat"},{"Discussion","Je vous écoute","heart"},{"Discussion","Je communique par écrit","chat"}};
        for(int i=0;i<rows.length;i++)if(i!=8)list.add(new Card("default-"+i,rows[i][0],rows[i][1],rows[i][2]));
        Card pain=new Card("template-pain","Besoins","J’ai mal …","help");Collections.addAll(pain.choices,"à la tête","à la gorge","au ventre","au dos");list.add(pain);
        return list;
    }
    static List<Card> parse(JSONObject root)throws IOException{
        if(root==null)throw new IOException("Cases manquantes");try{if(root.getInt("version")!=1)throw new IOException("Format des cases inconnu");String intro=root.getString("presentation");validateText(intro);
            JSONArray array=root.getJSONArray("items");if(array.length()>MAX_CARDS)throw new IOException("Trop de cases");List<Card> result=new ArrayList<>();Set<String> ids=new HashSet<>();
            for(int i=0;i<array.length();i++){JSONObject o=array.getJSONObject(i);String id=o.getString("id"),category=o.getString("category"),text=o.getString("text");validateText(text);if(id.isEmpty()||id.length()>80||!ids.add(id)||!Arrays.asList(CATEGORIES).contains(category))throw new IOException("Case invalide");Card c=new Card(id,category,text,o.optString("icon","chat"));c.favorite=o.getBoolean("favorite");JSONArray choices=o.getJSONArray("choices");if(choices.length()>12)throw new IOException("Trop de choix");for(int j=0;j<choices.length();j++){String choice=choices.getString(j);if(choice.trim().isEmpty()||choice.length()>160)throw new IOException("Choix invalide");c.choices.add(choice);}if(c.template() && (text.indexOf('…')<0 || text.indexOf('…')!=text.lastIndexOf('…')))throw new IOException("Utilisez un seul … pour la partie à compléter");result.add(c);}return migrateDefaults(result);
        }catch(JSONException e){throw new IOException("Cases illisibles",e);}}
    private static List<Card> migrateDefaults(List<Card> list){Card plain=null,choice=null;for(Card card:list){if(card.id.equals("default-8")&&card.category.equals("Besoins")&&!card.template()&&PhraseStore.normalized(card.text).equals(PhraseStore.normalized("J’ai mal")))plain=card;if(card.id.equals("template-pain")&&card.template()&&PhraseStore.normalized(card.text).equals(PhraseStore.normalized("J’ai mal …")))choice=card;}if(plain!=null&&choice!=null){choice.favorite|=plain.favorite;list.remove(plain);}removeObsoleteWant(list);return list;}
    private static void removeObsoleteWant(List<Card> list){Card want=null;List<Card> direct=new ArrayList<>();for(Card card:list){if(card.id.equals("template-want")&&card.category.equals("Besoins")&&card.text.equals("Je voudrais …")&&card.choices.equals(Arrays.asList("un verre d’eau","manger","me reposer","aller aux toilettes")))want=card;for(Card original:defaultNeeds())if(card.id.equals(original.id)&&card.category.equals(original.category)&&card.text.equals(original.text)&&!card.template())direct.add(card);}if(want!=null&&direct.size()==4){if(want.favorite)for(Card card:direct)card.favorite=true;list.remove(want);}}
    private static List<Card> defaultNeeds(){return Arrays.asList(new Card("default-6","Besoins","J’ai soif","cup"),new Card("default-7","Besoins","J’ai faim","cup"),new Card("default-10","Besoins","Je voudrais aller aux toilettes","chat"),new Card("default-11","Besoins","J’ai besoin de me reposer","clock"));}
    private static void validateText(String s)throws IOException{if(s.trim().isEmpty()||s.length()>PhraseStore.MAX_TEXT)throw new IOException("Texte vide ou trop long");}
    JSONObject toJson()throws IOException{return encode(cards,presentation);}
    static JSONObject encode(List<Card> list,String intro)throws IOException{try{JSONArray array=new JSONArray();for(Card c:list){JSONArray choices=new JSONArray();for(String s:c.choices)choices.put(s);array.put(new JSONObject().put("id",c.id).put("category",c.category).put("text",c.text).put("icon",c.icon).put("favorite",c.favorite).put("choices",choices));}JSONObject root=new JSONObject().put("version",1).put("presentation",intro).put("items",array);parse(root);return root;}catch(JSONException e){throw new IOException(e);}}
    List<Card> in(String category){List<Card> list=new ArrayList<>();for(Card c:cards)if(c.category.equals(category))list.add(c);return list;}
    List<Card> favorites(){List<Card> list=new ArrayList<>();for(Card c:cards)if(c.favorite)list.add(c);return list;}
    List<Card> all(){return new ArrayList<>(cards);}
    String presentation(){return presentation;}
    void presentation(String value)throws IOException{JSONObject json=encode(cards,value);DataFile.section(context,"cards",json);presentation=value;}
    void save(Card card)throws IOException{for(Card other:cards)if(!other.id.equals(card.id)&&other.category.equals(card.category)&&PhraseStore.normalized(other.text).equals(PhraseStore.normalized(card.text))&&other.choices.equals(card.choices))throw new IOException("Une case identique existe déjà dans cette famille.");List<Card> next=parse(toJson());boolean updated=false;for(int i=0;i<next.size();i++)if(next.get(i).id.equals(card.id)){next.set(i,card);updated=true;break;}if(!updated)next.add(card);commit(next);}
    void delete(String id)throws IOException{List<Card> next=parse(toJson());next.removeIf(c->c.id.equals(id));commit(next);}
    void favorite(String id)throws IOException{List<Card> next=parse(toJson());for(Card c:next)if(c.id.equals(id))c.favorite=!c.favorite;commit(next);}
    void move(String id,int delta)throws IOException{List<Card> next=parse(toJson());int index=-1;for(int i=0;i<next.size();i++)if(next.get(i).id.equals(id)){index=i;break;}if(index<0)return;int other=index+delta;while(other>=0 && other<next.size() && !next.get(other).category.equals(next.get(index).category))other+=delta;if(other>=0 && other<next.size()){Collections.swap(next,index,other);commit(next);}}
    private void commit(List<Card> next)throws IOException{JSONObject json=encode(next,presentation);DataFile.section(context,"cards",json);cards=next;}
}

