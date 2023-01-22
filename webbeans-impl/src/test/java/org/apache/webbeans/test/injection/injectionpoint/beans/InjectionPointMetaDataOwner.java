/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.injection.injectionpoint.beans;

import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;

import org.apache.webbeans.test.injection.injectionpoint.common.LoggerQualifier;

@RequestScoped
@Named("org.apache.webbeans.test.injection.injectionpoint.beans.InjectionPointMetaDataOwner")
public class InjectionPointMetaDataOwner
{
    private InjectionPoint injectionPoint = null;
    
    @Produces @LoggerQualifier
    public Logger getLogger(InjectionPoint injectionPoint)
    {
        this.injectionPoint = injectionPoint;
        return Logger.getLogger(injectionPoint.getBean().getBeanClass().getName());
    }
    
    
    public InjectionPoint getInjectionPoint()
    {
        return this.injectionPoint;
    }
}
