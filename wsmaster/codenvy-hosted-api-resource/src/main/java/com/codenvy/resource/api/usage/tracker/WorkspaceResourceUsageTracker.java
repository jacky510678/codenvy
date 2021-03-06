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
package com.codenvy.resource.api.usage.tracker;

import com.codenvy.resource.api.ResourceUsageTracker;
import com.codenvy.resource.api.type.WorkspaceResourceType;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ResourceImpl;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;

/**
 * Tracks usage of {@link WorkspaceResourceType} resource.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspaceResourceUsageTracker implements ResourceUsageTracker {
  private final Provider<WorkspaceManager> workspaceManagerProvider;
  private final AccountManager accountManager;

  @Inject
  public WorkspaceResourceUsageTracker(
      Provider<WorkspaceManager> workspaceManagerProvider, AccountManager accountManager) {
    this.workspaceManagerProvider = workspaceManagerProvider;
    this.accountManager = accountManager;
  }

  @Override
  public Optional<Resource> getUsedResource(String accountId)
      throws NotFoundException, ServerException {
    final Account account = accountManager.getById(accountId);
    final List<WorkspaceImpl> accountWorkspaces =
        workspaceManagerProvider.get().getByNamespace(account.getName(), false);
    if (!accountWorkspaces.isEmpty()) {
      return Optional.of(
          new ResourceImpl(
              WorkspaceResourceType.ID, accountWorkspaces.size(), WorkspaceResourceType.UNIT));
    } else {
      return Optional.empty();
    }
  }
}
