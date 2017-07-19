package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongPositionOfNullChecker extends Bug {

	public WrongPositionOfNullChecker(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	protected String getDescription() {
		return "Null checker is used in a wrong position. Please, check if there is a usage of an object before its null checker.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();


		for(InfixExpression infixExp:wholeCodeAST.getInfixExpressions()){
			
			ASTNode nullCheckingObject = getNullCheckingObject(infixExp);
			
			if(nullCheckingObject == null) continue;
			
			// get method dec that has infixExp
			MethodDeclaration methodDec = wholeCodeAST.getMethodDecBelongTo(infixExp);
			
			if(methodDec == null) continue;

			// get Line number
			int lineNum = wholeCodeAST.getLineNum(infixExp.getStartPosition());
			
			// get method invocations
			ArrayList<MethodInvocation> methodInvs = wholeCodeAST.getMethodInvocations(methodDec);
			// get field accesses
			ArrayList<FieldAccess> fieldAccesses = wholeCodeAST.getFieldAccesses(methodDec);
			// get QualifiedName 
			ArrayList<QualifiedName> qualifiedNames = wholeCodeAST.getQualifiedNames(methodDec);
			
			if(!existMethodInvBeforeNullChecker(methodInvs,lineNum,nullCheckingObject)
					&& !existFieldAccessBeforeNullChecker(fieldAccesses,lineNum,nullCheckingObject)
					&& !existQualifiedNameBeforeNullChecker(qualifiedNames,lineNum,nullCheckingObject)
					) continue;

			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, infixExp.toString(), infixExp.getParent().getParent().toString(),false, false));
		}

		return listDetRec;
	}

	private boolean existQualifiedNameBeforeNullChecker(ArrayList<QualifiedName> qualifiedNames, int lineNum, ASTNode nullCheckingObject) {
			for(QualifiedName qualifiedName:qualifiedNames){
			
			int lineNumOfMethodInv = wholeCodeAST.getLineNum(qualifiedName.getStartPosition());
			if(lineNumOfMethodInv >= lineNum) continue;
			
			if(qualifiedName.getQualifier().toString().equals(nullCheckingObject.toString())) return true;
			
		}
		
		return false;
	}

	private boolean existMethodInvBeforeNullChecker(ArrayList<MethodInvocation> methodInvs, int lineNum, ASTNode nullCheckingObject) {
		
		for(MethodInvocation methodInv:methodInvs){
			
			int lineNumOfMethodInv = wholeCodeAST.getLineNum(methodInv.getStartPosition());
			if(lineNumOfMethodInv >= lineNum) continue;
			
			if(methodInv.getExpression()==null) continue;
			
			if(methodInv.getExpression().toString().equals(nullCheckingObject.toString())) return true;
			
		}
		
		return false;
	}
	
	private boolean existFieldAccessBeforeNullChecker(ArrayList<FieldAccess> fieldAccesses, int lineNum, ASTNode nullCheckingObject) {
		
		for(FieldAccess fieldAccess:fieldAccesses){
			
			int lineNumOfMethodInv = wholeCodeAST.getLineNum(fieldAccess.getStartPosition());
			if(lineNumOfMethodInv >= lineNum) continue;
			
			if(fieldAccess.getExpression().toString().equals(nullCheckingObject.toString())) return true;
			
		}
		
		return false;
	}

	private ASTNode getNullCheckingObject(InfixExpression infixExp) {
		
		if(infixExp.getOperator() == Operator.NOT_EQUALS){
			if(infixExp.getLeftOperand().getNodeType() == ASTNode.NULL_LITERAL)
				return  infixExp.getRightOperand();
			
			if(infixExp.getRightOperand().getNodeType() == ASTNode.NULL_LITERAL)
				return  infixExp.getLeftOperand();
		}
		
		return null;
	}
}
