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

    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
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
        MultiEncodingSearch searcher = new MultiEncodingSearch(patterns, Lists.newArrayList("UTF-8", "KOI8-R", "CP1251", "CP866"));
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
        try {
            BufferedReader in = new BufferedReader(new FileReader(file.toFile()));
            String str = in.readLine();
            while (str != null) {
                byte[] bytes = str.getBytes();
                for (byte cur : bytes) {
                    if (searcher.proceed(cur)) {
                        out.println(file.toString() + ": " + str);
                        break;
                    }
                }
                str = in.readLine();
            }
            return FileVisitResult.CONTINUE;
        } catch (FileNotFoundException e) {
            return FileVisitResult.CONTINUE;
        }
    }

}
