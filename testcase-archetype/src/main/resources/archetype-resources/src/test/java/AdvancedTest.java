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


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.DummySSLSocketFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This is an advanced test template.
 * It shows how to configure the embedded ApacheDS:
 * <ul>
 * <li>Create a partition 'o=sevenSeas'</li>
 * <li>Enable LDAP and LDAPS protocol</li>
 * <li>Enable the 'nis' schema</li>
 * <li>Inject a new custom schema from an LDIF file</li>
 * <li>Inject test data from an LDIF file</li>
 * <li>Restart the embedded ApacheDS after each test method</li>
 * <li></li>
 * </ul>  
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.METHOD)
@Factory(AdvancedTestApacheDsFactory.class)
@ApplyLdifFiles(
    { "/sevenSeas_schema.ldif", "/sevenSeas_data.ldif" })
public class AdvancedTest
{
    // the LdapServer is injected on test startup
    public static LdapServer ldapServer;


    @Before
    public void enableNisSchema() throws Exception
    {
        List<Modification> mod = new ArrayList<Modification>();
        mod.add( new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, new DefaultClientAttribute(
            "m-disabled" ) ) );
        ldapServer.getDirectoryService().getAdminSession().modify( new LdapDN( "cn=nis,ou=schema" ), mod );
    }


    /**
     * Test that partition 'o=sevenSeas' exists.
     * 
     * @throws Exception
     */
    @Test
    public void testCustomPartitionExists() throws Exception
    {
        assertTrue( ldapServer.getDirectoryService().getAdminSession().exists( new LdapDN( "o=sevenSeas" ) ) );
    }


    /**
     * Test that LDAPS works. Use JNDI as the request must go over the wire.
     * Use a dummy SSL factory that accepts the self-signed server certificate.
     * 
     * @throws Exception
     */
    @Test
    public void testLdaps() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldaps://localhost:" + ldapServer.getPortSSL() );
        env.put( Context.SECURITY_PRINCIPAL, ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( "java.naming.ldap.factory.socket", DummySSLSocketFactory.class.getName() );
        LdapContext ctx = new InitialLdapContext( env, null );

        assertNotNull( ctx.lookup( "ou=users,ou=system" ) );
    }


    /**
     * Test that the custom schema and the data was injected.
     *
     * @throws Exception
     */
    @Test
    public void testCustomSchemaAndData() throws Exception
    {
        // use the helper method to create a JNDI context from the LdapServer
        LdapContext ctx = getWiredContext( ldapServer );

        Attributes attributes = ctx.getAttributes( "cn=HMS Victory,ou=ships,o=sevenSeas" );
        assertNotNull( attributes );
        assertNotNull( attributes.get( "objectClass" ) );
        assertTrue( attributes.get( "objectClass" ).contains( "ship" ) );
        assertNotNull( attributes.get( "numberOfGuns" ) );
        assertEquals( "104", attributes.get( "numberOfGuns" ).get() );
    }

}
