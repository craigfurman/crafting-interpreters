package lox

type Stmt interface {
	Accept(visitor StmtVisitor) error
}

type BlockStmt struct {
	statements []Stmt
}

type ExprStmt struct {
	expr Expr
}

type IfStmt struct {
	condition      Expr
	thenBr, elseBr Stmt
}

type PrintStmt struct {
	expr Expr
}

type VarStmt struct {
	name        Token
	initializer Expr
}

type StmtVisitor interface {
	VisitBlockStmt(stmt BlockStmt) error
	VisitExprStmt(stmt ExprStmt) error
	VisitIfStmt(stmt IfStmt) error
	VisitPrintStmt(stmt PrintStmt) error
	VisitVarStmt(stmt VarStmt) error
}

func (s BlockStmt) Accept(visitor StmtVisitor) error { return visitor.VisitBlockStmt(s) }
func (s ExprStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitExprStmt(s) }
func (s IfStmt) Accept(visitor StmtVisitor) error    { return visitor.VisitIfStmt(s) }
func (s PrintStmt) Accept(visitor StmtVisitor) error { return visitor.VisitPrintStmt(s) }
func (s VarStmt) Accept(visitor StmtVisitor) error   { return visitor.VisitVarStmt(s) }
