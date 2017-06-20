package ca.uwaterloo.ece.feedet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.bugpatterns.EqualToSameExpression;
import ca.uwaterloo.ece.feedet.bugpatterns.IllogicalCondition;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectDirectorySlash;
import ca.uwaterloo.ece.feedet.bugpatterns.IncorrectMapIterator;
import ca.uwaterloo.ece.feedet.bugpatterns.IntOverflowOfMathMin;
import ca.uwaterloo.ece.feedet.bugpatterns.MissingThrow;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantException;
import ca.uwaterloo.ece.feedet.bugpatterns.RedundantInstantiation;
import ca.uwaterloo.ece.feedet.bugpatterns.SameObjEquals;
import ca.uwaterloo.ece.feedet.bugpatterns.SleepWithNegativeValue;
import ca.uwaterloo.ece.feedet.bugpatterns.WrongIncrementer;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class BuggyChangeDetectorAndValidator implements Runnable{

	private String gitURI;
	private String projectName;
	private String patternName;
	private boolean help;
	private String strStartDate;
	private String strEndDate;
	private Date startDate;
	private Date endDate;
	private boolean verbose = false;

	private Git git;
	private Repository repo;
	
	private String[] mArgs;

	HashSet<DetectionRecord> identifiedPotentialBug = new HashSet<DetectionRecord>();
	HashMap<String,String> mapSha1ByPath = new HashMap<String,String>(); // key: path value: String for the fix sha1.
	
	public BuggyChangeDetectorAndValidator(){
		
	}
	
	public BuggyChangeDetectorAndValidator(String[] args){
		mArgs = args;
	}

	public static void main(String[] args) {
		new BuggyChangeDetectorAndValidator().run(args);
	}

	public void run(String[] args) {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}

			try {
				git = Git.open( new File(gitURI) );
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// (1) get all commits
			ArrayList<RevCommit> commits = Utils.getRevCommits(gitURI,startDate,endDate);

			repo = git.getRepository();

			// (2) retrieve changes
			//Collections.reverse(commits); // Retrieve commits from the oldest one.
			//int i=0;
			for(RevCommit rev:commits){
				//i++;
				// get a list of files in the commit
				//if(rev.getParentCount()<1) continue;
				RevCommit parent = rev.getParentCount()==0?null:rev.getParent(0);
				
				if(verbose) System.out.println("processing: " + rev.name());
				
				if(parent==null){ // for the first commit
					// get all initial files
					TreeWalk tw = new TreeWalk(repo);
					tw.reset();
					tw.setRecursive(true);
					
					try {
						tw.addTree(rev.getTree());
						while (tw.next()) {
							process(tw.getPathString(),rev);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					tw.close();
					
				}else{
					
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
							process(diff.getNewPath(),rev);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					df.close();
				}
			}
			repo.close();
			git.close();
		}
	}
	
	private void process(String path,RevCommit rev){
		String newPath = path;
		
		if(verbose) System.out.println("path: " + path);
		
		// ignore when no previous revision of a file, Test files, and non-java files.
		if(newPath.indexOf("Test")>=0  || !newPath.endsWith(".java") || Utils.isWordInStatement("test", newPath)) return;

		// ignore all files under test directory
		if(newPath.indexOf("/test")>=0) return;

		String id =  rev.name() + "";
		
		// get preFixSource and fixSource without comments
		//String prevFileSource=Utils.removeComments(Utils.fetchBlob(repo, id +  "~1", oldPath));
		String fileSource;
		try {
			fileSource = Utils.fetchBlob(repo, id, newPath);
			
			if(fileSource.split("\n").length >10000){ // skip a huge file
				//System.out.println(fileSource.length() + " " + i + " " + newPath);
				return;
			}
			fileSource = Utils.removeComments(fileSource);
	
			/*EditList editList = Utils.getEditListFromDiff(prevFileSource, fileSource);
	
			// get line indices that are related to BI lines.
			for(Edit edit:editList){
	
				if(edit.getType()!=Edit.Type.INSERT){
	
					int beginA = edit.getBeginA();
					int endA = edit.getEndA();
	
					for(int i=beginA; i < endA ; i++)
						lstIdxOfDeletedLinesInPrevFixFile.add(i);
	
				}else{
					int beginB = edit.getBeginB();
					int endB = edit.getEndB();
	
					for(int i=beginB; i < endB ; i++)
						lstIdxOfOnlyInsteredLinesInFixFile.add(i);
				}
			}*/
	
			//e70c560322ac0a0bb385de7b835e4cc275f774ba: lucene/src/java/org/apache/lucene/index/CompoundFileWriter.java
			//  
			//if(id.equals("7e7b34994f731f3c7775431ddabe1c9e42f33d12")
			//		&& newPath.equals("src/main/groovy/sql/Sql.java"))
			//detectLogFloating(fileSource,id,newPath);
			//detectEmptyArguments(fileSource,id,newPath);
			JavaASTParser preFixWholeCodeAST = new JavaASTParser(fileSource);
			
			if(patternName.isEmpty()){
				process(new RedundantException(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new IncorrectDirectorySlash(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				//new RedundantCondition(preFixWholeCodeAST,id,newPath,repo,identifiedPotentialBug).detect();
				process(new RedundantInstantiation(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new IllogicalCondition(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				//process(new IllogicalConditionNPE(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new IncorrectMapIterator(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new MissingThrow(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new WrongIncrementer(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new SameObjEquals(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				//process(new InconsistentNullChecker(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new EqualToSameExpression(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				//process(new MissingArrayLengthCheck(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new IntOverflowOfMathMin(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
				process(new SleepWithNegativeValue(projectName,preFixWholeCodeAST,id,newPath,repo).detect());
			}else{
				Class<?> bugPatternClass = Class.forName("ca.uwaterloo.ece.feedet.bugpatterns." + patternName);
				Constructor<?> constructor = bugPatternClass.getConstructor(String.class, JavaASTParser.class,String.class,String.class,Repository.class);
				process(((Bug)constructor.newInstance(projectName,preFixWholeCodeAST,id,newPath,repo)).detect());
			}
			
			// keep this commit and path to find a fix commit
			mapSha1ByPath.put(newPath,id);
			
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			System.err.println("Path " + id);
			System.err.println("Path " + newPath);
			System.exit(0);
		} catch (RevisionSyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("Please, check the pattern name is correct: " + patternName);
			System.exit(0);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void process(ArrayList<DetectionRecord> detectionRecords) {
		
		for(DetectionRecord detRec:detectionRecords){
			if(!identifiedPotentialBug.contains(detRec)){
				identifiedPotentialBug.add(detRec);
				
				System.out.println("###\t" + detRec.getPatternName() + "\t" 
									+ projectName + "\t"
									+ getFixRevision(detRec.getPath()) + "\t"
									+ detRec.getRevID() + "\t"
									+ detRec.getPath() + "\t" 
									+ detRec.getLineNum() + "\t" 
									+ detRec.getCode()
				);		
				System.out.println(detRec.getCode());
				System.out.println(detRec.getSurroundCode());
			}
		}
	}
	
	private String getFixRevision(String path) {
		
		String strFixInfo = "Alive";
		
		if(mapSha1ByPath.containsKey(path)){
			strFixInfo = mapSha1ByPath.get(path);
		}else{
			// if file is deleted or path changes in HEAD
			try {
				if (Utils.fetchBlob(repo, "HEAD", path).equals(""))
					strFixInfo = "path_deleted";
			} catch (RevisionSyntaxException | IOException e) {
				e.printStackTrace();
			}
		}
		
		return strFixInfo;
	}

	Options createOptions(){

		// create Options object
		Options options = new Options();

		// add options
		options.addOption(Option.builder("g").longOpt("git")
				.desc("Git URI")
				.hasArg()
				.argName("URI")
				.required()
				.build());
		
		options.addOption(Option.builder("p").longOpt("project")
				.desc("Project Name")
				.hasArg()
				.argName("Name")
				.required()
				.build());
		
		options.addOption(Option.builder("n").longOpt("patternname")
				.desc("Only apply one bug pattern when the name is specified. Default = all")
				.hasArg()
				.argName("Bug Pattern Name")
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
		
		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Verbose")
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());

		return options;
	}

	boolean parseOptions(Options options,String[] args){

		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);

			gitURI = cmd.getOptionValue("g");
			help = cmd.hasOption("h");
			projectName = cmd.getOptionValue("p");
			patternName = cmd.hasOption("n")?cmd.getOptionValue("n"):"";
			verbose = cmd.hasOption("v");

			if(cmd.hasOption("s")){
				strStartDate = cmd.getOptionValue("s");
				startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(strStartDate + " -0000");
			}

			if(cmd.hasOption("e")){
				strEndDate = cmd.getOptionValue("e");
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
		String header = "Execute Buggy Change Collector. On Windows, use BCDetector.bat instead of ./BCDetector";
		String footer ="\nPlease report issues at https://github.com/lifove/BICER/issues";
		formatter.printHelp( "./BCDetector", header, options, footer, true);
	}

	@Override
	public void run() {
		run(mArgs);
	}
}
