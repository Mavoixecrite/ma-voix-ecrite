package fr.mavoixecrite;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class CorrectionTest {
 FrenchCorrector corrector;
 @Before public void load()throws Exception{corrector=FrenchCorrector.load(RuntimeEnvironment.getApplication());}
 String corrected(String s){return corrector.correct(s,Collections.emptySet()).text;}
 @Test public void obviousTypo(){assertEquals("bonjour",corrected("bobjour"));assertEquals("Bonjour !",corrected("Bobjour !"));}
 @Test public void commonValidWordsAreKept(){for(String s:new String[]{"Je voudrais manger une fraise.","Il faut laisser faire.","Merci beaucoup.","La voie est libre.","Paris est une ville."})assertEquals(s,corrected(s));}
 @Test public void protectedPersonalNamesAreKept(){assertEquals("bobjour",corrector.correct("bobjour",Collections.singleton("bobjour")).text);assertEquals("Bonjour Zorwan",corrected("Bonjour Zorwan"));}
 @Test public void unknownWordsAndIdentifiersStay(){for(String s:new String[]{"xqzvpln","https://bobjour.fr","bobjour@example.fr","BONJOUR","abc123bobjour"})assertEquals(s,corrected(s));}
 @Test public void contractionsAndPunctuationPreserved(){assertEquals("bonjour, j’ai soif !",corrected("bobjour, j’ai soif !"));}
 @Test public void longPastedTextIsBounded(){String text=String.join(" ",Collections.nCopies(150,"bobjour"));assertTrue(corrected(text).startsWith("bonjour bonjour"));}
}
