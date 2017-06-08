package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public abstract class Bug {
	String projectName = "";
	String bugName = "";
	String sourceCode = "";
	JavaASTParser wholeCodeAST;
	String id = "";
	String path = "";
	Repository repo;
	
	void initialize(String prjName,JavaASTParser ast, String id, String path, Repository repo,String bugName){
		projectName = prjName;
		wholeCodeAST = ast;
		this.id = id;
		this.path = path;
		this.repo = repo;
		this.bugName = bugName;
	}
	
	abstract public ArrayList<DetectionRecord> detect();
	
	String getName(){
		return bugName;
	}
}
