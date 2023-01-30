package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Zac Nelson
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GITLET_FOLDER = new File(".gitlet");

    /** Staging folder. */
    static final File STAGING = new File(GITLET_FOLDER, "staging");

    /** Reme folder. */
    static final File REMOVE = new File(STAGING, "remove");

    /** Add folder. */
    static final File ADD = new File(STAGING, "add");

    /** Commit folder. */
    static final File COMMITS = new File(GITLET_FOLDER, "commits");

    /** Branches folder. */
    static final File BRANCHES = new File(GITLET_FOLDER, "branches");

    /** Blobs that contains a hashmap to blobs. */
    static final File BLOBS = new File(GITLET_FOLDER, "blobs");

    /** Pointer file to head branch. */
    static final File HEAD = new File(GITLET_FOLDER, "head");

    /** Pointer file to master branch. */
    static final File MASTER = new File(BRANCHES, "master");


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            } else if (args[0].equals("init")) {
                init();
            } else if (args[0].equals("add")) {
                checkInit();
                add(args[1]);
            } else if (args[0].equals("commit")) {
                if (args.length == 1) {
                    throw new GitletException("Please enter a commit message.");
                }
                checkInit();
                commit(args[1]);
            } else if (args[0].equals("log")) {
                checkInit();
                log();
            } else if (args[0].equals("checkout")) {
                checkInit();
                int len = args.length;
                if (len == 2) {
                    checkout(args[1]);
                } else if (len == 3) {
                    checkout(args[2], getHead());
                } else if (len == 4) {
                    if (args[2].equals("--")) {
                        checkout(args[3], args[1]);
                    } else {
                        throw new GitletException("Incorrect operands.");
                    }
                } else {
                    throw new GitletException("Incorrect operands.");
                }
            } else if (args[0].equals("rm")) {
                rm(args[1]);
            } else if (args[0].equals("global-log")) {
                globalLog();
            } else if (args[0].equals("find")) {
                find(args[1]);
            } else if (args[0].equals("status")) {
                checkInit();
                status();
            } else if (args[0].equals("branch")) {
                branch(args[1]);
            } else if (args[0].equals("rm-branch")) {
                rmBranch(args[1]);
            } else if (args[0].equals("reset")) {
                reset(args[1]);
            } else if (args[0].equals("merge")) {
                merge(args[1]);
            } else {
                throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void checkInit() {
        if (!GITLET_FOLDER.exists()) {
            String m = "Not in an initialized Gitlet directory.";
            throw new GitletException(m);
        }
    }

    public static void init() throws IOException {
        if (GITLET_FOLDER.exists()) {
            String m1 = "A Gitlet version-control system ";
            String m2 = "already exists in the current directory.";
            throw new GitletException(m1 + m2);
        }

        GITLET_FOLDER.mkdir();
        STAGING.mkdir();
        ADD.mkdir();
        REMOVE.mkdir();
        COMMITS.mkdir();
        BRANCHES.mkdir();
        BLOBS.mkdir();
        HEAD.createNewFile();
        MASTER.createNewFile();

        HashMap<String, String> map = new HashMap<String, String>();
        Commit initCommit = new Commit("initial commit", null, map);
        Utils.writeContents(HEAD, "master");
        saveCommit(initCommit);
    }

    public static void add(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new GitletException("File does not exist.");
        }
        byte[] serializedContents = Utils.readContents(file);
        String sha1 = Utils.sha1(fileName, serializedContents);
        Commit headCommit = getHeadCommit();
        if (!headCommit.getBlobMap().containsValue(sha1)) {
            stageFile(ADD, file, sha1);
            makeNewBlob(sha1, serializedContents);
        } else {
            new File(ADD, fileName).delete();
            new File(REMOVE, fileName).delete();
        }
    }


    public static void commit(String message) throws IOException {
        if (message.length() == 0) {
            throw new GitletException("Please enter a commit message.");
        }

        if (ADD.listFiles().length == 0 && REMOVE.listFiles().length == 0) {
            throw new GitletException("No changes added to the commit.");
        }

        String parentSha1 = getHead();
        Commit parentCommit = getHeadCommit();
        HashMap<String, String> parentMap = parentCommit.getBlobMap();

        Commit commit = new Commit(message, parentSha1, parentMap);

        File[] addFiles = ADD.listFiles();
        File[] removeFiles = REMOVE.listFiles();
        for (int i = 0; i < addFiles.length; i++) {
            String fileName = addFiles[i].getName();
            commit.addBlob(fileName, Utils.readContentsAsString(addFiles[i]));
        }
        for (int i = 0; i < removeFiles.length; i++) {
            String fileName = removeFiles[i].getName();
            commit.removeBlob(fileName);
        }
        cleanDirectory(ADD);
        cleanDirectory(REMOVE);
        saveCommit(commit);
    }

    public static void rm(String fileName) throws IOException {
        File addFile = new File(ADD, fileName);

        File file = new File(fileName);
        Commit currCommit = getHeadCommit();
        HashMap<String, String> map = currCommit.getBlobMap();


        if (!addFile.exists() && !map.containsKey(fileName)) {
            throw new GitletException("No reason to remove the file.");
        }

        if (addFile.exists()) {
            addFile.delete();
        }

        if (map.containsKey(fileName)) {
            String sha1 = map.get(fileName);
            stageFile(REMOVE, file, map.get(fileName));
            Utils.restrictedDelete(file);
        }
    }

    public static void log() {
        Commit currCommit = getHeadCommit();
        String parentSha1 = currCommit.getParent();
        while (parentSha1 != null) {
            printCommitInfo(currCommit);
            File newFile = new File(COMMITS, currCommit.getParent());
            currCommit = Utils.readObject(newFile, Commit.class);
            parentSha1 = currCommit.getParent();
        }
        printCommitInfo(currCommit);
    }

    public static void globalLog() {
        List<String> files = Utils.plainFilenamesIn(COMMITS);
        int fileLen = files.size();
        for (int i = 0; i < fileLen; i++) {
            printCommitInfo(getCommit(files.get(i)));
        }
    }

    public static void find(String message) {
        List<String> files = Utils.plainFilenamesIn(COMMITS);
        int fileLen = files.size();
        int counter = 0;
        for (int i = 0; i < fileLen; i++) {
            Commit commit = getCommit(files.get(i));
            if (commit.getMessage().equals(message)) {
                System.out.println(files.get(i));
                counter++;
            }
        }
        if (counter == 0) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    public static void status() {
        List<String> branches = Utils.plainFilenamesIn(BRANCHES);
        List<String> add = Utils.plainFilenamesIn(ADD);
        List<String> remove = Utils.plainFilenamesIn(REMOVE);
        String headName = Utils.readContentsAsString(HEAD);

        System.out.println("=== Branches ===");
        for (int i = 0; i < branches.size(); i++) {
            if (headName.equals(branches.get(i))) {
                System.out.println("*" + branches.get(i));
            } else {
                System.out.println(branches.get(i));
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (int i = 0; i < add.size(); i++) {
            System.out.println(add.get(i));
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (int i = 0; i < remove.size(); i++) {
            System.out.println(remove.get(i));
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
    }

    public static void checkout(String fileName, String commitSha1)
            throws IOException {
        Commit currCommit = getCommit(commitSha1);
        if (!currCommit.getBlobMap().containsKey(fileName)) {
            throw new GitletException("File does not exist in that commit.");
        }
        String sha1 = currCommit.getBlobMap().get(fileName);
        File blobFile = new File(BLOBS, sha1);
        byte[] contents = Utils.readContents(blobFile);
        File currFile = new File(fileName);
        if (!currFile.exists()) {
            currFile.createNewFile();
        }
        Utils.writeContents(currFile, contents);
    }

    public static void checkout(String branch) throws IOException {
        File branchFile = new File(BRANCHES, branch);
        File headBranch = getHeadBranch();

        if (!branchFile.exists()) {
            throw new GitletException("No such branch exists.");
        } else if (headBranch.getName().equals(branch)) {
            String m = "No need to checkout the current branch.";
            throw new GitletException((m));
        }

        Commit headCommit = getHeadCommit();
        HashMap<String, String> headMap = headCommit.getBlobMap();

        String branchSha1 = Utils.readContentsAsString(branchFile);
        Commit branchCommit = getCommit(branchSha1);
        HashMap<String, String> branchMap = branchCommit.getBlobMap();

        List<String> cwdFileNames = Utils.plainFilenamesIn(".");
        for (int i = 0; i < cwdFileNames.size(); i++) {
            String fileName = cwdFileNames.get(i);
            boolean inBranch = branchMap.containsKey(fileName);
            if (inBranch && !headMap.containsKey(fileName)) {
                String m1 = "There is an untracked file in the way; ";
                String m2 = "delete it, or add and commit it first.";
                throw new GitletException(m1 + m2);
            }
        }

        for (String branchFileName: branchMap.keySet()) {
            checkout(branchFileName, branchSha1);
        }
        for (String headFileName: headMap.keySet()) {
            if (!branchMap.containsKey(headFileName)) {
                File file = new File(headFileName);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        cleanDirectory(ADD);
        cleanDirectory(REMOVE);
        Utils.writeContents(HEAD, branch);
    }

    public static void branch(String branchName) {
        File branch = new File(BRANCHES, branchName);
        if (branch.exists()) {
            String m = "A branch with that name already exists.";
            throw new GitletException(m);
        }
        Utils.writeContents(branch, getHead());
    }

    public static void rmBranch(String branchName) {
        File branch = new File(BRANCHES, branchName);
        if (!branch.exists()) {
            String m = "A branch with that name does not exist.";
            throw new GitletException(m);
        } else if (branchName.equals(getHeadBranch().getName())) {
            throw new GitletException("Cannot remove the current branch.");
        } else {
            branch.delete();
        }
    }

    public static void reset(String commitID) throws IOException {
        Commit newCommit = getCommit(commitID);
        HashMap<String, String> newMap = newCommit.getBlobMap();

        Commit headCommit = getHeadCommit();
        HashMap<String, String> headMap = headCommit.getBlobMap();

        List<String> cwdFileNames = Utils.plainFilenamesIn(".");
        for (String fileName: cwdFileNames) {
            boolean inNewMap = newMap.containsKey(fileName);
            if (inNewMap && !headMap.containsKey(fileName)) {
                String m1 = "There is an untracked file in the way; ";
                String m2 = "delete it, or add and commit it first.";
                throw new GitletException(m1 + m2);
            }
        }


        for (String fileName: newMap.keySet()) {
            checkout(fileName, commitID);
        }

        for (String headFileName: headMap.keySet()) {
            if (!newMap.containsKey(headFileName)) {
                File file = new File(headFileName);
                if (file.exists()) {
                    file.delete();
                }
            }
        }

        cleanDirectory(ADD);
        cleanDirectory(REMOVE);
        Utils.writeContents(getHeadBranch(), commitID);
    }

    public static void merge(String branchName) throws IOException {
        int addLen = ADD.listFiles().length;
        int removeLen = REMOVE.listFiles().length;
        if (addLen > 0 || removeLen > 0) {
            throw new GitletException("You have uncommitted changes.");
        }

        File mergeBranch = new File(BRANCHES, branchName);
        File headBranch = getHeadBranch();
        if (!mergeBranch.exists()) {
            String m = "A branch with that name does not exist.";
            throw new GitletException(m);
        } else if (headBranch.equals(mergeBranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }

        Commit headCommit = getHeadCommit();
        Commit mergeCommit = getCommit(Utils.readContentsAsString(mergeBranch));
        Commit splitCommit = getSplitPoint(headCommit, mergeCommit);
        String headComID = Utils.readContentsAsString(headBranch);
        String mergeComID = Utils.readContentsAsString(mergeBranch);
        HashMap<String, String> headMap = headCommit.getBlobMap();
        HashMap<String, String> mergeMap = mergeCommit.getBlobMap();
        HashMap<String, String> splitMap = splitCommit.getBlobMap();

        List<String> cwdFileNames = Utils.plainFilenamesIn(".");
        for (String fileName: cwdFileNames) {
            boolean inMergeMap = mergeMap.containsKey(fileName);
            if (inMergeMap && !headMap.containsKey(fileName)) {
                String m1 = "There is an untracked file in the way; ";
                String m2 = "delete it, or add and commit it first.";
                throw new GitletException(m1 + m2);
            }
        }

        if (headCommit.equals(splitCommit)) {
            checkout(branchName);
            System.out.println("Current branch fast-forwarded.");
        } else if (mergeCommit.equals(splitCommit)) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
        }

    }

    public static Commit getSplitPoint(Commit commit1, Commit commit2) {
        while (commit1.getParent() != null) {
            while (commit2.getParent() != null) {
                String parent1 = commit1.getParent();
                String parent2 = commit2.getParent();
                if (parent1.equals(parent2)) {
                    return getCommit(parent1);
                }
                commit2 = getParentCommit(commit2);
            }
            commit1 = getParentCommit(commit1);
        }
        return commit1;
    }

    public static void cleanDirectory(File dir) {
        for (File file: dir.listFiles()) {
            file.delete();
        }
    }

    public static void saveCommit(Commit commit) throws IOException {
        byte[] serializedCommit = Utils.serialize(commit);
        String sha1 = Utils.sha1(serializedCommit);
        File commitFile = new File(COMMITS, sha1);
        commitFile.createNewFile();
        Utils.writeContents(commitFile, serializedCommit);

        Utils.writeContents(getHeadBranch(), sha1);
    }

    public static void stageFile(File addOrRemove, File file, String sha1)
            throws IOException {
        File stageFile = new File(addOrRemove, file.getName());
        if (!stageFile.exists()) {
            stageFile.createNewFile();
        }
        Utils.writeContents(stageFile, sha1);
    }

    public static void makeNewBlob(String sha1, byte [] serializedContents)
            throws IOException {
        File blob = new File(BLOBS, sha1);
        if (!blob.exists()) {
            blob.createNewFile();
            Utils.writeContents(blob, serializedContents);
        }
    }

    public static String getSHA1(File file) {
        byte[] serialized = Utils.readContents(file);
        return Utils.sha1(file.getName(), serialized);
    }

    public static String getHead() {
        return Utils.readContentsAsString(getHeadBranch());
    }

    public static File getHeadBranch() {
        String headBranch = Utils.readContentsAsString(HEAD);
        File branch = new File(BRANCHES, headBranch);
        return branch;
    }

    public static String getMaster() {
        return Utils.readContentsAsString(MASTER);
    }

    public static Commit getHeadCommit() {
        File headFile = new File(COMMITS, getHead());
        return Utils.readObject(headFile, Commit.class);
    }

    public static Commit getParentCommit(Commit currentCommit) {
        String parentSha1 = currentCommit.getParent();
        File parentFile = new File(COMMITS, parentSha1);
        return Utils.readObject(parentFile, Commit.class);
    }

    public static Commit getCommit(String sha1) {
        if (sha1.length() < Utils.UID_LENGTH) {
            for (String fileName: Utils.plainFilenamesIn(COMMITS)) {
                if (fileName.startsWith(sha1)) {
                    File commitFile = new File(COMMITS, fileName);
                    return Utils.readObject(commitFile, Commit.class);
                }
            }
            throw new GitletException("No commit with that id exists.");
        } else {
            File commitFile = new File(COMMITS, sha1);
            if (!commitFile.exists()) {
                throw new GitletException("No commit with that id exists.");
            }
            return Utils.readObject(commitFile, Commit.class);
        }
    }

    public static void printCommitInfo(Commit commit) {
        byte[] serialized = Utils.serialize(commit);
        System.out.println("===");
        System.out.println("commit " + Utils.sha1(serialized));
        System.out.println("Date: " + commit.getTime());
        System.out.println(commit.getMessage());
        System.out.println();
    }

}
