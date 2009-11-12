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
package ${package};


import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;


/**
 * A Servlet context listener to start and stop ApacheDS.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
 */
public class StartStopListener implements ServletContextListener
{

    private DirectoryService directoryService;

    private LdapServer ldapServer;


    /**
     * Startup ApacheDS embedded.
     */
    public void contextInitialized( ServletContextEvent evt )
    {
        try
        {
            directoryService = new DefaultDirectoryService();
            directoryService.setShutdownHookEnabled( true );

            ldapServer = new LdapServer();
            ldapServer.setDirectoryService( directoryService );
            ldapServer.setAllowAnonymousAccess( true );

            // Set LDAP port to 10389
            TcpTransport ldapTransport = new TcpTransport( 10389 );
            ldapServer.setTransports( ldapTransport );

            // Determine an appropriate working directory
            ServletContext servletContext = evt.getServletContext();
            File workingDir = ( File ) servletContext.getAttribute( "javax.servlet.context.tempdir" );
            directoryService.setWorkingDirectory( workingDir );

            directoryService.startup();
            ldapServer.start();

            // Store directoryService in context to provide it to servlets etc.
            servletContext.setAttribute( DirectoryService.JNDI_KEY, directoryService );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     * Shutdown ApacheDS embedded.
     */
    public void contextDestroyed( ServletContextEvent evt )
    {
        try
        {
            ldapServer.stop();
            directoryService.shutdown();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}