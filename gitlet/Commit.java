package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** Commit class for Gitlet.
 *  @author Zac Nelson
 */

public class Commit implements Serializable {

    /** The date the commit was made. */
    private String _timestamp;

    /** The message written in the commit. */
    private String _message;

    /** A SHA-1 ID pointer to the previous commit. */
    private String _parent;

    /** A map from file name to sha1 of a blob in this commit. */
    private HashMap<String, String> _blobMap;

    public Commit(String mess, String par, HashMap<String, String> parBlobMap) {
        _message = mess;
        _parent = par;
        _blobMap = parBlobMap;
        if (_parent == null) {
            _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
        } else {
            _timestamp = getDateTime();
        }
    }

    public void addBlob(String fileName, String sha1) {
        _blobMap.put(fileName, sha1);
    }

    public void removeBlob(String fileName) {
        _blobMap.remove(fileName);
    }

    public HashMap<String, String> getBlobMap() {
        return _blobMap; }

    public String getTime() {
        return _timestamp; }

    public String getMessage() {
        return _message; }

    public String getParent() {
        return _parent; }

    public String getDateTime() {
        Date date = new Date();
        String format = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

}
