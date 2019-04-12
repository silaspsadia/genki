import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    public static void genKey() {
        File keyFile = new File("./.genki/key.dat");
        try {
            FileOutputStream fos = new FileOutputStream(keyFile);
            byte[] bytes = new byte[16];
            (new SecureRandom()).nextBytes(bytes);
            fos.write(bytes, 0, 16);
            fos.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static byte[] fetchKey() {
        byte[] bytes = new byte[16];
        try {
            File keyFile = new File("./.genki/key.dat");
            FileInputStream inputStream = new FileInputStream(keyFile);
            inputStream.read(bytes, 0, 16);
            inputStream.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }

    public static SecretKeySpec getKeySpec() throws Exception {
        return new SecretKeySpec(fetchKey(), ALGORITHM);
    }

    public static Cipher getCipher() throws Exception {
        return Cipher.getInstance(TRANSFORMATION);
    }

    public static void encrypt(byte[] key, File inputFile, File outputFile)
        throws CryptoException {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile);
        inputFile.delete();
    }

    public static void decrypt(byte[] key, File inputFile, File outputFile)
        throws CryptoException {
        doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile);
        inputFile.delete();
    }

    public static boolean yesNo(String msg) {
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print(msg + " Are you sure? y/n ");
            String reply = scan.nextLine();
            switch (reply) {
                case "y":
                case "yes":
                case "yup": {
                    return true;
                }
                case "n":
                case "no":
                case "0": {
                    return false;
                }
                default: {
                    continue;
                }
            }
        }
    }

    private static void doCrypto(int cipherMode, byte[] key, File inputFile, File outputFile)
        throws CryptoException {
        try {
            Key secretKey = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(cipherMode, secretKey);

            FileInputStream inputStream = new FileInputStream(inputFile);
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            byte inputBytes[] = new byte[1024];

            while(inputStream.read(inputBytes) > 0) {
                byte outputBytes[] = cipher.update(inputBytes);
                outputStream.write(outputBytes);
            }

            inputStream.close();
            outputStream.close();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException
                | IOException ex) {
            throw new CryptoException("Error: xcrypting file: " + inputFile.getName(), ex);
        }
    }
}
