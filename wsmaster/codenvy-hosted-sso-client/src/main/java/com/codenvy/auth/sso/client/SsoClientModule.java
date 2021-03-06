/*
 * Copyright (c) [2012] - [2017] Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.codenvy.auth.sso.client;

import com.codenvy.auth.sso.client.token.ChainedTokenExtractor;
import com.codenvy.auth.sso.client.token.RequestTokenExtractor;
import com.google.inject.AbstractModule;
import org.eclipse.che.inject.DynaModule;

/**
 * Initialize all components necessary for sso client work inside of guice container.
 *
 * @author Sergii Kabashniuk
 */
@DynaModule
public class SsoClientModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(RequestTokenExtractor.class).to(ChainedTokenExtractor.class);
    bind(SessionStore.class);
  }
}
