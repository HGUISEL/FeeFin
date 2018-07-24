package ca.uwaterloo.ece.feedet.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * @author JC
 *
 */
/**
 * @author JC
 *
 */
public class Utils {
	static public ArrayList<String> getLines(String file,boolean removeHeader){
		ArrayList<String> lines = new ArrayList<String>();
		String thisLine="";
		//Open the file for reading
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((thisLine = br.readLine()) != null) { // while loop begins here
				lines.add(thisLine);
			} // end while 
			br.close();
		} // end try
		catch (IOException e) {
			System.err.println("Error: " + e);
			//System.exit(0);
		}

		if(removeHeader)
			lines.remove(0);

		return lines;
	}

	static public DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS);
	static public RawTextComparator diffComparator = RawTextComparator.WS_IGNORE_ALL;
	static public EditList getEditListFromDiff(String file1, String file2) {
		RawText rt1 = new RawText(file1.getBytes());
		RawText rt2 = new RawText(file2.getBytes());
		EditList diffList = new EditList();

		//diffList.addAll(new HistogramDiff().diff(RawTextComparator.WS_IGNORE_ALL, rt1, rt2));
		diffList.addAll(diffAlgorithm
				.diff(diffComparator, rt1, rt2));
		return diffList;
	}

	static public EditList getEditListFromDiff(Git git,String oldSha1, String newSha1, String path){

		Repository repo = git.getRepository();

		ObjectId oldId;
		try {
			oldId = repo.resolve(oldSha1 + "^{tree}:");
			ObjectId newId = repo.resolve(newSha1 + "^{tree}");


			ObjectReader reader = repo.newObjectReader();

			// setting for renamed or copied path
			Config config = new Config();
			config.setBoolean("diff", null, "renames", true);
			DiffConfig diffConfig = config.get(DiffConfig.KEY);

			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldId);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, newId);

			List<DiffEntry> diffs= git.diff()
					.setPathFilter(FollowFilter.create(path, diffConfig))
					.setNewTree(newTreeIter)
					.setOldTree(oldTreeIter)
					.call();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DiffFormatter df = new DiffFormatter(out);
			df.setDiffAlgorithm(diffAlgorithm);
			df.setDiffComparator(diffComparator);
			df.setRepository(repo);

			for(DiffEntry entry:diffs){

				df.format(entry);
				FileHeader fileHeader = df.toFileHeader( entry );
				if(!fileHeader.getNewPath().equals(path))
					continue;

				df.close();
				return fileHeader.toEditList();
			}

			df.close();

		} catch (IndexOutOfBoundsException e){

		}
		catch (RevisionSyntaxException | IOException | GitAPIException e) {
			e.printStackTrace();
		}

		return null;
	}

	static public String fetchBlob(Repository repo, String revSpec, String path) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException{

		// Resolve the revision specification
		final ObjectId id = repo.resolve(revSpec);

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		// Get the commit object for that revision
		RevWalk walk = new RevWalk(reader);
		RevCommit commit = walk.parseCommit(id);
		walk.close();

		// Get the revision's file tree
		RevTree tree = commit.getTree();
		// .. and narrow it down to the single file's path
		TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

		if (treewalk != null) {
			// use the blob id to read the file's data
			byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
			reader.close();
			return new String(data, "utf-8");
		} else {
			return "";
		}

	}

	static public boolean doesSameLineExist(String line,String[] lines,boolean trim,boolean ignoreLineComments,boolean isAddedLine,Edit edit){

		line = ignoreLineComments?removeComments(line):line;

		// if the line is not the added line in a BI change, then check the fix hunk type in only ADDED (Edit.Type.Insert). If only added, it is not a position change
		if(!isAddedLine){
			if(edit!=null && edit.getType().equals(Edit.Type.INSERT))
				return false;
		}

		for(String lineCompare:lines){
			lineCompare = ignoreLineComments?removeComments(lineCompare):lineCompare;
			if(trim){
				if(line.trim().equals(lineCompare.trim()))
					return true;
			}
			else{
				if(line.equals(lineCompare))
					return true;
			}
		}

		return false;
	}

	static public boolean doesContainLine(String line,String[] lines,boolean trim,boolean ignoreLineComments){

		line = ignoreLineComments?removeComments(line):line;

		for(String lineCompare:lines){
			lineCompare = ignoreLineComments?removeComments(lineCompare):lineCompare;
			if(trim){
				if(lineCompare.trim().contains(line.trim()))
					return true;
			}
			else{
				if(lineCompare.contains(line))
					return true;
			}
		}

		return false;
	}

	public static String removeComments(String code) {

		JavaASTParser codeAST = new JavaASTParser(code);
		@SuppressWarnings("unchecked")
		List<Comment> lstComments = codeAST.cUnit.getCommentList();

		for(Comment comment:lstComments){
			code = replaceComments(code,comment.getStartPosition(),comment.getLength());
		}

		return code;
	}

	private static String replaceComments(String code, int startPosition, int length) {

		String pre = code.substring(0,startPosition);
		String post = code.substring(startPosition+length,code.length());

		String comments = code.substring(startPosition, startPosition+length);

		comments = comments.replaceAll("\\S"," ");

		code = pre + comments + post;

		return code;
	}

	public static String removeOneLineComment(String line) {
		return line.replaceAll("(/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/|[ \\t]*//.*)", "");
	}

	public static String getStringDateTimeFromCommitTime(int commitTime){
		SimpleDateFormat ft =  new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
		Date commitDate = new Date(commitTime* 1000L);

		TimeZone GMT = TimeZone.getTimeZone("GMT");
		ft.setTimeZone(GMT);

		return ft.format(commitDate);
	}

	public static String getStringFromStringArray(String[] wholeFixCode) {

		String string="";

		for(String line:wholeFixCode)
			string += line +"\n";

		return string;
	}

	public static boolean compareMethodParametersFromAST(List<SingleVariableDeclaration> parameters1, List<SingleVariableDeclaration> parameters2) {

		if(parameters1.size()!=parameters2.size())
			return false;

		for(int i=0;i<parameters1.size();i++){
			String type1 = parameters1.get(i).getType().toString();
			String type2 = parameters2.get(i).getType().toString();
			if(!type1.equals(type2.toString()))
				return false;
		}

		return true;
	}

	public static int getStartPosition(String biSource, int lineNum) {

		int currentPosition = 0;
		String[] lines = biSource.split("\n");

		for(int i=0; i < lines.length; i++){
			if(i==lineNum-1)
				return currentPosition;

			currentPosition+=lines[i].length() + 1; // + 1 is for \n
		}

		return -1;
	}

	public static void writeAFile(String lines, String targetFileName){
		try {
			File file= new File(targetFileName);
			File parent = file.getParentFile();
			if(!parent.exists() && !parent.mkdirs()){
				System.err.println("Couldn't create dir: " + parent);
				System.exit(0);
			}
			FileOutputStream fos = new FileOutputStream(file);
			DataOutputStream dos=new DataOutputStream(fos);

			dos.writeBytes(lines);

			dos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public static void writeAFile(ArrayList<String> lines, String targetFileName){
		try {
			File file= new File(targetFileName);
			File parent = file.getParentFile();
			if(!parent.exists() && !parent.mkdirs()){
				System.err.println("Couldn't create dir: " + parent);
				System.exit(0);
			}
			FileOutputStream fos = new FileOutputStream(file);
			DataOutputStream dos=new DataOutputStream(fos);

			for(String line:lines){
				dos.write((line+"\n").getBytes());
			}
			//dos.writeBytes();
			dos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	static public ArrayList<RevCommit> getRevCommits(String gitURI,Date startDate,Date endDate) {
		ArrayList<RevCommit> commits = new ArrayList<RevCommit>();

		try {

			Git git = Git.open( new File(gitURI) );

			Iterable<RevCommit> logs = git.log()
					.call();

			//SimpleDateFormat ft =  new SimpleDateFormat ("E yyyy.MM.dd 'at' HH:mm:ss zzz");
			for (RevCommit rev : logs) {
				Date commitDate = new Date(rev.getCommitTime()* 1000L);

				if(startDate!=null && endDate!=null){
					if(startDate.compareTo(commitDate)<=0 && commitDate.compareTo(endDate)<=0)
						commits.add(rev);
				}else{
					commits.add(rev);
				}
			}             

		} catch (IOException | GitAPIException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}

		return commits;
	}

	public static boolean findByRegExp(String regExp,String string){
		Pattern pattern=Pattern.compile(regExp);
		Matcher m = pattern.matcher(string);
		return m.find();
	}

	public static ArrayList<String> listFilesInGit(Repository repository,String shaID) throws IOException {

		Ref ref = repository.findRef(shaID);
		ArrayList<String> lstFiles = new ArrayList<String>();

		// a RevWalk allows to walk over commits based on some filtering that is
		// defined
		RevWalk walk = new RevWalk(repository);

		RevCommit commit = walk.parseCommit(ref.getObjectId());
		RevTree tree = commit.getTree();

		// now use a TreeWalk to iterate over all files in the Tree recursively
		// you can set Filters to narrow down the results if needed
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		while (treeWalk.next()) {
			lstFiles.add(treeWalk.getPathString());
		}
		
		treeWalk.close();
		walk.close();
		
		return lstFiles;
	}
	
	
	/**
	 * Check of a word is in statement. e.g., statement: (test.super()) contains two words test and super.
	 * Regular expression has a wild card for word boundary \b
	 * @param word
	 * @param stmt
	 * @return
	 */
	public static boolean isWordInStatement(String word, String stmt) {
		String pattern = "\\b" + word + "\\b"; // \\b means word boundary
		Pattern r = Pattern.compile(pattern);
		
		Matcher m = r.matcher(stmt);
		
		return m.find();
	}
	
	/**
	 * Does stmt contain string represented by a regular expression
	 * @param regexp
	 * @param statement
	 * @return
	 */
	public static boolean containsStringByRegex(String regexp,String statement) {
		String pattern = regexp;
		Pattern r = Pattern.compile(pattern);
		
		Matcher m = r.matcher(statement);
		
		return m.find();
	}
	
	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
	public static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue),
									costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

	public static void removeAllFilesinDir(String dir) {
		try {
			File directory = new File(dir);
			if (!directory.exists()) return;
			FileUtils.cleanDirectory(directory);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
