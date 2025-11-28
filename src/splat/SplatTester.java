package splat;

import java.io.*;

public class SplatTester {

        private final boolean verbose = true;
        private final String testDirectoryName = "./tests";

        private File testDirectory;

        private int totalTests;
        private int totalTestsRun;

        private int[] successesByResult;
        private int[] resultCounts;
        private int[] falseThrows;

        public static void main(String[] args) throws Exception {
                SplatTester tester = new SplatTester();
                tester.runTests();
        }

        public SplatTester() {
                totalTests = 0;
                successesByResult = new int[]{0, 0, 0, 0, 0};
                resultCounts = new int[]{0, 0, 0, 0, 0};
                falseThrows = new int[]{0, 0, 0, 0};
        }

        public void runTests() throws Exception {
                testDirectory = new File(testDirectoryName);

                System.out.print("Opening test directory...");

                if (!testDirectory.exists() || !testDirectory.isDirectory()) {
                        System.out.println("error!");
                        System.out.println("Cannot find directory 'tests'.");
                        System.out.println("Please create one in your project folder, and add" + " the appropriate testing files.");
                        return;
                }

                System.out.println("success");

                File[] testFiles = testDirectory.listFiles((dir, name) -> name.endsWith(".splat"));

                totalTests = testFiles.length;
                System.out.println("Number of tests found: " + totalTests);

                System.out.println("Running tests...");

                for (File testFile : testFiles) {
                        int expectedResultCode = getExpectedResultCode(testFile.getName());
                        resultCounts[expectedResultCode]++;

                        runTest(testFile);
                }

                int totalSuccesses = 0;
                for (int i = 0; i < 5; i++) {
                        totalSuccesses += successesByResult[i];
                }
                double percentPass = 100.0 * totalSuccesses / totalTests;

                System.out.println("---------------------------");
                System.out.println("FINAL SPLAT TESTING RESULTS");
                System.out.println("---------------------------");
                System.out.println("Total tests cases:   " + totalTests);
                System.out.println("Test cases run:      " + totalTestsRun);
                System.out.println("Test cases passed:   " + totalSuccesses + " (" + String.format("%.1f", percentPass) + " %)");
                System.out.println("Results by case");
                System.out.println("  Lex Exception:       " + scoreString(0));
                System.out.println("    false throws: " + falseThrows[0]);
                System.out.println("  Parse Exception:     " + scoreString(1));
                System.out.println("    false throws: " + falseThrows[1]);
                System.out.println("  Semantic Exception:  " + scoreString(2));
                System.out.println("    false throws: " + falseThrows[2]);
                System.out.println("  Execution Exception: " + scoreString(3));
                System.out.println("    false throws: " + falseThrows[3]);
                System.out.println("  Execution Success:   " + scoreString(4));
        }

        private String scoreString(int resCode) {
                double percent = 100.0 * successesByResult[resCode] / resultCounts[resCode];
                return successesByResult[resCode] + " / " + resultCounts[resCode] + " (" + String.format("%.1f", percent) + " %)";
        }

        private int getExpectedResultCode(String filename) throws Exception {
                if (filename.endsWith("badlex.splat")) {
                        return 0;
                } else if (filename.endsWith("badparse.splat")){
                        return 1;
                } else if (filename.endsWith("badsemantics.splat")){
                        return 2;
                } else if (filename.endsWith("badexecution.splat")){
                        return 3;
                } else if (filename.endsWith("goodexecution.splat")){
                        return 4;
                }

                throw new Exception("Bad .splat test filename");
        }

        private int getActualResultCode(SplatException ex) throws Exception {
                String exceptionType = ex.getClass().getName();

                if (exceptionType.equals("splat.lexer.LexException")) {
                        return 0;
                } else if (exceptionType.equals("splat.parser.ParseException")) {
                        return 1;
                } else if (exceptionType.equals("splat.semanticanalyzer.SemanticAnalysisException")) {
                        return 2;
                } else if (exceptionType.equals("splat.executor.ExecutionException")) {
                        return 3;
                }

                throw new Exception("Non-splat exception thrown");
        }

        private void runTest(File testFile) throws Exception {
                totalTestsRun++;
                System.out.print("Test Case " + totalTestsRun + ": " + testFile.getName() + "...");

                Splat splat = new Splat(testFile);

                int expectedResultCode = getExpectedResultCode(testFile.getName());
                int actualResultCode;

                String exceptionMessage = "";

                PrintStream originalOut = new PrintStream(System.out);
                File progOutput = new File(testDirectory, "temp-out.txt");
                PrintStream outs = new PrintStream(progOutput);
                System.setOut(outs);

                try {
                        splat.processFileAndExecute();
                        actualResultCode = 4;
                } catch (SplatException ex) {
                        int ind = ex.getClass().getName().lastIndexOf('.');
                        exceptionMessage = " >>> " + ex.getClass().getName().substring(ind + 1) + ": " + ex.toString();
                        actualResultCode = getActualResultCode(ex);
                } catch (Exception ex) {
                        int ind = ex.getClass().getName().lastIndexOf('.');
                        exceptionMessage = " >>> " + ex.getClass().getName().substring(ind + 1) + ": " + ex.toString();
                        actualResultCode = -1;
                } finally {
                        outs.close();
                        System.setOut(originalOut);
                }

                if (expectedResultCode < 4) {
                        if (expectedResultCode == actualResultCode) {
                                System.out.println("passed (proper SplatException thrown during Phase " + (expectedResultCode + 1) + ")");
                                successesByResult[expectedResultCode]++;
                        } else {
                                System.out.println("failed (exception was expected to be thrown during Phase " + (expectedResultCode + 1) + ")");
                                if (actualResultCode < 4 && actualResultCode != -1) {
                                        falseThrows[actualResultCode]++;
                                }
                        }
                }

                if (expectedResultCode == 4 && actualResultCode != 4) {
                        System.out.println("failed (exception was thrown when execution should have been successful)");
                        if (actualResultCode != -1) {
                                falseThrows[actualResultCode]++;
                        }
                }

                if (verbose && exceptionMessage.length() > 0) {
                        System.out.println(exceptionMessage);
                }

                if (expectedResultCode == 4 && actualResultCode == 4) {
                        String testFilePath = testFile.getAbsolutePath();
                        String exFilename = testFilePath.substring(0, testFilePath.length() - 5) + "out";
                        File expectedOutput = new File(exFilename);

                        if (outputMatchesExpected(progOutput, expectedOutput)) {
                                System.out.println("passed (output matches expected results)");
                                successesByResult[4]++;
                        } else {
                                System.out.println("failed (output does not match expected results)");
                        }

                        if (verbose) {
                                printOutput(progOutput);
                                System.out.println();
                        }
                }

        }

        private boolean outputMatchesExpected(File output, File expected) throws IOException {
                if (!expected.exists()) {
                        System.out.println("File " + expected.getAbsolutePath() + " not found");
                        return false;
                }

                BufferedReader readerOut = new BufferedReader(new FileReader(output));
                BufferedReader readerEx = new BufferedReader(new FileReader(expected));

                boolean result = true;

                int chOut = readerOut.read();
                int chEx = readerEx.read();

                while (true) {
                        while (chOut == '\r') {
                                chOut = readerOut.read();
                        }
                        while (chEx == '\r') {
                                chEx = readerEx.read();
                        }

                        if (chOut == -1 && chEx == -1) {
                                result = true;
                                break;
                        } else if (chOut != chEx) {
                                result = false;
                                break;
                        }

                        chOut = readerOut.read();
                        chEx = readerEx.read();
                }

                readerOut.close();
                readerEx.close();

                return result;
        }

        private void printOutput(File file) throws IOException {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                int ch = reader.read();

                while (ch != -1) {
                        System.out.print((char) ch);
                        ch = reader.read();
                }

                reader.close();
        }
}
