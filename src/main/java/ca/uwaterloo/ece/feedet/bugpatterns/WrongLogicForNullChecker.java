package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class WrongLogicForNullChecker extends Bug {

	public WrongLogicForNullChecker(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// Q1
		for(ConditionalExpression condExp:wholeCodeAST.getConditionalExpressions()){
			
			if(!(condExp.getExpression() instanceof InfixExpression)) continue;
			
			InfixExpression infixExp = (InfixExpression)condExp.getExpression();
			
			if(!isNullChecker(infixExp)) continue;
			
			if(!loadKnownNull(infixExp)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(condExp.getStartPosition());
			
			listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, condExp.toString(), false, false));	
		}
		
		return listDetRec;
	}

	private boolean isNullChecker(InfixExpression infixExp) {
		
		InfixExpression.Operator operator = infixExp.getOperator();
		
		if(!(infixExp.getRightOperand() instanceof NullLiteral) && !(infixExp.getRightOperand() instanceof NullLiteral))
			return false; // there is no null literal? ignore
		
		ASTNode targetNode = infixExp.getRightOperand() instanceof NullLiteral? infixExp.getLeftOperand():infixExp.getRightOperand();
		
		if(targetNode instanceof InfixExpression || targetNode instanceof StringLiteral) // this can't be a target object "Failed to send join request message [addr=" + addr + ", msg="+ ioe != null
			return false; // ignore the target object is not SimpleName
		
		if(!(operator.equals(InfixExpression.Operator.EQUALS)
				|| operator.equals(InfixExpression.Operator.NOT_EQUALS)))
			return false;
		
		return true;
	}
	
	private boolean loadKnownNull(InfixExpression infixExp) {
		
		InfixExpression.Operator operator = infixExp.getOperator();
		ASTNode leftOperand = infixExp.getLeftOperand();
		ASTNode rightOperand = infixExp.getRightOperand();
		
		if(!(leftOperand instanceof NullLiteral || rightOperand instanceof NullLiteral)) return false;
		
		// comes here? it's a null checker!
		ConditionalExpression condExp = (ConditionalExpression) infixExp.getParent();
		
		String strThenExp = condExp.getThenExpression().toString();
		String strElseExp = condExp.getElseExpression().toString();
		
		// Problematic cases
		// (1) A == null? A:B; null == A? A:B;
		// (2) A != null? B:A; null != A? B:A;
		
		// A
		String targetObj = rightOperand instanceof NullLiteral? leftOperand.toString():rightOperand.toString();
		
		// (1)
		if(operator.equals(InfixExpression.Operator.EQUALS)){
			
			// cases that must be ignored
			if(casesToBeIgnored(condExp.getThenExpression(),targetObj,strThenExp)) return false;
			
			// Q2
			if(intentionallyLoadKnownNull(targetObj,condExp.getElseExpression(),condExp.getThenExpression())) return false;
			
			if(Utils.isWordInStatement(targetObj, strThenExp)) return true;
		}
		
		// (2)
		if(operator.equals(InfixExpression.Operator.NOT_EQUALS)){
			
			// cases that must be ignored
			if(casesToBeIgnored(condExp.getElseExpression(),targetObj,strElseExp)) return false;
			
			// Q2
			if(intentionallyLoadKnownNull(targetObj,condExp.getThenExpression(),condExp.getElseExpression())) return false;
						
			if(Utils.isWordInStatement(targetObj,strElseExp)) return true;
		}
		
		return false;
	}

	// Q2: intentionally return null object? (e.g. v == null ? v : v.getObject() or v != null ? v.getObject() : v)
	// other e.g., uri == null ? ((base == null) ? "" : base) + uri : uri.toString() => ExpForNotNull: MethodInvocation
	// value != null ? URLEncoder.encode(value,encoding) : value => ExpForNotNull: MethodInvocation
	// trees != null ? trees.tail : trees ==> ExpForNotNull: QualfiedName
	// result == null ? result : result + filename ==> ExpForNotNull: InfixExpression
	// 
	private boolean intentionallyLoadKnownNull(String targetObj, Expression expForNotNull,Expression expForNull) {
		
		/*if(!(expForNotNull instanceof MethodInvocation 
				|| expForNotNull instanceof QualifiedName 
				||  expForNotNull instanceof InfixExpression
			)
		)
			return false;*/
		
		// So, if the target object is used in exp for not null case, then using the target object in exp for null is intended.
		return DoesSomethingInExpForNotNullSameAsTargtObj(expForNotNull,targetObj);
	}

	private boolean DoesSomethingInExpForNotNullSameAsTargtObj(Expression exp, String targetObj) {

		ArrayList<SimpleName> simpleNames = wholeCodeAST.getSimpleNames(exp);
		ArrayList<QualifiedName> qualifiedNames = wholeCodeAST.getQualifiedNames(exp);
		ArrayList<MethodInvocation> methodInvocations = wholeCodeAST.getMethodInvocations(exp);
		
		for(SimpleName name:simpleNames){
			if(name.toString().equals(targetObj))
				return true;
		}
		
		// to deal with this example: r.lowerBound == null ? r.lowerBound == lowerBound : r.lowerBound.equals(lowerBound)
		// targetObj is a QualifiedName
		for(QualifiedName name:qualifiedNames){
			if(name.toString().equals(targetObj))
				return true;
		}
		
		// to deal with trgetObj is a method invocation
		// remote.getAddress() != null ? remote.getAddress().getHostAddress() : remote.getAddress()
		for(MethodInvocation name:methodInvocations){
			if(name.toString().equals(targetObj))
				return true;
		}

		return false;
	}

	private boolean casesToBeIgnored(Expression targetExp, String targetObj, String strExp) {
		
		if(strExp.contains("." + targetObj)
			|| strExp.matches(".*\"[^\"]*" + targetObj+".*\".*")
			|| targetExp instanceof StringLiteral
			|| (targetExp instanceof ParenthesizedExpression
				 && ((ParenthesizedExpression)targetExp).getExpression() instanceof Assignment
				 && ((Assignment)((ParenthesizedExpression)targetExp).getExpression()).getLeftHandSide().toString().equals(targetObj)
			   )
			|| targetExp instanceof Assignment
			|| onlySameNameMethodCall(targetExp,targetObj)
		)
			return true;
		
		return false;
	}

	// Q3
	private boolean onlySameNameMethodCall(Expression targetExp, String targetObj) {
		
		final ArrayList<MethodInvocation> methodInvs = new ArrayList<MethodInvocation>();
		
		targetExp.accept(new ASTVisitor() {
			
			public boolean visit(MethodInvocation node) {
				methodInvs.add(node);
				return super.visit(node);
			}
			
		});
		
		// TODO count the string of targetObj in targetExp
		// int numStrTargetObjInTargetExp = StringUtils.
		
		// 
		int numSameNameMethodCallAsTargetOjb = 0;
		for(MethodInvocation methodInv:methodInvs){
			if(methodInv.getName().toString().equals(targetObj))
				numSameNameMethodCallAsTargetOjb++;
		}
		
		if(numSameNameMethodCallAsTargetOjb>=1) return true;
		
		return false;
	}
}
