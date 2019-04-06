/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.resources.module;

import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.resources.api.Resource;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.*;

import static org.glassfish.resources.admin.cli.ResourceConstants.*;

/** A class that holds utility/helper routines. Expected to contain static
*  methods to perform utility operations.
*
* @since Appserver 9.0
*/
public class ResourceUtilities {

    private final static Logger _logger = LogDomains.getLogger(ResourceUtilities.class, LogDomains.RSR_LOGGER);
    private final static StringManager localStrings =
            StringManager.getManager(ResourceUtilities.class);

     private ResourceUtilities()/*disallowed*/ {
   }

   /**
    * Checks if any of the Resource in the given set has a conflict with
    * resource definitions in the domain.xml. A <b> conflict </b> is defined
    * based on the type of the resource. For example, a JDBC Resource has "jndi-name"
    * that is the identifying key where as for a JDBC Connection Pool, it is
    * the "name" that must be unique.
    * @param resSet a Set of Resource elements.
    * @param resources instance of ConfigContext that you want to confirm this against. May not be null.
    * @return a Set of Resource elements that contains conflicting elements from the given Set. This
    * method does not create any Resource elements. It just references an element in conflict
    * from a Set that is returned. If there are no conflicts, an empty Set is returned. This method
    * never returns a null. If the given Set is null, an empty Set is returned.
    */
   public static Set<org.glassfish.resources.api.Resource> getResourceConfigConflicts(final Set<org.glassfish.resources.api.Resource> resSet,
       final Resources resources) {
       final Set<Resource> conflicts = new HashSet<Resource>();
       if (resSet != null) {
           for (final org.glassfish.resources.api.Resource res : resSet) {
               boolean duplicate = hasDuplicate(resources, res);
               if (duplicate) {
                   conflicts.add(res);
               }
           }
       }
       return ( conflicts );
   }

    private static boolean hasDuplicate(Resources resources, Resource res) {
        final String id = getIdToCompare(res);
        return resources.getResourceByName(res.getClass(), id) != null;
    }

    private static String getIdToCompare(final Resource res) {
       final HashMap attrs = res.getAttributes();
       final String type = res.getType();
       String id;
       if (org.glassfish.resources.api.Resource.JDBC_CONNECTION_POOL.equals(type) ||
           Resource.CONNECTOR_CONNECTION_POOL.equals(type)){
          id = getNamedAttributeValue(attrs, CONNECTION_POOL_NAME);   // this should come from refactored stuff TBD
       }
       else if (org.glassfish.resources.api.Resource.CONNECTOR_SECURITY_MAP.equals(type)) {
           id = getNamedAttributeValue(attrs, SECURITY_MAP_NAME);  // this should come from refactored stuff TBD
       }
       else if (org.glassfish.resources.api.Resource.RESOURCE_ADAPTER_CONFIG.equals(type)) {
           id = getNamedAttributeValue(attrs, RESOURCE_ADAPTER_CONFIG_NAME);  // this should come from refactored stuff TBD
       }
       else if(org.glassfish.resources.api.Resource.CONNECTOR_WORK_SECURITY_MAP.equals(type)){
           id = getNamedAttributeValue(attrs, WORK_SECURITY_MAP_NAME);
       }
       else {
           //it is OK to assume that this Resource will one of the *RESOURCEs?
           id = getNamedAttributeValue(attrs, JNDI_NAME); // this should come from refactored stuff TBD
       }
       return ( id );
   }

     private static String getNamedAttributeValue(final HashMap attributes, final String name) {
         return (String)attributes.get(name);
     }

     /**
      * Resolves all duplicates and conflicts within an archive and returns a set
      * of resources that needs to be created for the archive being deployed. The
      * deployment backend would then use these set of resources to check for
      * conflicts with resources existing in domain.xml and then continue
      * with deployment.
      *
      * All resource duplicates within an archive found are flagged with a
      * WARNING and only one resource is added in the final <code>Resource</code>
      * <code>Set</code> returned.
      *
      * We currently do not handle any resource conflicts found within the archive
      * and the method throws an exception when this condition is detected.
      *
      * @param sunResList a list of <code>SunResourcesXML</code> corresponding to
      * sun-resources.xml found within an archive.
      *
      * @return a Set of <code>Resource</code>s that have been resolved of
      * duplicates and conflicts.
      *
      * @throws org.glassfish.resources.api.ResourceConflictException an exception is thrown when an archive is found to
      * have two or more resources that conflict with each other.
      */
     public static Set<org.glassfish.resources.api.Resource> resolveResourceDuplicatesConflictsWithinArchive(
             List<org.glassfish.resources.admin.cli.SunResourcesXML> sunResList) throws ResourceConflictException {
         boolean conflictExist = false;
         StringBuffer conflictingResources = new StringBuffer();
         Set<org.glassfish.resources.api.Resource> resourceSet = new HashSet<org.glassfish.resources.api.Resource>();
         Iterator<org.glassfish.resources.admin.cli.SunResourcesXML> sunResourcesXMLIter = sunResList.iterator();
         while(sunResourcesXMLIter.hasNext()){
             //get list of resources from one sun-resources.xml file
             org.glassfish.resources.admin.cli.SunResourcesXML sunResXML = sunResourcesXMLIter.next();
             List<org.glassfish.resources.api.Resource> resources = sunResXML.getResourcesList();
             Iterator<org.glassfish.resources.api.Resource> resourcesIter = resources.iterator();
             //for each resource mentioned
             while(resourcesIter.hasNext()){
                 org.glassfish.resources.api.Resource res = resourcesIter.next();
                 Iterator<org.glassfish.resources.api.Resource> resSetIter = resourceSet.iterator();
                 boolean addResource = true;
                 //check if a duplicate has already been added
                 while(resSetIter.hasNext()){
                     Resource existingRes = resSetIter.next();
                     if(existingRes.equals(res)){
                         //duplicate within an archive
                         addResource = false;
                         _logger.warning(localStrings.getString("duplicate.resource.sun.resource.xml",
                                 getIdToCompare(res), sunResXML.getXMLPath()));
                         break;
                     }
                     //check if another existing resource conflicts with the
                     //resource being added
                     if(existingRes.isAConflict(res)){
                         //conflict within an archive
                         addResource = false;
                         conflictingResources.append("\n");
                         String message = localStrings.getString("conflict.resource.sun.resource.xml",
                                 getIdToCompare(res), sunResXML.getXMLPath());
                         conflictingResources.append(message);
                         _logger.warning(message);
                         if(_logger.isLoggable(Level.FINE))
                             logAttributes(res);
                     }
                 }
                 if(addResource)
                     resourceSet.add(res);
             }
         }
         if(conflictingResources.toString().length() > 0){
             throw new ResourceConflictException(conflictingResources.toString());
         }
         return resourceSet;
     }

     /**
      * Checks if any of the Resource in the given set has a conflict with
      * resource definitions in the domain.xml. A <b> conflict </b> is defined
      * based on the type of the resource. For example, a JDBC Resource has "jndi-name"
      * that is the identifying key where as for a JDBC Connection Pool, it is
      * the "name" that must be unique.
      *
      * @param resList a Set of Resource elements.
      * @param resources all resources from domain.xml
      *
      * @throws org.glassfish.resources.api.ResourceConflictException an exception is thrown when an archive is found to
      * have two or more resources that conflict with resources already present in domain.xml.
      */
     public static void getResourceConflictsWithDomainXML(final List<Resource> resList,
             final Resources resources) throws ResourceConflictException {
         if (resList != null) {
             Iterator<org.glassfish.resources.api.Resource> iterRes = resList.iterator();
             StringBuffer conflictingResources = new StringBuffer();
             while (iterRes.hasNext()) {
                 org.glassfish.resources.api.Resource res = iterRes.next();
                 final String id = getIdToCompare(res);

                 if (resources.getResourceByName(res.getClass(), id) != null) {
                     conflictingResources.append("\n");
                     String message = localStrings.getString("conflict.resource.with.domain.xml",
                             getIdToCompare(res));
                     conflictingResources.append(message);
                     _logger.warning(message);
                     if(_logger.isLoggable(Level.FINE))
                         logAttributes(res);
                 }
             }
             if(conflictingResources.toString().length() > 0){
                 throw new ResourceConflictException(conflictingResources.toString());
             }
         }
     }

    private static void logAttributes(Resource res) {
        StringBuffer message = new StringBuffer();
        Set<Map.Entry> entries = res.getAttributes().entrySet();
        Iterator<Map.Entry> entriesIter = entries.iterator();
        while(entriesIter.hasNext()){
            Map.Entry entry = entriesIter.next();
            message.append(entry.getKey());
            message.append("=");
            message.append(entry.getValue());
            message.append(" ");
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(localStrings.getString("resource.attributes",
                    message.toString()));
        }
    }
}