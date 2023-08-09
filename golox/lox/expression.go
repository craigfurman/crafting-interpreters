package lox

type Expr interface {
	Accept(visitor ExprVisitor) (any, error)
}

type ExprVisitor interface {
	VisitBinaryExpr(expr BinaryExpr) (any, error)
	VisitGroupingExpr(expr GroupingExpr) (any, error)
	VisitLiteralExpr(expr LiteralExpr) (any, error)
	VisitUnaryExpr(expr UnaryExpr) (any, error)
}

type BinaryExpr struct {
	left     Expr
	operator Token
	right    Expr
}

type GroupingExpr struct {
	expr Expr
}

type LiteralExpr struct {
	value any
}

type UnaryExpr struct {
	operator Token
	right    Expr
}

func (e BinaryExpr) Accept(visitor ExprVisitor) (any, error)   { return visitor.VisitBinaryExpr(e) }
func (e GroupingExpr) Accept(visitor ExprVisitor) (any, error) { return visitor.VisitGroupingExpr(e) }
func (e LiteralExpr) Accept(visitor ExprVisitor) (any, error)  { return visitor.VisitLiteralExpr(e) }
func (e UnaryExpr) Accept(visitor ExprVisitor) (any, error)    { return visitor.VisitUnaryExpr(e) }
