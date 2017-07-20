package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingLongCast extends Bug {

	public MissingLongCast(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	protected String getDescription() {
		return "Multiplications or summations of int values need to be casted to long. Otherwise, there might be inteager overflow.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();

		// An example loop to get some AST nodes to analyze
		for(InfixExpression infixExp:wholeCodeAST.getInfixExpressions()){
			
			if(!(infixExp.getOperator() == InfixExpression.Operator.TIMES)) continue;
			
			if(infixExp.getParent() instanceof InfixExpression) continue;
			
			if(infixExp.getLeftOperand() instanceof NumberLiteral && infixExp.getRightOperand() instanceof NumberLiteral) continue;
			if(!(infixExp.getLeftOperand() instanceof SimpleName
					|| infixExp.getLeftOperand() instanceof QualifiedName
					|| infixExp.getLeftOperand() instanceof FieldAccess
					|| infixExp.getLeftOperand() instanceof ParenthesizedExpression
					|| infixExp.getLeftOperand() instanceof NumberLiteral)) continue;
			if(!(infixExp.getRightOperand() instanceof SimpleName
					|| infixExp.getRightOperand() instanceof QualifiedName
					|| infixExp.getRightOperand() instanceof FieldAccess
					|| infixExp.getRightOperand() instanceof ParenthesizedExpression
					|| infixExp.getRightOperand() instanceof NumberLiteral)) continue;

			//System.out.println(infixExp.toString());
			
			ArrayList<ASTNode> operands = getAllOperands(infixExp);

			if(containsLessThanTwoSimpleNamesOrQualfieldNamesOrFieldAccess(operands)) continue;
			
			// e.g., a * b * 1024L?
			if(containsLongNumberLiteral(operands)) continue;
			
			// contains (long) casting?
			if(infixExp.toString().contains("(long)")) continue;
			
			// Type of SimpleName or QualifiedName is int or Integer?
			if(!areAllNamesIntTypes(operands,getMethodDeclaration(infixExp))) continue;
			
			if(!assignedToLongType(infixExp)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(infixExp.getStartPosition());
			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, infixExp.getParent().toString(), false, false));

		}

		return listDetRec;
	}

	private boolean containsLessThanTwoSimpleNamesOrQualfieldNamesOrFieldAccess(ArrayList<ASTNode> operands) {
		
		int totalNames = 0;
		
		for(ASTNode operand:operands){
			if(operand instanceof SimpleName) totalNames++;
			if(operand instanceof QualifiedName) totalNames++;
			if(operand instanceof FieldAccess) totalNames++;
			if(operand instanceof ParenthesizedExpression) totalNames++;
			//if(operand instanceof NumberLiteral) totalNames++;
		}
		
		if(totalNames<2) return true;
		
		return false;
	}

	private boolean assignedToLongType(InfixExpression infixExp) {
		
		if(infixExp.getParent() instanceof VariableDeclarationFragment){
			
			ASTNode parent = ((VariableDeclarationFragment)infixExp.getParent());
			
			if(parent.getParent() instanceof VariableDeclarationStatement){
				if(((VariableDeclarationStatement)parent.getParent()).getType().toString().toLowerCase().equals("long"))
					return true;
			}
			
			if(parent.getParent() instanceof FieldDeclaration){
				if(((FieldDeclaration)parent.getParent()).getType().toString().toLowerCase().equals("long"))
					return true;
			}
		}
		
		if(infixExp.getParent() instanceof MethodInvocation){
			if(((MethodInvocation)infixExp.getParent()).getName().toString().equals("sleep"))
				return true;
			
			// check member methods
			ArrayList<MethodDeclaration> methodDecs = wholeCodeAST.getMethodDeclarations();
			for(MethodDeclaration methodDec:methodDecs){
				if(infixExp.getParent().equals(methodDec)){
					@SuppressWarnings("unchecked")
					List<SingleVariableDeclaration> parameters = methodDec.parameters();
					if(((SingleVariableDeclaration)parameters.get(getArgumentIndex(infixExp))).getType().toString().toLowerCase().equals("long"))
						return true;
					
				}
			}
			
			// TODO check methods in another class
			
		}
		
		// TODO check ClassInstanceCreation in another class
		if(infixExp.getParent() instanceof ClassInstanceCreation){
			
		}
		
		if(infixExp.getParent() instanceof Assignment){
			Assignment assignment = (Assignment) infixExp.getParent();
			String nameAssignedTo = assignment.getLeftHandSide().toString().replace("this.", "");
			
			// left fieldAccess
			if(assignment.getLeftHandSide() instanceof FieldAccess){
				HashMap<String,VariableDeclarationFragment> mapfieldDecs = wholeCodeAST.getMapForFieldDeclarations();
				
				if(mapfieldDecs.get(nameAssignedTo) != null && mapfieldDecs.get(nameAssignedTo).getParent() instanceof FieldDeclaration){
					FieldDeclaration fieldDec = (FieldDeclaration) mapfieldDecs.get(nameAssignedTo).getParent();
					if(fieldDec.getType().toString().toLowerCase().equals("long"))
							return true;
				}
			}
			
			// left SimpleName (local variable)
			if(assignment.getLeftHandSide() instanceof SimpleName){
				String name = assignment.getLeftHandSide().toString();
				ASTNode whereToGetVarDec = wholeCodeAST.getMethodDec(assignment.getLeftHandSide());
				ArrayList<VariableDeclaration> varDecs = whereToGetVarDec == null? new ArrayList<VariableDeclaration>():wholeCodeAST.getVariableDeclaration(wholeCodeAST.getMethodDec(assignment.getLeftHandSide()));
				for(VariableDeclaration varDec:varDecs){
					if(name.equals(varDec.getName().toString())){
						if(varDec instanceof VariableDeclarationFragment){
							VariableDeclarationFragment varDecFrag = (VariableDeclarationFragment) varDec;
							if(varDecFrag.getParent() instanceof VariableDeclarationStatement){
								if(((VariableDeclarationStatement)varDecFrag.getParent()).getType().toString().toLowerCase().equals("long"))
									return true;
							}
							if(varDecFrag.getParent() instanceof VariableDeclarationExpression){
								if(((VariableDeclarationExpression)varDecFrag.getParent()).getType().toString().toLowerCase().equals("long"))
									return true;
							}
						}
						
					}
					if(name.equals(varDec.getName().toString())){
						if(varDec instanceof SingleVariableDeclaration){
							SingleVariableDeclaration sigleVarDec = (SingleVariableDeclaration) varDec;
							if(sigleVarDec.getType().toString().toLowerCase().equals("long")){
								return true;
							}
						}
					}
				}
				HashMap<String,VariableDeclarationFragment> mapfieldDecs = wholeCodeAST.getMapForFieldDeclarations();
				if(mapfieldDecs.get(nameAssignedTo) != null && mapfieldDecs.get(nameAssignedTo).getParent() instanceof FieldDeclaration){
					FieldDeclaration fieldDec = (FieldDeclaration) mapfieldDecs.get(nameAssignedTo).getParent();
					if(fieldDec.getType().toString().toLowerCase().equals("long"))
							return true;
				}
			}
		}
		
		return false;
	}

	private int getArgumentIndex(InfixExpression infixExp) {
		
		MethodInvocation methodInv = (MethodInvocation)infixExp.getParent();
		@SuppressWarnings("unchecked")
		List<ASTNode> arguments = methodInv.arguments();
		
		for(int i=0; i < arguments.size(); i++){
			if(infixExp.equals(arguments.get(i)))
				return i;
		}
		
		return -1;
	}

	private MethodDeclaration getMethodDeclaration(ASTNode node) {
		
		if(node.getParent() == null)
			return null;
		
		if(node.getParent() instanceof MethodDeclaration)
			return (MethodDeclaration) node.getParent();
		
		return getMethodDeclaration(node.getParent());
	}

	private boolean areAllNamesIntTypes(ArrayList<ASTNode> operands,MethodDeclaration methodDec) {
		
		HashMap<String,VariableDeclarationFragment> fields = wholeCodeAST.getMapForFieldDeclarations();
		HashMap<String,VariableDeclaration> varDecs = wholeCodeAST.getMapForVariableDeclaration(methodDec);
		
		//int numOperands = operands.size();
		//int numUnknownType = 0;
		
		for(ASTNode operand:operands){
			
			// For SimpleName cases
			if(operand instanceof SimpleName){
				
				VariableDeclaration varDec = (VariableDeclaration) varDecs.get(operand.toString());
				VariableDeclarationFragment varDecFragForField = (VariableDeclarationFragment) fields.get(operand.toString());
				
				if(varDec==null && varDecFragForField==null) continue;
				
				if(varDec instanceof VariableDeclarationFragment){
					if(varDec.getParent() instanceof VariableDeclarationStatement){
						VariableDeclarationStatement varDecStmt = (VariableDeclarationStatement) varDec.getParent();
						if(!(varDecStmt.getType().toString().equals("int") || varDecStmt.getType().toString().equals("Integer"))){
							return false;
						}
					}
				}
				
				if(varDec instanceof SingleVariableDeclaration){
					SingleVariableDeclaration singleVarDec = (SingleVariableDeclaration) varDec;
					if(!(singleVarDec.getType().toString().equals("int") || singleVarDec.getType().toString().equals("Integer"))){
						return false;
					}
				}
				
				if(varDecFragForField==null) continue;
				
				if(varDecFragForField.getParent() instanceof VariableDeclarationStatement){
					VariableDeclarationStatement varDecStmt = (VariableDeclarationStatement) varDecFragForField.getParent();
					if(!(varDecStmt.getType().toString().equals("int") || varDecStmt.getType().toString().equals("Integer"))){
						return false;
					}
				}
				
				if(varDecFragForField.getParent() instanceof FieldDeclaration){
					FieldDeclaration fieldDec = (FieldDeclaration) varDecFragForField.getParent();
					if(!(fieldDec.getType().toString().equals("int") || fieldDec.getType().toString().equals("Integer"))){
						return false;
					}
				}
				
			}
			
			// TODO For QualifiedName cases
			// difficult to track its type, so just ignore just for now. But it can be tracked if it is not from external library.
			if(operand instanceof QualifiedName){
				
				return false;
				
				/*if(operand.toString().toLowerCase().contains("long"))	// workaround for some FPs.
					return false;
					
				numUnknownType++;
				
				if(numUnknownType==numOperands)
					return false;*/
			}
			
			if(operand instanceof FieldAccess){
				String key = operand.toString().replace("this.", "");
				if(fields.containsKey(key)){
					VariableDeclarationFragment VarDecFrag = wholeCodeAST.getMapForFieldDeclarations().get(key);
					FieldDeclaration fieldDec = (FieldDeclaration) VarDecFrag.getParent();
					if(fieldDec.getType().toString().toLowerCase().equals("long")) return false;
				}
			}
			
		}
		
		return true;
	}

	private boolean containsLongNumberLiteral(ArrayList<ASTNode> operands) {
		
		for(ASTNode operand:operands){
			if(operand instanceof NumberLiteral){
				NumberLiteral numberLiteral = (NumberLiteral) operand;
				if(numberLiteral.getToken().toLowerCase().endsWith("l"))
					return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<ASTNode> getAllOperands(InfixExpression infixExp) {
		ArrayList<ASTNode> operands = new ArrayList<ASTNode>();
		
		operands.addAll(extractOperands(infixExp.getLeftOperand()));
		operands.addAll(extractOperands(infixExp.getRightOperand()));
		operands.addAll(infixExp.extendedOperands());
		
		return operands;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<ASTNode> extractOperands(ASTNode operand) {
		
		final ArrayList<ASTNode> operands = new ArrayList<ASTNode>();
		
		/*
		 * 
		 * infixExp.getLeftOperand() instanceof SimpleName
		 * || infixExp.getLeftOperand() instanceof QualifiedName
					|| infixExp.getLeftOperand() instanceof FieldAccess
					|| infixExp.getLeftOperand() instanceof ParenthesizedExpression
					|| infixExp.getLeftOperand() instanceof NumberLiteral
		 */
		
		
		if(operand instanceof InfixExpression){
			InfixExpression opOfInfixExp = (InfixExpression) operand;
			
			if(opOfInfixExp.getOperator() != Operator.TIMES) return operands; // only consider operands for times.
			operands.addAll(extractOperands(opOfInfixExp.getLeftOperand()));
			operands.addAll(extractOperands(opOfInfixExp.getRightOperand()));
			operands.addAll(opOfInfixExp.extendedOperands());
			
		} else if(operand instanceof SimpleName
					|| operand instanceof QualifiedName
					|| operand instanceof FieldAccess
					|| operand instanceof NumberLiteral
					|| operand instanceof CastExpression
				) {
			operands.add(operand);

		} else if(operand instanceof ConditionalExpression){
			ConditionalExpression condExp = (ConditionalExpression) operand;
			operands.addAll(extractOperands(condExp.getThenExpression()));
			operands.addAll(extractOperands(condExp.getElseExpression()));
		}
		
		if(operand instanceof ParenthesizedExpression){
			ParenthesizedExpression parenthesizedExp = (ParenthesizedExpression)operand;
			if(parenthesizedExp.getExpression() instanceof InfixExpression)
				operands.addAll(extractOperands(parenthesizedExp.getExpression()));
			else
				operands.addAll(extractOperands(parenthesizedExp.getExpression()));
			
		}
		
		return operands;
	}
}
