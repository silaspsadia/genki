import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShardHandler {
    private static final int SHARD_SIZE = 1024;
    private static int HASH_SIZE = 80;
    private static byte[] shardBuffer = new byte[SHARD_SIZE];
    private static byte[] hashBuffer = new byte[HASH_SIZE];

    public static String bytesToHexString(byte[] hash) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] hashData(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    public static void shredFile(File inputFile) {
        try {
            String dictName = "./.genki/dicts/" + inputFile.getName() + ".genki";
            File dict = new File(dictName);
            FileOutputStream dictStream = new FileOutputStream(dict);
            FileInputStream fis = new FileInputStream(inputFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            int bytesRead = 0;
            while ((bytesRead = bis.read(shardBuffer)) > 0) {
                String filename = "./.genki/shards/" + bytesToHexString(hashData(shardBuffer));
                dictStream.write(filename.getBytes());
                File shardFile = new File(inputFile.getParent(), filename);
                try {
                    FileOutputStream out = new FileOutputStream(shardFile);
                    out.write(shardBuffer, 0, bytesRead);
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }
            }
            dictStream.close();
            fis.close();
            bis.close();
            inputFile.delete();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static File glueFile(String inputDir) {
        File inputFile = new File(inputDir);
        String inputName = inputFile.getName();
        String origName = inputName.substring(0, inputName.length() - 6);
        File origFile = new File(origName);
        try {
            FileOutputStream fos = new FileOutputStream(origFile);
            FileInputStream fis = new FileInputStream(inputFile);
            while(fis.read(hashBuffer) > 0) {
                String filename = new String(hashBuffer);
                File shardFile = new File(filename);
                try {
                    FileInputStream shardInputStream = new FileInputStream(shardFile);
                    int bytesRead = shardInputStream.read(shardBuffer);
                    fos.write(shardBuffer, 0, bytesRead);
                    shardInputStream.close();
                    (new File(filename)).delete();
                } catch (IOException ex0) {
                    System.out.println(ex0.getMessage());
                    ex0.printStackTrace();
                }
            }
            fos.close();
            fis.close();
            inputFile.delete();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return origFile;
    }
}