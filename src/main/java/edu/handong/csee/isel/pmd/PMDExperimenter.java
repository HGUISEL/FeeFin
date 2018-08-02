package edu.handong.csee.isel.pmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
	private String targetDirBeforeFix = "targetDirBeforeFix";
	private String targetDirAfterFix = "targetDirAfterFix";

	boolean VERBOSE = false;
	private Git git;
	private Repository repo;

	private String projectDir;
	private String projectName;
	private String pmdCommand;
	private int numThreads = 1;
	private boolean help;

	Date startDate, endDate;

	public static void main(String[] args) {

		PMDExperimenter experimenter = new PMDExperimenter();
		experimenter.run(args);
	}

	private void run(String[] args) {

		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}

			String gitURI = projectDir;

			if(pmdCommand == null)
				pmdCommand = System.getProperty("os.name").contains("Windows")?"pmd.bat":"pmd";

			if(args.length == 3)
				VERBOSE = true;
			
			// initiate tmp directory name
			targetDirBeforeFix += "-" + projectName;
			targetDirAfterFix += "-" + projectName;
			try {
				git = Git.open( new File(gitURI) );
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// (1) get all commits
			ArrayList<RevCommit> commits = Utils.getRevCommits(gitURI,startDate,endDate);

			repo = git.getRepository();

			// (2) retrieve changes
			for(RevCommit rev:commits) {
				// parent is a previous commit
				RevCommit parent = rev.getParentCount()==0?null:rev.getParent(0);

				if(VERBOSE)
					System.out.println("****** Processing : " + rev.getName() + " (" + Utils.getStringDateTimeFromCommitTime(rev.getCommitTime()) + ") " + rev.getShortMessage());

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
						int i=0;
						for (DiffEntry diff : diffs) {
							if(VERBOSE) System.out.print("Processing diffs = " + (++i) + "/" + diffs.size() + "\r");
							String newPath = diff.getNewPath();
							if(newPath.indexOf("Test")>=0  || !newPath.endsWith(".java") || Utils.isWordInStatement("test", newPath)) continue;
							// ignore all files under test directory
							if(newPath.indexOf("/test")>=0) continue;
							
							if(VERBOSE) System.out.println("- Processing : " + newPath);
							
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
					HashMap<String,DetectionRecord> changedRecords = applyPMD(targetDirAfterFix,rev, null);
					HashMap<String,DetectionRecord> recordsBeforeChanged = applyPMD(targetDirBeforeFix,parent, rev);

					ArrayList<String> results = getFixedAndAliveIssues(changedRecords, recordsBeforeChanged);

					for(String result:results) {
						System.out.println(result);
					}
				}
			}
		}
	}

	private HashMap<String,DetectionRecord> applyPMD(String srcDir, RevCommit rev, RevCommit currentRev) {

		// When applyPMD is called for the previous revision, use the current revision to show correct info for FIXED cases.
		// if currentRev is not null, applyPMD is called for the previous revision.
		RevCommit targetRev = currentRev == null? rev: currentRev; 
		String commitID = targetRev.getName();
		String prevCommitID = targetRev.getParents().length > 0? targetRev.getParent(0).getName():"";
		String date = Utils.getStringDateTimeFromCommitTime(targetRev.getCommitTime());
		String datePrevCommit = targetRev.getParents().length > 0?Utils.getStringDateTimeFromCommitTime(targetRev.getParent(0).getCommitTime()):"";

		HashMap<String,DetectionRecord> detectionResults = new HashMap<String,DetectionRecord>();
		HashMap<String,DetectionRecord> filteredRecords = null;

		Runtime rt = Runtime.getRuntime();
		try {


			String cmd = pmdCommand + " -d " + srcDir + " -f csv -R category/java/errorprone.xml/DataflowAnomalyAnalysis -t " + numThreads;
			if(VERBOSE)
				System.out.println(cmd);
			Process p = rt
					.exec(cmd);

			// create a thread that deals with output
			new Thread(new Runnable() {
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					try {
						//while ((input.readLine()) != null) {
						//	}
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}).start();

			// create a thread that deals with output
			//new Thread(new Runnable() {
			//	public void run() {
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

			try {
				String line = input.readLine(); // ignore header
				if(VERBOSE)
					System.out.println(line);
				while ((line = input.readLine()) != null) {
					DetectionRecord decRec = new DetectionRecord(commitID, date, line,prevCommitID,datePrevCommit,srcDir);
					decRec.setLine(Utils.readAFile(decRec.getFullFilePath())); // set line for decRec
					String partOfKeyFromCodeLine = decRec.getLine().trim().replaceAll("\\s+", "");
					detectionResults.put(decRec.getFile() + partOfKeyFromCodeLine,decRec);
					if(VERBOSE) {
						System.out.println(line);
						System.out.println("Detected: " + commitID + " " + decRec.getFile());
					}
				}
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			//	}
			//}).start();

			p.waitFor();

			filteredRecords = filterByInterest(detectionResults);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return filteredRecords;
	}

	private ArrayList<String> getFixedAndAliveIssues(HashMap<String,DetectionRecord> changedRecords, HashMap<String,DetectionRecord> recordsBeforeChanged) {

		// TYPE, prevCommitID, prevDate, changeCommitID, change_date, path, lineNum, line
		ArrayList<String> results = new ArrayList<String>();

		// check if there are FIXED cases
		// if keys in recordsBeforeFixed are not exist, it is FIXED.
		for(String key:recordsBeforeChanged.keySet()) {
			if(!changedRecords.containsKey(key)) {
				DetectionRecord decRec = recordsBeforeChanged.get(key);
				results.add("FIXED," + getInfo(decRec));
			} else {
				DetectionRecord decRec = changedRecords.get(key);
				results.add("ALIVE," + getInfo(decRec));
			}
		}
		
		// check if there are BI cases
		for(String key:changedRecords.keySet()) {
			DetectionRecord decRec = changedRecords.get(key);
			if(!recordsBeforeChanged.containsKey(key)) {
				results.add("BI," + getInfo(decRec));
			} else {
				String result = "ALIVE," + getInfo(decRec);
				if(!results.contains(result))
					results.add(result);
			}
		}
		
		// three types of changes: BI, FIXED, UNFIXED
		/*if(fixedRecords.size() < recordsBeforeFixed.size()) { // There is something FIXED.
			//loop by recordsBeforeFixed
			for(String key:recordsBeforeFixed.keySet()) {
				if(fixedRecords.containsKey(key)) { // ALIVE
					DetectionRecord decRec = fixedRecords.get(key);
					results.add("ALIVE," + getInfo(decRec));
				} else {	// FIXED
					DetectionRecord decRec = recordsBeforeFixed.get(key);
					results.add("FIXED," + getInfo(decRec));
				}
			}
		} else {
			//loop by fixedRecords
			for(String key:fixedRecords.keySet()) {
				if(recordsBeforeFixed.containsKey(key)) { // ALIVE
					DetectionRecord decRec = fixedRecords.get(key);
					results.add("ALIVE," + getInfo(decRec));
				} else { // BI
					DetectionRecord decRec = fixedRecords.get(key);
					results.add("BI," + getInfo(decRec));
				}
			}
		}*/

		return results;
	}

	private String getInfo(DetectionRecord decRec) {
		return decRec.getFile() + decRec.getLine().replaceAll(",", "").replaceAll("\\s", "") + "," + decRec.getRuleSet() + "," + decRec.getRule() + "," + decRec.getPrevCommitID() + "," + decRec.getDataOfPrevCommit() + ","
				+ decRec.getLastestCommitIDAnIssueExists() + "," + decRec.getDate() + ","
				+ decRec.getFile() + "," + decRec.getLineNum() + "," + decRec.getLine();
	}

	private void initTargetDir(String targetDir) {
		Utils.removeAllFilesinDir(targetDir);
	}

	private void saveSourceInTargetDir(String source, String targetDir, String newPath) {
		Utils.writeAFile(source, targetDir + File.separator + newPath);
	}

	private String getFullCodeOfTheChangedFile(String newPath, RevCommit rev) {

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

	private HashMap<String,DetectionRecord> filterByInterest(HashMap<String,DetectionRecord> detectionResults) {

		HashMap<String,DetectionRecord> filteredRecords = new HashMap<String,DetectionRecord>();

		for(String key:detectionResults.keySet()) {

			DetectionRecord decRec = detectionResults.get(key);

			if(decRec.rule.equals("DataflowAnomalyAnalysis")) {
				if(decRec.description.startsWith("Found 'DD'-anomaly")) {
					filteredRecords.put(key, decRec);
				}
			}
		}

		return filteredRecords;
	}

	Options createOptions(){

		// create Options object
		Options options = new Options();

		// add options
		options.addOption(Option.builder("d").longOpt("dir")
				.desc("Project git directory path")
				.hasArg()
				.argName("git directory path")
				.required()
				.build());

		options.addOption(Option.builder("n").longOpt("name")
				.desc("Target project name")
				.hasArg()
				.argName("proj. name")
				.required()
				.build());
		
		options.addOption(Option.builder("p").longOpt("pmd")
				.desc("pmd path. Default is pmd")
				.hasArg()
				.argName("pmd path")
				.build());

		options.addOption(Option.builder("t").longOpt("thread")
				.desc("The number of threads for pmd execution. Defualt is 1")
				.hasArg()
				.argName("num of threads")
				.build());

		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Turn on verbose.")
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());

		options.addOption(Option.builder("s").longOpt("startdate")
				.desc("Start date for collecting bug-introducing changes")
				.hasArg()
				.argName("Start date")
				.build());

		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes")
				.hasArg()
				.argName("End date")
				.build());

		return options;
	}

	boolean parseOptions(Options options,String[] args){

		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);

			projectDir = cmd.getOptionValue("d");
			projectName = cmd.getOptionValue("n");
			pmdCommand = cmd.getOptionValue("p");
			numThreads = cmd.hasOption("t")?Integer.parseInt(cmd.getOptionValue("t")):1;
			VERBOSE = cmd.hasOption("v");
			help = cmd.hasOption("h");

			if(cmd.hasOption("s")){
				String strStartDate = cmd.getOptionValue("s");
				startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(strStartDate + " -0000");
			}

			if(cmd.hasOption("e")){
				String strEndDate = cmd.getOptionValue("e");
				endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(strEndDate + " -0000");
			}

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Execute PMDExperimenter to identify bug-introducing, fixed, alive bugs detected by pmd.";
		String footer ="";
		formatter.printHelp( "./PMDExperimenter", header, options, footer, true);
	}
}
