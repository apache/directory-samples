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


import static junit.framework.Assert.assertNotNull;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPSearchResults;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This is a minimal test template.
 * It just starts up an embedded ApacheDS with default configuration:
 * <ul>
 * <li>Only system partion 'ou=system' and schema partition 'ou=schema' exist.</li>
 * <li>Only LDAP protocol (no LDAPS) is enabled, listening on port 1024.</li>
 * <li>The directory data can also be accessed directly via the core API.</li>
 * <li></li>
 * </ul>
 * 
 * The two tests methods demonstrate how to use the core API and JNDI 
 * to access the embedded ApacheDS within the JVM or over the wire.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(SiRunner.class)
public class MinimalTest
{
    // the LdapServer is injected on test startup
    public static LdapServer ldapServer;


    /**
     * Demonstrates how to use the core API.
     * Note that those requests don't go over the wire but access
     * the ApacheDS within the same JVM.
     * 
     * @throws Exception
     */
    @Test
    public void demonstrateCoreAPI() throws Exception
    {
        // get the admin session which has unlimited access rights to the directory
        CoreSession session = ldapServer.getDirectoryService().getAdminSession();
        Registries registries = ldapServer.getDirectoryService().getRegistries();

        // this entry should exist
        assertTrue( session.exists( new LdapDN( "ou=users,ou=system" ) ) );

        // this entry should not exist
        assertFalse( session.exists( new LdapDN( "uid=foo.bar,ou=users,ou=system" ) ) );

        // create an entry
        ServerEntry newEntry = new DefaultServerEntry( registries, new LdapDN( "uid=foo.bar,ou=users,ou=system" ) );
        newEntry.add( "objectClass", "top", "person", "organizationalPerson", "inetOrgPerson" );
        newEntry.add( "uid", "foo.bar" );
        newEntry.add( "cn", "Foo Bar" );
        newEntry.add( "sn", "Bar" );
        newEntry.add( "givenName", "Foo" );
        session.add( newEntry );

        // lookup entry
        ServerEntry entry = session.lookup( new LdapDN( "uid=foo.bar,ou=users,ou=system" ) );
        assertTrue( entry.hasObjectClass( "inetOrgPerson" ) );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( "Foo Bar", entry.get( "cn" ).get().getString() );

        // modify entry
        List<Modification> modifications = new ArrayList<Modification>();
        modifications.add( new ClientModification( ModificationOperation.ADD_ATTRIBUTE, new DefaultClientAttribute(
            "mail", "bar@example.com", "foo.bar@example.com" ) ) );
        modifications.add( new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, new DefaultClientAttribute(
            "givenName" ) ) );
        modifications.add( new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultClientAttribute(
            "description", "This is Foo Bar." ) ) );
        session.modify( new LdapDN( "uid=foo.bar,ou=users,ou=system" ), modifications );

        // search
        EntryFilteringCursor cursor = session.search( new LdapDN( "ou=users,ou=system" ), SearchScope.ONELEVEL,
            new EqualityNode<String>( "uid", new ClientStringValue( "foo.bar" ) ), AliasDerefMode.DEREF_ALWAYS, null );
        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertNotNull( cursor.get() );
        assertEquals( "uid=foo.bar,ou=users,ou=system", cursor.get().getDn().getUpName() );
        assertFalse( cursor.next() );
        cursor.close();

        // delete entry
        session.delete( new LdapDN( "uid=foo.bar,ou=users,ou=system" ) );
    }


    /**
     * Demonstrates how to use the JNDI API to access the embedded
     * ApacheDS over the wire.
     * 
     * @throws Exception
     */
    @Test
    public void demonstrateJndiAPI() throws Exception
    {
        // use the helper method to create a JNDI context from the LdapServer
        LdapContext ctx = getWiredContext( ldapServer );

        // this entry should exist
        assertNotNull( ctx.lookup( "ou=users,ou=system" ) );

        // this entry should not exist
        try
        {
            ctx.lookup( "uid=foo.bar,ou=users,ou=system" );
            fail( "uid=foo.bar,ou=users,ou=system doesn't exist." );
        }
        catch ( NameNotFoundException e )
        {
            // expected
            assertTrue( e.getMessage().contains( "32" ) );
        }

        // create an entry
        Attributes newAttributes = new BasicAttributes();
        Attribute newOcAttribute = new BasicAttribute( "objectClass" );
        newOcAttribute.add( "top" );
        newOcAttribute.add( "person" );
        newOcAttribute.add( "organizationalPerson" );
        newOcAttribute.add( "inetOrgPerson" );
        newAttributes.put( newOcAttribute );
        newAttributes.put( "uid", "foo.bar" );
        newAttributes.put( "cn", "Foo Bar" );
        newAttributes.put( "sn", "Bar" );
        newAttributes.put( "givenName", "Foo" );
        ctx.bind( "uid=foo.bar,ou=users,ou=system", null, newAttributes );

        // lookup entry
        Attributes attributes = ctx.getAttributes( "uid=foo.bar,ou=users,ou=system" );
        assertNotNull( attributes.get( "objectClass" ) );
        assertTrue( attributes.get( "objectClass" ).contains( "inetOrgPerson" ) );
        assertNotNull( attributes.get( "cn" ) );
        assertEquals( "Foo Bar", attributes.get( "cn" ).get() );

        // modify entry
        ModificationItem[] modificatons = new ModificationItem[3];
        modificatons[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute( "mail",
            "foo.bar@example.com" ) );
        modificatons[1] = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute( "givenName" ) );
        modificatons[2] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute( "description",
            "This is Foo Bar." ) );
        ctx.modifyAttributes( "uid=foo.bar,ou=users,ou=system", modificatons );

        // search
        SearchControls searchControls = new SearchControls();
        NamingEnumeration<SearchResult> results = ctx.search( "ou=users,ou=system", "(uid=foo.bar)", searchControls );
        assertTrue( results.hasMore() );
        SearchResult next = results.next();
        assertNotNull( next );
        assertEquals( "uid=foo.bar,ou=users,ou=system", next.getNameInNamespace() );
        assertFalse( results.hasMore() );
        results.close();

        // delete entry
        ctx.unbind( "uid=foo.bar,ou=users,ou=system" );

        ctx.close();
    }


    /**
     * Demonstrates how to use the Netscape LDAP API to access the embedded
     * ApacheDS over the wire.
     * 
     * @throws Exception
     */
    @Test
    public void demonstrateNetscapeLdapAPI() throws Exception
    {
        // use the helper method to create a Connection from the LdapServer
        LDAPConnection connection = getWiredConnection( ldapServer );

        // this entry should exist
        assertNotNull( connection.read( "ou=users,ou=system" ) );

        // this entry should not exist
        try
        {
            connection.read( "uid=foo.bar,ou=users,ou=system" );
            fail( "uid=foo.bar,ou=users,ou=system doesn't exist." );
        }
        catch ( LDAPException e )
        {
            // expected
            assertEquals( 32, e.getLDAPResultCode() );
        }

        // create an entry
        LDAPAttributeSet newAttributes = new LDAPAttributeSet();
        LDAPAttribute newOcAttribute = new LDAPAttribute( "objectClass" );
        newOcAttribute.addValue( "top" );
        newOcAttribute.addValue( "person" );
        newOcAttribute.addValue( "organizationalPerson" );
        newOcAttribute.addValue( "inetOrgPerson" );
        newAttributes.add( newOcAttribute );
        newAttributes.add( new LDAPAttribute( "uid", "foo.bar" ) );
        newAttributes.add( new LDAPAttribute( "cn", "Foo Bar" ) );
        newAttributes.add( new LDAPAttribute( "sn", "Bar" ) );
        newAttributes.add( new LDAPAttribute( "givenName", "Foo" ) );
        LDAPEntry newEntry = new LDAPEntry( "uid=foo.bar,ou=users,ou=system", newAttributes );
        connection.add( newEntry );

        // lookup entry
        LDAPEntry entry = connection.read( "uid=foo.bar,ou=users,ou=system" );
        assertNotNull( entry.getAttribute( "objectClass" ) );
        assertTrue( Arrays.asList( entry.getAttribute( "objectClass" ).getStringValueArray() ).contains(
            "inetOrgPerson" ) );
        assertNotNull( entry.getAttribute( "cn" ) );
        assertEquals( "Foo Bar", entry.getAttribute( "cn" ).getStringValues().nextElement() );

        // modify entry
        LDAPModification[] modificatons = new LDAPModification[3];
        modificatons[0] = new LDAPModification( LDAPModification.ADD, new LDAPAttribute( "mail", "foo.bar@example.com" ) );
        modificatons[1] = new LDAPModification( LDAPModification.DELETE, new LDAPAttribute( "givenName" ) );
        modificatons[2] = new LDAPModification( LDAPModification.REPLACE, new LDAPAttribute( "description",
            "This is Foo Bar." ) );
        connection.modify( "uid=foo.bar,ou=users,ou=system", modificatons );

        // search
        LDAPSearchResults results = connection.search( "ou=users,ou=system", LDAPConnection.SCOPE_ONE, "(uid=foo.bar)",
            null, true );
        assertTrue( results.hasMoreElements() );
        LDAPEntry result = results.next();
        assertNotNull( result );
        assertEquals( "uid=foo.bar,ou=users,ou=system", result.getDN() );
        assertFalse( results.hasMoreElements() );

        // delete entry
        connection.delete( "uid=foo.bar,ou=users,ou=system" );
    }

}
