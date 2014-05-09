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
package org.apache.commons.vfs2.provider.smb;

import java.util.regex.Pattern;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.URLFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;

/**
 * Implementation for sftp. set default port to 139
 */
public class SmbFileNameParser extends URLFileNameParser
{
    private final static SmbFileNameParser INSTANCE = new SmbFileNameParser();
    
    private static final String IPV6_STD_PATTERN = "^\\[([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7})\\]$";

    private static final String IPV6_HEX_COMPRESSED_PATTERN = "^\\[((([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)" + "::"
        + "(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?))\\]$";

    public SmbFileNameParser()
    {
        super(139);
    }

    public static FileNameParser getInstance()
    {
        return INSTANCE;
    }

    @Override
    public FileName parseUri(final VfsComponentContext context, final FileName base, final String filename) throws FileSystemException
    {
        final StringBuilder name = new StringBuilder();

        // Extract the scheme and authority parts
        final Authority auth = extractToPath(filename, name);

        // extract domain
        String username = auth.getUserName();
        final String domain = extractDomain(username);
        if (domain != null)
        {
            username = username.substring(domain.length() + 1);
        }

        // Decode and adjust separators
        UriParser.canonicalizePath(name, 0, name.length(), this);
        UriParser.fixSeparators(name);

        // Extract the share
        final String share = UriParser.extractFirstElement(name);
        if (share == null || share.length() == 0)
        {
            throw new FileSystemException("vfs.provider.smb/missing-share-name.error", filename);
        }

        // Normalise the path.  Do this after extracting the share name,
        // to deal with things like smb://hostname/share/..
        final FileType fileType = UriParser.normalisePath(name);
        final String path = name.toString();

        return new SmbFileName(
            auth.getScheme(),
            auth.getHostName(),
            auth.getPort(),
            username,
            auth.getPassword(),
            domain,
            share,
            path,
            fileType);
    }

    private String extractDomain(final String username)
    {
        if (username == null)
        {
            return null;
        }

        for (int i = 0; i < username.length(); i++)
        {
            if (username.charAt(i) == '\\')
            {
                return username.substring(0, i);
            }
        }

        return null;
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
