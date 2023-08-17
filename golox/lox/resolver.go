package lox

type FunctionType string
type ClassType string

const (
	FuncTypeNone   FunctionType = "NONE"
	FuncTypeFunc   FunctionType = "FUNCTION"
	FuncTypeInit   FunctionType = "INITIALIZER"
	FuncTypeMethod FunctionType = "METHOD"

	ClassTypeNone  ClassType = "NONE"
	ClassTypeClass ClassType = "CLASS"
	ClassTypeSub   ClassType = "SUBCLASS"
)

type Resolver struct {
	interpreter     *Interpreter
	scopes          *Stack[map[string]bool]
	currentFunction FunctionType
	currentClass    ClassType
}

func NewResolver(interpreter *Interpreter) *Resolver {
	return &Resolver{
		interpreter:     interpreter,
		scopes:          &Stack[map[string]bool]{},
		currentFunction: FuncTypeNone,
		currentClass:    ClassTypeNone,
	}
}

func (r *Resolver) resolve(statements []Stmt) {
	for _, statement := range statements {
		must(statement.Accept(r))
	}
}

func (r *Resolver) VisitAssignExpr(expr *AssignExpr) (any, error) {
	_, err := expr.expr.Accept(r)
	must(err)
	r.resolveLocal(expr, expr.name)
	return nil, nil
}

func (r *Resolver) VisitBinaryExpr(expr *BinaryExpr) (any, error) {
	_, err := expr.left.Accept(r)
	must(err)
	_, err = expr.right.Accept(r)
	must(err)
	return nil, nil
}

func (r *Resolver) VisitCallExpr(expr *CallExpr) (any, error) {
	_, err := expr.callee.Accept(r)
	must(err)
	for _, arg := range expr.arguments {
		_, err := arg.Accept(r)
		must(err)
	}
	return nil, nil
}

func (r *Resolver) VisitGetExpr(expr *GetExpr) (any, error) {
	return expr.obj.Accept(r)
}

func (r *Resolver) VisitGroupingExpr(expr *GroupingExpr) (any, error) {
	return expr.expr.Accept(r)
}

func (r *Resolver) VisitLiteralExpr(expr *LiteralExpr) (any, error) {
	return nil, nil
}

func (r *Resolver) VisitLogicalExpr(expr *LogicalExpr) (any, error) {
	_, err := expr.left.Accept(r)
	must(err)
	_, err = expr.right.Accept(r)
	must(err)
	return nil, nil
}

func (r *Resolver) VisitSetExpr(expr *SetExpr) (any, error) {
	_, err := expr.obj.Accept(r)
	must(err)
	return expr.value.Accept(r)
}

func (r *Resolver) VisitThisExpr(expr *ThisExpr) (any, error) {
	if r.currentClass == ClassTypeNone {
		tokenError(expr.keyword, "Can't use 'this' outside of a class.")
	}
	r.resolveLocal(expr, expr.keyword)
	return nil, nil
}

func (r *Resolver) VisitUnaryExpr(expr *UnaryExpr) (any, error) {
	return expr.right.Accept(r)
}

func (r *Resolver) VisitVarExpr(expr *VarExpr) (any, error) {
	if r.scopes.IsEmpty() {
		return nil, nil
	}
	initialized, ok := r.scopes.Peek()[expr.name.lexeme]
	if ok && !initialized {
		tokenError(expr.name, "Can't read local variable in its own initializer.")
	}
	r.resolveLocal(expr, expr.name)
	return nil, nil
}

func (r *Resolver) VisitBlockStmt(stmt BlockStmt) error {
	r.beginScope()
	r.resolve(stmt.statements)
	r.endScope()
	return nil
}

func (r *Resolver) VisitClassStmt(stmt ClassStmt) error {
	enclosing := r.currentClass
	r.currentClass = ClassTypeClass

	r.declare(stmt.name)
	r.define(stmt.name)

	r.beginScope()
	r.scopes.Peek()["this"] = true

	for _, method := range stmt.methods {
		kind := FuncTypeMethod
		r.resolveFunction(method, kind)
	}

	r.endScope()

	r.currentClass = enclosing
	return nil
}

func (r *Resolver) VisitExprStmt(stmt ExprStmt) error {
	_, err := stmt.expr.Accept(r)
	return err
}

func (r *Resolver) VisitFuncStmt(stmt FuncStmt) error {
	r.declare(stmt.name)
	r.define(stmt.name)
	r.resolveFunction(stmt, FuncTypeFunc)
	return nil
}

func (r *Resolver) VisitIfStmt(stmt IfStmt) error {
	_, err := stmt.condition.Accept(r)
	must(err)
	must(stmt.thenBr.Accept(r))
	if stmt.elseBr != nil {
		must(stmt.elseBr.Accept(r))
	}
	return nil
}

func (r *Resolver) VisitPrintStmt(stmt PrintStmt) error {
	_, err := stmt.expr.Accept(r)
	return err
}

func (r *Resolver) VisitReturnStmt(stmt ReturnStmt) error {
	if r.currentFunction == FuncTypeNone {
		tokenError(stmt.keyword, "Can't return from top-level code.")
	}
	if stmt.value != nil {
		_, err := stmt.value.Accept(r)
		must(err)
	}
	return nil
}

func (r *Resolver) VisitVarStmt(stmt VarStmt) error {
	r.declare(stmt.name)
	if stmt.initializer != nil {
		_, err := stmt.initializer.Accept(r)
		must(err)
	}
	r.define(stmt.name)
	return nil
}

func (r *Resolver) VisitWhileStmt(stmt WhileStmt) error {
	_, err := stmt.condition.Accept(r)
	must(err)
	return stmt.body.Accept(r)
}

func (r *Resolver) beginScope() {
	r.scopes.Push(map[string]bool{})
}

func (r *Resolver) endScope() {
	r.scopes.Pop()
}

func (r *Resolver) declare(name Token) {
	// The resolver only operates on non-global scopes. The interpreter
	// special-cases the global scope, and will search it if no local scope
	// resolves a given name.
	if r.scopes.IsEmpty() {
		return
	}

	if _, ok := r.scopes.Peek()[name.lexeme]; ok {
		tokenError(name, "Already a variable with this name in this scope.")
	}
	r.scopes.Peek()[name.lexeme] = false
}

func (r *Resolver) define(name Token) {
	if r.scopes.IsEmpty() {
		return
	}
	r.scopes.Peek()[name.lexeme] = true
}

func (r *Resolver) resolveLocal(expr Expr, name Token) {
	for i := r.scopes.Size() - 1; i >= 0; i-- {
		if _, ok := r.scopes.PeekI(i)[name.lexeme]; ok {
			r.interpreter.resolve(expr, r.scopes.Size()-1-i)
			return
		}
	}
}

func (r *Resolver) resolveFunction(fn FuncStmt, kind FunctionType) {
	enclosing := r.currentFunction
	r.currentFunction = kind

	r.beginScope()
	for _, param := range fn.params {
		r.declare(param)
		r.define(param)
	}
	r.resolve(fn.body)
	r.endScope()

	r.currentFunction = enclosing
}
