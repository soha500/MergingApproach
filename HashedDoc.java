package org.eclipse.epsilon.egl;
//2/2/2021 ... , I added two new method to get the content of original file, 

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

import org.eclipse.epsilon.egl.output.OutputBuffer;

public class HashedDoc {
	
	/**
	 *  Fields that used in this class
	 */
	
	List<String> lines;
	private String body;
	private String oldHash;
	private String newHash;
	
	/** 
	 * @param fileContent the file to be represented by this HashedDoc
	 */
	
	public HashedDoc (String fileContent) {
		
		if (fileContent == "") {
			lines = new ArrayList<>();
			body = "";
			oldHash = "";
			newHash = "";
			return;
		}
		
		// looking for the last three lines and remove them
		{
			String[] linesArr = fileContent.split("\n", -1);
			oldHash = linesArr[linesArr.length - 2];
			lines = new ArrayList<String>(Arrays.asList(linesArr));
			lines.remove(lines.size() - 1);
			lines.remove(lines.size() - 1);
			lines.remove(lines.size() - 1);
		}
		body = String.join("\n", lines);
		newHash = OutputBuffer.makeHashLine(body);
	}
	
	public String getOldHash(boolean asDoc) {
		return asDoc ? makeHashDoc(oldHash) : oldHash; //XbK6CQ==q3PAAA==MPU=fQ==
	}
	
	public String getNewHash(boolean asDoc) {
		return asDoc ? makeHashDoc(newHash) : newHash;
	}
	
	public List<String> getLines() {
		return lines;
	}
	
	public String getBody() {
		return body;
	}
	
	/**
	 * This method to transforms a hash line into a hash list
	 * 
	 * @param hashLine
	 */
	
	private static String makeHashDoc (String hashLine) {
		return String.join("\n", hashLine.split("(?<=\\G....)"));
	}
	
	/**
	 * This method compares the old hashes for the auto-generated lines with 
	 * the new hashes for the new lines that about to be generated.
	 * It also ignore any Protected/Sync regions in the generated files because their length unknown.
	 * Finally, it returns the original content.
	 * 
	 * @return original content without regions
	 */
	
	// Extracting original contents from modified contents
	public String originalContent () {
		List<String> originalLines = new ArrayList<>();
		String[] hashes = getOldHash(true).split("\n"); //[XbK6, CQ==, l1dz, ASA=, ZXyh, ASA=, q3PA, ASA=, iZ4f, MPU=, fQ==]
		String[] cleanLines = OutputBuffer.contentWithoutRegions(body).split("\n", -1); // new lines
		int l;
		int h;
		for (l = 0, h = 0; l < cleanLines.length && h < hashes.length; ++l)
			// Lines..all lines in generated files with added lines as well.
			if (OutputBuffer.hashLine(cleanLines[l]).equals(hashes[h])) { 
				originalLines.add(cleanLines[l]);
				++h;
			}
		return String.join("\n", originalLines);
	}
	
	/**
	 * This method compares the old hashes for the auto-generated lines with 
	 * newly generated hashes for the new lines that about to be generated.
	 * It also ignore any Protected/Sync regions in the generated files because their length unknown.
	 * Finally, it returns a boolean for if all the original lines are present.
	 *
	 * @return boolean if all the original lines are present
	 */

	// Detecting if any of the generated content has been modified and return a message...
	public boolean allOriginalLinesPresent () {
		String[] hashes = getOldHash(true).split("\n", -1);
		String[] cleanLines = OutputBuffer.contentWithoutRegions(body).split("\n", -1);
		int h = 0;
		for (int l = 0; l < cleanLines.length && h < hashes.length; ++l)
			if (OutputBuffer.hashLine(cleanLines[l]).equals(hashes[h]))
				++h;
		
		return h == hashes.length;
	}
	
//	// New changes... to detect where exactlly line/s have been deleted or modified
//	public class LinesPerLine {
//		public int Line;
//		public int Lines;
//	}
//	// until here
	
	/**
	 * This method checks where aut-generated line/s has been modified or deleted
	 */
	
//	// New changes... to detect where exactlly line/s have been deleted or modified
//	public List<LinesPerLine> missingOriginalLines() {
//		String[] hashes = getOldHash(true).split("\n", -1); //[XbK6, CQ==, q3PA, AA==, MPU=, fQ==]
//		String[] cleanLines = OutputBuffer.contentWithoutRegions(body).split("\n", -1); //[public class TemperatureController {, 	, 	public int execute(int temperature, int targetTemperature) , , 	}	, }]
//		List<LinesPerLine> missingLines = new ArrayList<>(); // []
//		int consecutiveLost = 0;
//		LinesPerLine lpl = null;
//		for (int h = 0, last = -1; h < hashes.length; ++h) {
//			boolean found = false;
//			for (int l = Math.max(last, 0); l < cleanLines.length; ++l)
//				if (OutputBuffer.hashLine(cleanLines[l]).equals(hashes[h])) {
//					found = true;
//					last = l;
//					consecutiveLost = 0;
//					break;
//				}
//			if (!found) {
//				if (consecutiveLost == 0) {
//					lpl = new LinesPerLine();
//					lpl.Line = last + 1;
//					lpl.Lines = 1;
//					missingLines.add(lpl);
//				} else {
//					++lpl.Lines;
//				}
//				++consecutiveLost;
//			}
//		}
//		return missingLines;
//	}
//  //until here
	
	/**
	 * This method uses to check if there is a hash 
	 * line at the end of the content
	 * 
	 * @param content of the file
	 * @return boolean if there is a hash line at the end of the content
	 */

	// check if the lastLine contian hash or not
	public static boolean hasHashLine(String content) {
		String[] linesArr = content.split("\n", -1);
		if (linesArr.length < 3)
			return false;
		String lastLine = linesArr[linesArr.length - 1];
		return lastLine.startsWith("*/") || lastLine.startsWith("-->");
	}
	
	/**
	 * This method removes a specified number of lines from the end of a string
	 */
	
	// remove last line = conflicted
	public static String removeEndLines (String document, int numLines) {
		String[] linesArr = document.split("\n", -1);
		List<String> lines = new ArrayList<>(Arrays.asList(linesArr));
		for (int i = 0; i < numLines; ++i) {
			lines.remove(lines.size() - 1);
		}
		return String.join("\n", lines);
	}
}