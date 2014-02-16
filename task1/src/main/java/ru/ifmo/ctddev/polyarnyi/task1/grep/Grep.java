package ru.ifmo.ctddev.polyarnyi.task1.grep;

import com.google.common.collect.Lists;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Date: 15.02.14 at 23:05
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class Grep extends SimpleFileVisitor<Path> {

    private static final String USAGE = "Proper Usage is: \"java Grep string1 string2 ... stringN\"\n" +
            "Or to enter strings one by one in console: \"java Grep -\"";
    private static final String STRINGS_FROM_CONSOLE = "-";
    private static final String ERROR_IO_EXCEPTION = "Input/Output error has occurred!";
    private static final int MAX_SIZE_OF_MESSAGE = 128;

    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {
            if (args.length == 0) {
                System.out.println(USAGE);
            } else if (args.length == 1 && STRINGS_FROM_CONSOLE.equals(args[0])) {
                String s = in.readLine();
                while (s != null && !s.isEmpty()) {
                    searchLinesContains(Lists.newArrayList(s), System.out);
                    s = in.readLine();
                }
            } else {
                searchLinesContains(Lists.newArrayList(args), System.out);
            }
        } catch (IOException e) {
            System.out.println(ERROR_IO_EXCEPTION);
        }
    }

    private static void searchLinesContains(List<String> patterns, PrintStream out) throws IOException {
        MultiEncodingSearch searcher = new MultiEncodingSearch(patterns, Lists.newArrayList("CP866", "KOI8-R", "CP1251", "UTF-8"));
        Path curPath = Paths.get("");
        Files.walkFileTree(curPath, new Grep(searcher, out));
    }

    private final MultiEncodingSearch searcher;
    private final PrintStream out;

    public Grep(MultiEncodingSearch searcher, PrintStream out) {
        this.searcher = searcher;
        this.out = out;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
        searcher.reset();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            int lineNumber = 1;
            boolean containsFirstSymbol = true;
            byte[] buffer = new byte[MAX_SIZE_OF_MESSAGE];
            int first = 0;
            int size = 0;
            int curByte = in.read();
            while (curByte != -1) {
                if (curByte == '\n') {
                    first = 0;
                    size = 0;
                    containsFirstSymbol = true;
                    lineNumber++;
                } else if (curByte != '\r') {
                    if (size < MAX_SIZE_OF_MESSAGE) {
                        size++;
                        buffer[size - 1] = (byte) curByte;
                    } else {
                        buffer[first] = (byte) curByte;
                        first = (first + 1) % MAX_SIZE_OF_MESSAGE;
                        containsFirstSymbol = false;
                    }
                }
                String encodingOfFounded = searcher.proceed(curByte);
                curByte = in.read();
                if (encodingOfFounded != null) {
                    byte[] bytes = new byte[size];
                    System.arraycopy(buffer, first, bytes, 0, Math.min(size, MAX_SIZE_OF_MESSAGE - first));
                    System.arraycopy(buffer, 0, bytes, Math.min(size, MAX_SIZE_OF_MESSAGE - first), first);
                    out.println(file.toString() + '(' + lineNumber + ')' + ": "
                            + (containsFirstSymbol ? "" : "...")
                            + new String(bytes, encodingOfFounded)
                            + ((curByte == '\r' || curByte == '\n') ? "" : "..."));
                }
            }
            return FileVisitResult.CONTINUE;
        } catch (FileNotFoundException e) {
            return FileVisitResult.CONTINUE;
        }
    }

}
