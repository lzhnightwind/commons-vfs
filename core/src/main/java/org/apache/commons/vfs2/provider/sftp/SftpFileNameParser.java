/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.provider.sftp;

import java.util.regex.Pattern;

import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.URLFileNameParser;

/**
 * Implementation for SFTP. Sets the default port to 22.
 */
public class SftpFileNameParser extends URLFileNameParser
{
    private static final int DEFAULT_PORT = 22;

    private static final SftpFileNameParser INSTANCE = new SftpFileNameParser();
    
    private static final String IPV6_STD_PATTERN = "^\\[([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7})\\]$";

    private static final String IPV6_HEX_COMPRESSED_PATTERN = "^\\[((([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)" + "::"
        + "(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?))\\]$";

    /**
     * Creates a new instance with a the default port 22.
     */
    public SftpFileNameParser()
    {
        super(DEFAULT_PORT);
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance.
     */
    public static FileNameParser getInstance()
    {
        return INSTANCE;
    }
    
    @Override
    protected String extractHostName( StringBuilder name ) 
    {
        int pos = 0;
        
        // Support the URL described in RFC 2732
        if ( name.charAt( 0 ) == '[' && name.indexOf( "]" ) != 0 )
        {
            pos = name.indexOf( "]" ) + 1;
        }

        // If not include ipv6 address, parse it as before
        if ( pos == 0 ) 
        {
            return super.extractHostName( name );
        }
        
        final Pattern ipv6_std_pattern = Pattern.compile( IPV6_STD_PATTERN );
        final Pattern ipv6_hex_compressed_pattern = Pattern.compile( IPV6_HEX_COMPRESSED_PATTERN );
        
        final String hostname = name.substring( 0, pos );

        if ( ipv6_std_pattern.matcher( hostname ).matches()
            || ipv6_hex_compressed_pattern.matcher( hostname ).matches() )
        {
            name.delete(0, pos);
            return hostname;  
        }
        else {
            return super.extractHostName( name );
        }

    }
}
