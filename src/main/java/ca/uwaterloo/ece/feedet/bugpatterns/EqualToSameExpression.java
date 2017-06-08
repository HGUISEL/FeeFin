package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class EqualToSameExpression extends Bug {
	
	public EqualToSameExpression(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// example
		//      if (replicaInfo != null &&
		// -        replicaInfo.getGenerationStamp() == replicaInfo.getGenerationStamp()) {
		//	+        block.getGenerationStamp() == replicaInfo.getGenerationStamp()) {
		//	       return remove(key);
		for(InfixExpression exp:wholeCodeAST.getInfixExpressions()){
			
			if(!exp.getOperator().equals(InfixExpression.Operator.EQUALS)) continue;

			if(exp.getLeftOperand().toString().equals(exp.getRightOperand().toString())){
				
				// Revision 1-1
				//float == float is same as Float.isNan() so, false positive for float and double variables.
				String operandType = wholeCodeAST.getType(exp.getLeftOperand(),path,repo,id);
				if(operandType.toLowerCase().startsWith("float") || operandType.toLowerCase().startsWith("double")) continue;
				//if(isAPossibleRealNumberComparisonForNaN(exp.getLeftOperand(),getOnlyName(exp.getLeftOperand()))) continue;
				
				// Revision 1-2
				// Compares the same number literals?
				// This code could be intentionally written by a developer to make its condition always be true.
				if(IsEqualToTheSameLiteral(exp)) continue;
				
				// Revision 1-3
				// Object primitive cache-related?
				// This code could be intentionally written by a developer to check the number are from cached values.
				if(IsCacheRelated(exp)) continue;
				
				int lineNum = wholeCodeAST.getLineNum(exp.getStartPosition());
				detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, exp.toString(), false, false));
			}			
		}
		
		return detRec;
	}

	private boolean IsCacheRelated(ASTNode node) {
		
		// limit cache-related scope in method or a block
		if(node instanceof MethodDeclaration || node instanceof Block){
			if(node.toString().toLowerCase().contains("cache"))
				return true;
		}
		
		if(node == null)
			return false;
		
		return IsCacheRelated(node.getParent());
	}

	private boolean IsEqualToTheSameLiteral(InfixExpression exp) {
		
		if(exp.getLeftOperand() instanceof NumberLiteral || exp.getLeftOperand() instanceof StringLiteral){// && (exp.getParent() instanceof DoStatement || exp.getParent() instanceof WhileStatement)){
			return true;
		}
		
		return false;
	}
}
