package enigma;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import ucb.util.CommandArgs;

import static enigma.EnigmaException.*;



/** Enigma simulator.
 *  @author Zac Nelson
 */
public final class Main {

    /** Process a sequence of encryptions and decryptions, as
     *  specified by ARGS, where 1 <= ARGS.length <= 3.
     *  ARGS[0] is the name of a configuration file.
     *  ARGS[1] is optional; when present, it names an input file
     *  containing messages.  Otherwise, input comes from the standard
     *  input.  ARGS[2] is optional; when present, it names an output
     *  file for processed messages.  Otherwise, output goes to the
     *  standard output. Exits normally if there are no errors in the input;
     *  otherwise with code 1. */
    public static void main(String... args) {
        try {
            CommandArgs options =
                new CommandArgs("--verbose --=(.*){1,3}", args);
            if (!options.ok()) {
                throw error("Usage: java enigma.Main [--verbose] "
                            + "[INPUT [OUTPUT]]");
            }

            _verbose = options.contains("--verbose");
            new Main(options.get("--")).process();
            return;
        } catch (EnigmaException excp) {
            System.err.printf("Error: %s%n", excp.getMessage());
        }
        System.exit(1);
    }

    /** Open the necessary files for non-option arguments ARGS (see comment
      *  on main). */
    Main(List<String> args) {
        _config = getInput(args.get(0));

        if (args.size() > 1) {
            _input = getInput(args.get(1));
        } else {
            _input = new Scanner(System.in);
        }

        if (args.size() > 2) {
            _output = getOutput(args.get(2));
        } else {
            _output = System.out;
        }
    }

    /** Return a Scanner reading from the file named NAME. */
    private Scanner getInput(String name) {
        try {
            return new Scanner(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Return a PrintStream writing to the file named NAME. */
    private PrintStream getOutput(String name) {
        try {
            return new PrintStream(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Configure an Enigma machine from the contents of configuration
     *  file _config and apply it to the messages in _input, sending the
     *  results to _output. */
    private void process() {
        Machine mach = readConfig();

        while (_input.hasNextLine()) {
            String start = _input.nextLine();
            if (start.length() == 0) {
                _output.println();
            } else if (start.charAt(0) == '*') {
                String setting = start.substring(1);
                setUp(mach, setting);
            } else {
                String inputMsg = start.replaceAll("\\s", "");
                for (int i = 0; i < inputMsg.length(); i++) {
                    if (!_alphabet.contains(inputMsg.charAt(i))) {
                        throw new EnigmaException("input not in alphabet");
                    }
                }
                String outputMsg = mach.convert(inputMsg);
                printMessageLine(outputMsg);
            }
        }
    }




    /** Return an Enigma machine configured from the contents of configuration
     *  file _config. */
    private Machine readConfig() {
        try {
            _alphabet = new Alphabet(_config.nextLine());
            int numRotors = _config.nextInt();
            int pawls = _config.nextInt();
            _config.nextLine();
            int i = 0;
            while (_config.hasNext()) {
                allRotors.add(i, readRotor());
                i++;
            }
            return new Machine(_alphabet, numRotors, pawls, allRotors);
        } catch (NoSuchElementException excp) {
            throw error("configuration file truncated");
        }
    }

    /** Return a rotor, reading its description from _config. */
    private Rotor readRotor() {
        try {
            String stringName =  _config.next();
            String typeAndSetting = _config.next();
            String perms = _config.nextLine();

            if (_config.hasNext("\\(.*")) {
                perms = perms + _config.nextLine();
            }

            if (typeAndSetting.charAt(0) == 'M') {
                String notches = typeAndSetting.substring(1);
                Permutation movingPerm = new Permutation(perms, _alphabet);
                return new MovingRotor(stringName, movingPerm, notches);
            } else if (typeAndSetting.charAt(0) == 'N') {
                Permutation nonMovingPerm = new Permutation(perms, _alphabet);
                return new Rotor(stringName, nonMovingPerm);
            } else if (typeAndSetting.charAt(0) == 'R') {
                Permutation reflectPerm = new Permutation(perms, _alphabet);
                return new Reflector(stringName, reflectPerm);
            }
            throw new EnigmaException("Improper Rotor type");
        } catch (NoSuchElementException excp) {
            throw error("bad rotor description");
        }
    }

    /** Set M according to the specification given on SETTINGS,
     *  which must have the format specified in the assignment. */
    private void setUp(Machine M, String settings) {
        Scanner setting = new Scanner(settings);
        String[] myRotors = new String[M.numRotors()];
        ArrayList<String> allRotorsString = new ArrayList<String>();

        for (int i = 0; i < allRotors.size(); i++) {
            allRotorsString.add(allRotors.get(i).name());
        }

        for (int i = 0; i < M.numRotors(); i++) {
            myRotors[i] = setting.next();
        }

        for (int i = 0; i < myRotors.length; i++) {
            if (!allRotorsString.contains(myRotors[i])) {
                throw new EnigmaException("rotor is not valid");
            }
        }

        for (int i = 0; i < myRotors.length; i++) {
            for (int j = i + 1; j < myRotors.length; j++) {
                if (myRotors[i].equals(myRotors[j])) {
                    throw new EnigmaException("Duplicate rotor name");
                }
            }
        }

        String wheel = setting.next();

        if (wheel.length() != (myRotors.length - 1)) {
            throw new EnigmaException("wheel too short or long");
        }

        M.clearRotors();
        M.insertRotors(myRotors);
        M.setRotors(wheel);
        if (setting.hasNext("\\(.*")) {
            String plugboardPerm = setting.nextLine();
            M.setPlugboard(new Permutation(plugboardPerm, _alphabet));
        }

        for (int i = 1; i < M.getMyRotors().size(); i++) {
            if (M.getMyRotors().get(i).reflecting()) {
                throw new EnigmaException("Reflector in wrong place");
            }
        }

    }

    /** Return true iff verbose option specified. */
    static boolean verbose() {
        return _verbose;
    }

    /** Print MSG in groups of five (except that the last group may
     *  have fewer letters). */
    private void printMessageLine(String msg) {
        for (int i = 0; i < msg.length(); i += 5) {
            if ((i + 5) < msg.length()) {
                _output.print(msg.substring(i, i + 5) + " ");
            } else {
                _output.println(msg.substring(i));
            }
        }
    }

    /** Alphabet used in this machine. */
    private Alphabet _alphabet;

    /** Source of input messages. */
    private Scanner _input;

    /** Source of machine configuration. */
    private Scanner _config;

    /** File for encoded/decoded messages. */
    private PrintStream _output;

    /** arrayt list of all rotors. */
    private ArrayList<Rotor> allRotors = new ArrayList<Rotor>();

    /** True if --verbose specified. */
    private static boolean _verbose;
}
