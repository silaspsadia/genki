import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GenkiCLI {
    private static Scanner console;

    public static void main(String[] args) { ;
        console = new Scanner(System.in);
        System.out.println(
                "\n" +
                "############################\n" +
                "###   /--.\"  -  /  -+-   ###\n" +
                "###     /      /   -+-   ###\n" +
                "###    /      /     |    ###\n" +
                "############################\n\n" +
                "    Fast. Secure. Genki." +
                "\n"
        );
        while (true) {
            System.out.print(">>> ");
            String input = console.nextLine();
            handleCommand(input);
        }
    }

    private static void handleCommand(String line) {
        StringTokenizer st = new StringTokenizer(line);
        int count = st.countTokens();
        if (count == 0) {
            return;
        }
        if (count < 1 || count > 3) {
            System.err.println("Error : invalid command entered\n");
            return;
        }
        String operation = st.nextToken();
        switch (operation) {
            case "l":
            case "list":
            case "s":
            case "show" : {
                if (count == 1) {
                    System.out.println("Showing all genki stores:");
                    List<File> list = Arrays.asList((new File(".")).listFiles())
                            .stream()
                            .filter(f -> f.getName().endsWith(".genkis"))
                            .collect(Collectors.toList());
                    if (list.isEmpty()) {
                        System.out.println();
                        return;
                    }
                    for (File f : list) {
                        System.out.print("... " + f.getName().substring(0, f.getName().length() - 7));
                        System.out.println("   \t" + FileUtils.sizeOfDirectory(f));
                    }
                } else if (count == 2) {
                    String name = st.nextToken();
                    String filename = name + ".genkis";
                    if (!(new File(filename)).exists()) {
                        System.err.println("Error : no Genki store \'" + name + "\'");
                    }
                    LinkedList<File> list = new LinkedList<>(Arrays.asList((new File(filename)).listFiles()));
                    System.out.println(name + "\t" + FileUtils.sizeOfDirectory(new File(filename)));
                    for (File f : list) {
                        System.out.print("... ");
                        if (f.isDirectory()) {
                            System.out.print("/");
                        }
                        System.out.println(f.getName());
                    }
                } else {
                    System.err.println("Error : malformed command");
                }
                return;
            }

            case "i":
            case "init" : {
                if (count != 2) {
                    System.err.println("Error : malformed command");
                    return;
                }
                String name = st.nextToken();
                GenkiVFS.initGenkiStore(name);
                System.out.println("\nDone.\n");
                return;
            }

            case "imp":
            case "import" : {
                if (count != 3) {
                    System.err.println("Error : malformed command");
                    return;
                }
                String from = st.nextToken();
                String to = st.nextToken();

                File fromFile = new File(Paths.get(from).toString());
                if (fromFile.getName().endsWith(".genkis")) {
                    System.err.println("Error : first argument must not be a genki store");
                    return;
                }
                if (!Files.exists(Paths.get(from))) {
                    System.err.println("Error : path " + fromFile.getPath() + " is invalid");
                    return;
                }

                File toFile = new File(to + ".genkis");
                if (!toFile.getName().endsWith(".genkis")) {
                    System.err.println("Error : second argument must be a valid genki store");
                    return;
                }

                if (fromFile.isDirectory()) {
                    GenkiVFS vfs = new GenkiVFS();
                    vfs.setBaseDir(toFile.getPath());
                    System.out.println(toFile.getPath());
                    // TODO: change from naive (use old API) to more targeted
                    // TODO: key coherence
                    System.out.println("\nLoading files into staging area. . .");
                    try {
                        FileUtils.copyDirectory(fromFile, toFile);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println("Encrypting and shredding. . .");

                    System.out.println("Done.\n");
                } else {
                    try {
                        FileUtils.copyFile(fromFile, toFile);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                return;
            }

            case "exp":
            case "export" : {
                return;
            }

            case "e":
            case "exit" : {
                System.out.println("\nありがとうございました。");
                System.out.println("Thank you. Bye.\n");
                System.exit(0);
            }

            case "w":
            case "wipe" : {
                if (count != 2) {
                    System.err.println("Error : malformed command");
                    return;
                }
                String name = st.nextToken();
                String filename = name + ".genkis";
                File file = new File(filename);
                if (!file.exists()) {
                    System.err.println("Error : no Genki store \'" + name + "\'");
                    return;
                }
                if (!CryptUtils.yesNo("This will result in permanent data loss.")) {
                    return;
                }
                // TODO: Safe Delete
                try {
                    FileUtils.cleanDirectory(file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println("\nDone.\n");
                return;
            }

            case "wall":
            case "wipeall" : {
                if (count != 1) {
                    System.err.println("Error : malformed command");
                    return;
                }
                if (!CryptUtils.yesNo("This will result in permanent data loss.")) {
                    return;
                }

                List<File> list = Arrays.asList((new File(".")).listFiles())
                        .stream()
                        .filter(listFile -> listFile.isDirectory() && listFile.getName().endsWith(".genkis") )
                        .collect(Collectors.toList());

                for (File f : list) {
                    try {
                        FileUtils.cleanDirectory(f);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                System.out.println("\nDone.\n");
                return;
            }

            case "d":
            case "del": {
                if (count != 2) {
                    System.err.println("Error : malformed command");
                    return;
                }
                String name = st.nextToken();
                String filename = name + ".genkis";
                File file = new File(filename);
                if (!file.exists()) {
                    System.err.println("Error : no Genki store \'" + name + "\'");
                    return;
                }
                if (!CryptUtils.yesNo("This will result in permanent data loss.")) {
                    return;
                }
                // TODO: Safe Delete
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println("\nDone.\n");
                return;
            }

            case "dall":
            case "deleteall" : {
                if (count != 1) {
                    System.err.println("Error : malformed command");
                    return;
                }
                if (!CryptUtils.yesNo("This will result in permanent data loss.")) {
                    return;
                }
                LinkedList<File> list = new LinkedList<>(Arrays.asList((new File(".")).listFiles()));
                for (File f : list) {
                    if (f.isDirectory() && f.getName().endsWith(".genkis")) {
                        try {
                            FileUtils.deleteDirectory(f);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                System.out.println("\nDone.\n");
                return;
            }

            default : {
                System.err.println("Error : invalid command entered");
                return;
            }
        }
    }
}
