package tools.aqua.dse.bounds;

import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.util.ArrayList;
import java.util.List;


public class BoundedSolverContext extends SolverContext {

	private static class StackElement {
		final ArrayList<Expression<Boolean>> exprsn = new ArrayList<>();
	}

	private final SolverContext ctx;

	private final int bound;

	private final int itr;

	private final ArrayList<StackElement> dkStack = new ArrayList<>();

	private StackElement current;
	private BoundedSolver solver;

	public BoundedSolverContext(SolverContext ctx, int bound, int itr, BoundedSolver solver) {
		this.ctx = ctx;
		this.bound = bound;
		this.itr = itr;
		this.solver = solver;
	}

	@Override
	public void push() {
		ctx.push();
		current = new StackElement();
		dkStack.add(current);
	}

	@Override
	public void pop(int n) {
		for (int i = 0; i < n; i++) {
			current = dkStack.remove(dkStack.size() - 1);
		}
		ctx.pop(n);
	}

	@Override
	public Result solve(Valuation vltn) {
		return solveWithBound(vltn);
	}

	@Override
	public void add(List<Expression<Boolean>> list) {
		for (Expression<Boolean> e : list) {
			ctx.add(e);
			current.exprsn.add(e);
		}
	}

	@Override
	public void dispose() {
		ctx.dispose();
	}


	private Result solveWithBound(Valuation vals) {

		Expression all = ExpressionUtil.and(current.exprsn);
		for (StackElement s : dkStack) {
			all = ExpressionUtil.and(all, ExpressionUtil.and(s.exprsn));
		}

		Result res = null;
		for (int i = 1; i <= itr; i++) {
			Expression<Boolean> boundExpr = solver.getBound(all, i);
			ArrayList<Expression<Boolean>> bounds = new ArrayList<>();
			bounds.add(boundExpr);
			push();
			add(bounds);
			res = ctx.solve(vals);
			pop();
			if (res == Result.SAT) {
				return res;
			}
		}
		return ctx.solve(vals);
	}

}
