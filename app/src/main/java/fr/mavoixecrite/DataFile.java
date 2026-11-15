package fr.mavoixecrite;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** One atomic document for phrases and personal cards, including complete restores. */
final class DataFile {
    static final String NAME="mavoix-data-v3.json";
    static JSONObject read(Context context)throws IOException{
        File file=new File(context.getFilesDir(),NAME);if(!file.exists())return null;
        try(InputStream in=new FileInputStream(file)){JSONObject root=new JSONObject(PhraseStore.readBounded(in));validate(root);JSONObject cards=root.getJSONObject("cards");root.put("cards",CardStore.encode(CardStore.parse(cards),cards.getString("presentation")));return root;}
        catch(JSONException e){throw new IOException("Données personnelles illisibles",e);}
    }
    static void validate(JSONObject root)throws IOException{
        try{if(!root.getString("format").equals("MaVoixEcriteData") || root.getInt("version")!=3)throw new IOException("Format de données inconnu");PhraseStore.parse(root.getJSONObject("phrases").toString());CardStore.parse(root.getJSONObject("cards"));}
        catch(JSONException e){throw new IOException("Sauvegarde incomplète",e);}
    }
    static JSONObject snapshot(Context context)throws IOException{
        JSONObject root=read(context);if(root!=null)return root;
        try{return new JSONObject().put("format","MaVoixEcriteData").put("version",3).put("phrases",new JSONObject(new PhraseStore(context).exportJson())).put("cards",new CardStore(context).toJson());}
        catch(JSONException e){throw new IOException(e);}
    }
    static synchronized void section(Context context,String name,JSONObject value)throws IOException{
        try{JSONObject root=snapshot(context);root.put(name,value);write(context,root);}catch(JSONException e){throw new IOException(e);}
    }
    static void write(Context context,JSONObject root)throws IOException{
        validate(root);byte[] bytes=root.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>PhraseStore.MAX_BYTES)throw new IOException("Sauvegarde trop grande (2 Mo maximum)");
        File target=new File(context.getFilesDir(),NAME),temp=new File(target+".new");
        try{try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){temp.delete();throw e;}
    }
    static JSONObject checkedBackup(String json)throws IOException{
        try{JSONObject root=new JSONObject(json);if(root.optString("format").equals("MaVoixEcrite")){PhraseStore.parse(json);return root;}validate(root);JSONObject cards=root.getJSONObject("cards");root.put("cards",CardStore.encode(CardStore.parse(cards),cards.getString("presentation")));return root;}catch(JSONException e){throw new IOException("Sauvegarde illisible",e);}
    }
    static synchronized void restore(Context c,String json)throws IOException{JSONObject root=checkedBackup(json);if(root.optString("format").equals("MaVoixEcrite"))section(c,"phrases",root);else write(c,root);}
}

