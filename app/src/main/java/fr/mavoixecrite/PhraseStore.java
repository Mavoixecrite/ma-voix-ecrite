package fr.mavoixecrite;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.*;

/** One atomic, validated document. Import never merges or overwrites before validation. */
public final class PhraseStore {
    public static final int MAX_PHRASES=2000, MAX_TEXT=2000, MAX_BYTES=2_000_000;
    public static final class Phrase {
        public final String text; public int count; public long last;
        public final int[] hours=new int[24];
        Phrase(String t,int c,long l){text=t;count=c;last=l;}
    }
    private final Context context;
    private final File file;
    private LinkedHashMap<String,Phrase> phrases=new LinkedHashMap<>();
    public PhraseStore(Context c) throws IOException {
        context=c; file=new File(c.getFilesDir(),"phrases-v2.json");
        JSONObject combined=DataFile.read(c);
        if(combined!=null){
            try{phrases=parse(combined.getJSONObject("phrases").toString());}catch(JSONException e){throw new IOException(e);}
        } else if(file.exists()) {
            try(InputStream in=new FileInputStream(file)){phrases=parse(readBounded(in));}
        } else {
            String legacy=c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("phrases_v1","{}");
            try {
                JSONObject old=new JSONObject(legacy);
                if(old.length()>MAX_PHRASES)throw new IOException("Trop de phrases anciennes");
                for(Iterator<String> it=old.keys();it.hasNext();) {
                    String s=it.next(); validateText(s); int count=old.getInt(s);
                    if(count<1)throw new IOException("Fréquence invalide");
                    phrases.put(s,new Phrase(s,count,0));
                }
            } catch(JSONException e){throw new IOException("Anciennes données illisibles",e);}
        }
    }
    public static String readBounded(InputStream in) throws IOException {
        if(in==null)throw new IOException("Fichier inaccessible");
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("Sauvegarde trop grande");out.write(b,0,n);}
        return out.toString(StandardCharsets.UTF_8.name());
    }
    private static void validateText(String s)throws IOException {
        if(s==null || s.trim().isEmpty() || s.length()>MAX_TEXT)throw new IOException("Phrase vide ou trop longue");
    }
    static LinkedHashMap<String,Phrase> parse(String data)throws IOException {
        try {
            JSONObject root=new JSONObject(data);
            if(!root.getString("format").equals("MaVoixEcrite") || root.getInt("version")!=2)throw new IOException("Format de sauvegarde inconnu");
            JSONArray array=root.getJSONArray("phrases");
            if(array.length()>MAX_PHRASES)throw new IOException("Trop de phrases");
            LinkedHashMap<String,Phrase> result=new LinkedHashMap<>();
            for(int i=0;i<array.length();i++) {
                JSONObject o=array.getJSONObject(i);String text=o.getString("text");validateText(text);
                int count=o.getInt("count");long last=o.getLong("last");
                if(count<1 || last<0 || result.containsKey(text))throw new IOException("Données de phrase invalides");
                Phrase p=new Phrase(text,count,last);JSONArray h=o.getJSONArray("hours");
                if(h.length()!=24)throw new IOException("Habitudes invalides");
                for(int j=0;j<24;j++){p.hours[j]=h.getInt(j);if(p.hours[j]<0)throw new IOException("Habitudes invalides");}
                result.put(text,p);
            }
            return result;
        } catch(JSONException e){throw new IOException("Sauvegarde illisible",e);}
    }
    public String exportJson() throws IOException{return encode(phrases);}
    private static String encode(Map<String,Phrase> entries)throws IOException {
        try {
            JSONArray arr=new JSONArray();
            for(Phrase p:entries.values()) {
                JSONArray h=new JSONArray();for(int n:p.hours)h.put(n);
                arr.put(new JSONObject().put("text",p.text).put("count",p.count).put("last",p.last).put("hours",h));
            }
            String data=new JSONObject().put("format","MaVoixEcrite").put("version",2).put("phrases",arr).toString();
            if(data.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IOException("Mémoire pleine : exportez puis supprimez des phrases");
            return data;
        }catch(JSONException e){throw new IOException(e);}
    }
    private void persist(Map<String,Phrase> next)throws IOException {
        try{DataFile.section(context,"phrases",new JSONObject(encode(next)));}catch(JSONException e){throw new IOException(e);}
    }
    public void restore(String data)throws IOException {
        LinkedHashMap<String,Phrase> next=parse(data);persist(next);phrases=next;
    }
    public void record(String text,long now,int hour)throws IOException {
        text=text.trim();validateText(text);
        if(hour<0 || hour>23 || now<0)throw new IOException("Date invalide");
        LinkedHashMap<String,Phrase> next=parse(exportJson());
        Phrase p=next.get(text);
        if(p==null){if(next.size()>=MAX_PHRASES)throw new IOException("Mémoire de phrases pleine");p=new Phrase(text,0,0);next.put(text,p);}
        p.count=p.count==Integer.MAX_VALUE?p.count:p.count+1;p.last=now;
        if(p.hours[hour]<Integer.MAX_VALUE)p.hours[hour]++;
        persist(next);phrases=next;
    }
    public void delete(String text)throws IOException {
        LinkedHashMap<String,Phrase> next=new LinkedHashMap<>(phrases);next.remove(text);persist(next);phrases=next;
    }
    static String normalized(String s){return Normalizer.normalize(s.toLowerCase(Locale.FRENCH),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('’','\'').trim().replaceAll("\\s+"," ");}
    static int match(String query,String text) {
        String q=normalized(query), t=normalized(text);
        if(q.isEmpty() || t.startsWith(q))return 2;
        // Keep fuzzy matching bounded even when a long paragraph is pasted.
        if(q.length()<3 || q.length()>64 || q.length()>t.length()+1)return 0;
        String prefix=t.substring(0,Math.min(t.length(),q.length()));
        int[] prev=new int[prefix.length()+1];for(int j=0;j<prev.length;j++)prev[j]=j;
        for(int i=1;i<=q.length();i++) {int[] row=new int[prev.length];row[0]=i;
            for(int j=1;j<row.length;j++)row[j]=Math.min(Math.min(row[j-1]+1,prev[j]+1),prev[j-1]+(q.charAt(i-1)==prefix.charAt(j-1)?0:1));prev=row;}
        return prev[prefix.length()]<=1?1:0;
    }
    public List<Phrase> suggestions(String query,int hour) {
        List<Phrase> result=new ArrayList<>();for(Phrase p:phrases.values())if(match(query,p.text)>0)result.add(p);
        result.sort(Comparator.<Phrase>comparingInt(p->match(query,p.text)).reversed()
            .thenComparing(Comparator.<Phrase>comparingLong(p->(long)p.count+2L*p.hours[hour]).reversed())
            .thenComparing(Comparator.<Phrase>comparingLong(p->p.last).reversed()).thenComparing(p->p.text));
        return result.subList(0,Math.min(5,result.size()));
    }
    public List<Phrase> history(){List<Phrase> result=new ArrayList<>(phrases.values());result.sort(Comparator.comparingLong((Phrase p)->p.last).reversed().thenComparing(p->p.text));return result;}
}
