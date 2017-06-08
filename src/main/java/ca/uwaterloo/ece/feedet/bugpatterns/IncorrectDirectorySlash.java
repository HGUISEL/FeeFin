package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class IncorrectDirectorySlash extends Bug {

	public IncorrectDirectorySlash(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		String fileSource = wholeCodeAST.getStringCode();
		
		// detect "File outputDir = new File(args[1] + "-tmp");"
		// (1) there is a dir path concatenation as an argument when initiating File class?
		//for(VariableDeclarationFragment sVD:preFixWholeCodeAST.getVariableDeclarationFragments()){
		int lineNum=0;
		for(String sVD:fileSource.split("\n")){
			lineNum++;
			// Create a Pattern object
			//Find something like: File(args[i] + "-tmp")
			Pattern r = Pattern.compile("=\\s*new File\\(\\s*args\\[.+\\]\\s*\\+\\s*\\\"[^/].+\\)");
			// Pattern r = Pattern.compile("=\\s*new File\\([^,]+\\s*\\+\\s*.+\\)");

			Matcher m = r.matcher(sVD);
			if(m.find()){
				detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, sVD.toString(), false, false));
			}
		}
		
		return detRec;
	}
	

}
