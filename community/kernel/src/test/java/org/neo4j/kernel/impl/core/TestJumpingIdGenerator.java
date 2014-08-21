/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.core;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.neo4j.kernel.IdGeneratorFactory;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.impl.nioneo.store.IdGenerator;
import org.neo4j.test.IdJumpingFileSystemAbstraction;
import org.neo4j.test.IdJumpingIdGeneratorFactory;
import org.neo4j.test.IdJumpingFileSystemAbstraction.IdJumpingFileChannel;

import static org.junit.Assert.assertEquals;

public class TestJumpingIdGenerator
{
    @Test
    public void testIt() throws Exception
    {
        int sizePerJump = 1000;
        IdGeneratorFactory factory = new IdJumpingIdGeneratorFactory( sizePerJump );
        IdGenerator generator = factory.get( IdType.NODE );
        for ( int i = 0; i < sizePerJump/2; i++ )
        {
            assertEquals( (long)i, generator.nextId() );
        }
        
        for ( int i = 0; i < sizePerJump-1; i++ )
        {
            long expected = 0x100000000L-sizePerJump/2+i;
            if ( expected >= 0xFFFFFFFFL )
            {
                expected++;
            }
            assertEquals( expected, generator.nextId() );
        }

        for ( int i = 0; i < sizePerJump; i++ )
        {
            assertEquals( 0x200000000L-sizePerJump/2+i, generator.nextId() );
        }

        for ( int i = 0; i < sizePerJump; i++ )
        {
            assertEquals( 0x300000000L-sizePerJump/2+i, generator.nextId() );
        }
    }
    
    @Test
    public void testOffsettedFileChannel() throws Exception
    {
        File fileName = new File("target/var/neostore.nodestore.db");
        IdJumpingFileSystemAbstraction offsettedFileSystem = new IdJumpingFileSystemAbstraction( 10 );
        offsettedFileSystem.deleteFile( fileName );
        offsettedFileSystem.mkdirs( fileName.getParentFile() );
        IdGenerator idGenerator = new IdJumpingIdGeneratorFactory( 10 ).get( IdType.NODE );
        IdJumpingFileChannel channel = (IdJumpingFileChannel) offsettedFileSystem.open( fileName, "rw" );
        
        for ( int i = 0; i < 16; i++ )
        {
            writeSomethingLikeNodeRecord( channel, idGenerator.nextId(), i );
        }
        
        channel.close();
        channel = (IdJumpingFileChannel) offsettedFileSystem.open( fileName, "rw" );
        idGenerator = new IdJumpingIdGeneratorFactory( 10 ).get( IdType.NODE );
        
        for ( int i = 0; i < 16; i++ )
        {
            assertEquals( i, readSomethingLikeNodeRecord( channel, idGenerator.nextId() ) );
        }
        
        channel.close();
        offsettedFileSystem.shutdown();
    }

    private byte readSomethingLikeNodeRecord( IdJumpingFileChannel channel, long id ) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate( 9 );
        channel.position( id*9 );
        channel.read( buffer );
        buffer.flip();
        buffer.getLong();
        return buffer.get();
    }

    private void writeSomethingLikeNodeRecord( IdJumpingFileChannel channel, long id, int justAByte ) throws IOException
    {
        channel.position( id*9 );
        ByteBuffer buffer = ByteBuffer.allocate( 9 );
        buffer.putLong( 4321 );
        buffer.put( (byte) justAByte );
        buffer.flip();
        channel.write( buffer );
    }
}