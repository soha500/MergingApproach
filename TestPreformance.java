package org.eclipse.epsilon.egl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.eclipse.epsilon.egl.output.OutputBuffer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

// System.out.println("Working Directory = " + System.getProperty("user.dir"));

public class TestPreformance {

	private static String FolderPath(int numberOfFiles) {
		return "/Users/sultanalmutairi/git/EglEngine/org.eclipse.epsilon.egl.engine/PreformancTests/boiler-To-Generate-"
		+ numberOfFiles + "-Files/TheGeneratedFiles-" + numberOfFiles;
		}

	enum TestStrategy {
		OneLineAdded,
		MultipleLinesAdded,
		OneLineModified,
		OneLineDeleted,
		OneConflict,
		MultipleConflict,
	}
	
	enum TestStatus {
		Successful,           
		MergeConflict,	
		LineLost,
		ChangeNotDetected,
		ConflictNotDetected, 
		MergeFailure,
	}
	
	@Rule
    public TestName name = new TestName();

	/*
	 * To write the results of the tests to CSV file. 
	 */
	
	public static void writeResultsToCsvFile(long duration, TestStrategy strategy, int numLines) {
		try {
			String filePath= "PerformanceTestsResults25Dec2022.csv";
			boolean header = !new File(filePath).exists();
			FileWriter fw = new FileWriter(filePath, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			if (header)
				pw.println("Strategy,Number of Lines,Duration");
			pw.println(String.format("%s,%d,%d",strategy, numLines, duration));
			pw.flush();
			fw.close();

		} catch (Exception E) {
			System.out.println("There are errors!!");
		}
	}
	
	/*
	 * Strategy based on the size of the file 
	 */
	
	private String runTest(TestStrategy strategy) throws IOException {
		for (int numLines = 500; numLines <= 10000; numLines += 500) {
			System.out.println(numLines);
			// pass number in the name of the folder..
			String folderPath = FolderPath(100);

			File[] files = Arrays.copyOfRange(new File(folderPath).listFiles(), 0, 100);

			for (File f : files) {
				if (!f.isFile())
					continue;
				List<String> lines = readTestFileLines(f.getAbsolutePath(), numLines);
				
				TestStatus status = null;
				switch (strategy) {
				case OneLineAdded:
					status = doLineAddTest(lines);
					break;
				case MultipleLinesAdded:
					status = doLinesAddTest(lines);
					break;
				case OneLineModified:
					status = doLineModifyTest(lines);
					break;
				case OneLineDeleted:
					status = doLineDeleteTest(lines);
					break;
				case OneConflict:
					status = doLineConflictTest(lines);
					break;
				case MultipleConflict:
					status = doLinesConflictTest(lines);
					break;
				default:
					break;
				}
				if (status == null) {
					return "Test not supported";
				}
				if (status == TestStatus.MergeConflict) {
					return "The file " + f.getName() + " had a merge conflict.";
				}
				if (status == TestStatus.LineLost) {
					return "The file " + f.getName() + " lost its new line.";
				}
				if (status == TestStatus.MergeFailure) {
					return "The file " + f.getName() + " did not merge successfully.";
				}
			}
		}
		return "";
	}
	
	/*
	 * Read the content of the file 
	 */
	
	private static List<String> readTestFileLines(String path, int numLines) throws IOException {
		HashedDoc hashedDoc = new HashedDoc(String.join("\n", Files.readAllLines(Paths.get(path))));
		String rehashedDoc = OutputBuffer.documentWithAppendedHashLine(String.join("\n", hashedDoc.getLines().subList(0, numLines)));
		return new ArrayList<> (Arrays.asList(rehashedDoc.split("\n", -1)));
	}
	
	/*
	 * Add random string 
	 */
	
	public String generateRandomString() { 
	    int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 10;
	    Random random = new Random();
	    StringBuilder buffer = new StringBuilder(targetStringLength);
	    for (int i = 0; i < targetStringLength; i++) {
	        int randomLimitedInt = leftLimit + (int) 
	          (random.nextFloat() * (rightLimit - leftLimit + 1));
	        buffer.append((char) randomLimitedInt);
	    }
	    String generatedString = buffer.toString();

	    return generatedString;
	}
	
	/*
	 * Scenario 1, One Line Added
	 * one line has been added into the auto-generated lines and copy one of the exixting line
	 */
	
	private TestStatus doLineAddTest(List<String> existingLines) throws IOException {
		String transformationContents = String.join("\n", existingLines);
		Random random = new Random();
		// choose copy random line 
		int copyRandomLine = (int) (random.nextFloat() * (existingLines.size() - 4));
		// choose paste random line
		int pasteRandomLine = (int) (random.nextFloat() * (existingLines.size() - 4));

		existingLines.add(pasteRandomLine, existingLines.get(copyRandomLine));

		String modifiedContents = String.join("\n", existingLines);
		long start = System.currentTimeMillis();

		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(modifiedContents, transformationContents);
		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.OneLineAdded, existingLines.size() - 4);
		if (results.getStatus() != MergingAndConflicts.StatusCode.MergedSuccessfully) {
			return TestStatus.MergeFailure;
		}
		if (!results.getNewContents().equals(modifiedContents)) {
			return TestStatus.LineLost;
		}
		return TestStatus.Successful;
	}
	
	/*
	 * Scenario 2, Multiple lines added
	 * Multiple lines have been added into the auto-generated lines 
	 */
	
	private TestStatus doLinesAddTest(List<String> existingLines) throws IOException {
		String transformationContents = String.join("\n", existingLines);
		Random random = new Random();
		// add rundom string in random line..
		// increase size by %50
	
		for (int i = 0; i < 100; i++) {
			int randomLine = (int) (random.nextFloat() * (existingLines.size() - 4));
			String randomString = generateRandomString();
			existingLines.add(randomLine, randomString);
		}

		String modifiedContents = String.join("\n", existingLines);
		long start = System.currentTimeMillis();

		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(modifiedContents, transformationContents);
		writeResultsToCsvFile(
				System.currentTimeMillis() - start,
				TestStrategy.MultipleLinesAdded, existingLines.size() + 100);
		if (results.getStatus() != MergingAndConflicts.StatusCode.MergedSuccessfully) {
			return TestStatus.MergeFailure;
		}
		if (!results.getNewContents().equals(modifiedContents)) {
			return TestStatus.LineLost;
		}
		return TestStatus.Successful;
	}
	
	/*
	 * Scenario 3, One Conflict, 
	 * in the same position of the template and it's generated file two different values has been added after the first run (there's a conflicts in this case)
	 */
	
	private TestStatus doLineConflictTest(List<String> existingLines) throws IOException {

		List<String> transformationLines = new ArrayList<>(existingLines);
		int line = (int) (Math.random() * (existingLines.size() - 4));
		// 
		existingLines.add(line, generateRandomString());
		transformationLines.add(line, generateRandomString());
		
		String existingContents = String.join("\n", existingLines);
		String transformationContents = String.join("\n", transformationLines);
		long start = System.currentTimeMillis();

		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(existingContents, transformationContents);
		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.OneConflict, existingLines.size() - 4);
//		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.OneConflict, existingLines.size() -1);
		if (results.getStatus() != MergingAndConflicts.StatusCode.ConflictsFound) {
			return TestStatus.ConflictNotDetected;
		}
		return TestStatus.Successful;
	}
	
	/*
	 * Scenario 4, Multiple Conflicts, 
	 * in the same position of the template and it's generated file two different values has been added after the first run (there's a conflicts in this case)
	 */
	
	private TestStatus doLinesConflictTest(List<String> existingLines) throws IOException {

		List<String> transformationLines = new ArrayList<>(existingLines);
		for (int i = 0; i < 100; i++) {	
			int line = (int) (Math.random() * (existingLines.size() - 4));
			existingLines.add(line, generateRandomString());
			transformationLines.add(line, generateRandomString());
		}
		String existingContents = String.join("\n", existingLines);
		String transformationContents = String.join("\n", transformationLines);
		long start = System.currentTimeMillis();

		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(existingContents, transformationContents);
		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.MultipleConflict, existingLines.size() - 4);
		if (results.getStatus() != MergingAndConflicts.StatusCode.ConflictsFound) {
			return TestStatus.ConflictNotDetected;
		}
		return TestStatus.Successful;
	}
	
	/*
	 * Scenario 5, One Line Modified 
	 * at least one of the auto-generated lines has been modified.
	 */
	
	private TestStatus doLineModifyTest(List<String> existingLines) throws IOException {
		String transformationContents = String.join("\n", existingLines);
		int line = (int) (Math.random() * (existingLines.size() - 4));
		String randomString = generateRandomString();
		existingLines.set(line, randomString);
		String existingContents = String.join("\n", existingLines);
		long start = System.currentTimeMillis();

		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(existingContents, transformationContents);
		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.OneLineModified, existingLines.size() - 3);
		if (results.getStatus() != MergingAndConflicts.StatusCode.OriginalWasModified) {
			return TestStatus.ChangeNotDetected;
		}
		return TestStatus.Successful;
	}
	
	/*
	 * Scenario 6, One Line Deleted
	 * At least one of the auto-generated lines has been deleted.
	 */
	
	private TestStatus doLineDeleteTest(List<String> existingLines) throws IOException {
		String transformationContents = String.join("\n", existingLines);
		int line = (int) (Math.random() * (existingLines.size() - 4));
		existingLines.remove(line);
		String existingContents = String.join("\n", existingLines);

		long start = System.currentTimeMillis();

		MergingAndConflicts results = MergingAndConflicts.DoMergingAndConflicts(existingContents, transformationContents);
		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.OneLineDeleted, existingLines.size() - 2);
//		writeResultsToCsvFile(System.currentTimeMillis() - start, TestStrategy.OneLineDeleted, existingLines.size() + 1);
		if (results.getStatus() != MergingAndConflicts.StatusCode.OriginalWasModified) {
			return TestStatus.ChangeNotDetected;
		}
		return TestStatus.Successful;
	}
	
// Tests are from here 
	
	/*
	 * Test 1, One Line added.
	 */
	
	@Test
	public void AddingLine() throws IOException {
		String status = runTest(TestStrategy.OneLineAdded);
		if (status != "") {
			Assert.fail(status);
		}
	}

	/*
	 * Test 2, Multiple Lines Added.
	 */
	
	@Test
	public void AddingLines() throws IOException {
		String status = runTest(TestStrategy.MultipleLinesAdded);
		if (status != "") {
			Assert.fail(status);
		}
	}
	
	/*
	 * Test 3, One Conflict Line.
	 */
	
	@Test
	public void ConflictingLine() throws IOException {
		String status = runTest(TestStrategy.OneConflict);
		if (status != "") {
			Assert.fail(status);
		}
	}
	
	/*
	 * Test 4, Multple Conflicts Lines.
	 */
	
	@Test
	public void ConflictingLines() throws IOException {
		String status = runTest(TestStrategy.MultipleConflict);
		if (status != "") {
			Assert.fail(status);
		}
	}
	
	/*
	 * Test 5, One Modyfied Line.
	 */
	
	@Test
	public void ModifyingLine() throws IOException {
		String status = runTest(TestStrategy.OneLineModified);
		if (status != "") {
			Assert.fail(status);
		}
	}

	/*
	 * Test 6, One Deleted Line.
	 */
	
	@Test
	public void DeletingLine() throws IOException {
		String status = runTest(TestStrategy.OneLineDeleted);
		if (status != "") {
			Assert.fail(status);
		}
	}
}