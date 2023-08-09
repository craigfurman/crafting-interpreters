package lox

type Stmt interface {
	Accept(visitor StmtVisitor) error
}

type ExprStmt struct {
	expr Expr
}

type PrintStmt struct {
	expr Expr
}

type StmtVisitor interface {
	VisitExprStmt(stmt ExprStmt) error
	VisitPrintStmt(stmt PrintStmt) error
}

func (s ExprStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitExprStmt(s) }
func (s PrintStmt) Accept(visitor StmtVisitor) error { return visitor.VisitPrintStmt(s) }
