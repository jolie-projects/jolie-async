/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

public class SodepProtocol extends CommProtocol
{
	private Charset stringCharset = Charset.forName( "UTF8" );
	
	private String readString( DataInput in )
		throws IOException
	{
		int len = in.readInt();
		if ( len > 0 ) {
			byte[] bb = new byte[ len ];
			in.readFully( bb );
			return new String( bb, stringCharset );
		}
		return "";
	}
	
	private void writeString( DataOutput out, String str )
		throws IOException
	{
		if ( str.isEmpty() ) {
			out.writeInt( 0 );
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter( bos, stringCharset );
			writer.write( str );
			writer.close();
			byte[] bb = bos.toByteArray();
			out.writeInt( bb.length );
			out.write( bb );
		}
	}
	
	private void writeFault( DataOutput out, FaultException fault )
		throws IOException
	{
		writeString( out, fault.faultName() );
		writeValue( out, fault.value() );
	}
	
	private void writeValue( DataOutput out, Value value )
		throws IOException
	{
		Object valueObject = value.valueObject();
		if ( valueObject == null ) {
			out.writeByte( 0 );
		} else if ( valueObject instanceof String ) {
			out.writeByte( 1 );
			writeString( out, (String)valueObject );
		} else if ( valueObject instanceof Integer ) {
			out.writeByte( 2 );
			out.writeInt( ((Integer)valueObject).intValue() );
		} else if ( valueObject instanceof Double ) {
			out.writeByte( 3 );
			out.writeDouble( ((Double)valueObject).doubleValue() );
		} else {
			out.writeByte( 0 );
		}

		Map< String, ValueVector > children = value.children();
		out.writeInt( children.size() );
		for( Entry< String, ValueVector > entry : children.entrySet() ) {
			writeString( out, entry.getKey() );
			out.writeInt( entry.getValue().size() );
			for( Value v : entry.getValue() )
				writeValue( out, v );
		}
	}
	
	private void writeMessage( DataOutput out, CommMessage message )
		throws IOException
	{
		writeString( out, message.resourcePath() );
		writeString( out, message.operationName() );
		FaultException fault = message.fault();
		if ( fault == null ) {
			out.writeBoolean( false );
		} else {
			out.writeBoolean( true );
			writeFault( out, fault );
		}
		writeValue( out, message.value() );
	}
	
	private Value readValue( DataInput in )
		throws IOException
	{
		Value value = Value.create();
		Object valueObject = null;
		byte b = in.readByte();
		if ( b == 1 ) { // String
			valueObject = readString( in );
		} else if ( b == 2 ) { // Integer
			valueObject = new Integer( in.readInt() );
		} else if ( b == 3 ) { // Double
			valueObject = new Double( in.readInt() );
		}

		value.setValue( valueObject );
				
		Map< String, ValueVector > children = value.children();
		String s;
		int n, i, size, k;
		n = in.readInt(); // How many children?
		ValueVector vec;
		
		for( i = 0; i < n; i++ ) {
			s = readString( in );
			vec = ValueVector.create();
			size = in.readInt();
			for( k = 0; k < size; k++ ) {
				vec.add( readValue( in ) );
			}
			children.put( s, vec );
		}
		return value;
	}
	
	private FaultException readFault( DataInput in )
		throws IOException
	{
		String faultName = readString( in );
		Value value = readValue( in );
		return new FaultException( faultName, value );
	}
	
	private CommMessage readMessage( DataInput in )
		throws IOException
	{
		String resourcePath = readString( in );
		String operationName = readString( in );
		FaultException fault = null;
		if ( in.readBoolean() == true ) {
			fault = readFault( in );
		}
		Value value = readValue( in );
		return new CommMessage( operationName, resourcePath, value, fault );
	}
	
	public SodepProtocol( VariablePath configurationPath )
	{
		super( configurationPath );
	}
	
	public SodepProtocol clone()
	{
		return new SodepProtocol( configurationPath );
	}

	public void send( OutputStream ostream, CommMessage message )
		throws IOException
	{
		if ( getParameterVector( "keepAlive" ).first().intValue() == 0 ) {
			channel.setToBeClosed( true );
		} else {
			channel.setToBeClosed( false );
		}

		String charset = getParameterVector( "charset" ).first().strValue();
		if ( !charset.isEmpty() ) {
			stringCharset = Charset.forName( charset );
		}
		GZIPOutputStream gzip = null;
		String compression = getParameterVector( "compression" ).first().strValue();
		if ( "gzip".equals( compression ) ) {
			gzip = new GZIPOutputStream( ostream );
			ostream = gzip;
		}
		
		DataOutputStream oos = new DataOutputStream( ostream );
		writeMessage( oos, message );
		oos.flush();
		if ( gzip != null )
			gzip.finish();
	}

	public CommMessage recv( InputStream istream )
		throws IOException
	{
		if ( getParameterVector( "keepAlive" ).first().intValue() == 0 ) {
			channel.setToBeClosed( true );
		} else {
			channel.setToBeClosed( false );
		}

		String charset = getParameterVector( "charset" ).first().strValue();
		if ( !charset.isEmpty() ) {
			stringCharset = Charset.forName( charset );
		}
		String compression = getParameterVector( "compression" ).first().strValue();
		if ( "gzip".equals( compression ) ) {
			istream = new GZIPInputStream( istream );
		}
		
		DataInputStream ios = new DataInputStream( istream );
		return readMessage( ios );
	}
}