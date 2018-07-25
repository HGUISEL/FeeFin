package edu.handong.csee.isel.pmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import ca.uwaterloo.ece.feedet.utils.Utils;

public class PMDExperimenter {
	final private String targetDirBeforeFix = "targetDirBeforeFix";
	final private String targetDirAfterFix = "targetDirAfterFix";
	boolean VERBOSE = false;
	private Git git;
	private Repository repo;
	String pmdCommand = "";

	public static void main(String[] args) {
		
		PMDExperimenter experimenter = new PMDExperimenter();
		experimenter.run(args);
	}

	private void run(String[] args) {
		
		String gitURI = args[0];
		if(args.length >= 2)
			pmdCommand = args[1];
		
		if(args.length == 3)
			VERBOSE = true;
		
		try {
			git = Git.open( new File(gitURI) );
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// (1) get all commits
		ArrayList<RevCommit> commits = Utils.getRevCommits(gitURI,null,null);
		
		repo = git.getRepository();
		
		// (2) retrieve changes
		for(RevCommit rev:commits) {
			// parent is a previous commit
			RevCommit parent = rev.getParentCount()==0?null:rev.getParent(0);
			
			// init targetDir
			initTargetDir(targetDirBeforeFix);
			initTargetDir(targetDirAfterFix);
			
			if(parent!=null) {
				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
				df.setRepository(repo);
				df.setDiffAlgorithm(Utils.diffAlgorithm);
				df.setDiffComparator(Utils.diffComparator);
				df.setDetectRenames(true);
				List<DiffEntry> diffs;
				try {
					// do diff
					diffs = df.scan(parent.getTree(), rev.getTree());
					for (DiffEntry diff : diffs) {
						String prevSource = getFullCodeOfTheChangedFile(diff.getOldPath(),parent); // in case a file name changes, we need to get source from the old path
						String fixedSource = getFullCodeOfTheChangedFile(diff.getNewPath(),rev);
						saveSourceInTargetDir(prevSource, targetDirBeforeFix,diff.getNewPath()); // set new path even for the prev revision
						saveSourceInTargetDir(fixedSource, targetDirAfterFix,diff.getNewPath()); 
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				df.close();
				
				// apply PMD
				ArrayList<DetectionRecord> fixedRecords = applyPMD(targetDirAfterFix,rev);
				ArrayList<DetectionRecord> recordsBeforeFixed = applyPMD(targetDirBeforeFix,parent);
				
				ArrayList<String> results = getFixedAndAliveIssues(fixedRecords, recordsBeforeFixed);
			}
		}
	}

	private ArrayList<DetectionRecord> applyPMD(String srcDir, RevCommit rev) {
		
		String commitID = rev.getName();
		String date = Utils.getStringDateTimeFromCommitTime(rev.getCommitTime());
		
		if(pmdCommand.isEmpty())
			pmdCommand = System.getProperty("os.name").contains("Windows")?"pmd.bat":"pmd";
		
		ArrayList<DetectionRecord> detectionResults = new ArrayList<DetectionRecord>();
		
		Runtime rt = Runtime.getRuntime();
		try {
			
			
			String cmd = pmdCommand + " -d " + srcDir + " -f csv -R category/java/errorprone.xml/DataflowAnomalyAnalysis";
			if(VERBOSE)
				System.out.println(cmd);
			Process p = rt
					.exec(cmd);

			
			// create a thread that deals with output
			new Thread(new Runnable() {
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

					try {
						String line = input.readLine(); // ignore header
						if(VERBOSE)
							System.out.println(line);
						while ((line = input.readLine()) != null) {
							DetectionRecord decRec = new DetectionRecord(commitID, date, line);
							decRec.setLine(Utils.readAFile(decRec.getFile())); // set line for decRec
							detectionResults.add(decRec);
							if(VERBOSE) {
								System.out.println(line);
								System.out.println("Detected: " + commitID + " " + decRec.getFile());
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

			p.waitFor();
			
			ArrayList<DetectionRecord> filteredRecords = filterByInterest(detectionResults);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return detectionResults;
	}

	private ArrayList<String> getFixedAndAliveIssues(ArrayList<DetectionRecord> fixedRecords, ArrayList<DetectionRecord> recordsBeforeFixed) {
		
		ArrayList<String> results = new ArrayList<String>();
		
		// three types of changes: BI, FIXED, UNFIXED
		if(fixedRecords.size() < recordsBeforeFixed.size()) {
			for(DetectionRecord decRecBeforeFixed:recordsBeforeFixed) {
				for(DetectionRecord decRecFixed:fixedRecords) {
					
				}
			}
			System.out.println(recordsBeforeFixed.get(0).getLastestCommitIDAnIssueExists() + " " + recordsBeforeFixed.get(0).getFile());
		}
		
		return results;
	}

	private void initTargetDir(String targetDir) {
		Utils.removeAllFilesinDir(targetDir);
	}

	private void saveSourceInTargetDir(String source, String targetDir, String newPath) {
		Utils.writeAFile(source, targetDir + File.separator + newPath);
	}

	private String getFullCodeOfTheChangedFile(String newPath, RevCommit rev) {
		
		// ignore when no previous revision of a file, Test files, and non-java files.
		if(newPath.indexOf("Test")>=0  || !newPath.endsWith(".java") || Utils.isWordInStatement("test", newPath)) return ""; // return empty string to avoid to analyze

		// ignore all files under test directory
		if(newPath.indexOf("/test")>=0) return "";
		
		String id =  rev.name() + "";
		String fileSource = null;
		
		try {
			fileSource = Utils.fetchBlob(repo, id, newPath);
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileSource;
	}

	private ArrayList<DetectionRecord> filterByInterest(ArrayList<DetectionRecord> detectionResults) {
		
		ArrayList<DetectionRecord> filteredRecords = new ArrayList<DetectionRecord>();
		
		for(DetectionRecord decRec:detectionResults) {
			
			if(decRec.rule.equals("DataflowAnomalyAnalysis")) {
				if(decRec.description.startsWith("Found 'DD'-anomaly")) {
					filteredRecords.add(decRec);
				}
			}
		}
		
		return filteredRecords;
	}

	private void printResults(ArrayList<DetectionRecord> detectionResults) {

		for (DetectionRecord decRec : detectionResults) {
			decRec.showSummary();
		}

	}
}
