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
package org.neo4j.kernel.impl.transaction.xaframework;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.transaction.xaframework.log.entry.LogHeader;

import static java.lang.Math.max;

import static org.neo4j.kernel.impl.transaction.xaframework.log.entry.LogHeaderParser.LOG_HEADER_SIZE;
import static org.neo4j.kernel.impl.transaction.xaframework.log.entry.LogHeaderParser.readLogHeader;

/**
 * Used to figure out what logical log file to open when the database
 * starts up.
 */
public class PhysicalLogFiles
{
    public interface LogVersionVisitor
    {
        void visit( File file, long logVersion );
    }

    private static class HighestLogVersionVisitor implements LogVersionVisitor
    {
        private long highest = -1;

        @Override
        public void visit( File file, long logVersion )
        {
            highest = max( highest, logVersion );
        }
    }

    private final File logBaseName;
    private final Pattern logFilePattern;
    private final FileSystemAbstraction fileSystem;

    public PhysicalLogFiles( File directory, String name, FileSystemAbstraction fileSystem )
    {
        this.logBaseName = new File( directory, name );
        this.logFilePattern = Pattern.compile( name + "\\.v\\d+" );
        this.fileSystem = fileSystem;
    }

    public PhysicalLogFiles( File directory, FileSystemAbstraction fileSystem )
    {
        this( directory, PhysicalLogFile.DEFAULT_NAME, fileSystem );
    }

    public File getLogFileForVersion( long version )
    {
        return new File( logBaseName.getPath() + ".v" + version );
    }

    public boolean versionExists( long version )
    {
        return fileSystem.fileExists( getLogFileForVersion( version ) );
    }

    public LogHeader extractHeader( long version ) throws IOException
    {
        return readLogHeader( fileSystem, getLogFileForVersion( version ) );
    }

    public boolean hasAnyTransaction( long version )
    {
        return fileSystem.getFileSize( getLogFileForVersion( version ) ) > LOG_HEADER_SIZE;
    }

    public long getHighestLogVersion()
    {
        HighestLogVersionVisitor visitor = new HighestLogVersionVisitor();
        accept( visitor );
        return visitor.highest;
    }

    public void accept( LogVersionVisitor visitor )
    {
        for ( File file : fileSystem.listFiles( logBaseName.getParentFile() ) )
        {
            if ( logFilePattern.matcher( file.getName() ).matches() )
            {
                visitor.visit( file, getLogVersion( file ) );
            }
        }
    }

    public static long getLogVersion( File historyLogFile )
    {
        // Get version based on the name
        String name = historyLogFile.getName();
        String toFind = ".v";
        int index = name.lastIndexOf( toFind );
        if ( index == -1 )
        {
            throw new RuntimeException( "Invalid log file '" + historyLogFile + "'" );
        }
        return Integer.parseInt( name.substring( index + toFind.length() ) );
    }
}