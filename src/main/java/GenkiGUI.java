import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class GenkiGUI extends Application {
    private static GenkiVFS vfs;
    private static DirectoryChooser dc;
    private static File defaultDir;
    private Stage window;
    private Button initButton;
    private Button deleteButton;
    private Button genkeyButton;
    private Button secureButton;
    private Button recoverButton;
    private Button secureToButton;
    private Button recoverFromButton;


    public static void main(String[] args) {
        vfs = new GenkiVFS();
        defaultDir = new File("./");
        vfs.setBaseDir("./");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("Genki");
        VBox layout = new VBox(10);
        layout.setPrefWidth(800);

        initButton = new Button("Allocate Genki Here");
        initButton.setMinWidth(layout.getPrefWidth());
        initButton.setOnAction(e -> {
            String title = "Genki Initialization Tool";
            boolean flag = vfs.initializeGenkiStore();
            if (flag) {
                AlertBox.display(title, "Genki data store initialized.");
            } else {
                AlertBox.display(title, "Error: Genki data store already exists.");
            }
        });

        deleteButton = new Button("Clear Genki Data Store");
        deleteButton.setMinWidth(layout.getPrefWidth());
        deleteButton.setOnAction(e -> {
            String title = "Genki Initialization Tool";
            boolean flag = vfs.deleteGenkiStore();
            if (flag) {
                AlertBox.display(title, "Genki data store deleted.");
            } else {
                AlertBox.display(title, "Error: Cannot delete Genki data store.");
            }
        });

        genkeyButton = new Button("Generate Key Here");
        genkeyButton.setMinWidth(layout.getPrefWidth());
        genkeyButton.setOnAction(e -> {
            String title = "Genki Key Gen Tool";
            vfs.setBaseDir(".");
            if (!vfs.genkiDirExists()) {
                AlertBox.display(title, "Error: Genki data store not initialized.");
                return;
            }
            if (!ChoiceBox.display(title, "This will overwrite any existing keys. Continue?")) {
                return;
            }
            vfs.genKey();
            AlertBox.display(title, "New key generated.");
        });

        secureButton = new Button("Secure All Files");
        secureButton.setMinWidth(layout.getPrefWidth());
        secureButton.setOnAction(e -> {
            String title = "Genki Secure Tool";
            if (vfs.currDirIsEmpty()) {
                AlertBox.display(title, "Error: No files to secure in current directory.");
                return;
            }
            if (!vfs.genkiDirExists()) {
                AlertBox.display(title, "Error: Genki data store not initialized.");
                return;
            } else if (!vfs.keyFileExists()) {
                AlertBox.display(title, "Error: Key file not loaded.");
                return;
            }
            if (!ChoiceBox.display(title, "This will shred and encrypt all files. Continue?")) {
                return;
            }
            vfs.setBaseDir(".");
            long start = System.currentTimeMillis();
            vfs.secureAll();
            long end = System.currentTimeMillis();
            AlertBox.display(title, "Securing took " + ((end - start) / 1000) + " seconds.");
        });

        recoverButton = new Button("Recover All Files");
        recoverButton.setMinWidth(layout.getPrefWidth());
        recoverButton.setOnAction(e -> {
            String title = "Genki Recovery Tool";
            vfs.setBaseDir(".");
            if (!vfs.genkiDirExists()) {
                AlertBox.display(title, "Error: Genki data store not initialized.");
                return;
            } else if (!vfs.keyFileExists()) {
                AlertBox.display(title, "Error: Key file not loaded.");
                return;
            }
            boolean dictsDirIsEmpty = vfs.dictsDirIsEmpty();
            boolean shardsDirIsEmpty = vfs.shardsDirIsEmpty();
            if (dictsDirIsEmpty && shardsDirIsEmpty) {
                AlertBox.display(title, "Error: No files to recover from disk.");
                return;
            } else if ((!dictsDirIsEmpty && shardsDirIsEmpty) || (dictsDirIsEmpty && !shardsDirIsEmpty)) {
                AlertBox.display(title, "Warning: defragmented data detected.");
                return;
            }
            long start = System.currentTimeMillis();
            vfs.recoverAll();
            long end = System.currentTimeMillis();
            AlertBox.display(title, "Recovery took " + ((end - start) / 1000) + " seconds.");
        });

        secureToButton = new Button("Secure All To...");
        secureToButton.setMinWidth(layout.getPrefWidth());
        secureToButton.setOnAction(e -> {
            dc = new DirectoryChooser();
            String title = "Genki Secure Tool";
            dc.setTitle("Select Shard Storage Directory");
            dc.setInitialDirectory(defaultDir);
            String selectedPath = dc.showDialog(window).getPath();
            vfs.setBaseDir(selectedPath);
            File genkiDirFromPath = new File(selectedPath + "/.genki");
            try {
                if (!genkiDirFromPath.exists()) {
                    if (ChoiceBox.display("Genki Allocation Tool", "No shard store found. Allocate one?")) {
                        vfs.initializeGenkiStore();
                        if (vfs.currDirHasKey()) {
                            if (ChoiceBox.display(title, "Use current Genki shard store key?")) {
                                String currKeyPath = "./.genki/key.dat";
                                if (!(new File(currKeyPath)).exists()) {
                                    AlertBox.display(title, "Error: no key found in current Genki shard store.");
                                    return;
                                } else {
                                    File target = new File(selectedPath + "/.genki/key.dat");
                                    System.out.println(target.getPath());
                                    Files.copy(Paths.get(currKeyPath), Paths.get(selectedPath + "/.genki/key.dat"), StandardCopyOption.REPLACE_EXISTING);
                                }
                            } else {
                                AlertBox.display(title, "Exiting Genki Secure Tool.");
                                return;
                            }
                        }
                    } else {
                        return;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            long start = System.currentTimeMillis();
            vfs.secureAll();
            long end = System.currentTimeMillis();
            AlertBox.display(title, "Securing took " + ((end - start) / 1000) + " seconds.");
        });


        recoverFromButton = new Button("Recover From...");
        recoverFromButton.setMinWidth(layout.getPrefWidth());
        recoverFromButton.setOnAction(e -> {
            dc = new DirectoryChooser();
            String title = "Genki Recovery Tool";
            dc.setTitle("Select Directory to Recover Files From");
            dc.setInitialDirectory(defaultDir);
            String selectedPath = dc.showDialog(window).getPath();
            vfs.setBaseDir(selectedPath);
            File genkiDirFromPath = new File(selectedPath + "/.genki");
            if (!genkiDirFromPath.exists()) {
            }
        });


        layout.getChildren().addAll(initButton, deleteButton, genkeyButton, secureButton, recoverButton, secureToButton, recoverFromButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 800, 600);
        window.setScene(scene);
        window.show();
    }
}
