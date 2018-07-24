package edu.handong.csee.isel.pmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
	final private String targetDir = "targetDir";
	boolean VERBOSE = true;
	private Git git;
	private Repository repo;

	public static void main(String[] args) {
		PMDExperimenter experimenter = new PMDExperimenter();
		experimenter.run(args);
	}

	private void run(String[] args) {
		
		String gitURI = args[0];
		
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
			RevCommit parent = rev.getParentCount()==0?null:rev.getParent(0);
			
			// init targetDir
			initTargetDir();
			
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
						String source = getFullCodeOfTheChangedFile(diff.getNewPath(),rev);
						saveSourceInTargetDir(source,diff.getNewPath());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				df.close();
			}
			
			// apply PMD
			applyPMD(targetDir,rev.name());
		}
	}

	private void applyPMD(String srcDir, String commitID) {
		
		String pmdCommand = System.getProperty("os.name").contains("Windows")?"pmd.bat":"pmd";

		Runtime rt = Runtime.getRuntime();
		try {
			if(VERBOSE)
				System.out.println("PMD");
			Process p = rt
					.exec(pmdCommand + " -d " + srcDir + " -f csv -R category/java/errorprone.xml/DataflowAnomalyAnalysis");

			ArrayList<DetectionRecord> detectionResults = new ArrayList<DetectionRecord>();
			// create a thread that deals with output
			new Thread(new Runnable() {
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

					try {
						String line = input.readLine(); // ignore header
						while ((line = input.readLine()) != null) {
							DetectionRecord decRec = new DetectionRecord(line);
							detectionResults.add(decRec);
							if(VERBOSE)
								System.out.println("Detected: " + commitID + " " + decRec.getFile());
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

			p.waitFor();
			
			ArrayList<DetectionRecord> filteredRecords = filterByInterest(detectionResults);

			printResults(filteredRecords);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initTargetDir() {
		Utils.removeAllFilesinDir(targetDir);
	}

	private void saveSourceInTargetDir(String source, String newPath) {
		Utils.writeAFile(source, targetDir + File.separator + newPath);
	}

	private String getFullCodeOfTheChangedFile(String newPath, RevCommit rev) {
		
		// ignore when no previous revision of a file, Test files, and non-java files.
		if(newPath.indexOf("Test")>=0  || !newPath.endsWith(".java") || Utils.isWordInStatement("test", newPath)) return null;

		// ignore all files under test directory
		if(newPath.indexOf("/test")>=0) return null;
		
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
