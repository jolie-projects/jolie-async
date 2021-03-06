/***************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.embedding.js;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.script.Invocable;
import javax.script.ScriptException;
import jolie.ExecutionContext;
import jolie.StatefulContext;
import jolie.js.JsUtils;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.StatefulMessage;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;

/**
 * @author Fabrizio Montesi
 * 
 * TODO: this shouldn't be polled
 */
public class JavaScriptCommChannel extends CommChannel
{
	private final Invocable invocable;
	private final Map< Long, CommMessage > messages = new ConcurrentHashMap< Long, CommMessage >();
	private final Object json;

	@Override
	public StatefulContext getContextFor( Long id, boolean isRequest )
	{
		throw new UnsupportedOperationException( "Not supported." );
	}

	@Override
	protected void recievedResponse( CommMessage msg )
	{
		throw new UnsupportedOperationException( "Not supported." );
	}

	@Override
	protected void messageRecv( StatefulContext ctx, CommMessage message )
	{
		throw new UnsupportedOperationException( "Not supported." );
	}
	
	private final static class JsonMethods {
		private final static String STRINGIFY = "stringify", PARSE = "parse";
	}
	
	public JavaScriptCommChannel( Invocable invocable, Object json )
	{
		this.invocable = invocable;
		this.json = json;
	}

	@Override
	public CommChannel createDuplicate()
	{
		return new JavaScriptCommChannel( invocable, json );
	}

	@Override
	protected void sendImpl( StatefulMessage msg, Function<Void, Void> completionHandler )
		throws IOException
	{
		Object returnValue = null;
		try {
			StringBuilder builder = new StringBuilder();
			JsUtils.valueToJsonString( msg.message().value(), true, Type.UNDEFINED, builder );
			Object param = invocable.invokeMethod( json, JsonMethods.PARSE, builder.toString() );
			returnValue = invocable.invokeFunction( msg.message().operationName(), param );
		} catch( ScriptException e ) {
			throw new IOException( e );
		} catch( NoSuchMethodException e ) {
			throw new IOException( e );
		}
		
		CommMessage response;
		if ( returnValue != null ) {
			Value value = Value.create();
			
			if ( returnValue instanceof Value ) {
				value.refCopy( (Value)returnValue );
			} else {
				try {
					Object s = invocable.invokeMethod( json, JsonMethods.STRINGIFY, returnValue );
					JsUtils.parseJsonIntoValue( new StringReader( (String)s ), value, true );
				} catch( ScriptException e ) {
					// TODO: do something here, maybe encode an internal server error
				} catch( NoSuchMethodException e ) {
					// TODO: do something here, maybe encode an internal server error
				}
				
				value.setValue( returnValue );
			}
			
			response = new CommMessage(
				msg.message().id(),
				msg.message().operationName(),
				msg.message().resourcePath(),
				value,
				null,
				false
			);
		} else {
			response = CommMessage.createEmptyResponse( msg.message() );
		}
		
		messages.put( msg.message().id(), response );
	}
	
	@Override
	protected CommMessage recvImpl()
		throws IOException
	{
		throw new IOException( "Unsupported operation" );
	}
	
	@Override
	public CommMessage recvResponseFor( ExecutionContext ctx, CommMessage request )
		throws IOException
	{
		return messages.remove( request.id() );
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException
	{}

	@Override
	protected void closeImpl()
	{}
}
