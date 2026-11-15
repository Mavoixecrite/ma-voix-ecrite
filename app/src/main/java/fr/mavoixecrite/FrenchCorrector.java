package fr.mavoixecrite;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;

/** Offline, conservative one-edit correction. Valid words and protected personal words stay intact. */
final class FrenchCorrector {
    static final Pattern WORD=Pattern.compile("[\\p{L}]+(?:[’'][\\p{L}]+)*");
    static final class Entry {final String word;final float frequency;Entry(String w,float f){word=w;frequency=f;}}
    static final class Result {final String text;final boolean changed;Result(String t,boolean c){text=t;changed=c;}}
    private final Map<String,Entry> words=new HashMap<>();private static volatile FrenchCorrector shared;
    static FrenchCorrector load(Context context)throws IOException{if(shared!=null)return shared;synchronized(FrenchCorrector.class){if(shared==null){FrenchCorrector corrector=new FrenchCorrector();try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(context.getAssets().open("french-lexique383.dat")),StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null){if(Thread.currentThread().isInterrupted())throw new IOException("Chargement interrompu");String[] f=line.split("\\t");if(f.length!=3)throw new IOException("Dictionnaire invalide");corrector.words.put(f[0],new Entry(f[1],Float.parseFloat(f[2])));}}shared=corrector;}return shared;}}
    static String key(String word){return Normalizer.normalize(word.toLowerCase(Locale.FRENCH),Normalizer.Form.NFD).replaceAll("\\p{M}+","");}
    static Set<String> protectedWords(CardStore cards){Set<String> result=new HashSet<>();if(cards!=null)for(CardStore.Card card:cards.all()){Matcher m=WORD.matcher(card.text);while(m.find())result.add(key(m.group()));for(String choice:card.choices){m=WORD.matcher(choice);while(m.find())result.add(key(m.group()));}}return result;}
    Result correct(String text,Set<String> personal){Matcher matcher=WORD.matcher(text);StringBuffer out=new StringBuffer();boolean changed=false;int index=0;
        while(matcher.find()){String original=matcher.group(),replacement=original;boolean identifier=(matcher.start()>0 && "@/_0123456789".indexOf(text.charAt(matcher.start()-1))>=0)||(matcher.end()<text.length() && "@/_0123456789".indexOf(text.charAt(matcher.end()))>=0);
            if(!identifier && original.length()>=5 && original.length()<=24 && original.indexOf('\'')<0 && original.indexOf('’')<0 && (!Character.isUpperCase(original.charAt(0))||index==0) && !original.equals(original.toUpperCase(Locale.FRENCH))){String normalized=key(original);if(!words.containsKey(normalized) && !personal.contains(normalized)){Entry best=best(normalized);if(best!=null){replacement=best.word;if(Character.isUpperCase(original.charAt(0)))replacement=Character.toUpperCase(replacement.charAt(0))+replacement.substring(1);}}}
            if(!replacement.equals(original))changed=true;matcher.appendReplacement(out,Matcher.quoteReplacement(replacement));index++;
        }matcher.appendTail(out);
        String polished=polishGrammar(out.toString());
        if(!polished.equals(out.toString()))changed=true;
        return new Result(polished,changed);}
    static String polishGrammar(String input){
        String s=input;
        s=s.replaceAll("(?i)\\best[- ]ce que\\s+(fermer|ouvrir|allumer|éteindre|approcher|appeler|attendre|répéter|venir|donner|aider|m[’']aider|boire|manger|sortir|dormir|changer|baisser|monter|brancher|noter|voir)\\b","Est-ce que tu peux $1");
        s=s.replaceAll("(?i)\\b(je voudrais|pouvez-vous|tu peux|vous pouvez|je vais|je peux)\\s+appelez\\b","$1 appeler");
        s=s.replaceAll("(?i)\\b(tu peux|vous pouvez|pouvez-vous)\\s+(la porte)\\b","$1 fermer $2");
        s=s.replaceAll("(?i)\\b(tu peux|vous pouvez|pouvez-vous)\\s+(la fenêtre)\\b","$1 ouvrir $2");
        s=s.replaceAll("(?i)\\b(tu peux|vous pouvez|pouvez-vous)\\s+(la lumière|la télévision)\\b","$1 éteindre $2");
        s=s.replaceAll("(?i)\\b(je peux)\\s+(de l[’']eau|un café|un verre d[’']eau|mes médicaments|mon téléphone|mes lunettes)\\b","$1 avoir $2");
        s=s.replaceAll("(?i)\\bj[’']ai mal à\\s+(au|aux|à la|à l[’'])\\b","J’ai mal $1");
        s=s.replaceAll("(?i)\\bj[’']ai mal à\\s+le\\b","J’ai mal au");
        s=s.replaceAll("(?i)\\bj[’']ai mal à\\s+les\\b","J’ai mal aux");
        s=s.replaceAll("(?i)\\bj[’']ai besoin de\\s+de l[’']eau\\b","J’ai besoin d’eau");
        s=s.replaceAll("(?i)\\bj[’']ai besoin de\\s+à\\s+(manger|boire)\\b","J’ai besoin de $1");
        s=s.replaceAll("(?i)\\bj[’']ai besoin de\\s+le\\b","J’ai besoin du");
        s=s.replaceAll("(?i)\\bj[’']ai besoin de\\s+les\\b","J’ai besoin des");
        s=s.replaceAll("(?i)\\bj[’']ai besoin de\\s+(aller|appeler|aide|eau|un|une|avoir|être|un oreiller|un antidouleur)\\b","J’ai besoin d’$1");
        s=s.replaceAll("(?i)\\bje voudrais\\s+à\\s+(manger|boire)\\b","Je voudrais $1");
        s=s.replaceAll("(?i)\\boù est\\s+(mes|les|des|vos|nos)\\b","Où sont $1");
        s=s.replaceAll("(?i)\\bque\\s+(il|elle|on)\\b","qu’$1");
        s=s.replaceAll("(?i)\\bje\\s+(ai|aimerais|écoute|entends|espère|attends|appelle|arrive)\\b","j’$1");
        return s;
    }
    private Entry best(String query){Set<String> candidates=new HashSet<>();String alphabet="abcdefghijklmnopqrstuvwxyz";
        for(int i=0;i<query.length();i++){candidates.add(query.substring(0,i)+query.substring(i+1));for(int j=0;j<alphabet.length();j++)candidates.add(query.substring(0,i)+alphabet.charAt(j)+query.substring(i+1));if(i+1<query.length())candidates.add(query.substring(0,i)+query.charAt(i+1)+query.charAt(i)+query.substring(i+2));}
        for(int i=0;i<=query.length();i++)for(int j=0;j<alphabet.length();j++)candidates.add(query.substring(0,i)+alphabet.charAt(j)+query.substring(i));
        Entry first=null;float second=0;
        for(String candidate:candidates){if(candidate.isEmpty()||candidate.charAt(0)!=query.charAt(0))continue;Entry e=words.get(candidate);if(e==null||e.frequency<20)continue;if(first==null||e.frequency>first.frequency){if(first!=null)second=Math.max(second,first.frequency);first=e;}else second=Math.max(second,e.frequency);}
        return first!=null && (second==0 || first.frequency>=second*8)?first:null;
    }
}

