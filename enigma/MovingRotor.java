package enigma;

import static enigma.EnigmaException.*;

/** Class that represents a rotating rotor in the enigma machine.
 *  @author Zac Nelson
 */
class MovingRotor extends Rotor {

    /** A rotor named NAME whose permutation in its default setting is
     *  PERM, and whose notches are at the positions indicated in NOTCHES.
     *  The Rotor is initally in its 0 setting (first character of its
     *  alphabet).
     */
    MovingRotor(String name, Permutation perm, String notches) {
        super(name, perm);
        _notches = notches;
        set(setting());
    }
    /** Returns true if the moving rotor is at a notch. */
    @Override
    boolean atNotch() {
        return _notches.indexOf(alphabet().toChar(setting())) > -1;
    }
    @Override boolean rotates() {
        return true;
    }

    @Override
    void advance() {
        set(setting() + 1);
    }

    @Override
    String notches() {
        return _notches;
    }

    /** notches. */
    private String _notches;

}
