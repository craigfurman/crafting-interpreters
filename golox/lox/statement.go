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

type VarStmt struct {
	name        Token
	initializer Expr
}

type StmtVisitor interface {
	VisitExprStmt(stmt ExprStmt) error
	VisitPrintStmt(stmt PrintStmt) error
	VisitVarStmt(stmt VarStmt) error
}

func (s ExprStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitExprStmt(s) }
func (s PrintStmt) Accept(visitor StmtVisitor) error { return visitor.VisitPrintStmt(s) }
func (s VarStmt) Accept(visitor StmtVisitor) error   { return visitor.VisitVarStmt(s) }
