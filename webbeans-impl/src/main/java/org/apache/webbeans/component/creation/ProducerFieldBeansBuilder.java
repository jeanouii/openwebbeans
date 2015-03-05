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
package org.apache.webbeans.component.creation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.inject.Named;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.spi.api.ResourceReference;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;


/**
 * @param <T> bean class type
 */
public class ProducerFieldBeansBuilder<T>
{    
    
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;

    /**
     * Creates a new instance.
     * 
     */
    public ProducerFieldBeansBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(webBeansContext, "webBeansContext may not be null");
        Asserts.assertNotNull(annotatedType, "annotated type may not be null");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ProducerFieldBean<?>> defineProducerFields(InjectionTargetBean<T> bean)
    {
        Set<ProducerFieldBean<?>> producerBeans = new HashSet<ProducerFieldBean<?>>();
        Set<AnnotatedField<? super T>> annotatedFields = annotatedType.getFields();        
        for(AnnotatedField<? super T> annotatedField: annotatedFields)
        {
            if(annotatedField.isAnnotationPresent(Produces.class) && annotatedField.getJavaMember().getDeclaringClass().equals(annotatedType.getJavaClass()))
            {
                Annotation[] anns = AnnotationUtil.asArray(annotatedField.getAnnotations());
                Field field = annotatedField.getJavaMember();
                
                //Producer field for resource
                Annotation resourceAnnotation = AnnotationUtil.hasOwbInjectableResource(anns);                
                //Producer field for resource
                if(resourceAnnotation != null)
                {                    
                    //Check for valid resource annotation
                    //WebBeansUtil.checkForValidResources(annotatedField.getDeclaringType().getJavaClass(), field.getType(), field.getName(), anns);
                    ResourceReference<T, Annotation> resourceRef = new ResourceReference<T, Annotation>(annotatedType.getJavaClass(), field.getName(),
                                                                                                        (Class<T>)field.getType(), resourceAnnotation);

                    //Can not define EL name
                    if(annotatedField.isAnnotationPresent(Named.class))
                    {
                        throw new WebBeansConfigurationException("Resource producer annotated field : " + annotatedField + " can not define EL name");
                    }

                    final BeanAttributes<T> beanAttributes = fireProcessBeanAttributes(annotatedField);
                    if (beanAttributes != null)
                    {
                        ResourceBeanBuilder<T, Annotation> resourceBeanCreator
                                = new ResourceBeanBuilder<T, Annotation>(bean, resourceRef, annotatedField, beanAttributes);
                        ResourceBean<T, Annotation> resourceBean = resourceBeanCreator.getBean();
                        resourceBean.setProducerField(field);
                        producerBeans.add(resourceBean);
                        webBeansContext.getWebBeansUtil().setBeanEnableFlagForProducerBean(bean, resourceBean, anns);
                    }
                }
                else
                {
                    BeanAttributes<T> beanAttributes = fireProcessBeanAttributes(annotatedField);

                    ProducerFieldBeanBuilder<T, ProducerFieldBean<T>> producerFieldBeanCreator
                        = new ProducerFieldBeanBuilder<T, ProducerFieldBean<T>>(bean, annotatedField, beanAttributes);
                    ProducerFieldBean<T> producerFieldBean = producerFieldBeanCreator.getBean();

                    //X TODO validateProxyable returns the exception, throw the returned exception??
                    webBeansContext.getDeploymentValidationService().validateProxyable(producerFieldBean);
                    producerFieldBeanCreator.validate();

                    producerFieldBean.setProducerField(field);

                    webBeansContext.getWebBeansUtil().setBeanEnableFlagForProducerBean(bean, producerFieldBean, anns);
                    WebBeansUtil.checkProducerGenericType(producerFieldBean, annotatedField.getJavaMember());

                    producerBeans.add(producerFieldBean);
                }
            }
        }

        return producerBeans;
    }


    private BeanAttributes<T> fireProcessBeanAttributes(AnnotatedField<? super T> annotatedField)
    {
        return webBeansContext.getWebBeansUtil().fireProcessBeanAttributes(
                annotatedField, annotatedField.getJavaMember().getType(),
                BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes((AnnotatedField<T>)annotatedField).build());
    }
}
