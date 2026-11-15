package fr.mavoixecrite;
import android.widget.EditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={26,35})
public class WritingSuggestionsTest {
    @Test public void completionAtCursorPreservesRestOfMessage()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=controller.get();
            a.getSharedPreferences("options",0).edit().putBoolean("correct",false).commit();
            EditText input=a.findViewById(R.id.message_input);
            input.setText("Mes lun sont ici");input.setSelection(7);
            var method=MainActivity.class.getDeclaredMethod("insertWords",String.class);method.setAccessible(true);method.invoke(a,"lunettes");
            assertEquals("Mes lunettes sont ici",input.getText().toString());
            assertTrue(input.getSelectionStart()<input.length());
        }
    }
    @Test public void typingPrefixOnlyShowsMatchingCompletions()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            var method=MainActivity.class.getDeclaredMethod("computeAdaptiveWords",String.class);method.setAccessible(true);
            List<?> words=(List<?>)method.invoke(controller.get(),"Je cherche mes lun");
            assertFalse(words.isEmpty());
            for(Object word:words)assertTrue(FrenchCorrector.key(word.toString()).startsWith("lun"));
        }
    }
    @Test public void emptySuggestionDoesNotChangeDraft(){assertEquals("Bonjour",MainActivity.stitchFrenchGrammar("Bonjour",""));}
}
