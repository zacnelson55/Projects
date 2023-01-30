package enigma;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;

import static enigma.EnigmaException.*;

/** Class that represents a complete enigma machine.
 *  @author Zac Nelson
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */
    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        _alphabet = alpha;
        _numRotors = numRotors;
        _pawls = pawls;
        _allRotors = allRotors;
        _myRotors = new ArrayList<Rotor>();
        _rotorMap = new HashMap<String, Rotor>();
        _allRotorsArray = _allRotors.toArray();
        _plugboard = null;

        for (int i = 0; i < _allRotorsArray.length; i++) {
            Rotor rotor = ((Rotor) _allRotorsArray[i]);
            _rotorMap.put(rotor.name(), rotor);
        }
    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return _numRotors;
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return _pawls;
    }

    /** Return Rotor #K, where Rotor #0 is the reflector, and Rotor
     *  #(numRotors()-1) is the fast Rotor.  Modifying this Rotor has
     *  undefined results. */
    Rotor getRotor(int k) {
        return _myRotors.get(k);
    }

    Alphabet alphabet() {
        return _alphabet;
    }

    void clearRotors() {
        _myRotors.clear();
    }


    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    void insertRotors(String[] rotors) {
        for (int i = 0; i < rotors.length; i++) {
            _myRotors.add(i, _rotorMap.get(rotors[i]));
        }
    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).  */
    void setRotors(String setting) {
        for (int i = 0; i < setting.length(); i++) {
            int newSetting = _alphabet.toInt(setting.charAt(i));
            _myRotors.get(i + 1).set(newSetting);
        }
    }

    /** Return the current plugboard's permutation. */
    Permutation plugboard() {
        return _plugboard;
    }

    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    int convert(int c) {
        advanceRotors();
        if (Main.verbose()) {
            System.err.printf("[");
            for (int r = 1; r < numRotors(); r += 1) {
                System.err.printf("%c",
                        alphabet().toChar(getRotor(r).setting()));
            }
            System.err.printf("] %c -> ", alphabet().toChar(c));
        }
        if (!(plugboard() == null)) {
            c = plugboard().permute(c);
        }
        if (Main.verbose()) {
            System.err.printf("%c -> ", alphabet().toChar(c));
        }
        c = applyRotors(c);
        if (!(plugboard() == null)) {
            c = plugboard().permute(c);
        }
        if (Main.verbose()) {
            System.err.printf("%c%n", alphabet().toChar(c));
        }
        return c;
    }

    /** Advance all rotors to their next position. */
    private void advanceRotors() {
        ArrayList<Rotor> rotorsToAdvance = new ArrayList<Rotor>();
        rotorsToAdvance.add(_myRotors.get(numRotors() - 1));
        for (int i = numRotors() - 1; i > 0; i--) {
            if (_myRotors.get(i - 1).rotates() && _myRotors.get(i).atNotch()) {
                if (!rotorsToAdvance.contains(_myRotors.get(i))) {
                    rotorsToAdvance.add(_myRotors.get(i));
                }
                if (!rotorsToAdvance.contains(_myRotors.get(i - 1))) {
                    rotorsToAdvance.add(_myRotors.get(i - 1));
                }
            }
        }

        for (int i = 0; i < rotorsToAdvance.size(); i++) {
            rotorsToAdvance.get(i).advance();
        }
    }

    /** Return the result of applying the rotors to the character C (as an
     *  index in the range 0..alphabet size - 1). */
    private int applyRotors(int c) {
        int result = c;
        for (int i = _numRotors - 1; i > 0; i--) {
            result = _myRotors.get(i).convertForward(result);
        }
        for (int i = 0; i < _numRotors; i++) {
            result = _myRotors.get(i).convertBackward(result);
        }
        return result;
    }

    ArrayList<Rotor> getMyRotors() {
        return _myRotors;
    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {
        String result = "";
        for (int i = 0; i < msg.length(); i++) {
            int num = _alphabet.toInt(msg.charAt(i));
            char toAdd = _alphabet.toChar(convert(num));
            result = result + toAdd;
        }
        return result;
    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;

    /** number of rotors. */
    private int _numRotors;

    /** number of pawls. */
    private int _pawls;

    /** collection of all rotors. */
    private Collection<Rotor> _allRotors;

    /** array list of my rotors. */
    private ArrayList<Rotor> _myRotors;

    /** map to rotors. */
    private HashMap<String, Rotor> _rotorMap;

    /** all rotors as array. */
    private Object[] _allRotorsArray;

    /** plugboard permutation. */
    private Permutation _plugboard;

}
