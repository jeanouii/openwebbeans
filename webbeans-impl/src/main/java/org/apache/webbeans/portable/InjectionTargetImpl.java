/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.portable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethod;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ExceptionUtil;


public class InjectionTargetImpl<T> extends AbstractProducer<T> implements InjectionTarget<T>
{

    private AnnotatedType<T> type;
    private AnnotatedConstructor<T> constructor;
    private WebBeansContext context;

    /**
     * If the InjectionTarget has a &#064;PostConstruct method, <code>null</code> if not.
     * This method only gets used if the produced instance is not intercepted.
     * This method must have the signature <code>void METHOD();</code>
     */
    private Method postConstructMethod;

    /**
     * If the InjectionTarget has a &#064;PreDestroy method, <code>null</code> if not.
     * This method only gets used if the produced instance is not intercepted.
     * This method must have the signature <code>void METHOD();</code>
     */
    private Method preDestroyMethod;

    public InjectionTargetImpl(AnnotatedType<T> annotatedType, Set<InjectionPoint> points, WebBeansContext webBeansContext,
                               Method postConstructMethod, Method preDestroyMethod)
    {
        super(points);
        Asserts.assertNotNull(annotatedType);
        Asserts.assertNotNull(webBeansContext);
        type = annotatedType;
        context = webBeansContext;
        this.postConstructMethod = postConstructMethod;
        this.preDestroyMethod = preDestroyMethod;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        return new InjectableConstructor<T>(getConstructor().getJavaMember(), this, (CreationalContextImpl<T>) creationalContext).doInjection();
    }

    @Override
    public void dispose(T instance)
    {
    }

    @Override
    public void inject(T instance, CreationalContext<T> context)
    {
        inject(instance.getClass(), instance, (CreationalContextImpl<T>) context);
    }

    private void inject(Class<?> type, T instance, CreationalContextImpl<T> context)
    {
        if (type == null || type.equals(Object.class))
        {
            return;
        }
        inject(type.getSuperclass(), instance, context);
        injectFields(type, instance, context);
        injectMethods(type, instance, context);
        injectInitializerMethods(type, instance, context);
    }

    private void injectFields(Class<?> type, T instance, CreationalContextImpl<T> context)
    {
        for (InjectionPoint injectionPoint : getInjectionPoints())
        {
            if (injectionPoint.getMember().getDeclaringClass().equals(type))
            {
                if (injectionPoint.getMember() instanceof Field)
                {
                    new InjectableField<T>((Field) injectionPoint.getMember(), instance, this, context).doInjection();
                }
            }
        }
    }

    private void injectMethods(Class<?> type, T instance, CreationalContextImpl<T> context)
    {
        Set<Member> injectedMethods = new HashSet<Member>();
        for (InjectionPoint injectionPoint : getInjectionPoints())
        {
            if (injectionPoint.getMember().getDeclaringClass().equals(type))
            {
                if (injectionPoint.getMember() instanceof Method
                        && !injectedMethods.contains(injectionPoint.getMember())
                        && !isProducerMethod(injectionPoint)
                        && !isDisposalMethod(injectionPoint)
                        && !isObserverMethod(injectionPoint))
                {
                    new InjectableMethod<T>((Method) injectionPoint.getMember(), instance, this, context).doInjection();
                    injectedMethods.add(injectionPoint.getMember());
                }
            }
        }
    }

    /**
     * Performs injection on initializer methods, which are methods that are annotated with &#64;Inject,
     * but have no parameter and thus no injection point.
     */
    private void injectInitializerMethods(Class<?> declaringType, T instance, CreationalContextImpl<T> context)
    {
        for (AnnotatedMethod<? super T> method : type.getMethods())
        {
            if (method.getDeclaringType().getJavaClass().equals(declaringType) && method.isAnnotationPresent(Inject.class) && method.getParameters().isEmpty())
            {
                new InjectableMethod<T>(method.getJavaMember(), instance, this, context).doInjection();
            }
        }
    }

    @Override
    public void postConstruct(T instance)
    {
        if (postConstructMethod != null)
        {
            try
            {
                if (!postConstructMethod.isAccessible())
                {
                    postConstructMethod.setAccessible(true);
                }
                postConstructMethod.invoke(instance);
            }
            catch (Exception e)
            {
                ExceptionUtil.throwAsRuntimeException(e);
            }
        }
        else
        {
            //X TODO check if there is an interceptor
        }
    }

    @Override
    public void preDestroy(T instance)
    {
        if (preDestroyMethod != null)
        {
            try
            {
                if (!preDestroyMethod.isAccessible())
                {
                    preDestroyMethod.setAccessible(true);
                }
                preDestroyMethod.invoke(instance);
            }
            catch (Exception e)
            {
                ExceptionUtil.throwAsRuntimeException(e);
            }
        }
        else
        {
            //X TODO check if there is an interceptor
        }
    }

    private AnnotatedConstructor<T> getConstructor()
    {
        if (constructor != null)
        {
            return constructor;
        }
        AnnotatedConstructor<T> constructor = null;
        for (InjectionPoint injectionPoint : getInjectionPoints())
        {
            if (injectionPoint.getAnnotated() instanceof AnnotatedConstructor)
            {
                if (constructor == null)
                {
                    constructor = (AnnotatedConstructor<T>) injectionPoint.getAnnotated();
                }
                else if (!constructor.equals(injectionPoint.getAnnotated()))
                {
                    throw new IllegalArgumentException("More than one constructor found for injection: "
                                                       + constructor.getJavaMember() + " and " + ((AnnotatedMember)injectionPoint.getAnnotated()).getJavaMember());
                }
            }
        }
        this.constructor = new AnnotatedConstructorImpl<T>(context, getDefaultConstructor(), type);
        return this.constructor;
    }

    private Constructor<T> getDefaultConstructor()
    {
        return context.getWebBeansUtil().getNoArgConstructor(type.getJavaClass());
    }
    
    private boolean isProducerMethod(InjectionPoint injectionPoint)
    {
        return ((AnnotatedElement)injectionPoint.getMember()).isAnnotationPresent(Produces.class);
    }

    private boolean isObserverMethod(InjectionPoint injectionPoint)
    {
        if (!(injectionPoint.getMember() instanceof Method))
        {
            return false;
        }
        Method method = (Method) injectionPoint.getMember();
        for (Annotation[] annotations : method.getParameterAnnotations())
        {
            for (Annotation annotation : annotations)
            {
                if (annotation.annotationType().equals(Observes.class))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDisposalMethod(InjectionPoint injectionPoint)
    {
        if (!(injectionPoint.getMember() instanceof Method))
        {
            return false;
        }
        Method method = (Method) injectionPoint.getMember();
        for (Annotation[] annotations : method.getParameterAnnotations())
        {
            for (Annotation annotation : annotations)
            {
                if (annotation.annotationType().equals(Disposes.class))
                {
                    return true;
                }
            }
        }
        return false;
    }
}