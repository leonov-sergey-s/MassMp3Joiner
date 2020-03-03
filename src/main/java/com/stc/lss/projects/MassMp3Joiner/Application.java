/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stc.lss.projects.MassMp3Joiner;

import com.stc.lss.projects.MassMp3Joiner.util.Partition;
import com.stc.lss.projects.MassMp3Joiner.util.SyncPipe;
import lombok.SneakyThrows;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author lss
 */
public class Application {
    private int chunkSize = 3;
    private Path inputDir = Paths.get(".");
    private Path outputDir = Paths.get(".");
    private boolean shuffle = true;
    private Path mp3wrap = Paths.get(".");
    private Path mp3val = Paths.get(".");

    private List<String> getMp3Files(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".mp3"))
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private int fixMp3File(String mMp3FilePath) {
        try {
            String command = mp3val.toAbsolutePath() + " " +
                    "\"" + mMp3FilePath + "\" -f -si -nb";
            Process p = Runtime.getRuntime().exec(command);
            new Thread(new SyncPipe(p.getInputStream(), System.out)).start();
            return p.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    private int joinMp3Files(List<String> listMp3Files, String outputMp3FilePath) {
        try {
            //TODO: path to param
            String command = mp3wrap.toAbsolutePath() + " " +
                    "\"" + outputMp3FilePath + "\" " +
                    listMp3Files.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(" "));
            Process p = Runtime.getRuntime().exec(command);
            new Thread(new SyncPipe(p.getInputStream(), System.out)).start();
            int returnCode = p.waitFor();
            if (returnCode == 0) return fixMp3File(outputMp3FilePath + "_MP3WRAP.mp3");
            return returnCode;
        } catch (Exception e) {
            return -1;
        }
    }

    @SneakyThrows
    public void start() throws IOException {

        List<String> list = getMp3Files(inputDir);
        if (shuffle) Collections.shuffle(list);
        List<List<String>> listOfChunks = Partition.ofSize(list, chunkSize);

        if (!listOfChunks.isEmpty()) {
            System.out.println("\n***TOTAL CHUNKS: " + listOfChunks.size());
            Files.newDirectoryStream(outputDir, "*.mp3").forEach(path -> path.toFile().delete());
        }
        int done = 0;
        int failed = 0;
        for (int i = 0; i < listOfChunks.size(); i++) {
            String n = String.valueOf(i + 1);
            System.out.println("\n***CHUNK #" + n + ":\n");
            int returnCode = joinMp3Files(
                    listOfChunks.get(i),
                    outputDir.resolve(n).toAbsolutePath().toString()
            );
            if (returnCode == 0) done++;
            else failed++;

            System.out.println("\n***CHUNK #" + n + (returnCode == 0 ? " - OK" : " - FAILED"));
        }
        System.out.println("\n***TOTAL DONE: " + done);
        System.out.println("***TOTAL FAILED: " + failed);
    }

    private void parseProperties() {
        try {
            File file = new File("./config.properties");
            if (file.exists()) {
                Properties property = new Properties();
                FileInputStream fis = new FileInputStream(file);
                property.load(fis);
                fis.close();
                if (property.containsKey("mp3wrap")) this.mp3wrap = Paths.get(property.getProperty("mp3wrap"));
                if (property.containsKey("mp3val")) this.mp3val = Paths.get(property.getProperty("mp3val"));
                if (property.containsKey("chunk-size"))
                    this.chunkSize = Integer.parseInt(property.getProperty("chunk-size", "3"));
                if (property.containsKey("shuffle"))
                    this.shuffle = Boolean.parseBoolean(property.getProperty("shuffle", "true"));
                if (property.containsKey("input-dir")) this.inputDir = Paths.get(property.getProperty("input-dir"));
                if (property.containsKey("output-dir")) this.outputDir = Paths.get(property.getProperty("output-dir"));
            }
        } catch (IOException e) {
            //skip errors
        }
    }

    private void parseArgs(Options options, String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("mp3wrap")) this.mp3wrap = Paths.get(cmd.getOptionValue("mp3wrap"));
            if (cmd.hasOption("mp3val")) this.mp3val = Paths.get(cmd.getOptionValue("mp3val"));
            if (cmd.hasOption("chunk-size")) this.chunkSize = Integer.parseInt(cmd.getOptionValue("chunk-size"));
            if (cmd.hasOption("shuffle")) this.shuffle = Boolean.parseBoolean(cmd.getOptionValue("shuffle"));
            if (cmd.hasOption("input-dir")) this.inputDir = Paths.get(cmd.getOptionValue("input-dir"));
            if (cmd.hasOption("output-dir")) this.outputDir = Paths.get(cmd.getOptionValue("output-dir"));
        } catch (ParseException exp) {
            System.err.println("Parsing failed. Reason: " + exp.getMessage());
        }
    }

    private boolean validateParams() {
        try {
            if (!mp3wrap.toFile().exists()) throw new RuntimeException("mp3wrap util is not exists");
            if (!mp3val.toFile().exists()) throw new RuntimeException("mp3val util is not exists");
            if (!inputDir.toFile().exists()) throw new RuntimeException("Input directory not exists");
            if (!outputDir.toFile().exists()) throw new RuntimeException("Output directory not exists");
            if (chunkSize <= 1) throw new RuntimeException("chunk-size must be more than one");
        } catch (RuntimeException exp) {
            System.err.println("Error: " + exp.getMessage());
            return false;
        }
        return true;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        Options options = new Options();
        options.addOption(new Option("c", "chunk-size", true, "number of mp3 files to join"));
        options.addOption(new Option("i", "input-dir", true, "directory to recursive scan"));
        options.addOption(new Option("o", "output-dir", true, "directory to write joined mp3 files"));
        options.addOption(new Option("s", "shuffle", false, "shuffle"));
        options.addOption(new Option("mp3wrap", true, "path to mp3wrap util"));
        options.addOption(new Option("mp3val", true, "path to mp3val util"));

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("massMp3Joiner", null, options, "\nNOTE: All mp3 files in output directory will be deleted before start mp3wrap!");

        Application app = new Application();
        app.parseProperties();
        app.parseArgs(options, args);
        if (app.validateParams()) app.start();
    }

}
