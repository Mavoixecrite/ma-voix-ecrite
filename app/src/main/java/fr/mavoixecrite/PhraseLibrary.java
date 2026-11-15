package fr.mavoixecrite;

import android.app.*;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** A small set of visible choices at a time; selecting a phrase always speaks it. */
final class PhraseLibrary {
    static final String[][] TOPICS={
        {"Essentiel","Me laisser la parole","Laissez-moi le temps de répondre.","Adressez-vous directement à moi.","Je vous comprends, même si je ne peux pas vous répondre à voix haute.","Posez-moi une question à la fois.","Posez-moi une question avec une réponse oui ou non.","Je n’ai pas fini.","Ce n’est pas ce que je voulais dire.","Je voudrais changer ma réponse.","Je préfère écrire.","Je voudrais vous montrer quelque chose."},
        {"Essentiel","Mes choix et mes limites","Je suis d’accord.","Je ne suis pas d’accord.","Je ne veux pas.","Arrêtez, s’il vous plaît.","Expliquez-moi avant de commencer.","Je voudrais réfléchir avant de décider.","Je ne sais pas.","Je préfère ne pas répondre.","J’ai besoin d’un moment pour moi.","Je veux que cette conversation reste privée."},
        {"Besoins","Confort et quotidien","J’ai trop chaud.","J’ai froid.","Je voudrais changer de position.","Aidez-moi à m’asseoir.","Aidez-moi à me lever.","Je voudrais m’allonger.","J’ai besoin d’aller aux toilettes.","J’ai besoin d’aide pour me laver.","J’ai besoin de mes lunettes.","J’ai besoin de mon appareil auditif.","Pouvez-vous rapprocher mon téléphone ou ma tablette ?","Branchez mon appareil, s’il vous plaît."},
        {"Besoins","Gênes et soins","J’ai du mal à respirer.","J’ai du mal à avaler.","J’ai la bouche sèche.","J’ai besoin de cracher.","J’ai envie de vomir.","J’ai des vertiges.","J’ai des démangeaisons.","Je ressens une grande fatigue.","J’ai besoin d’un mouchoir.","Mon pansement me gêne.","Mon dispositif médical me gêne.","Appelez un soignant, s’il vous plaît."},
        {"Besoins","Médicaments","Je voudrais parler de mes médicaments.","À quoi sert ce médicament ?","Quel est le nom de ce médicament ?","Quand dois-je le prendre ?","Quelle dose m’a été prescrite ?","Pouvez-vous vérifier mon ordonnance avec moi ?","Je ne me rappelle plus si je l’ai pris.","Je n’arrive pas à avaler ce médicament.","Je pense avoir un effet indésirable.","Je voudrais parler de mes allergies.","J’ai besoin d’aide pour prendre mon traitement.","Je voudrais parler à un médecin ou à un pharmacien."},
        {"Besoins","Comprendre mes soins","Que va-t-il se passer ?","Est-ce que cela va faire mal ?","Combien de temps cela va-t-il durer ?","Pouvez-vous m’expliquer autrement ?","Je voudrais poser une question.","Attendez un peu avant de commencer.","Je souhaite faire une pause.","Je voudrais voir mon médecin.","Je voudrais qu’un proche soit présent.","Pouvez-vous noter les informations importantes ?"},
        {"Besoins","Boire et manger","Je voudrais savoir ce que je peux boire ou manger.","J’ai besoin d’aide pour manger.","Je voudrais une serviette.","C’est trop chaud.","C’est trop froid.","Je n’ai plus faim.","Je voudrais autre chose.","J’aimerais choisir mon repas."},
        {"Discussion","Émotions et présence","Ça me fait plaisir.","Je suis triste.","J’ai peur.","Cela m’inquiète.","Je me sens en colère.","Je me sens seul.","J’ai besoin qu’on me rassure.","Restez un peu avec moi.","J’ai besoin de calme.","Je voudrais parler de ce que je ressens."},
        {"Discussion","Mes proches et ma vie","Ça me fait plaisir de vous voir.","Merci d’être là.","Je t’aime.","Tu me manques.","Comment s’est passée ta journée ?","Raconte-moi ce qui se passe à la maison.","Je voudrais appeler un proche.","Je voudrais envoyer un message.","J’aimerais écouter de la musique.","J’aimerais regarder la télévision.","J’ai envie de rire un peu.","À bientôt."},
        {"Discussion","Mes envies et mes opinions","J’aime bien.","Je n’aime pas.","C’est important pour moi.","J’ai une idée.","Je ne suis pas du même avis.","J’aimerais choisir moi-même.","J’aimerais sortir un peu.","Je voudrais changer de sujet.","Je plaisante.","Et toi, qu’en penses-tu ?"},
        {"Discussion","Sorties et démarches","Bonjour, je communique avec cette application.","J’ai un rendez-vous.","Je voudrais un renseignement.","Pouvez-vous me montrer où aller ?","Pouvez-vous l’écrire ?","Combien cela coûte-t-il ?","Je voudrais payer.","J’ai besoin d’aide pour remplir ce document.","Je voudrais parler à la personne qui s’occupe de mon dossier.","Pouvez-vous attendre que j’aie terminé d’écrire ?"}
    };
    static final String[][] BODY={
        {"Tête et visage","à la tête","au front","à la tempe","à l’œil","à l’oreille","au nez","à la joue","à la mâchoire","aux dents","à la bouche","à la langue"},
        {"Cou et gorge","à la gorge","au cou","à la nuque"},
        {"Poitrine et ventre","à la poitrine","aux côtes","au ventre","à l’estomac","au bas-ventre","à l’aine"},
        {"Dos et bassin","dans le haut du dos","au milieu du dos","dans le bas du dos","au bassin","à la hanche","au coccyx","à la fesse"},
        {"Bras et mains","à l’épaule","au bras","au coude","à l’avant-bras","au poignet","à la main","aux doigts"},
        {"Jambes et pieds","à la cuisse","au genou","au mollet","au tibia","à la cheville","au talon","au pied","aux orteils"},
        {"Autre endroit","à la peau","à une plaie","à une cicatrice","aux parties intimes","partout","à plusieurs endroits"}
    };
    static final String[] SENSATIONS={"Cela brûle.","Cela pique.","Cela serre.","Cela lance.","Cela tire.","Cela fait comme des décharges électriques.","J’ai des crampes.","J’ai des fourmillements.","La douleur est constante.","La douleur vient et repart."};
    static final String[] TIMES={"Cela vient de commencer.","Depuis ce matin.","Depuis hier.","Depuis plusieurs jours.","Depuis plus longtemps.","Je ne sais pas depuis quand."};
    private final Activity activity;private final Consumer<String> speak,add;private final Runnable medication,stop;
    private final java.util.function.BiConsumer<String,String> speakIncrement;
    private AlertDialog dialog;
    PhraseLibrary(Activity activity,Consumer<String> speak,Consumer<String> add,Runnable medication){this(activity,speak,add,medication,()->{},(inc,full)->speak.accept(full));}
    PhraseLibrary(Activity activity,Consumer<String> speak,Consumer<String> add,Runnable medication,Runnable stop){this(activity,speak,add,medication,stop,(inc,full)->speak.accept(full));}
    PhraseLibrary(Activity activity,Consumer<String> speak,Consumer<String> add,Runnable medication,Runnable stop,java.util.function.BiConsumer<String,String> speakIncrement){this.activity=activity;this.speak=speak;this.add=add;this.medication=medication;this.stop=stop;this.speakIncrement=speakIncrement;}
    void close(){if(dialog!=null)dialog.dismiss();}
    private void page(String title,String help,String[] labels,Consumer<Integer> selected,Runnable back,Consumer<Integer> held){
        if(dialog!=null)dialog.dismiss();
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);int pad=Ui.dp(activity,16);box.setPadding(pad,0,pad,0);
        if(!help.isEmpty())Ui.text(activity,box,help+(labels.length>5?"\nFaites défiler pour voir la suite.":""),16);
        ScrollView scroll=new ScrollView(activity);LinearLayout rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
        for(int i=0;i<labels.length;i++){final int index=i;Button b=Ui.button(activity,rows,labels[i],()->selected.accept(index));b.setTextSize(20);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);if(held!=null)b.setOnLongClickListener(v->{held.accept(index);return true;});}
        box.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        dialog=new AlertDialog.Builder(activity).setTitle(title).setView(box).setNegativeButton("Fermer",null).setPositiveButton("Arrêter",null).create();
        if(back!=null)dialog.setButton(AlertDialog.BUTTON_NEUTRAL,"Retour",(d,w)->back.run());
        if(back!=null)dialog.setOnKeyListener((d,key,event)->{if(key!=android.view.KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==android.view.KeyEvent.ACTION_UP)back.run();return true;});
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->stop.run());
        // Bound the scrolling content on small phones and in landscape.
        int max=(int)(activity.getResources().getDisplayMetrics().heightPixels*.62f);
        box.measure(android.view.View.MeasureSpec.makeMeasureSpec(activity.getResources().getDisplayMetrics().widthPixels,android.view.View.MeasureSpec.AT_MOST),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
        if(box.getMeasuredHeight()>max)scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,Math.max(Ui.dp(activity,96),max-(box.getMeasuredHeight()-scroll.getMeasuredHeight()))));
    }
    void open(String family){List<Integer> indices=new ArrayList<>();List<String> labels=new ArrayList<>();if(family.isEmpty()||family.equals("Besoins")){indices.add(-1);labels.add("Douleur · choisir un endroit ›");}for(int i=0;i<TOPICS.length;i++)if(family.isEmpty()||TOPICS[i][0].equals(family)){indices.add(i);labels.add(TOPICS[i][1]+" ›");}if(family.isEmpty()||family.equals("Besoins")){indices.add(-2);labels.add("Mes médicaments · personnaliser ›");}if(!family.isEmpty()){indices.add(-3);labels.add("Toutes les situations ›");}
        page(family.isEmpty()?"Toutes les situations":"Plus de phrases · "+family,"Choisissez une situation.",labels.toArray(new String[0]),n->{int i=indices.get(n);if(i==-1)pain();else if(i==-2){dialog.dismiss();medication.run();}else if(i==-3)open("");else topic(i,family);},family.isEmpty()?null:()->open(""),null);
    }
    private void topic(int index,String family){String[] t=TOPICS[index];String[] phrases=Arrays.copyOfRange(t,2,t.length);page(t[1],"Touchez pour parler. Appui long pour ajouter une phrase à vos cases.",phrases,n->speak.accept(phrases[n]),()->open(family),n->add.accept(phrases[n]));}
    void pain(){String[] labels=new String[BODY.length+2];for(int i=0;i<BODY.length;i++)labels[i]=BODY[i][0]+" ›";labels[BODY.length]="J’ai mal, sans pouvoir préciser où.";labels[BODY.length+1]="Je n’ai pas mal actuellement.";page("Où avez-vous mal ?","Choisissez une zone, puis un endroit. La phrase sera prononcée.",labels,n->{if(n>=BODY.length){speak.accept(labels[n]);return;}locations(n);},()->open("Besoins"),null);}
    private void locations(int group){String[] choices=Arrays.copyOfRange(BODY[group],1,BODY[group].length);page(BODY[group][0],"Touchez un endroit pour le dire.",choices,n->{Pain p=new Pain(choices[n]);speak.accept(p.sentence());details(p);},this::pain,null);}
    static final class Pain {
        final String location;String side="",sensation="",time="",intensity="";
        Pain(String location){this.location=location;}
        String sentence(){return "J’ai mal "+location+(side.isEmpty()?"":", "+side)+"."+(sensation.isEmpty()?"":" "+sensation)+(intensity.isEmpty()?"":" Ma douleur est de "+intensity+" sur 10.")+(time.isEmpty()?"":" "+time);}
    }
    private void details(Pain p){page("Préciser si vous le souhaitez",p.sentence(),new String[]{"🔊 Dire toute la phrase complète","Le côté ›","La sensation ›","L’intensité de 1 à 10 ›","Depuis quand ›","Ajouter cette phrase à mes cases"},n->{if(n==0)speak.accept(p.sentence());if(n==1)detail(p,"Le côté",new String[]{"du côté gauche","du côté droit","des deux côtés","au centre","Ne pas préciser"},1);if(n==2)detail(p,"La sensation",SENSATIONS,2);if(n==3){String[] levels=new String[10];for(int i=0;i<10;i++)levels[i]=(i+1)+" sur 10";detail(p,"Intensité · 1 faible, 10 maximale",levels,3);}if(n==4)detail(p,"Depuis quand",TIMES,4);if(n==5)add.accept(p.sentence());},this::pain,null);}
    private void detail(Pain p,String title,String[] values,int field){page(title,"Touchez un choix : seul ce détail sera prononcé à voix haute.",values,n->{String v=values[n];String spokenPart="";if(field==1){p.side=n==4?"":v;spokenPart=p.side;}if(field==2){p.sensation=v;spokenPart=v;}if(field==3){p.intensity=""+(n+1);spokenPart="Douleur à "+v;}if(field==4){p.time=v;spokenPart=v;}if(!spokenPart.isEmpty())speakIncrement.accept(spokenPart,p.sentence());details(p);},()->details(p),null);}
}
