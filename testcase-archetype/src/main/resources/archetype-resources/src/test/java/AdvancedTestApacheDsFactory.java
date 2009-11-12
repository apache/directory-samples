/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package ${groupId};


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.mina.util.AvailablePortFinder;


/**
 * A LdapServerFactory for AdvancedTest.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdvancedTestApacheDsFactory implements LdapServerFactory
{
    public LdapServer newInstance() throws Exception
    {
        // default configuration
        DirectoryService service = new DefaultDirectoryService();
        IntegrationUtils.doDelete( service.getWorkingDirectory() );
        service.getChangeLog().setEnabled( true );
        service.setAllowAnonymousAccess( true );
        service.setShutdownHookEnabled( false );

        // create a new partition "o=sevenSeas" with some indices
        JdbmPartition example = new JdbmPartition();
        example.setCacheSize( 500 );
        example.setSuffix( "o=sevenSeas" );
        example.setId( "sevenSeas" );
        Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "cn" ) );
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "ou" ) );
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "dc" ) );
        indexedAttrs.add( new JdbmIndex<String, ServerEntry>( "objectClass" ) );
        example.setIndexedAttributes( indexedAttrs );
        service.addPartition( example );

        // create the LDAP Server, create a transport for LDAP and LDAPS
        LdapServer ldapServer = new LdapServer();
        ldapServer.setDirectoryService( service );
        int ldapPort = AvailablePortFinder.getNextAvailable( 1024 );
        TcpTransport ldapTransport = new TcpTransport( ldapPort );
        int ldapsPort = AvailablePortFinder.getNextAvailable( ldapPort + 1 );
        TcpTransport ldapsTransport = new TcpTransport( ldapsPort );
        ldapsTransport.setEnableSSL( true );
        ldapServer.setTransports( ldapTransport, ldapsTransport );
        ldapServer.setAllowAnonymousAccess( true );

        return ldapServer;
    }

}
