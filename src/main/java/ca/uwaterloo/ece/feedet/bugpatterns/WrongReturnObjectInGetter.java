package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongReturnObjectInGetter extends Bug {


	final double SIMILARITY_THD = 0.45; // decided by testing on FP examples

	public WrongReturnObjectInGetter(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "It might be that a getter returns a wrong field when considering their names.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();

		/*
		 *    public int getNumPendingApplications() {
		 -    return numActiveApps;
		 +    return numPendingApps;
		 */

		for(MethodDeclaration mdec: wholeCodeAST.getMethodDeclarations()){
			String mName = mdec.getName().getIdentifier();

			if(!mName.startsWith("get") || mName.length() <= 3) continue; // only consider getter

			// check the method is a getter for a field
			if(!isGetterForFields(mdec,wholeCodeAST.getFieldDeclarations())) continue;

			int lineNum = wholeCodeAST.getLineNum(mdec.getStartPosition());

			if(mdec.getBody()==null)
				continue;

			@SuppressWarnings("unchecked")
			List<Statement> lists = mdec.getBody().statements();

			/*
			 *a function only contains return a statement 
			 */
			if (lists!=null && lists.size()==1 &&(lists.get(0) instanceof ReturnStatement)){		

				for(Statement st :lists){
					if(st instanceof ReturnStatement){
						Expression exp = ((ReturnStatement) st).getExpression();

						// Q2: return a method?
						if(!(exp instanceof FieldAccess || exp instanceof SimpleName))
							continue;

						// Q3: method name is get()?
						if(mName.equals("get"))
							continue;

						String retValue = exp.toString();

						if(!similar(mName, retValue)){
							detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, mdec.toString(), false, false));	
						}
					}
				}
			}
		}	
		return detRec;
	}

	private boolean isGetterForFields(MethodDeclaration mDec, ArrayList<FieldDeclaration> lstFileds) {

		String stemName = mDec.getName().toString().replace("get", "");
		String stemNameLowerCase = stemName.toLowerCase();

		for(FieldDeclaration fieldDec:lstFileds){
			
			// Q6 return type and filed type is same? If not, ignore.
			if (mDec.getReturnType2() == null) continue;
			if(!mDec.getReturnType2().toString().equals(fieldDec.getType().toString()))
				continue; 
			
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> varDecFragList=fieldDec.fragments();
			for(VariableDeclarationFragment varDecFrag: varDecFragList){

				String fieldName = varDecFrag.getName().toString();
				String stemFieldName = fieldName.substring(0,fieldName.length()-1);
				String stemFieldNameLowerCase = stemFieldName.toLowerCase();
				if(stemNameLowerCase.startsWith(stemFieldNameLowerCase) && stemFieldNameLowerCase.length() >=5 && fromSameClass(mDec,varDecFrag)
						&& stemFieldNameLowerCase.length()/(float)stemNameLowerCase.length() > 0.55)  // exact match with more than 55% of characters of an identical substring.
					return true;
			}
		}

		return false;
	}

	private boolean fromSameClass(MethodDeclaration mDec, VariableDeclarationFragment varDecFrag) {

		String classNameOfMDec = getClassName(mDec);
		String classNameOfvDec = getClassName(varDecFrag);

		if(classNameOfMDec.equals(classNameOfvDec))
			return true;

		return false;
	}

	private String getClassName(ASTNode node) {

		if(node.getParent() == null) return "";

		if(node.getParent() instanceof TypeDeclaration)
			return ((TypeDeclaration) node.getParent()).getName().toString();

		return getClassName(node.getParent());
	}

	private boolean similar(String methodName, String returnValue){

		String stemMethodName = methodName.replace("get", "");
		String stemMethodNameLowerCase = stemMethodName.toLowerCase();
		String stemReturnExp = returnValue.substring(0,returnValue.length()-1);
		String stemReturnExpLowerCase = stemReturnExp.toLowerCase();
		stemReturnExpLowerCase = stemReturnExpLowerCase.startsWith("this.")?stemReturnExpLowerCase.replace("this.", ""):stemReturnExpLowerCase;

		if(stemMethodNameLowerCase.contains(stemReturnExpLowerCase))
			return true;

		// check abbreviation of return exp
		if(stemMethodName.length() > stemReturnExp.length()){
			String stemMethodNameByCapitals = getAbbrev(stemMethodName);

			return stemMethodNameByCapitals.toLowerCase().contains(stemReturnExpLowerCase);
		}

		// Q5:
		if(returnValue.toLowerCase().contains(stemMethodNameLowerCase))
			return true;


		return false;
	}

//	public static double textSimilarity(String s1, String s2) {
//		String longer = s1, shorter = s2;
//		if (s1.length() < s2.length()) { // longer should always have greater length
//			longer = s2; shorter = s1;
//		}
//		int longerLength = longer.length();
//		if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
//		return (longerLength - Utils.editDistance(longer, shorter)) / (double) longerLength;
//	}

	private String getAbbrev(String stemMethodName) {

		String abbrev = "";

		for(int i=0;i<stemMethodName.length();i++){
			if(Character.isUpperCase(stemMethodName.charAt(i))) abbrev+=stemMethodName.charAt(i);
		}

		return abbrev;
	}
}