package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class IncorrectMapIterator extends Bug {

	public IncorrectMapIterator(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Map interator contains an error. Check whether the interation should be conducted by a list of keys or values.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// examples
		// Map m = nsm.getPrefixToURIMapping();
		// (1) for (Iterator i = m.values().iterator(); i.hasNext();) {
		// or
		// Iterator i = m.values()
		// (2) while(i.hasNext()){
		//
		// Map.Entry e = (Map.Entry) i.next();
		
		// (1) for
		for(ForStatement forStmt:wholeCodeAST.getForStatements()){
			
			for(Object initStmt:forStmt.initializers()){
				if(initStmt instanceof VariableDeclarationExpression && initStmt.toString().contains(".values().iterator()")){
					VariableDeclarationExpression varDec = (VariableDeclarationExpression) initStmt;
					if(!(varDec.getType() instanceof ParameterizedType)){
						for(Object obj:varDec.fragments()){
							VariableDeclarationFragment varDecFrag = (VariableDeclarationFragment) obj;
							String itrName = varDecFrag.getName().toString();
							if(Utils.containsStringByRegex("\\(\\s*Map\\.Entry\\s*\\)\\s*" + itrName + "\\.next\\(\\)", forStmt.getBody().toString())){
								int lineNum = wholeCodeAST.getLineNum(forStmt.getStartPosition());
								detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, forStmt.toString(), false, false));
							}		
						}
					}
				}
			}
		}
		// (2) while TODO
		
		return detRec;
	}
}
