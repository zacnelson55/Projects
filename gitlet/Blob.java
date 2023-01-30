package gitlet;

import java.io.File;
import java.io.Serializable;

/** Represents a blob with serialized contents.
 * @author Zac Nelson
 */

public class Blob implements Serializable {

    /** File path of blob. */
    private File _filePath;

    /** File name. */
    private String _fileName;

    /** SHA-1 ID of the blob object. */
    private String _sha1ID;

    /** Serialized contents of the blob object. */
    private byte[] _serializedFile;

    /**
     * Creates a blob object.
     * @param file Pathway to file from CWD.
     */
    public Blob(File file) {
        _filePath = file;
        _fileName = file.getName();
        _serializedFile = Utils.readContents(file);
    }


    public File getFilePath() {
        return _filePath;
    }


    public byte[] getSerializedFile() {
        return _serializedFile;
    }
}
