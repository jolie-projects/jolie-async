/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie;

/**
 *
 * @author Fabrizio Montesi
 */
public class JolieExecutorThread extends Thread implements InterpreterThread
{
	private StatefulContext sessionContext;
	private Interpreter interpreter;
	
	public JolieExecutorThread( Runnable r, Interpreter interpreter )
	{
		super( r, interpreter.programFilename() + "-" + JolieThread.createThreadName() );
		this.interpreter = interpreter;
	}

	public final void sessionContext( StatefulContext ctx ) 
	{
		sessionContext = ctx;
	}
	
	public final StatefulContext sessionContext() 
	{
		return sessionContext;
	}
	
	@Override
	public Interpreter interpreter()
	{
		return interpreter;
	}
	
	public static JolieExecutorThread currentThread()
	{
		final Thread t = Thread.currentThread();
		return ( t instanceof JolieExecutorThread ) ? (JolieExecutorThread)t : null;
	}
}
