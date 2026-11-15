package fr.mavoixecrite;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

final class Ui {
    interface ImeObserver { void onImeVisibility(boolean visible); }
    static final int BG=0xffF4F3EC, INK=0xff1D3535, MUTED=0xff526665, TEAL=0xff1D635F, LINE=0xffC8D6D0, URGENT=0xffB84A27, CARD_BORDER=0xffC6D4CE, URGENT_BG=0xffFFF1E3, URGENT_BORDER=0xffB84A27;
    static int dp(Activity a,int n){return (int)(a.getResources().getDisplayMetrics().density*n+.5f);}
    static GradientDrawable shape(int color,int radius,Activity a){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));return d;}
    static GradientDrawable borderedShape(int color,int strokeColor,float strokeDp,int radius,Activity a){GradientDrawable d=shape(color,radius,a);d.setStroke((int)(a.getResources().getDisplayMetrics().density*strokeDp+.5f),strokeColor);return d;}
    static GradientDrawable borderedShape(int color,int radius,int strokeColor,float strokeDp,Activity a){return borderedShape(color,strokeColor,strokeDp,radius,a);}
    static android.graphics.drawable.Drawable touch(Activity a,int color,int radius){
        int border=color==Color.WHITE?0xffC6D4CE:color==0xffDFEDE2?0xff68A881:color==0xffF3E4DB?0xffD48875:color==0xffFFF1E3?0xffD98A4E:0x00000000;
        GradientDrawable base=border!=0?borderedShape(color,border,1.5f,radius,a):shape(color,radius,a);
        return new RippleDrawable(ColorStateList.valueOf(0x26375E59),base,shape(Color.WHITE,radius,a));
    }
    static Drawable choiceBackground(Activity a){GradientDrawable base=borderedShape(0xffE3F0EC,0xff5C9B8E,2f,20,a);return new RippleDrawable(ColorStateList.valueOf(0x26375E59),base,shape(Color.WHITE,20,a));}
    static Drawable urgentBackground(Activity a){return urgentBackground(a,false);}
    static Drawable urgentBackground(Activity a,boolean choice){GradientDrawable base=borderedShape(0xffFFF1E3,0xffD9824B,choice?2.2f:1.8f,20,a);return new RippleDrawable(ColorStateList.valueOf(0x26B84A27),base,shape(Color.WHITE,20,a));}
    static LinearLayout root(Activity a){
        a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(android.os.Build.VERSION.SDK_INT>=30)a.getWindow().setDecorFitsSystemWindows(false);
        boolean small=a.getResources().getConfiguration().screenWidthDp<600;
        int hp=dp(a,small?14:24), vp=dp(a,small?8:12);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setPadding(hp,vp,hp,vp);
        root.setOnApplyWindowInsetsListener((v,i)->{
            if(android.os.Build.VERSION.SDK_INT>=30){Insets s=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(hp+s.left,vp+s.top,hp+s.right,vp+s.bottom);if(a instanceof ImeObserver)((ImeObserver)a).onImeVisibility(i.isVisible(WindowInsets.Type.ime()));}
            return i;});
        return root;
    }
    static TextView label(Activity a,String value,int size,int color){TextView t=new TextView(a);t.setText(value);t.setTextColor(color);t.setTextSize(size);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(a,3),1f);return t;}
    static TextView text(Activity a,LinearLayout root,String value,int size){TextView t=label(a,value,size,INK);t.setPadding(0,dp(a,6),0,dp(a,8));root.addView(t);return t;}
    static Button action(Activity a,String value,boolean primary,Runnable action){Button b=new Button(a);b.setAllCaps(false);b.setText(value);b.setTextSize(18);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(primary?Color.WHITE:TEAL);b.setBackground(touch(a,primary?TEAL:0xffE6EDE9,16));b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(a,56));b.setMinimumHeight(dp(a,56));b.setPadding(dp(a,16),dp(a,10),dp(a,16),dp(a,10));b.setStateListAnimator(null);b.setOnClickListener(v->action.run());return b;}
    static Button button(Activity a,LinearLayout root,String value,Runnable action){Button b=action(a,value,false,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(a,8);root.addView(b,lp);return b;}
    static void gap(Activity a,LinearLayout root,int n){View v=new View(a);root.addView(v,new LinearLayout.LayoutParams(1,dp(a,n)));}
    static void error(Activity a,String msg){new android.app.AlertDialog.Builder(a).setTitle("Impossible de terminer").setMessage(msg).setPositiveButton("D’accord",null).show();}
    static final class Glyph extends Drawable {
        final String kind;final int color;final Paint p=new Paint(3);
        Glyph(String k,int c){kind=k;color=c;}
        @Override public void draw(Canvas c){c.save();Rect r=getBounds();c.translate(r.left,r.top);c.scale(r.width()/32f,r.height()/32f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.4f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            Path path=new Path();
            switch(kind){
                case "choice":for(int y:new int[]{8,16,24}){c.drawCircle(6,y,1.5f,p);c.drawLine(12,y,25,y,p);}break;
                case "yes":path.moveTo(6,16);path.lineTo(13,23);path.lineTo(26,9);c.drawPath(path,p);break;
                case "no":c.drawLine(9,9,23,23,p);c.drawLine(23,9,9,23,p);break;
                case "heart":path.moveTo(16,26);path.cubicTo(-2,14,7,2,16,11);path.cubicTo(25,2,34,14,16,26);c.drawPath(path,p);break;
                case "sun":c.drawCircle(16,16,6,p);for(int i=0;i<8;i++){double an=i*Math.PI/4;c.drawLine(16+(float)Math.cos(an)*10,16+(float)Math.sin(an)*10,16+(float)Math.cos(an)*13,16+(float)Math.sin(an)*13,p);}break;
                case "clock":c.drawCircle(16,16,11,p);c.drawLine(16,9,16,16,p);c.drawLine(16,16,21,19,p);break;
                case "cup":c.drawRoundRect(7,9,22,25,3,3,p);c.drawArc(18,11,29,21,-90,180,false,p);c.drawLine(10,5,10,3,p);c.drawLine(17,5,17,3,p);break;
                case "food":c.drawCircle(16,17,9,p);c.drawCircle(16,17,4,p);c.drawLine(4,8,4,25,p);c.drawLine(28,8,28,25,p);break;
                case "bed":c.drawLine(4,11,4,25,p);c.drawLine(28,16,28,25,p);c.drawLine(4,21,28,21,p);c.drawRoundRect(7,14,15,19,2,2,p);c.drawRoundRect(16,15,27,21,3,3,p);break;
                case "wc":c.drawRoundRect(6,4,26,28,3,3,p);c.drawCircle(16,11,2.2f,p);c.drawLine(16,14,16,22,p);c.drawLine(12,17,20,17,p);break;
                case "bell":path.moveTo(10,21);path.lineTo(22,21);path.quadTo(21,10,16,8);path.quadTo(11,10,10,21);c.drawPath(path,p);c.drawLine(7,21,25,21,p);c.drawArc(13,21,19,26,0,180,false,p);break;
                case "help":c.drawCircle(16,16,11,p);c.drawLine(16,9,16,23,p);c.drawLine(9,16,23,16,p);break;
                case "voice":c.drawArc(9,8,23,24,-55,110,false,p);c.drawArc(6,3,29,29,-55,110,false,p);path.moveTo(3,13);path.lineTo(8,13);path.lineTo(14,8);path.lineTo(14,24);path.lineTo(8,19);path.lineTo(3,19);path.close();c.drawPath(path,p);break;
                case "menu":for(int i=0;i<3;i++){p.setStyle(Paint.Style.FILL);c.drawCircle(8+i*8,16,1.6f,p);}break;
                case "trash":c.drawLine(8,9,24,9,p);c.drawRoundRect(10,9,22,27,2,2,p);c.drawLine(13,5,19,5,p);c.drawLine(14,14,14,22,p);c.drawLine(18,14,18,22,p);break;
                default:path.moveTo(7,7);path.lineTo(25,7);path.lineTo(25,21);path.lineTo(16,21);path.lineTo(10,27);path.lineTo(10,21);path.lineTo(7,21);path.close();c.drawPath(path,p);
            }c.restore();
        }
        @Override public void setAlpha(int a){p.setAlpha(a);} @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static void glyph(Activity a,Button b,String kind,int color,int size){Glyph d=new Glyph(kind,color);d.setBounds(0,0,dp(a,size),dp(a,size));b.setCompoundDrawables(d,null,null,null);b.setCompoundDrawablePadding(dp(a,14));}
}
