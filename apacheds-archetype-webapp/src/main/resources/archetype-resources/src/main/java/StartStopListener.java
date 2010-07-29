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
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.factory.JdbmPartitionFactory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.server.core.factory.JdbmPartitionFactory;

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
            workingDir = new File( workingDir, "server-work" );
            workingDir.mkdirs();
            
            directoryService.setWorkingDirectory( workingDir );

            initSchema();
            initSystemPartition();
            
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
    
    
    /**
     * Inits the schema and schema partition.
     */
    private void initSchema() throws Exception
    {
        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = directoryService.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

        // Extract the schema on disk (a brand new one) and load the registries
        File serverWorkDirectory = new File( workingDirectory );
        File schemaRepository = new File( serverWorkDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( serverWorkDirectory );
        if( ! schemaRepository.exists() )
        {
            // extract only if the schema directory is not present
            extractor.extractOrCopy();
        }
        else
        {
            System.out.println( "schema partition directory exists, skipping schema extraction" );
        }

        schemaPartition.setWrappedPartition( ldifPartition );

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        directoryService.setSchemaManager( schemaManager );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager( schemaManager );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            System.out.println( errors );
            throw new RuntimeException( "there were errors while loading schema" );
        }
    }

    
    /**
     * Inits the system partition.
     * 
     * @throws Exception the exception
     */
    private void initSystemPartition() throws Exception
    {
        // change the working directory to something that is unique
        // on the system and somewhere either under target directory
        // or somewhere in a temp area of the machine.
        JdbmPartitionFactory partitionFactory = new JdbmPartitionFactory();

        // Inject the System Partition
        Partition systemPartition = partitionFactory.createPartition( "system", ServerDNConstants.SYSTEM_DN, 500,
            new File( directoryService.getWorkingDirectory(), "system" ) );
        systemPartition.setSchemaManager( directoryService.getSchemaManager() );

        partitionFactory.addIndex( systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100 );

        directoryService.setSystemPartition( systemPartition );
    }

}