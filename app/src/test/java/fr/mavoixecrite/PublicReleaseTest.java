package fr.mavoixecrite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={26,35})
public class PublicReleaseTest {
    @Test public void noMicrophoneOrNetworkPermission() throws Exception {
        var app=RuntimeEnvironment.getApplication();
        PackageInfo info=app.getPackageManager().getPackageInfo(app.getPackageName(),PackageManager.GET_PERMISSIONS);
        if(info.requestedPermissions!=null)for(String permission:info.requestedPermissions){
            assertNotEquals(Manifest.permission.RECORD_AUDIO,permission);
            assertNotEquals(Manifest.permission.INTERNET,permission);
        }
    }
    @Test public void removedAudioClassesAreAbsent(){
        for(String name:new String[]{"ConversationActivity","LocalDictation","ConversationStore"}){
            assertThrows(ClassNotFoundException.class,()->Class.forName("fr.mavoixecrite."+name));
        }
    }
    @Test @Config(qualifiers="w393dp-h851dp") public void phoneFeedbackUsesProjectMailbox()throws Exception{feedback();}
    @Test @Config(qualifiers="w800dp-h1280dp") public void tabletFeedbackUsesProjectMailbox()throws Exception{feedback();}
    private void feedback()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity activity=controller.get();
            var method=MainActivity.class.getDeclaredMethod("openFeedbackDialog");method.setAccessible(true);method.invoke(activity);
            var dialog=ShadowAlertDialog.getLatestAlertDialog();
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            Intent intent=Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals("mailto:mavoixecrite@proton.me",intent.getDataString());
            assertTrue(intent.getStringExtra(Intent.EXTRA_SUBJECT).contains("[MAVOIX-RETOUR] [IDEE]"));
            String body=intent.getStringExtra(Intent.EXTRA_TEXT);
            assertTrue(body.contains(BuildConfig.VERSION_NAME));
            assertFalse(body.contains("APPAREIL:"));
            assertFalse(body.contains("gmail.com"));
        }
    }
}
