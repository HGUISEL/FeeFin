package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingLForLong extends Bug {

	public MissingLForLong(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "L is missed for a variable or a field defined Long of long.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(FieldDeclaration fieldDec:wholeCodeAST.getFieldDeclarations()){
			
			if(!isLongType(fieldDec)) continue;
			
			if(!areAllOperandsNumericAndAtLeastOneL(fieldDec)) continue;
			
				// get Line number
			int lineNum = wholeCodeAST.getLineNum(fieldDec.getStartPosition());
				
			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, fieldDec.toString(), false, false));
		}
		
		return listDetRec;
	}

	@SuppressWarnings("unchecked")
	private boolean areAllOperandsNumericAndAtLeastOneL(FieldDeclaration fieldDec) {
		
		List<VariableDeclarationFragment> varDecFrags = fieldDec.fragments();
		
		for(VariableDeclarationFragment varDecFrag:varDecFrags){
			Expression exp = varDecFrag.getInitializer();
			if(exp instanceof InfixExpression ){
				
				ArrayList<NumberLiteral> numberLiterals = new ArrayList<NumberLiteral>();
				
				InfixExpression infixExp = ((InfixExpression)exp);
				if(!(infixExp.getLeftOperand() instanceof NumberLiteral)) return false;
				numberLiterals.add((NumberLiteral)infixExp.getLeftOperand());
				
				if(!(infixExp.getRightOperand() instanceof NumberLiteral)) return false;
				numberLiterals.add((NumberLiteral)infixExp.getRightOperand());
				
				for(ASTNode node:(List<ASTNode>)infixExp.extendedOperands()){
					if(!(node instanceof NumberLiteral)) return false;
					numberLiterals.add((NumberLiteral)node);
				}
				
				for(NumberLiteral numberLiteral:numberLiterals){
					if(numberLiteral.toString().toLowerCase().endsWith("l"))
						return false;
				}
				
				// compute actual values
				int maxInt = Integer.MAX_VALUE;
				if(compute(numberLiterals) <= maxInt) return false;
				
				return true;
			}
		}
		
		return false;
	}

	private Long compute(ArrayList<NumberLiteral> numberLiterals) {
		
		ArrayList<Long> values = new ArrayList<Long>();
		
		for(NumberLiteral numLiteral:numberLiterals){
			values.add(Long.parseLong(numLiteral.toString()));
		}
		
		Long finalvalue = 1L;
		for(Long value:values){
			finalvalue *= value;
		}
		
		return finalvalue;
	}

	private boolean isLongType(FieldDeclaration fieldDec) {
		return fieldDec.getType().toString().toLowerCase().equals("long");
	}
}
