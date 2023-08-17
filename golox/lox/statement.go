package lox

type Stmt interface {
	Accept(visitor StmtVisitor) error
}

type BlockStmt struct {
	statements []Stmt
}

type ClassStmt struct {
	name       Token
	superclass VarExpr
	methods    []FuncStmt
}

type ExprStmt struct {
	expr Expr
}

type FuncStmt struct {
	name   Token
	params []Token
	body   []Stmt
}

type IfStmt struct {
	condition      Expr
	thenBr, elseBr Stmt
}

type PrintStmt struct {
	expr Expr
}

type ReturnStmt struct {
	keyword Token
	value   Expr
}

type VarStmt struct {
	name        Token
	initializer Expr
}

type WhileStmt struct {
	condition Expr
	body      Stmt
}

type StmtVisitor interface {
	VisitBlockStmt(stmt BlockStmt) error
	VisitClassStmt(stmt ClassStmt) error
	VisitExprStmt(stmt ExprStmt) error
	VisitFuncStmt(stmt FuncStmt) error
	VisitIfStmt(stmt IfStmt) error
	VisitPrintStmt(stmt PrintStmt) error
	VisitReturnStmt(stmt ReturnStmt) error
	VisitVarStmt(stmt VarStmt) error
	VisitWhileStmt(stmt WhileStmt) error
}

func (s BlockStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitBlockStmt(s) }
func (s ClassStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitClassStmt(s) }
func (s ExprStmt) Accept(visitor StmtVisitor) error   { return visitor.VisitExprStmt(s) }
func (s FuncStmt) Accept(visitor StmtVisitor) error   { return visitor.VisitFuncStmt(s) }
func (s IfStmt) Accept(visitor StmtVisitor) error     { return visitor.VisitIfStmt(s) }
func (s PrintStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitPrintStmt(s) }
func (s ReturnStmt) Accept(visitor StmtVisitor) error { return visitor.VisitReturnStmt(s) }
func (s VarStmt) Accept(visitor StmtVisitor) error    { return visitor.VisitVarStmt(s) }
func (s WhileStmt) Accept(visitor StmtVisitor) error  { return visitor.VisitWhileStmt(s) }
