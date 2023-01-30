package enigma;
import static enigma.EnigmaException.*;

/** An alphabet of encodable characters.  Provides a mapping from characters
 *  to and from indices into the alphabet.
 *  @author Zac Nelson
 */
class Alphabet {

    /** A new alphabet containing CHARS. The K-th character has index
     *  K (numbering from 0). No character may be duplicated. */
    private String _alph;
    Alphabet(String chars) {
        _alph = chars;
    }

    /** A default alphabet of all upper-case characters. */
    Alphabet() {
        this("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    /** Returns the size of the alphabet. */
    int size() {
        return _alph.length();
    }

    /** Returns true if CH is in this alphabet. */
    boolean contains(char ch) {
        return _alph.indexOf(ch) > -1;
    }

    /** Returns character number INDEX in the alphabet, where
     *  0 <= INDEX < size(). */
    char toChar(int index) {
        if ((0 <= index) && (index < size())) {
            return _alph.charAt(index);
        } else {
            throw error("index out range");
        }
    }

    /** Returns the index of character CH which must be in
     *  the alphabet. This is the inverse of toChar(). */
    int toInt(char ch) {
        return _alph.indexOf(ch);
    }

}
