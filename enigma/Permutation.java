package enigma;

import java.util.HashMap;

import static enigma.EnigmaException.*;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author Zac Nelson
 */
class Permutation {

    /** list of cycles. */
    private String[] cycleList;

    /** map of cycles. */
    private HashMap<Character, Character> _cycles;

    /** map of inverse cycles. */
    private HashMap<Character, Character> _inverses;
    Permutation(String cycles, Alphabet alphabet) {
        _cycles = new HashMap<Character, Character>();
        _inverses = new HashMap<Character, Character>();
        int openParenth = 0;
        int closeParenth = 0;

        for (int i = 0; i < cycles.length(); i++) {
            if (cycles.charAt(i) == '(') {
                openParenth++;
            }
            if (cycles.charAt(i) == ')') {
                closeParenth++;
            }
        }
        if (openParenth != closeParenth) {
            throw new EnigmaException("Unmatched parentheses");
        }


        _alphabet = alphabet;
        cycles = cycles.replaceAll("[\\(\\s]+", "");
        cycleList = cycles.split("\\)");
        String[] inverseCycleList = new String[cycleList.length];
        for (int i = 0; i < cycleList.length; i++) {
            String invStr = "";
            char ch;
            for (int x = 0; x < cycleList[i].length(); x++) {
                ch = cycleList[i].charAt(x);
                invStr = ch + invStr;
            }
            inverseCycleList[i] = invStr;
        }
        for (int i = 0; i < cycleList.length; i++) {
            addCycle(cycleList[i]);
            addInverseCycle(inverseCycleList[i]);
        }
        for (int i = 0; i < _alphabet.size(); i++) {
            if (_cycles.get(_alphabet.toChar(i)) == null) {
                _cycles.put(_alphabet.toChar(i), _alphabet.toChar(i));
                _inverses.put(_alphabet.toChar(i), _alphabet.toChar(i));
            }
        }

    }
    HashMap getCycle() {
        return _cycles;
    }
    HashMap getInverses() {
        return _inverses;
    }

    /** Add the cycle c0->c1->...->cm->c0 to the permutation, where CYCLE is
     *  c0c1...cm. */
    private void addCycle(String cycle) {
        if (cycle.length() > 0) {
            char first = cycle.charAt(0);
            char last = cycle.charAt(cycle.length() - 1);
            for (int i = 0; i < cycle.length() - 1; i++) {
                _cycles.put(cycle.charAt(i), cycle.charAt(i + 1));
            }
            _cycles.put(last, first);
        }
    }
    private void addInverseCycle(String invCycle) {
        if (invCycle.length() > 0) {
            char first = invCycle.charAt(0);
            char last = invCycle.charAt(invCycle.length() - 1);
            for (int i = 0; i < invCycle.length() - 1; i++) {
                _inverses.put(invCycle.charAt(i), invCycle.charAt(i + 1));
            }
            _inverses.put(last, first);
        }
    }

    /** Return the value of P modulo the size of this permutation. */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /** Returns the size of the alphabet I permute. */
    int size() {
        return _alphabet.size();
    }

    /** Return the result of applying this permutation to P modulo the
     *  alphabet size. */
    int permute(int p) {
        return _alphabet.toInt(_cycles.get(_alphabet.toChar(wrap(p))));
    }

    /** Return the result of applying the inverse of this permutation
     *  to  C modulo the alphabet size. */
    int invert(int c) {
        return _alphabet.toInt(_inverses.get(_alphabet.toChar(wrap(c))));
    }

    /** Return the result of applying this permutation to the index of P
     *  in ALPHABET, and converting the result to a character of ALPHABET. */
    char permute(char p) {
        return _alphabet.toChar(permute(_alphabet.toInt(p)));
    }

    /** Return the result of applying the inverse of this permutation to C. */
    char invert(char c) {
        return _alphabet.toChar(invert(_alphabet.toInt(c)));
    }

    /** Return the alphabet used to initialize this Permutation. */
    Alphabet alphabet() {
        return _alphabet;
    }

    /** Return true iff this permutation is a derangement (i.e., a
     *  permutation for which no value maps to itself). */
    boolean derangement() {
        return (_cycles.size() == _alphabet.size());
    }

    /** Alphabet of this permutation. */
    private Alphabet _alphabet;
}
