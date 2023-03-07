package org.eclipse.epsilon.egl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.epsilon.egl.output.OutputBuffer;

public class MergingAndConflicts {
	
	/**
	 * An enum for all status codes that is used in this class 
	 */
	
	public enum StatusCode {
		ConflictsFound,
		ConflictsResolved,
		OriginalWasModified,
		MergedSuccessfully,
		NotYetMerged
	}
	
	/**
	 * Fields that used in this class to store a merging/conflicts status, and different contents
	 */
	
	private StatusCode _status;
	private String _newContents;
	private String _conflictContents;
	
	/** 
	* Private constructor
	*/
	
	private MergingAndConflicts (StatusCode status, String newContents, String conflictContents) {
		_status = status;
		_newContents = newContents;
	    _conflictContents = conflictContents;
	}
	
	public StatusCode getStatus () {
		return _status;
	}
	
	public String getNewContents () {
		return _newContents;
	}
	
	public String getConflictContents () {
		return _conflictContents;
	}
	
//	// New changes... to detect where exactlly line/s have been deleted or modified
//	public List<LinesPerLine> getMissingLines() {
//		return _missingLines;
//	}
//	// until here
	
	
	/**
	 * This method checks for various states the content may be in.
	 * if the contents were previously conflicted, and
	 * if so, returns ConflictsResolved.
	 * If not so, it checks if all original lines are still present, and
	 * if not, returns OriginalWasModified.
	 * Otherwise it will return a intermediate status.
	 * 
	 * @param existingContents
	 * @return a MergingAndConflicts object containing a status and two types of content.
	 */
	
	public static MergingAndConflicts CheckContents (String existingContents) {
		// check if the existing line has a hash at the buttom
		if (existingContents.endsWith("conflicted")) {
			// remove the last lane = conflicted
			String newContents = HashedDoc.removeEndLines(existingContents, 1);
			return new MergingAndConflicts(StatusCode.ConflictsResolved, newContents, "");
		}
		
		// check if any of the original lines was modified.
		if (!new HashedDoc(existingContents).allOriginalLinesPresent()) {
			return new MergingAndConflicts(StatusCode.OriginalWasModified, "", "");
		}
		
		return new MergingAndConflicts(StatusCode.NotYetMerged, existingContents, "");
	}
	
	/**
	 * This method checks if there is any modification or deletion to auto-generated lines
	 * Also checks if there is any conflicts between templates and generated files.
	 * It creates hashes for both existingContents and newContents, 
	 * from the hashes of existingContents it can get the originalContent.
	 * It also remove the content of regions and return them back after merging new lines.
	 * Finally, if it finds any conflicts it requires user's interaction to fix it before continue do the merging,
	 * or it returns a success status with the merged content.
	 *
	 * @param existingContents, newContents.
	 * @return a MergingAndConflicts object containing a status and different types of content.
	 */
	
	// existingContents = the contents in the disk..
	// newContents = contents that comes from the transformation..
	public static MergingAndConflicts DoMergingAndConflicts (String existingContents, String newContents) throws IOException {
		
		{
			MergingAndConflicts checked = CheckContents(existingContents);
			// not merge if there is a conflicts or the orginal was modified.. 
			if (checked.getStatus() != StatusCode.NotYetMerged) {
				return checked;
			}
		}
		
		// create hashes for existingContents and newContents,
		HashedDoc existingDoc = new HashedDoc(existingContents);
		String originalDoc = existingDoc.originalContent();
		HashedDoc newDoc = new HashedDoc(newContents);
		
		// remove regions to not merge their content..
		Map<String, List<String>> regions = new TreeMap<>();
		String doc1 = removeRegions(newDoc.getBody(), regions);
		String doc2 = removeRegions(existingDoc.getBody(), regions);
		
		// merge the contents
		newContents = new MergeTester2().merge(originalDoc, doc1, doc2);
		
		// Add regions back into contents
		for (String regionToken : regions.keySet()) {
			List<String> newLines = new ArrayList<>();
			for (String line : newContents.split("\n", -1)) {
				newLines.add(line);
				if (line.equals(regionToken))
					newLines.addAll(regions.get(regionToken));
			}
			newContents = String.join("\n", newLines);
		}
		
		// if conflicts found ...
		if (newContents.contains("\n=======\n")) {
			String existingWithHash = OutputBuffer.documentWithAppendedHashLine(existingDoc.getBody(), originalDoc);
			existingWithHash += "\nconflicted";
			return new MergingAndConflicts(StatusCode.ConflictsFound, existingWithHash, newContents); // newContents the contents of conflictid file .conflicted
		}
		// if there is no conflicts.. 
		newContents = OutputBuffer.documentWithAppendedHashLine(newContents, originalDoc);
		return new MergingAndConflicts(StatusCode.MergedSuccessfully, newContents, "");
	}

	/**
	 * This method removes the regions from some content, returning the content 
	 * without regions, and populating a map of region name -> lines.
	 * It starts by searching for a head of Protected or Sync regions.
	 * Then it ignores any contents until it finds the end of the regions.
	 * All regions are stores in the regions parameter.
	 * Finally, it returns the content without regions.
	 * 
	 * @param content, and regions
	 * @return the content without regions
	 */
	
	private static String removeRegions(String content, Map<String, List<String>> regions) {
		List<String> newLines = new ArrayList<String>();
		String[] lines = content.split("\n", -1);
		String inRegion = "";
		for (String line : lines) {
			if (!inRegion.isEmpty()) {
				regions.get(inRegion).add(line);
				if (OutputBuffer.isRegionEnd(line))
					inRegion = "";
				continue;
			}
			if (OutputBuffer.isRegionStart(line)) {
				inRegion = line;
				regions.put(inRegion, new ArrayList<>());
			}
			newLines.add(line);
		}
		return String.join("\n", newLines);
	}
}