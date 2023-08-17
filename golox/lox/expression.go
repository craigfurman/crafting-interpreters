package lox

type Expr interface {
	Accept(visitor ExprVisitor) (any, error)
}

type ExprVisitor interface {
	VisitAssignExpr(expr *AssignExpr) (any, error)
	VisitBinaryExpr(expr *BinaryExpr) (any, error)
	VisitCallExpr(expr *CallExpr) (any, error)
	VisitGetExpr(expr *GetExpr) (any, error)
	VisitGroupingExpr(expr *GroupingExpr) (any, error)
	VisitLiteralExpr(expr *LiteralExpr) (any, error)
	VisitLogicalExpr(expr *LogicalExpr) (any, error)
	VisitSetExpr(expr *SetExpr) (any, error)
	VisitThisExpr(expr *ThisExpr) (any, error)
	VisitUnaryExpr(expr *UnaryExpr) (any, error)
	VisitVarExpr(expr *VarExpr) (any, error)
}

type AssignExpr struct {
	name Token
	expr Expr
}

type BinaryExpr struct {
	left     Expr
	operator Token
	right    Expr
}

type CallExpr struct {
	callee    Expr
	paren     Token
	arguments []Expr
}

type GetExpr struct {
	obj  Expr
	name Token
}

type GroupingExpr struct {
	expr Expr
}

type LiteralExpr struct {
	value any
}

type LogicalExpr struct {
	left     Expr
	operator Token
	right    Expr
}

type SetExpr struct {
	obj   Expr
	name  Token
	value Expr
}

type ThisExpr struct {
	keyword Token
}

type UnaryExpr struct {
	operator Token
	right    Expr
}

type VarExpr struct {
	name Token
}

func (e *AssignExpr) Accept(visitor ExprVisitor) (any, error)   { return visitor.VisitAssignExpr(e) }
func (e *BinaryExpr) Accept(visitor ExprVisitor) (any, error)   { return visitor.VisitBinaryExpr(e) }
func (e *CallExpr) Accept(visitor ExprVisitor) (any, error)     { return visitor.VisitCallExpr(e) }
func (e *GetExpr) Accept(visitor ExprVisitor) (any, error)      { return visitor.VisitGetExpr(e) }
func (e *GroupingExpr) Accept(visitor ExprVisitor) (any, error) { return visitor.VisitGroupingExpr(e) }
func (e *LiteralExpr) Accept(visitor ExprVisitor) (any, error)  { return visitor.VisitLiteralExpr(e) }
func (e *SetExpr) Accept(visitor ExprVisitor) (any, error)      { return visitor.VisitSetExpr(e) }
func (e *ThisExpr) Accept(visitor ExprVisitor) (any, error)     { return visitor.VisitThisExpr(e) }
func (e *LogicalExpr) Accept(visitor ExprVisitor) (any, error)  { return visitor.VisitLogicalExpr(e) }
func (e *UnaryExpr) Accept(visitor ExprVisitor) (any, error)    { return visitor.VisitUnaryExpr(e) }
func (e *VarExpr) Accept(visitor ExprVisitor) (any, error)      { return visitor.VisitVarExpr(e) }
