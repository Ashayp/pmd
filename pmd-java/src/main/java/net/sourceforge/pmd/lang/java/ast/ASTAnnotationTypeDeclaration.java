/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
/* Generated By:JJTree: Do not edit this line. ASTAnnotationTypeDeclaration.java */

package net.sourceforge.pmd.lang.java.ast;

import java.util.List;

public class ASTAnnotationTypeDeclaration extends AbstractAnyTypeDeclaration {


    public ASTAnnotationTypeDeclaration(int id) {
        super(id);
    }

    public ASTAnnotationTypeDeclaration(JavaParser p, int id) {
        super(p, id);
    }

    /**
     * Accept the visitor.
     */
    @Override
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }


    @Override
    public TypeKind getTypeKind() {
        return TypeKind.ANNOTATION;
    }


    @Override
    public List<ASTAnyTypeBodyDeclaration> getDeclarations() {
        return getFirstChildOfType(ASTAnnotationTypeBody.class)
            .findChildrenOfType(ASTAnyTypeBodyDeclaration.class);
    }
}