/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;

/**
 * Reprensts bean instances that are contextual
 * and injectable by the container.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean representation type
 */
public interface Bean<T> extends Contextual<T>
{
	
    /**
     * Returns api types of a bean.
     * 
     * @return api types of a bean
     */
    public abstract Set<Type> getTypes();

    /**
     * Returns binding types of a bean.
     * 
     * @return binding types of a bean
     */
    public abstract Set<Annotation> getBindings();


    /**
     * Returns bean deployment type.
     * 
     * @return bean's deployment type.
     */
    public abstract Class<? extends Annotation> getDeploymentType();
    
    
    /**
     * Returns scope types of a bean.
     * 
     * @return scope type
     */
    public abstract Class<? extends Annotation> getScopeType();

    /**
     * Returns name of a bean.
     * 
     * @return name of a bean
     */
    public abstract String getName();

    /**
     * Returns true if bean is capable of
     * serializable, false otherwise.
     * 
     * @return true if bean is serializable
     */
    public abstract boolean isSerializable();

    /**
     * If bean is nullable return true, false
     * otherwise. 
     * 
     * <p>
     * Nullable means that if producer
     * bean api type is primitive, its nullable property
     * will be false.
     * </p>
     * 
     * @return true if bean is nullable.
     */
    public abstract boolean isNullable();

    /**
     * Returns all injection points of this bean.
     * 
     * @return injection points
     */
    public abstract Set<InjectionPoint> getInjectionPoints();
    
    /**
     * Returns class of bean.
     * 
     * @return class of bean that it represents
     */
    public abstract Class<?> getBeanClass();
    
    /**
     * Returns bean stereotypes.
     * 
     * @return bean stereotypes
     */
    public abstract Set<Annotation> getStereotypes();

}