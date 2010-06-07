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
package org.apache.webbeans.conversation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.Conversation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.util.Asserts;

/**
 * Manager for the conversations.
 * Each conversation is related with conversation id and session id.
 * 
 * @version $Rev$ $Date$
 *
 */
public class ConversationManager
{
    /**Current conversations*/
    private ConcurrentHashMap<Conversation, ConversationContext> conversations = new ConcurrentHashMap<Conversation, ConversationContext>();

    /**
     * Creates new conversation manager
     */
    public ConversationManager()
    {
        
    }

    /**
     * Gets conversation manager instance.
     * @return conversation manager
     */
    public static ConversationManager getInstance()
    {
        ConversationManager manager = (ConversationManager) WebBeansFinder.getSingletonInstance(ConversationManager.class.getName());
        
        return manager;
    }

    /**
     * Adds new conversation context.
     * @param conversation new conversation
     * @param context new context
     */
    public void addConversationContext(Conversation conversation, ConversationContext context)
    {
        conversations.put(conversation, context);
    }
    
    /**
     * Check conversation id exists.
     * @param conversationId conversation id
     * @return true if this conversation exist
     */
    public boolean isConversationExistWithGivenId(String conversationId)
    {
        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            if (conv.getId().equals(conversationId))
            {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Remove given conversation.
     * @param conversation conversation instance
     * @return context
     */
    public ConversationContext removeConversation(Conversation conversation)
    {
        Asserts.assertNotNull(conversation, "conversation can not be null");

        return conversations.remove(conversation);
    }

    /**
     * Gets conversation's context instance.
     * @param conversation conversation instance
     * @return conversation related context
     */
    public ConversationContext getConversationContext(Conversation conversation)
    {
        Asserts.assertNotNull(conversation, "conversation can not be null");

        return conversations.get(conversation);
    }

    /**
     * Gets conversation with id and session id.
     * @param conversationId conversation id
     * @param sessionId session id
     * @return conversation
     */
    public Conversation getPropogatedConversation(String conversationId, String sessionId)
    {
        Asserts.assertNotNull(conversationId, "conversationId parameter can not be null");
        Asserts.assertNotNull(sessionId,"sessionId parameter can not be null");

        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            if (conv.getId().equals(conversationId) && conv.getSessionId().equals(sessionId))
            {
                return conv;
            }
        }

        return null;
    }

    /**
     * Destroy conversations with given session id.
     * @param sessionId session id
     */
    public void destroyConversationContextWithSessionId(String sessionId)
    {
        Asserts.assertNotNull(sessionId, "sessionId parameter can not be null");

        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            if (conv.getSessionId().equals(sessionId))
            {
                ConversationContext ctx = getConversationContext(conv);
                if (ctx != null) 
                {
                    ctx.destroy();
                }
                it.remove();
            }
        }
    }

    /**
     * Gets conversation instance from conversation bean.
     * @return conversation instance
     */
    @SuppressWarnings("unchecked")
    public Conversation getConversationBeanReference()
    {
    	BeanManager beanManager = BeanManagerImpl.getManager();
        Bean<Conversation> bean = (Bean<Conversation>)beanManager.getBeans(Conversation.class, new DefaultLiteral()).iterator().next();
        Conversation conversation =(Conversation) beanManager.getReference(bean, Conversation.class, beanManager.createCreationalContext(bean));

        return conversation;
    }

    /**
     * Destroy unactive conversations.
     */
    public void destroyWithRespectToTimout()
    {
        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            long timeout = conv.getTimeout();

            if (timeout != 0L)
            {
                if ((System.currentTimeMillis() - conv.getActiveTime()) > timeout)
                {
                    ConversationContext ctx = getConversationContext(conv);
                    if (ctx != null) 
                    {
                        ctx.destroy();
                    }

                    it.remove();
                }
            }
        }
    }

    /**
     * Destroys all conversations
     */
    public void destroyAllConversations()
    {
        Collection<ConversationContext> collection = this.conversations.values();
        if(collection != null && collection.size() > 0)
        {
            for (ConversationContext context : collection) 
            {
                context.destroy();
            }            
        }
        
        //Clear conversations
        conversations.clear();
    }
}
