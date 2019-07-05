/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.dfa;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.dfa.DataFlowNode;
import net.sourceforge.pmd.lang.dfa.StartOrEndDataFlowNode;
import net.sourceforge.pmd.lang.dfa.VariableAccess;
import net.sourceforge.pmd.lang.java.ast.ASTAssignmentOperator;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceBodyDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTVariableInitializer;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.ast.JavaParserVisitorAdapter;
import net.sourceforge.pmd.lang.java.symboltable.JavaNameOccurrence;
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;

/**
 * Searches for special nodes and computes based on the sequence, the type of
 * access of a variable.
 * 
 * @since Created on 14.07.2004
 * @author raik, Sven Jacob
 */
public class VariableAccessVisitor extends JavaParserVisitorAdapter {

    public void compute(ASTMethodDeclaration node) {
        if (node.jjtGetParent() instanceof ASTClassOrInterfaceBodyDeclaration) {
            this.computeNow(node);
        }
    }

    public void compute(ASTConstructorDeclaration node) {
        this.computeNow(node);
    }

    private void computeNow(Node node) {
        

        DataFlowNode inode = node.getDataFlowNode();

        List<VariableAccess> undefinitions = markUsages(inode);

        // all variables are first in state undefinition
        DataFlowNode firstINode = inode.getFlow().get(0);
        firstINode.setVariableAccess(undefinitions);

        // all variables are getting undefined when leaving scope
        DataFlowNode lastINode = inode.getFlow().get(inode.getFlow().size() - 1);
        lastINode.setVariableAccess(undefinitions);
    }

    private List<VariableAccess> markUsages(DataFlowNode inode) {
        // undefinitions was once a field... seems like it works fine as a local
        List<VariableAccess> undefinitions = new ArrayList<>();
        Set<Map<VariableNameDeclaration, List<NameOccurrence>>> variableDeclarations = collectDeclarations(inode);
        for (Map<VariableNameDeclaration, List<NameOccurrence>> declarations : variableDeclarations) {
            for (Map.Entry<VariableNameDeclaration, List<NameOccurrence>> entry : declarations.entrySet()) {
                VariableNameDeclaration vnd = entry.getKey();
                if (vnd.getAccessNodeParent() instanceof ASTFormalParameter) {
                    // no definition/undefinition/references for parameters
                    continue;
                } else if (vnd.getAccessNodeParent().getFirstDescendantOfType(ASTVariableInitializer.class) != null) {
                    // add definition for initialized variables
                    addVariableAccess(vnd.getNode(), new VariableAccess(VariableAccess.DEFINITION, vnd.getImage()),
                            inode.getFlow());
                }
                undefinitions.add(new VariableAccess(VariableAccess.UNDEFINITION, vnd.getImage()));

                //make arraylist which could store all name occurrences
                List<NameOccurrence> data = new ArrayList<>();

                //make arraylist which could store sorted name occurrences
                List<NameOccurrence> order = new ArrayList<>();

                //add all occurrences to arraylist(data)
                for (NameOccurrence occurrence : entry.getValue()) {
                    data.add(occurrence);
                }
                
                //make arraylist which could store pair structure which is consist of assignment operator and related Name occurrences
                ArrayList<SimpleEntry<Node, NameOccurrence>> assignments = new ArrayList<>();

                //travers all elements which are in arraylist(data)
                for (NameOccurrence name : data) {
                    //Make a flag which is for preventing double addition of same name occurrence
                    int flag = 0;
                    //traverse parents nodes until it is method declaration or constructor declaration
                    checker :
                    for (Node temp = name.getLocation(); !(temp.jjtGetParent() instanceof ASTMethodDeclaration) 
                        && !(temp.jjtGetParent() instanceof ASTConstructorDeclaration); temp = temp.jjtGetParent()) {
                        //traverse children nodes to check it has assignment operator as a child 
                        for (int i = 0; i < temp.jjtGetNumChildren(); i++) {
                            if (temp.jjtGetChild(i) instanceof ASTAssignmentOperator) {
                                //if assignment operator is found, add to arraylist(assignments) with the assignment operator and its name
                                assignments.add(new SimpleEntry(temp.jjtGetChild(i), name));
                                //change flag
                                flag = 1;
                                break checker;    
                            }
                        }
                    }
                    //if a name is not related to assignment operator, add to arraylist(assignments) with null and its name
                    if (flag == 0) {
                        assignments.add(new SimpleEntry(null, name));
                    }
                }
                //swap the arraylist(assignments)'s elements, when it share same assignment operator
                for (int i = 0; i < assignments.size() - 1; i++) {
                    if (assignments.get(i).getKey() != null 
                        && assignments.get(i).getKey().equals(assignments.get(i + 1).getKey())) {
                        Collections.swap(assignments, i, i + 1);
                    }
                }
                //add sorted element from assignments(arraylist) to order(arraylist) 
                for (SimpleEntry<Node, NameOccurrence> tempPair : assignments) {
                    order.add(tempPair.getValue());
                }
                //add access to the sorted arraylist
                for (NameOccurrence orderedOccurrence : order) {
                    addAccess((JavaNameOccurrence) orderedOccurrence, inode);
                }
            }
        }
        return undefinitions;
    }

    private Set<Map<VariableNameDeclaration, List<NameOccurrence>>> collectDeclarations(DataFlowNode inode) {
        Set<Map<VariableNameDeclaration, List<NameOccurrence>>> decls = new HashSet<>();
        Map<VariableNameDeclaration, List<NameOccurrence>> varDecls;
        for (int i = 0; i < inode.getFlow().size(); i++) {
            DataFlowNode n = inode.getFlow().get(i);
            if (n instanceof StartOrEndDataFlowNode) {
                continue;
            }
            varDecls = ((JavaNode) n.getNode()).getScope().getDeclarations(VariableNameDeclaration.class);
            if (!decls.contains(varDecls)) {
                decls.add(varDecls);
            }
        }
        return decls;
    }

    private void addAccess(JavaNameOccurrence occurrence, DataFlowNode inode) {
        if (occurrence.isOnLeftHandSide()) {
            this.addVariableAccess(occurrence.getLocation(),
                    new VariableAccess(VariableAccess.DEFINITION, occurrence.getImage()), inode.getFlow());
        } else if (occurrence.isOnRightHandSide()
                || !occurrence.isOnLeftHandSide() && !occurrence.isOnRightHandSide()) {
            this.addVariableAccess(occurrence.getLocation(),
                    new VariableAccess(VariableAccess.REFERENCING, occurrence.getImage()), inode.getFlow());
        }
    }

    /**
     * Adds a VariableAccess to a dataflow node.
     * 
     * @param node
     *            location of the access of a variable
     * @param va
     *            variable access to add
     * @param flow
     *            dataflownodes that can contain the node.
     */
    private void addVariableAccess(Node node, VariableAccess va, List<DataFlowNode> flow) {
        // backwards to find the right inode (not a method declaration)
        for (int i = flow.size() - 1; i > 0; i--) {
            DataFlowNode inode = flow.get(i);
            if (inode.getNode() == null) {
                continue;
            }

            List<? extends Node> children = inode.getNode().findDescendantsOfType(node.getClass());
            for (Node n : children) {
                if (node.equals(n)) {
                    List<VariableAccess> v = new ArrayList<>();
                    v.add(va);
                    inode.setVariableAccess(v);
                    return;
                }
            }
        }
    }

}
