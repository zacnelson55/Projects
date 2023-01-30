package enigma;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import static org.junit.Assert.*;

import static enigma.TestUtils.*;

/** The suite of all JUnit tests for the Permutation class.
 *  @author Zac Nelson
 */
public class PermutationTest {

    /** Testing time limit. */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    /* ***** TESTING UTILITIES ***** */

    private Permutation perm;
    private String alpha = UPPER_STRING;

    /** Check that perm has an alphabet whose size is that of
     *  FROMALPHA and TOALPHA and that maps each character of
     *  FROMALPHA to the corresponding character of FROMALPHA, and
     *  vice-versa. TESTID is used in error messages. */
    private void checkPerm(String testId,
                           String fromAlpha, String toAlpha) {
        int N = fromAlpha.length();
        assertEquals(testId + " (wrong length)", N, perm.size());
        for (int i = 0; i < N; i += 1) {
            char c = fromAlpha.charAt(i), e = toAlpha.charAt(i);
            assertEquals(msg(testId, "wrong translation of '%c'", c),
                         e, perm.permute(c));
            assertEquals(msg(testId, "wrong inverse of '%c'", e),
                         c, perm.invert(e));
            int ci = alpha.indexOf(c), ei = alpha.indexOf(e);
            assertEquals(msg(testId, "wrong translation of %d", ci),
                         ei, perm.permute(ci));
            assertEquals(msg(testId, "wrong inverse of %d", ei),
                         ci, perm.invert(ei));
        }
    }

    /* ***** TESTS ***** */

    @Test
    public void checkIdTransform() {
        perm = new Permutation("", UPPER);
        checkPerm("identity", UPPER_STRING, UPPER_STRING);
    }
    @Test
    public void constructorTest1() {
        Alphabet alph1 = new Alphabet("abcdefghi");
        Permutation perm1 = new Permutation("(adcb)", alph1);
        assertEquals('c', perm1.getCycle().get('d'));
        assertEquals('a', perm1.getInverses().get('d'));
        assertEquals('i', perm1.getCycle().get('i'));
        assertEquals('i', perm1.getInverses().get('i'));
    }
    @Test
    public void constructorTest2() {
        Alphabet alph2 = new Alphabet("abcdefghi");
        Permutation perm2 = new Permutation("(adcb) (geih)", alph2);
        assertEquals('c', perm2.getCycle().get('d'));
        assertEquals('a', perm2.getInverses().get('d'));
        assertEquals('f', perm2.getCycle().get('f'));
        assertEquals('i', perm2.getCycle().get('e'));
        assertEquals('f', perm2.getInverses().get('f'));
        assertEquals('g', perm2.getInverses().get('e'));
    }

}
