package lox

type Expr interface {
	Accept(visitor ExprVisitor) any
}

type BinaryExpr struct {
	left     Expr
	operator Token
	right    Expr
}

func (e BinaryExpr) Accept(visitor ExprVisitor) any { return visitor.VisitBinaryExpr(e) }

type GroupingExpr struct {
	expr Expr
}

func (e GroupingExpr) Accept(visitor ExprVisitor) any { return visitor.VisitGroupingExpr(e) }

type LiteralExpr struct {
	value any
}

func (e LiteralExpr) Accept(visitor ExprVisitor) any { return visitor.VisitLiteralExpr(e) }

type UnaryExpr struct {
	operator Token
	right    Expr
}

func (e UnaryExpr) Accept(visitor ExprVisitor) any { return visitor.VisitUnaryExpr(e) }

type ExprVisitor interface {
	VisitBinaryExpr(expr BinaryExpr) any
	VisitGroupingExpr(expr GroupingExpr) any
	VisitLiteralExpr(expr LiteralExpr) any
	VisitUnaryExpr(expr UnaryExpr) any
}
