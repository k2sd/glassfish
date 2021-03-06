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

package com.sun.enterprise.tools.verifier.tests.ejb.beanclass;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Modifier;

/** 
 * Enterprise Java Bean class modifiers test.  The class must not be defined 
 * as abstract.
 */
public class EjbClassModifiersAbstract extends EjbTest  { 


    /**
     * Enterprise Java Bean class modifiers test.  The class must not be defined
     * as abstract.
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor   
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
        boolean shouldBeAbstract = false;
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (descriptor instanceof EjbEntityDescriptor) {
            String persistentType = ((EjbEntityDescriptor)descriptor).getPersistenceType();
            if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistentType)) {
                if (EjbCMPEntityDescriptor.CMP_1_1!=((EjbCMPEntityDescriptor) descriptor).getCMPVersion()) {
                    shouldBeAbstract = true;
                }
            }
        }
        Class c = loadEjbClass(descriptor, result);
        if (c!=null) {

            boolean isAbstract = false;
            int modifiers = c.getModifiers();
            if (Modifier.isAbstract(modifiers)) {
                isAbstract = true;
            }

            if (!isAbstract && !shouldBeAbstract) {
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
                result.passed(smh.getLocalString
			      (getClass().getName() + ".passed",
			       "[ {0} ] properly declares non-abstract class modifier.",
			       new Object[] {descriptor.getEjbClassName()}));
            }
            if (isAbstract && shouldBeAbstract) {
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
                result.passed(smh.getLocalString
			      (getClass().getName() + ".passed2",
			       "[ {0} ] properly declares abstract class modifier.",
			       new Object[] {descriptor.getEjbClassName()}));
            }
            if (isAbstract && !shouldBeAbstract ) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
			      (getClass().getName() + ".failed",
			       "Error: Ejb Class [ {0} ] was found, but was declared as abstract.  The class  [ {1} ] must not be abstract.",
			       new Object[] {descriptor.getEjbClassName(),descriptor.getEjbClassName()}));
            }
            if (!isAbstract && shouldBeAbstract ) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
			      (getClass().getName() + ".failed2",
			       "Error: CMP 2.0 Entity Bean Class [ {0} ] was found, but was declared as non abstract.  The class  [ {1} ] must be abstract.",
			       new Object[] {descriptor.getEjbClassName(),descriptor.getEjbClassName()}));
            }
	    
        }
        return result;
 
    }
}
