/***************************************************************************
 *   Copyright (C) 2011 by Karoly Szanto                                   *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.behaviours;

import jolie.StatefulContext;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

/**
 * Add an expression value to a VariablePath's value, assigning the resulting
 * value to the VariablePath.
 * @see Expression
 * @see VariablePath
 * @author Karoly Szanto
 */
public class AddAssignmentBehaviour implements Behaviour, Expression
{
	final private VariablePath varPath;
	final private Expression expression;

	/** Constructor.
	 *
	 * @param varPath the variable which will receive the value
	 * @param expression the expression of which the evaluation will be added to the the variable's value
	 */
	public AddAssignmentBehaviour( VariablePath varPath, Expression expression )
	{
		this.varPath = varPath;
		this.expression = expression;
	}

	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new AddAssignmentBehaviour(
			(VariablePath) varPath.cloneExpression( reason ),
			expression.cloneExpression( reason ) );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason )
	{
		return new AddAssignmentBehaviour(
			(VariablePath) varPath.cloneExpression( reason ),
			expression.cloneExpression( reason ) );
	}

	/** Evaluates the expression and adds its value to the variable's value.
	 * @param ctx */
	public void run(StatefulContext ctx)
	{
		if ( ctx.isKilled() ) {
			return;
		}
		varPath.getValue().add( expression.evaluate() );
	}

	@Override
	public Value evaluate()
	{
		Value val = varPath.getValue();
		val.add( expression.evaluate() );
		return val;
	}

	@Override
	public boolean isKillable()
	{
		return true;
	}
}
