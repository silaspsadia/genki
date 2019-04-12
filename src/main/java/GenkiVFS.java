import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class GenkiVFS {
    private static final int SHARD_SIZE = 4194304;
    private static final int HASH_SIZE = 64;
    private static byte[] shardBuffer;
    private static byte[] hashBuffer;
    private static String baseDir;
    private static String genkiDir;
    private static String shardDir;
    private static String dictsDir;
    private static SecretKeySpec key;
    private static Cipher cipher;
    private static Queue<File> fileQueue;
    private static Queue<File> recoverQueue;
    private static ZipParameters zp;

    public GenkiVFS() {
        shardBuffer = new byte[SHARD_SIZE];
        hashBuffer = new byte[HASH_SIZE];
        fileQueue = new LinkedList<>();
        recoverQueue = new LinkedList<>();
        zp = new ZipParameters();
        zp.setEncryptFiles(false);
        zp.setCompressionLevel(Zip4jConstants.COMP_STORE);
    }

    public static void initGenkiStore(String name) {
        String filename = name + ".genkis";
        if (new File(filename).exists()) {
            System.err.println("Error : Genki store \'" + name + "\' already exists.");
            return;
        }
        try {
            Files.createDirectories(Paths.get(filename));
            Files.createDirectories(Paths.get(filename + "/shards"));
            Files.createDirectories(Paths.get(filename + "/dicts"));
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean initializeGenkiStore() {
        if (new File(genkiDir).exists()) {
            return false;
        }
        try {
            Files.createDirectories(Paths.get(genkiDir));
            Files.createDirectories(Paths.get(shardDir));
            Files.createDirectories(Paths.get(dictsDir));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean deleteGenkiStore() {
        try {
            File genkiDir = new File("./.genki");
            FileUtils.deleteDirectory(genkiDir);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void genKey() {
        File genki = new File(genkiDir);
        if (!genki.exists()) {
            genki.delete();
        }
        CryptUtils.genKey();
    }

    public static void secureAll() {
        try {
            key = CryptUtils.getKeySpec();
            cipher = CryptUtils.getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, key);

            File currDirectory = new File("./");
            fileQueue = new LinkedList<>(Arrays.asList(currDirectory.listFiles()));
            fileQueue.removeIf(i -> i.getName().equals("dir.zip") || i.getName().equals(".genki") || i.getName().equals("genki.jar") || i.getName().equals(".DS_Store"));

            SecureThread t1 = new SecureThread();
            SecureThread t2 = new SecureThread();
            SecureThread t3 = new SecureThread();
            SecureThread t4 = new SecureThread();
            t1.start();
            t2.start();
            t3.start();
            t4.start();
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void recoverAll() {
        try {
            key = CryptUtils.getKeySpec();
            cipher = CryptUtils.getCipher();
            cipher.init(Cipher.DECRYPT_MODE, key);

            File dicts = new File(dictsDir);
            recoverQueue = new LinkedList<>(Arrays.asList(dicts.listFiles()));

            for (File f : recoverQueue) {
                System.out.println(f.getName());
            }
            RecoverThread r1 = new RecoverThread();
            r1.start();
            r1.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized File pollQueueSafe(Queue<File> q) {
        return q.poll();
    }

    private static class SecureThread extends Thread {
        @Override
        public void run() {
            int bytesRead;
            while (true) {
                if (fileQueue.isEmpty()) {
                    return;
                }
                try {
                    File f = pollQueueSafe(fileQueue);
                    System.out.println();
                    System.out.println(f.getName());
                    if (f.isDirectory()) {
                        ZipFile dirZip = new ZipFile(f.getName() + ".genkid");
                        dirZip.addFolder(f, zp);
                        FileUtils.deleteDirectory(f);
                        File dir = new File(f.getName() + ".genkid");
                        fileQueue.add(dir);
                        continue;
                    }
                    File dict = new File(dictsDir + f.getName() + ".genki");
                    OutputStream dictStream = Files.newOutputStream(Paths.get(dict.getPath()));
                    InputStream fis = Files.newInputStream(Paths.get(f.getPath()));
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    byte[] shardBuffer = new byte[SHARD_SIZE];
                    while ((bytesRead = bis.read(shardBuffer)) > 0) {
                        String hexString = ShardHandler.bytesToHexString(ShardHandler.hashData(shardBuffer));
                        dictStream.write(hexString.getBytes());
                        File shardFile = new File(shardDir + hexString);
                        OutputStream fos = Files.newOutputStream(Paths.get(shardFile.getPath()));
                        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                        cos.write(shardBuffer, 0, bytesRead);
                        cos.close();
                        fos.close();
                    }
                    dictStream.close();
                    fis.close();
                    f.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class RecoverThread extends Thread {
        @Override
        public void run() {
            int shardDataRead;
            while (true) {
                if (recoverQueue.isEmpty()) {
                    return;
                }
                try {
                    File fromQueue = pollQueueSafe(recoverQueue);
                    String dictName = fromQueue.getName();
                    String gluedName = dictName.substring(0, dictName.length() - 6);
                    File gluedFile = new File(gluedName);
                    InputStream fis = Files.newInputStream(Paths.get(fromQueue.getPath()));
                    OutputStream fos = Files.newOutputStream(Paths.get(gluedFile.getPath()));
                    while (fis.read(hashBuffer) > 0) {
                        String hashName = new String(hashBuffer);
                        System.out.println(hashName);
                        String filename = shardDir + hashName;
                        File shardFile = new File(filename);
                        InputStream sis = Files.newInputStream(Paths.get(shardFile.getPath()));
                        CipherInputStream cis = new CipherInputStream(sis, cipher);
                        byte[] inputBytes = new byte[8192];
                        while ((shardDataRead = cis.read(inputBytes)) > 0) {
                            fos.write(inputBytes, 0, shardDataRead);
                        }
                        cis.close();
                        sis.close();
                    }
                    fis.close();
                    fos.close();
                    if (gluedFile.getName().endsWith(".genkid")) {
                        System.out.println("Directory: " + gluedFile.getName());
                        String dirName = gluedFile.getName().substring(0, gluedFile.getName().length() - 7);
                        File dir = new File(dirName);
                        dir.mkdir();
                        ZipFile dirZip = new ZipFile(gluedFile);
                        dirZip.extractAll("./");
                        gluedFile.delete();
                    }
                    fromQueue.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void setBaseDir(String path) {
        baseDir = path;
        genkiDir = baseDir + "/.genki/";
        shardDir = genkiDir + "/shards/";
        dictsDir = genkiDir + "/dicts/";
    }

    public static boolean currDirHasKey() {
        return (new File("./.genki/key.dat")).exists();
    }

    public static boolean genkiDirExists() {
        return (new File(genkiDir)).exists();
    }

    public static boolean keyFileExists() {
        return (new File(genkiDir + "/key.dat")).exists();
    }

    public static boolean currDirIsEmpty() { return (new ArrayList(Arrays.asList((new File(baseDir)).listFiles()))).isEmpty(); }

    public static boolean shardsDirIsEmpty() {
        File shards = new File(shardDir);
        ArrayList<File> arr = new ArrayList(Arrays.asList(shards.listFiles()));
        return arr.isEmpty();
    }

    public static boolean dictsDirIsEmpty() {
        File dicts = new File(dictsDir);
        ArrayList<File> arr = new ArrayList(Arrays.asList(dicts.listFiles()));
        return arr.isEmpty();
    }
}
